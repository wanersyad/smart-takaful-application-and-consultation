package com.muqmeen.takaful.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultation_applications")
public class ConsultationApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(length = 120)
    private String applicantFullName;

    @Column(length = 160)
    private String email;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 500)
    private String homeAddress;

    @Column(length = 120)
    private String occupation;

    @Column(length = 120)
    private String positionTitle;

    @Column(length = 160)
    private String employerName;

    @Column(length = 500)
    private String workplaceAddress;

    private BigDecimal annualIncome;

    @Column(length = 120)
    private String bankName;

    @Column(length = 80)
    private String bankAccountNumber;

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_front_file_id")
    private StoredFile icFrontFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_back_file_id")
    private StoredFile icBackFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signature_file_id")
    private StoredFile signatureFile;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationNominee> nominees = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<StoredFile> files = new ArrayList<>();

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private Quotation quotation;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;

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

    public boolean isEditableByCustomer() {
        return status == ApplicationStatus.DRAFT || status == ApplicationStatus.NEEDS_INFO;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getApplicantFullName() { return applicantFullName; }
    public void setApplicantFullName(String applicantFullName) { this.applicantFullName = applicantFullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getHomeAddress() { return homeAddress; }
    public void setHomeAddress(String homeAddress) { this.homeAddress = homeAddress; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String getWorkplaceAddress() { return workplaceAddress; }
    public void setWorkplaceAddress(String workplaceAddress) { this.workplaceAddress = workplaceAddress; }
    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public StoredFile getIcFrontFile() { return icFrontFile; }
    public void setIcFrontFile(StoredFile icFrontFile) { this.icFrontFile = icFrontFile; }
    public StoredFile getIcBackFile() { return icBackFile; }
    public void setIcBackFile(StoredFile icBackFile) { this.icBackFile = icBackFile; }
    public StoredFile getSignatureFile() { return signatureFile; }
    public void setSignatureFile(StoredFile signatureFile) { this.signatureFile = signatureFile; }
    public List<ApplicationNominee> getNominees() { return nominees; }
    public void setNominees(List<ApplicationNominee> nominees) { this.nominees = nominees; }
    public List<StoredFile> getFiles() { return files; }
    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
