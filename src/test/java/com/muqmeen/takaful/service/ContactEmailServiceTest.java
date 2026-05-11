package com.muqmeen.takaful.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class ContactEmailServiceTest {

    @Test
    void fallsBackToFormSubmitWhenSmtpIsNotConfigured() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ObjectProvider<JavaMailSender> mailProvider = mock();
        when(mailProvider.getIfAvailable()).thenReturn(null);

        ContactEmailService service = new ContactEmailService(
                mailProvider,
                "s72370@ocean.umt.edu.my",
                "no-reply@muqmeengroup.local",
                true,
                "https://formsubmit.co/ajax",
                restClientBuilder
        );

        server.expect(once(), requestTo("https://formsubmit.co/ajax/s72370@ocean.umt.edu.my"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"email\":\"aminah@example.com\"")))
                .andExpect(content().string(containsString("\"_replyto\":\"aminah@example.com\"")))
                .andExpect(content().string(containsString("\"message\":\"Please explain AnugerahMax.\"")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        service.send(new ContactEmailService.ContactMessage(
                "Aminah",
                "aminah@example.com",
                "60123456789",
                "PruBSN AnugerahMax",
                "Please explain AnugerahMax."
        ));

        server.verify();
    }
}
