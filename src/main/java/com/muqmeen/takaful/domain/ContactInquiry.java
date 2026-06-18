package com.muqmeen.takaful.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_inquiries")
public class ContactInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String email;

    @Size(max = 40)
    @Column(length = 40)
    private String phoneNumber;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String subject;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String message;

    // What the person wants to consult about (e.g. Family protection, Medical, Hibah). Optional.
    @Size(max = 80)
    @Column(length = 80)
    private String topic;

    // How they prefer to be reached back (WhatsApp / Call / Email). Optional.
    @Size(max = 30)
    @Column(length = 30)
    private String preferredContact;

    // Funnel status: NEW, CONTACTED, SCHEDULED, COMPLETED, CLOSED. See ConsultationStatus.
    @Column(nullable = false, length = 30)
    private String status = "NEW";

    // Internal notes the agent keeps while working the lead (not shown to the customer).
    @Size(max = 2000)
    @Column(length = 2000)
    private String agentNotes;

    @Column(length = 2000)
    private String deliverySummary;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime contactedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getPreferredContact() { return preferredContact; }
    public void setPreferredContact(String preferredContact) { this.preferredContact = preferredContact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAgentNotes() { return agentNotes; }
    public void setAgentNotes(String agentNotes) { this.agentNotes = agentNotes; }
    public String getDeliverySummary() { return deliverySummary; }
    public void setDeliverySummary(String deliverySummary) { this.deliverySummary = deliverySummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getContactedAt() { return contactedAt; }
    public void setContactedAt(LocalDateTime contactedAt) { this.contactedAt = contactedAt; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
