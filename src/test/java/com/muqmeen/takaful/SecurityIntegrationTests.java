package com.muqmeen.takaful;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.domain.Payment;
import com.muqmeen.takaful.repository.LeadRepository;
import com.muqmeen.takaful.repository.PaymentRepository;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.PaymentService;
import com.muqmeen.takaful.service.chat.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "toyyibpay.mode=mock",
        "toyyibpay.secret-key=test-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @MockitoBean
    private ChatService chatService;

    @Test
    void anonymousUsersCanBrowsePublicRoutesAndSeeLoginContract() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Consult Agent")))
                .andExpect(content().string(containsString("View Details")))
                .andExpect(content().string(containsString("PruBSN AnugerahMax")))
                .andExpect(content().string(containsString("/login?redirect=")))
                .andExpect(content().string(not(containsString("/admin"))));

        mockMvc.perform(get("/success"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/payment/mock/MGM-TEST"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousSubmitLeadRedirectsToLoginWhenCsrfIsValid() throws Exception {
        mockMvc.perform(post("/submit-lead")
                        .with(csrf())
                        .param("fullName", "Aminah")
                        .param("phoneNumber", "60123456789")
                .param("productType", "Hibah Al-Wasiyyah")
                .param("consultationMode", "WhatsApp")
                .param("tipAmount", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void customerRegisterLoginAndLogoutWorksWithCsrf() throws Exception {
        MvcResult result = mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nur Test")
                        .param("email", "nur.register@example.com")
                        .param("phoneNumber", "60123456780")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/account").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nur.register@example.com")));

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "nur.register@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void authenticatedCustomerCanSubmitLeadAndSeeItInAccount() throws Exception {
        Customer customer = customerService.register(
                "Siti Customer",
                "siti.customer@example.com",
                "60122223333",
                "password123"
        );

        mockMvc.perform(post("/submit-lead")
                        .with(user(customer.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("fullName", customer.getFullName())
                        .param("phoneNumber", customer.getPhoneNumber())
                        .param("productType", "PruBSN Medical")
                        .param("consultationMode", "WhatsApp")
                        .param("tipAmount", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/success?leadId=*"));

        mockMvc.perform(get("/account").with(user(customer.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PruBSN Medical")))
                .andExpect(content().string(containsString("SKIPPED")));
    }

    @Test
    void customerCannotViewAnotherCustomersLeadThroughSuccessPage() throws Exception {
        Customer owner = customerService.register("Owner", "owner@example.com", "60111111111", "password123");
        Customer other = customerService.register("Other", "other@example.com", "60111111112", "password123");
        Lead lead = saveLead(owner, "Private Product", BigDecimal.ZERO);

        mockMvc.perform(get("/success")
                        .param("leadId", lead.getId().toString())
                        .with(user(other.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Private Product"))));
    }

    @Test
    void chatEndpointAllowsAnonymousRequestsWithCsrf() throws Exception {
        when(chatService.reply(eq("What is Takaful?"), anyList()))
                .thenReturn("Takaful is Shariah-compliant protection.");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is Takaful?\",\"history\":[]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"reply\":\"Takaful is Shariah-compliant protection.\"}"));
    }

    @Test
    void adminPagesRedirectAnonymousUsersAndRejectCustomers() throws Exception {
        Customer customer = customerService.register("Admin No", "not.admin@example.com", "60133334444", "password123");

        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/admin").with(user(customer.getEmail()).roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/admin/products").with(user(customer.getEmail()).roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void authenticatedAdminCanAccessAdminPages() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void sharedLoginAcceptsAdminUsername() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "password")
                        .param("redirect", "/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void adminLoginPageDoesNotOfferCustomerRegistration() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin sign in")))
                .andExpect(content().string(containsString("Admin username")))
                .andExpect(content().string(not(containsString("Create an account"))));
    }

    @Test
    void failedAdminLoginReturnsToAdminLoginPage() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "wrong-password")
                        .param("redirect", "/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    void adminLoginCanReplaceExistingCustomerSession() throws Exception {
        MvcResult customerLogin = mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Session Customer")
                        .param("email", "session.customer@example.com")
                        .param("phoneNumber", "60130001111")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) customerLogin.getRequest().getSession(false);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(post("/login")
                        .session(session)
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "password")
                        .param("redirect", "/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void customerLoginPageStaysCustomerEvenAfterAdminRouteAttempt() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome back")))
                .andExpect(content().string(containsString("Email address")))
                .andExpect(content().string(containsString("Create an account")))
                .andExpect(content().string(not(containsString("Admin sign in"))))
                .andExpect(content().string(not(containsString("Admin username"))));
    }

    @Test
    void adminMutationsRejectMissingCsrfAndAcceptValidCsrf() throws Exception {
        Lead lead = saveLead(null, "PruBSN Medical", BigDecimal.ZERO);

        mockMvc.perform(post("/admin/lead/{id}/status", lead.getId())
                        .with(user("admin").roles("ADMIN"))
                        .param("status", "CONTACTED"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/lead/{id}/status", lead.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("status", "CONTACTED"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void toyyibPayMockModeCreatesPaymentAndRedirectsToMockGateway() throws Exception {
        Customer customer = customerService.register("Tip User", "tip.user@example.com", "60199990000", "password123");

        mockMvc.perform(post("/submit-lead")
                        .with(user(customer.getEmail()).roles("USER"))
                        .with(csrf())
                        .param("fullName", customer.getFullName())
                        .param("phoneNumber", customer.getPhoneNumber())
                        .param("productType", "Hibah Al-Wasiyyah")
                        .param("consultationMode", "Voice/Video Call")
                        .param("tipAmount", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/payment/mock/MGM-*"));

        Payment payment = paymentRepository.findAll().stream()
                .filter(row -> customer.getId().equals(row.getCustomer().getId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/payment/callback")
                        .param("billcode", payment.getBillCode())
                        .param("status_id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/success?leadId=*"));
    }

    @Test
    void callbackHashVerificationRejectsInvalidHashAndAcceptsValidHash() throws Exception {
        Customer customer = customerService.register("Hash User", "hash.user@example.com", "60177770000", "password123");
        Lead lead = saveLead(customer, "Hash Product", BigDecimal.TEN);
        Payment payment = new Payment();
        payment.setLead(lead);
        payment.setCustomer(customer);
        payment.setExternalReferenceNo("MGM-HASH-1");
        payment.setBillCode("BILLHASH1");
        payment.setAmountCents(1000);
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        mockMvc.perform(post("/payment/callback")
                        .param("status", "1")
                        .param("order_id", payment.getExternalReferenceNo())
                        .param("refno", "TP123")
                        .param("billcode", payment.getBillCode())
                        .param("hash", "wrong"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/payment/callback")
                        .param("status", "1")
                        .param("order_id", payment.getExternalReferenceNo())
                        .param("refno", "TP123")
                        .param("billcode", payment.getBillCode())
                        .param("hash", paymentService.expectedHash("1", payment.getExternalReferenceNo(), "TP123")))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    private Lead saveLead(Customer customer, String productType, BigDecimal tipAmount) {
        Lead lead = new Lead();
        lead.setCustomer(customer);
        lead.setFullName(customer == null ? "Admin Lead" : customer.getFullName());
        lead.setPhoneNumber(customer == null ? "60198765432" : customer.getPhoneNumber());
        lead.setProductType(productType);
        lead.setConsultationMode("WhatsApp");
        lead.setTipAmount(tipAmount);
        lead.setPaymentStatus(tipAmount != null && tipAmount.compareTo(BigDecimal.ZERO) > 0 ? "PENDING" : "SKIPPED");
        return leadRepository.save(lead);
    }
}
