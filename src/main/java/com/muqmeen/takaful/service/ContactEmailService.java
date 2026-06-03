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
    private final String delivery;
    private final String mailHost;
    private final String resendApiKey;
    private final String resendBaseUrl;
    private final boolean formSubmitEnabled;
    private final String formSubmitBaseUrl;

    public ContactEmailService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${contact.recipient:s72370@ocean.umt.edu.my}") String recipient,
                               @Value("${contact.from:${spring.mail.username:no-reply@muqmeengroup.local}}") String from,
                               @Value("${contact.delivery:auto}") String delivery,
                               @Value("${spring.mail.host:}") String mailHost,
                               @Value("${contact.resend.api-key:}") String resendApiKey,
                               @Value("${contact.resend.base-url:https://api.resend.com}") String resendBaseUrl,
                               @Value("${contact.formsubmit.enabled:true}") boolean formSubmitEnabled,
                               @Value("${contact.formsubmit.base-url:https://formsubmit.co/ajax}") String formSubmitBaseUrl,
                               RestClient.Builder restClientBuilder) {
        this.mailSender = mailSender.getIfAvailable();
        this.restClient = restClientBuilder.build();
        this.recipient = recipient;
        this.from = from;
        this.delivery = normalizeDelivery(delivery);
        this.mailHost = mailHost;
        this.resendApiKey = resendApiKey;
        this.resendBaseUrl = resendBaseUrl;
        this.formSubmitEnabled = formSubmitEnabled;
        this.formSubmitBaseUrl = formSubmitBaseUrl;
    }

    public void send(ContactMessage message) {
        switch (delivery) {
            case "smtp" -> sendViaSmtp(message);
            case "resend" -> sendViaResend(message);
            case "formsubmit" -> sendViaFormSubmit(message);
            default -> sendAuto(message);
        }
    }

    private void sendAuto(ContactMessage message) {
        if (smtpConfigured()) {
            try {
                sendViaSmtp(message);
                return;
            } catch (ContactEmailException ex) {
                if (!resendConfigured() && !formSubmitEnabled) {
                    throw ex;
                }
            }
        }

        if (resendConfigured()) {
            try {
                sendViaResend(message);
                return;
            } catch (ContactEmailException ex) {
                if (!formSubmitEnabled) {
                    throw ex;
                }
            }
        }

        if (formSubmitEnabled) {
            sendViaFormSubmit(message);
            return;
        }

        throw new ContactEmailException("Email service is not configured.");
    }

    private void sendViaSmtp(ContactMessage message) {
        if (!smtpConfigured()) {
            throw new ContactEmailException("SMTP email is not configured.");
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setFrom(from);
        email.setReplyTo(message.email());
        email.setSubject("Muqmeen contact request: " + message.subject());
        email.setText(emailBody(message));

        try {
            mailSender.send(email);
        } catch (MailException ex) {
            throw new ContactEmailException("Unable to send contact email.", ex);
        }
    }

    private void sendViaResend(ContactMessage message) {
        if (!resendConfigured()) {
            throw new ContactEmailException("Resend email API is not configured.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from);
        payload.put("to", new String[] { recipient });
        payload.put("reply_to", message.email());
        payload.put("subject", "Muqmeen contact request: " + message.subject());
        payload.put("text", emailBody(message));

        try {
            restClient.post()
                    .uri(resendEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ContactEmailException("Unable to send contact email through Resend.", ex);
        }
    }

    private void sendViaFormSubmit(ContactMessage message) {
        if (!formSubmitEnabled) {
            throw new ContactEmailException("FormSubmit email fallback is disabled.");
        }

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

    private URI resendEndpoint() {
        return UriComponentsBuilder.fromUriString(resendBaseUrl)
                .pathSegment("emails")
                .build()
                .toUri();
    }

    private String emailBody(ContactMessage message) {
        return """
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
        );
    }

    private boolean smtpConfigured() {
        return mailSender != null && mailHost != null && !mailHost.isBlank();
    }

    private boolean resendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank()
                && resendBaseUrl != null && !resendBaseUrl.isBlank();
    }

    private String normalizeDelivery(String value) {
        if (value == null || value.isBlank()) {
            return "auto";
        }
        return value.trim().toLowerCase();
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
