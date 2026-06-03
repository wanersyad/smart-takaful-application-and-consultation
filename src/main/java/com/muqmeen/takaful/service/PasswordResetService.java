package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.PasswordResetToken;
import com.muqmeen.takaful.repository.CustomerRepository;
import com.muqmeen.takaful.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetEmailService emailService;
    private final CustomerService customerService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String appBaseUrl;

    public PasswordResetService(CustomerRepository customerRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordResetEmailService emailService,
                                CustomerService customerService,
                                @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.customerRepository = customerRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.customerService = customerService;
        this.appBaseUrl = appBaseUrl;
    }

    @Transactional
    public void requestReset(String email) {
        Optional<Customer> customer = customerRepository.findByEmailIgnoreCase(normalizeEmail(email));
        if (customer.isEmpty()) {
            return;
        }

        tokenRepository.findByCustomerAndUsedAtIsNull(customer.get())
                .forEach(token -> token.setUsedAt(LocalDateTime.now()));

        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setCustomer(customer.get());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        tokenRepository.save(token);

        emailService.sendResetLink(customer.get(), resetUrl(rawToken));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(rawToken))
                .orElseThrow(() -> new InvalidResetTokenException("This reset link is invalid or has already been used."));

        if (token.isExpired(LocalDateTime.now())) {
            token.setUsedAt(LocalDateTime.now());
            throw new InvalidResetTokenException("This reset link has expired. Please request a new one.");
        }

        customerService.updatePassword(token.getCustomer(), newPassword);
        token.setUsedAt(LocalDateTime.now());
    }

    private String resetUrl(String token) {
        return UriComponentsBuilder.fromUriString(appBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static class InvalidResetTokenException extends RuntimeException {
        public InvalidResetTokenException(String message) {
            super(message);
        }
    }
}
