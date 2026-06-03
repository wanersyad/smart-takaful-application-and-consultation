package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer register(String fullName, String email, String phoneNumber, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateCustomerException("An account with this email already exists.");
        }

        Customer customer = new Customer();
        customer.setFullName(fullName == null ? null : fullName.trim());
        customer.setEmail(normalizedEmail);
        customer.setPhoneNumber(normalizePhoneNumber(phoneNumber));
        customer.setPasswordHash(passwordEncoder.encode(password));
        return customerRepository.save(customer);
    }

    public Optional<Customer> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return customerRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    public Optional<Customer> currentCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return findByEmail(authentication.getName());
    }

    public Customer updatePassword(Customer customer, String password) {
        customer.setPasswordHash(passwordEncoder.encode(password));
        return customerRepository.save(customer);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9]", "");
    }

    public static class DuplicateCustomerException extends RuntimeException {
        public DuplicateCustomerException(String message) {
            super(message);
        }
    }
}
