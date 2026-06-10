package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Customer;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sends the password-reset link to the customer. Mirrors {@link ContactEmailService}: in
 * production the app delivers mail through the Resend HTTP API (no SMTP host is configured),
 * so this service must use Resend too — otherwise reset emails silently never send. Falls back
 * to SMTP when that is the configured transport, and reports a clear error when neither is set.
 */
@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final String from;
    private final String delivery;
    private final String mailHost;
    private final String resendApiKey;
    private final String resendBaseUrl;
    private final Set<String> allowedRecipients;

    public PasswordResetEmailService(ObjectProvider<JavaMailSender> mailSender,
                                     @Value("${contact.from:${spring.mail.username:no-reply@muqmeengroup.local}}") String from,
                                     @Value("${contact.delivery:auto}") String delivery,
                                     @Value("${spring.mail.host:}") String mailHost,
                                     @Value("${contact.resend.api-key:}") String resendApiKey,
                                     @Value("${contact.resend.base-url:https://api.resend.com}") String resendBaseUrl,
                                     @Value("${password-reset.allowed-recipients:}") String allowedRecipients,
                                     RestClient.Builder restClientBuilder) {
        this.mailSender = mailSender.getIfAvailable();
        this.restClient = restClientBuilder.build();
        this.from = from;
        this.delivery = delivery == null || delivery.isBlank() ? "auto" : delivery.trim().toLowerCase();
        this.mailHost = mailHost;
        this.resendApiKey = resendApiKey;
        this.resendBaseUrl = resendBaseUrl;
        this.allowedRecipients = parseAllowed(allowedRecipients);
    }

    private static Set<String> parseAllowed(String csv) {
        Set<String> set = new HashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String trimmed = part.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    set.add(trimmed);
                }
            }
        }
        return set;
    }

    /**
     * Without a verified sending domain, Resend only delivers to the account owner's own email.
     * The allow-list (password-reset.allowed-recipients) reflects that: in test mode it contains
     * just that address, so we skip the doomed send for anyone else and let the caller show the
     * normal "if the account exists, a link was sent" message. Once a domain is verified, set the
     * list to "*" (or clear it) to deliver to every customer.
     */
    public boolean canDeliverTo(String email) {
        if (allowedRecipients.isEmpty() || allowedRecipients.contains("*")) {
            return true;
        }
        return email != null && allowedRecipients.contains(email.trim().toLowerCase());
    }

    public void sendResetLink(Customer customer, String resetUrl) {
        String subject = "Reset your Muqmeen Group password";
        String body = """
                Hi %s,

                Use this link to reset your Muqmeen Group password:
                %s

                This link expires in 30 minutes. If you did not request this, you can ignore this email.
                """.formatted(customer.getFullName(), resetUrl);

        switch (delivery) {
            case "smtp" -> sendViaSmtp(customer.getEmail(), subject, body);
            case "resend" -> sendViaResend(customer.getEmail(), subject, body);
            default -> sendAuto(customer.getEmail(), subject, body);
        }
    }

    private void sendAuto(String to, String subject, String body) {
        if (smtpConfigured()) {
            sendViaSmtp(to, subject, body);
            return;
        }
        if (resendConfigured()) {
            sendViaResend(to, subject, body);
            return;
        }
        throw new PasswordResetEmailException("Password reset email is not configured.");
    }

    private void sendViaSmtp(String to, String subject, String body) {
        if (!smtpConfigured()) {
            throw new PasswordResetEmailException("Password reset email (SMTP) is not configured.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new PasswordResetEmailException("Unable to send password reset email.", ex);
        }
    }

    private void sendViaResend(String to, String subject, String body) {
        if (!resendConfigured()) {
            throw new PasswordResetEmailException("Password reset email (Resend) is not configured.");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from);
        payload.put("to", new String[] { to });
        payload.put("subject", subject);
        payload.put("text", body);
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
            throw new PasswordResetEmailException("Unable to send password reset email through Resend.", ex);
        }
    }

    private URI resendEndpoint() {
        return UriComponentsBuilder.fromUriString(resendBaseUrl)
                .pathSegment("emails")
                .build()
                .toUri();
    }

    private boolean smtpConfigured() {
        return mailSender != null && mailHost != null && !mailHost.isBlank();
    }

    private boolean resendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank()
                && resendBaseUrl != null && !resendBaseUrl.isBlank();
    }

    public static class PasswordResetEmailException extends RuntimeException {
        public PasswordResetEmailException(String message) {
            super(message);
        }

        public PasswordResetEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
