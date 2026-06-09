package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
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

    public List<Customer> listForAdmin(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return customerRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(customer -> normalizedQuery.isBlank()
                        || customer.getFullName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || customer.getEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || customer.getPhoneNumber().contains(normalizedQuery))
                .toList();
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Customer setActive(Customer customer, boolean active) {
        customer.setActive(active);
        return customerRepository.save(customer);
    }

    public Customer updateAccountDetails(Customer customer, String fullName, String email, String phoneNumber) {
        String normalizedEmail = normalizeEmail(email);
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, customer.getId())) {
            throw new DuplicateCustomerException("An account with this email already exists.");
        }

        customer.setFullName(fullName.trim());
        customer.setEmail(normalizedEmail);
        customer.setPhoneNumber(normalizePhoneNumber(phoneNumber));
        return customerRepository.save(customer);
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
