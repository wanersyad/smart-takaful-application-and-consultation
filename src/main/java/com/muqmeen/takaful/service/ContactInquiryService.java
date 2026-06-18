package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.ConsultationStatus;
import com.muqmeen.takaful.domain.ContactInquiry;
import com.muqmeen.takaful.repository.ContactInquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        inquiry.setTopic(input.topic());
        inquiry.setPreferredContact(input.preferredContact());
        inquiry.setStatus(ConsultationStatus.NEW.name());
        return contactInquiryRepository.save(inquiry);
    }

    @Transactional(readOnly = true)
    public Optional<ContactInquiry> findById(Long id) {
        return contactInquiryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ContactInquiry> listAll() {
        return contactInquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Move a consultation through its funnel (NEW → CONTACTED → SCHEDULED → COMPLETED / CLOSED)
     * and stamp the matching timestamp so the agent can see when each step happened.
     */
    public ContactInquiry updateStatus(Long id, ConsultationStatus status) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found"));
        inquiry.setStatus(status.name());
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case CONTACTED -> inquiry.setContactedAt(now);
            case SCHEDULED -> inquiry.setScheduledAt(now);
            case COMPLETED -> { inquiry.setCompletedAt(now); inquiry.setResolvedAt(now); }
            case CLOSED -> inquiry.setResolvedAt(now);
            case NEW -> { /* reopened; leave timestamps as-is */ }
        }
        return contactInquiryRepository.save(inquiry);
    }

    public ContactInquiry updateNotes(Long id, String agentNotes) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found"));
        inquiry.setAgentNotes(agentNotes);
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
        // The consultation stays a real lead (status untouched) even if the notification email
        // could not be sent — we just record why, so the agent still follows up.
        inquiry.setDeliverySummary("Email delivery failed: " + summary);
        return contactInquiryRepository.save(inquiry);
    }

    public void delete(Long id) {
        contactInquiryRepository.findById(id).ifPresent(contactInquiryRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ContactInquiry> recent() {
        // Dashboard widget shows active leads — completed/closed ones drop off the list.
        return contactInquiryRepository.findTop10ByStatusNotInOrderByCreatedAtDesc(
                List.of(ConsultationStatus.COMPLETED.name(), ConsultationStatus.CLOSED.name()));
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return contactInquiryRepository.countByStatus(ConsultationStatus.NEW.name());
    }

    public record ContactInput(String fullName, String email, String phoneNumber, String subject,
                               String message, String topic, String preferredContact) {
    }
}
