package com.muqmeen.takaful.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", unique = true)
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, unique = true, length = 80)
    private String externalReferenceNo;

    @Column(unique = true, length = 80)
    private String billCode;

    @Column(nullable = false)
    private Integer amountCents;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 120)
    private String providerRefNo;

    @Column(length = 1000)
    private String rawCallbackSummary;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getExternalReferenceNo() { return externalReferenceNo; }
    public void setExternalReferenceNo(String externalReferenceNo) { this.externalReferenceNo = externalReferenceNo; }

    public String getBillCode() { return billCode; }
    public void setBillCode(String billCode) { this.billCode = billCode; }

    public Integer getAmountCents() { return amountCents; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }
    public BigDecimal getAmountRinggit() {
        if (amountCents == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProviderRefNo() { return providerRefNo; }
    public void setProviderRefNo(String providerRefNo) { this.providerRefNo = providerRefNo; }

    public String getRawCallbackSummary() { return rawCallbackSummary; }
    public void setRawCallbackSummary(String rawCallbackSummary) { this.rawCallbackSummary = rawCallbackSummary; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
