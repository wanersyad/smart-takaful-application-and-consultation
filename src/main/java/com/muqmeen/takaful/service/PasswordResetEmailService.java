package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Customer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String from;

    public PasswordResetEmailService(ObjectProvider<JavaMailSender> mailSender,
                                     @Value("${spring.mail.host:}") String mailHost,
                                     @Value("${contact.from:${spring.mail.username:no-reply@muqmeengroup.local}}") String from) {
        this.mailSender = mailSender.getIfAvailable();
        this.mailHost = mailHost;
        this.from = from;
    }

    public void sendResetLink(Customer customer, String resetUrl) {
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            throw new PasswordResetEmailException("Password reset email is not configured.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customer.getEmail());
        message.setFrom(from);
        message.setSubject("Reset your Muqmeen Group password");
        message.setText("""
                Hi %s,

                Use this link to reset your Muqmeen Group password:
                %s

                This link expires in 30 minutes. If you did not request this, you can ignore this email.
                """.formatted(customer.getFullName(), resetUrl));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new PasswordResetEmailException("Unable to send password reset email.", ex);
        }
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
