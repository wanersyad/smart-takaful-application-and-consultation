package com.muqmeen.takaful.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ContactEmailService {

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final String recipient;
    private final String from;
    private final boolean formSubmitEnabled;
    private final String formSubmitBaseUrl;

    public ContactEmailService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${contact.recipient:s72370@ocean.umt.edu.my}") String recipient,
                               @Value("${contact.from:${spring.mail.username:no-reply@muqmeengroup.local}}") String from,
                               @Value("${contact.formsubmit.enabled:true}") boolean formSubmitEnabled,
                               @Value("${contact.formsubmit.base-url:https://formsubmit.co/ajax}") String formSubmitBaseUrl,
                               RestClient.Builder restClientBuilder) {
        this.mailSender = mailSender.getIfAvailable();
        this.restClient = restClientBuilder.build();
        this.recipient = recipient;
        this.from = from;
        this.formSubmitEnabled = formSubmitEnabled;
        this.formSubmitBaseUrl = formSubmitBaseUrl;
    }

    public void send(ContactMessage message) {
        if (mailSender != null) {
            try {
                sendViaSmtp(message);
                return;
            } catch (ContactEmailException ex) {
                if (!formSubmitEnabled) {
                    throw ex;
                }
            }
        }

        if (!formSubmitEnabled) {
            throw new ContactEmailException("Email service is not configured.");
        }

        sendViaFormSubmit(message);
    }

    private void sendViaSmtp(ContactMessage message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setFrom(from);
        email.setReplyTo(message.email());
        email.setSubject("Muqmeen contact request: " + message.subject());
        email.setText("""
                New Muqmeen Group contact request

                Name: %s
                Email: %s
                Phone: %s
                Interest: %s

                Message:
                %s
                """.formatted(
                message.fullName(),
                message.email(),
                blankToDash(message.phoneNumber()),
                message.subject(),
                message.message()
        ));

        try {
            mailSender.send(email);
        } catch (MailException ex) {
            throw new ContactEmailException("Unable to send contact email.", ex);
        }
    }

    private void sendViaFormSubmit(ContactMessage message) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("name", message.fullName());
        payload.put("email", message.email());
        payload.put("_replyto", message.email());
        payload.put("phone", blankToDash(message.phoneNumber()));
        payload.put("interest", message.subject());
        payload.put("message", message.message());
        payload.put("_subject", "Muqmeen contact request: " + message.subject());
        payload.put("_template", "table");
        payload.put("_captcha", "false");

        try {
            restClient.post()
                    .uri(formSubmitEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ContactEmailException("Unable to send contact email through fallback provider.", ex);
        }
    }

    private URI formSubmitEndpoint() {
        return UriComponentsBuilder.fromUriString(formSubmitBaseUrl)
                .pathSegment(recipient)
                .build()
                .toUri();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public record ContactMessage(String fullName, String email, String phoneNumber, String subject, String message) {
    }

    public static class ContactEmailException extends RuntimeException {
        public ContactEmailException(String message) {
            super(message);
        }

        public ContactEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
