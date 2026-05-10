package com.muqmeen.takaful.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactEmailService {

    private final JavaMailSender mailSender;
    private final String recipient;
    private final String from;

    public ContactEmailService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${contact.recipient:s72370@ocean.umt.edu.my}") String recipient,
                               @Value("${contact.from:${spring.mail.username:no-reply@muqmeengroup.local}}") String from) {
        this.mailSender = mailSender.getIfAvailable();
        this.recipient = recipient;
        this.from = from;
    }

    public void send(ContactMessage message) {
        if (mailSender == null) {
            throw new ContactEmailException("Email service is not configured.");
        }

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
