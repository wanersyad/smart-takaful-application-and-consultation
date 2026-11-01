package com.muqmeen.takaful;

import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.ContactInquiry;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.PlanFinderResponse;
import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.repository.ConsultationApplicationRepository;
import com.muqmeen.takaful.repository.ContactInquiryRepository;
import com.muqmeen.takaful.repository.PaymentRepository;
import com.muqmeen.takaful.repository.PlanFinderResponseRepository;
import com.muqmeen.takaful.repository.ProductRepository;
import com.muqmeen.takaful.service.ApplicationService;
import com.muqmeen.takaful.service.ContactEmailService;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.PlanFinderService;
import com.muqmeen.takaful.service.ProductService;
import com.muqmeen.takaful.service.chat.GeminiClient;
import com.muqmeen.takaful.service.chat.GeminiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "toyyibpay.mode=mock",
        "file-storage.mode=local",
        "file-storage.local-upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlanFinderIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PlanFinderResponseRepository planFinderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ConsultationApplicationRepository applicationRepository;
    @Autowired private ContactInquiryRepository contactInquiryRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CustomerService customerService;
    @Autowired private ApplicationService applicationService;
    @Autowired private PlanFinderService planFinderService;
    @Autowired private ProductService productService;
    @MockitoBean private GeminiClient geminiClient;
    @MockitoBean private ContactEmailService contactEmailService;

    private Product medical;
    private Product education;

    @BeforeEach
    void setUp() {
        planFinderRepository.deleteAll();
        paymentRepository.deleteAll();
        applicationRepository.deleteAll();
        contactInquiryRepository.deleteAll();
        productRepository.deleteAll();
        medical = saveProduct("Medic Shield", "Medical", "Covers hospital admission and medical bills.");
        education = saveProduct("Edu Future", "Education", "Builds a fund for your children's education.");
    }

    @Test
    void aiSuggestionIsValidatedAgainstRealProductsAndNeverSeesHealthAnswer() throws Exception {
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateJson(anyString(), anyString())).thenReturn("""
                {"summary": "Two plans fit your goals.",
                 "recommendations": [
                   {"productId": %d, "fit": "HIGH", "reason": "Helps with your children's education fund."},
                   {"productId": 999999, "fit": "HIGH", "reason": "Invented product that must be dropped."},
                   {"productId": %d, "fit": "MEDIUM", "reason": "Adds hospital cover for the family."}
                 ],
                 "agentChecks": ["Confirm the education fund term matches the children's ages."]}
                """.formatted(education.getId(), medical.getId()));

        mockMvc.perform(submit().param("healthCondition", "YES"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/plan-finder/result/*"));

        PlanFinderResponse saved = only();
        assertEquals("AI", saved.getSource());
        List<PlanFinderService.Recommendation> recs = planFinderService.recommendations(saved);
        assertEquals(List.of(education.getId(), medical.getId()), recs.stream().map(PlanFinderService.Recommendation::productId).toList());
        List<String> checks = planFinderService.agentChecks(saved);
        assertTrue(checks.stream().anyMatch(c -> c.contains("medical condition")), "rule-based health check kept for the agent");
        assertTrue(checks.contains("Confirm the education fund term matches the children's ages."));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateJson(anyString(), prompt.capture());
        assertFalse(prompt.getValue().toLowerCase().contains("medical condition"), "health question must not reach the AI");
        assertTrue(prompt.getValue().contains("productId=" + medical.getId()));

        mockMvc.perform(get("/plan-finder/result/" + saved.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edu Future")))
                .andExpect(content().string(containsString("Helps with your children")))
                .andExpect(content().string(not(containsString("Invented product"))));
    }

    @Test
    void fallsBackToRuleMatchingWhenAiFails() throws Exception {
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateJson(anyString(), anyString())).thenThrow(new GeminiException("503 overloaded"));

        mockMvc.perform(submit("ms", "MEDICAL"))
                .andExpect(status().is3xxRedirection());

        PlanFinderResponse saved = only();
        assertEquals("RULES", saved.getSource());
        assertEquals("ms", saved.getLanguage());
        List<PlanFinderService.Recommendation> recs = planFinderService.recommendations(saved);
        assertEquals(medical.getId(), recs.get(0).productId(), "keyword match on 'medical/hospital' ranks first");
        assertTrue(recs.get(0).reason().startsWith("Sepadan"), "reason written in Malay");
    }

    @Test
    void worksWithoutGeminiKey() throws Exception {
        when(geminiClient.isConfigured()).thenReturn(false);
        mockMvc.perform(submit()).andExpect(status().is3xxRedirection());
        assertEquals("RULES", only().getSource());
        verify(geminiClient, never()).generateJson(anyString(), anyString());
    }

    @Test
    void rejectsMissingGoals() throws Exception {
        mockMvc.perform(post("/plan-finder").with(csrf())
                        .param("lang", "en").param("ageBand", "26-35").param("maritalStatus", "MARRIED")
                        .param("childrenBand", "2").param("existingCoverage", "NONE")
                        .param("budgetBand", "100_200").param("occupationClass", "OFFICE"))
                .andExpect(redirectedUrl("/plan-finder?lang=en"))
                .andExpect(flash().attributeExists("planFinderError"));
        assertEquals(0, planFinderRepository.count());
    }

    @Test
    void resultFollowsTheCustomerIntoApplicationConsultationAndAdminScreens() throws Exception {
        when(geminiClient.isConfigured()).thenReturn(false);
        mockMvc.perform(submit().param("healthCondition", "YES")).andExpect(status().is3xxRedirection());
        PlanFinderResponse response = only();
        Customer customer = customerService.register("Plan User", "plan." + UUID.randomUUID() + "@example.com", "60123456789", "password123");

        // Apply from the result page -> the application carries the questionnaire.
        mockMvc.perform(get("/applications/new")
                        .param("productId", education.getId().toString())
                        .param("planFinder", response.getToken())
                        .with(user(customer.getEmail()).roles("USER")))
                .andExpect(status().is3xxRedirection());
        ConsultationApplication application = applicationRepository.findAll().get(0);
        assertEquals(application.getId(), planFinderRepository.findByToken(response.getToken()).orElseThrow().getApplication().getId());

        mockMvc.perform(get("/admin/applications/" + application.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("needs questionnaire")))
                .andExpect(content().string(containsString("Existing medical condition")))
                .andExpect(content().string(containsString("Confirm before quoting")));
        mockMvc.perform(get("/admin/applications/" + application.getId() + "/quotation").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("needs questionnaire")));

        // Consultation request from the result page -> linked, and the visitor returns to the result.
        mockMvc.perform(post("/contact").with(csrf())
                        .param("fullName", "Plan Visitor")
                        .param("email", "visitor@example.com")
                        .param("topic", "Plan Finder: Edu Future")
                        .param("message", "I'd like to discuss.")
                        .param("planFinderToken", response.getToken()))
                .andExpect(redirectedUrl("/plan-finder/result/" + response.getToken() + "#consult"));
        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        mockMvc.perform(get("/admin/consultations/" + inquiry.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("needs questionnaire")));

        // Deleting the application keeps the questionnaire but clears the link.
        applicationService.deleteApplication(application.getId());
        PlanFinderResponse after = planFinderRepository.findByToken(response.getToken()).orElseThrow();
        assertNull(after.getApplication());
        assertNotNull(after.getInquiry());
    }

    @Test
    void publicPagesAreReachableWithoutLogin() throws Exception {
        mockMvc.perform(get("/plan-finder")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Which Takaful plan fits you?")));
        mockMvc.perform(get("/plan-finder").param("lang", "ms")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Pelan Takaful mana yang sesuai untuk anda?")));
        mockMvc.perform(get("/plan-finder/result/" + UUID.randomUUID()))
                .andExpect(redirectedUrl("/plan-finder"));
        mockMvc.perform(get("/")).andExpect(content().string(containsString("href=\"/plan-finder\"")));
    }

    private MockHttpServletRequestBuilder submit() {
        return submit("en", "EDUCATION", "FAMILY_PROTECTION");
    }

    private MockHttpServletRequestBuilder submit(String lang, String... goals) {
        // Unique client IP per request so the per-IP rate limiter never interferes with tests.
        String ip = "10.0." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
        return post("/plan-finder").with(csrf())
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .param("lang", lang)
                .param("ageBand", "36-45")
                .param("maritalStatus", "MARRIED")
                .param("childrenBand", "2")
                .param("goals", goals)
                .param("existingCoverage", "EMPLOYER")
                .param("budgetBand", "100_200")
                .param("occupationClass", "OFFICE");
    }

    private PlanFinderResponse only() {
        List<PlanFinderResponse> all = planFinderRepository.findAll();
        assertEquals(1, all.size());
        return all.get(0);
    }

    private Product saveProduct(String name, String category, String summary) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryLabel(category);
        product.setSummary(summary);
        product.setDescription(summary);
        product.setIconClass("fa-shield-halved");
        product.setActive(true);
        return productService.save(product); // via the service so the landing-products cache is evicted
    }
}
