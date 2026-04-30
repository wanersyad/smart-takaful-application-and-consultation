package com.muqmeen.takaful;

import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.repository.LeadRepository;
import com.muqmeen.takaful.service.chat.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeadRepository leadRepository;

    @MockitoBean
    private ChatService chatService;

    @Test
    void publicPagesRemainAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/success"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/payment/mock/MGM-TEST"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/payment/callback")
                        .param("billcode", "MGM-MISSING")
                        .param("status_id", "3"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void publicPostRoutesRequireCsrfButNotLogin() throws Exception {
        mockMvc.perform(post("/submit-lead")
                        .param("fullName", "Aminah")
                        .param("phoneNumber", "60123456789")
                        .param("productType", "Hibah Al-Wasiyyah")
                        .param("consultationMode", "WhatsApp")
                        .param("tipAmount", "0"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/submit-lead")
                        .with(csrf())
                        .param("fullName", "Aminah")
                        .param("phoneNumber", "60123456789")
                        .param("productType", "Hibah Al-Wasiyyah")
                        .param("consultationMode", "WhatsApp")
                        .param("tipAmount", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/success?leadId=*"));
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
    void adminPagesRedirectAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedAdminCanAccessAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminMutationsRejectMissingCsrfAndAcceptValidCsrf() throws Exception {
        Lead lead = new Lead();
        lead.setFullName("Siti");
        lead.setPhoneNumber("60198765432");
        lead.setProductType("PruBSN Medical");
        lead.setConsultationMode("WhatsApp");
        lead.setTipAmount(BigDecimal.ZERO);
        lead = leadRepository.save(lead);

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
}
