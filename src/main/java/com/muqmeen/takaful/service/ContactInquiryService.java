package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.ContactInquiry;
import com.muqmeen.takaful.repository.ContactInquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ContactInquiryService {

    private final ContactInquiryRepository contactInquiryRepository;

    public ContactInquiryService(ContactInquiryRepository contactInquiryRepository) {
        this.contactInquiryRepository = contactInquiryRepository;
    }

    public ContactInquiry create(ContactInput input) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setFullName(input.fullName());
        inquiry.setEmail(input.email());
        inquiry.setPhoneNumber(input.phoneNumber());
        inquiry.setSubject(input.subject());
        inquiry.setMessage(input.message());
        inquiry.setStatus("NEW");
        return contactInquiryRepository.save(inquiry);
    }

    public ContactInquiry markDelivered(Long id, String summary) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact inquiry not found"));
        inquiry.setDeliverySummary(summary);
        return contactInquiryRepository.save(inquiry);
    }

    public ContactInquiry markFailed(Long id, String summary) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact inquiry not found"));
        inquiry.setStatus("DELIVERY_FAILED");
        inquiry.setDeliverySummary(summary);
        return contactInquiryRepository.save(inquiry);
    }

    public ContactInquiry markResolved(Long id) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact inquiry not found"));
        inquiry.setStatus("RESOLVED");
        inquiry.setResolvedAt(LocalDateTime.now());
        return contactInquiryRepository.save(inquiry);
    }

    public void delete(Long id) {
        contactInquiryRepository.findById(id).ifPresent(contactInquiryRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ContactInquiry> recent() {
        // Only open enquiries — resolving one removes it from the dashboard list.
        return contactInquiryRepository.findTop10ByStatusNotOrderByCreatedAtDesc("RESOLVED");
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return contactInquiryRepository.countByStatus("NEW");
    }

    public record ContactInput(String fullName, String email, String phoneNumber, String subject, String message) {
    }
}
