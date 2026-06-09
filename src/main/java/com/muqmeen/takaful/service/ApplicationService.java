package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.ApplicationNominee;
import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.CustomerProfile;
import com.muqmeen.takaful.domain.FilePurpose;
import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.domain.StoredFile;
import com.muqmeen.takaful.repository.ConsultationApplicationRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApplicationService {

    private final ConsultationApplicationRepository applicationRepository;
    private final ProductService productService;
    private final CustomerProfileService customerProfileService;
    private final FileStorageService fileStorageService;

    public ApplicationService(ConsultationApplicationRepository applicationRepository,
                              ProductService productService,
                              CustomerProfileService customerProfileService,
                              FileStorageService fileStorageService) {
        this.applicationRepository = applicationRepository;
        this.productService = productService;
        this.customerProfileService = customerProfileService;
        this.fileStorageService = fileStorageService;
    }

    public ConsultationApplication start(Customer customer, Long productId) {
        Product product = productService.findActiveById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        CustomerProfile profile = customerProfileService.getOrCreate(customer);

        ConsultationApplication application = new ConsultationApplication();
        application.setCustomer(customer);
        application.setProduct(product);
        application.setApplicantFullName(customer.getFullName());
        application.setEmail(customer.getEmail());
        application.setPhoneNumber(customer.getPhoneNumber());
        application.setHomeAddress(nullToBlank(profile.getHomeAddress()));
        application.setOccupation(nullToBlank(profile.getOccupation()));
        application.setPositionTitle(nullToBlank(profile.getPositionTitle()));
        application.setEmployerName(nullToBlank(profile.getEmployerName()));
        application.setWorkplaceAddress(nullToBlank(profile.getWorkplaceAddress()));
        application.setAnnualIncome(defaultDecimal(profile.getAnnualIncome()));
        application.setBankName(nullToBlank(profile.getBankName()));
        application.setBankAccountNumber(nullToBlank(profile.getBankAccountNumber()));
        application.setHeightCm(defaultDecimal(profile.getHeightCm()));
        application.setWeightKg(defaultDecimal(profile.getWeightKg()));
        return applicationRepository.save(application);
    }

    public ConsultationApplication save(Customer customer,
                                        Long applicationId,
                                        ApplicationInput input,
                                        MultipartFile icFront,
                                        MultipartFile icBack,
                                        String signatureDataUrl,
                                        boolean submit) {
        ConsultationApplication application = findOwned(applicationId, customer)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!application.isEditableByCustomer()) {
            throw new IllegalStateException("Submitted applications cannot be edited unless corrections are requested.");
        }
        copyInput(application, input);
        replaceNominees(application, input.nominees());
        attachFiles(application, customer, icFront, icBack, signatureDataUrl);
        if (submit) {
            boolean resolvingCorrection = application.getStatus() == ApplicationStatus.NEEDS_INFO
                    && !isBlank(application.getCorrectionRequest());
            validateSubmission(application);
            application.setStatus(ApplicationStatus.SUBMITTED);
            application.setSubmittedAt(LocalDateTime.now());
            if (resolvingCorrection) {
                application.setCorrectionResolvedAt(LocalDateTime.now());
            }
        }
        return applicationRepository.save(application);
    }

    public List<ConsultationApplication> listForCustomer(Customer customer) {
        List<ConsultationApplication> applications = applicationRepository.findAllByCustomerOrderByCreatedAtDesc(customer);
        applications.forEach(this::initializeDetails);
        return applications;
    }

    public List<ConsultationApplication> listForAdmin() {
        List<ConsultationApplication> applications = applicationRepository.findAllByOrderByCreatedAtDesc();
        applications.forEach(this::initializeDetails);
        return applications;
    }

    public Optional<ConsultationApplication> findOwned(Long id, Customer customer) {
        return applicationRepository.findByIdAndCustomer(id, customer).map(this::initializeDetails);
    }

    public Optional<ConsultationApplication> findById(Long id) {
        return applicationRepository.findById(id).map(this::initializeDetails);
    }

    public void deleteDraft(Customer customer, Long id) {
        ConsultationApplication application = findOwned(id, customer)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Only drafts can be deleted.");
        }
        applicationRepository.delete(application);
    }

    public void deleteApplication(Long id) {
        ConsultationApplication application = applicationRepository.findById(id)
                .map(this::initializeDetails)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        deleteApplicationInternal(application);
    }

    /**
     * Deletes an application and everything it owns: nominees, quotation (items + payment),
     * and the stored IC/signature/uploaded files (both metadata rows and their bytes).
     * Used by admin cleanup and by {@link #deleteApplicationsForCustomer(Customer)}.
     */
    void deleteApplicationInternal(ConsultationApplication application) {
        // Detach the file references the application points at so the cascade delete of the
        // owning rows does not collide with the FK columns on consultation_applications.
        application.setIcFrontFile(null);
        application.setIcBackFile(null);
        application.setSignatureFile(null);
        List<StoredFile> ownedFiles = new ArrayList<>(application.getFiles());
        application.getFiles().clear();
        for (StoredFile file : ownedFiles) {
            fileStorageService.delete(file);
        }
        applicationRepository.delete(application);
    }

    public void deleteApplicationsForCustomer(Customer customer) {
        List<ConsultationApplication> applications =
                applicationRepository.findAllByCustomerOrderByCreatedAtDesc(customer);
        for (ConsultationApplication application : applications) {
            deleteApplicationInternal(initializeDetails(application));
        }
    }

    public ConsultationApplication updateStatus(Long id, ApplicationStatus status) {
        return updateStatus(id, status, null, null);
    }

    public ConsultationApplication updateStatus(Long id, ApplicationStatus status, String adminReviewNotes, String correctionRequest) {
        ConsultationApplication application = applicationRepository.findById(id)
                .map(this::initializeDetails)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        application.setStatus(status);
        application.setReviewedAt(LocalDateTime.now());
        application.setAdminReviewNotes(adminReviewNotes);
        if (status == ApplicationStatus.NEEDS_INFO) {
            application.setCorrectionRequest(correctionRequest);
            application.setCorrectionRequestedAt(LocalDateTime.now());
            application.setCorrectionResolvedAt(null);
        }
        return applicationRepository.save(application);
    }

    private void copyInput(ConsultationApplication application, ApplicationInput input) {
        application.setApplicantFullName(input.applicantFullName());
        application.setEmail(input.email());
        application.setPhoneNumber(input.phoneNumber());
        application.setHomeAddress(input.homeAddress());
        application.setOccupation(input.occupation());
        application.setPositionTitle(input.positionTitle());
        application.setEmployerName(input.employerName());
        application.setWorkplaceAddress(input.workplaceAddress());
        application.setAnnualIncome(input.annualIncome());
        application.setBankName(input.bankName());
        application.setBankAccountNumber(input.bankAccountNumber());
        application.setHeightCm(input.heightCm());
        application.setWeightKg(input.weightKg());
    }

    private void replaceNominees(ConsultationApplication application, List<NomineeInput> nominees) {
        application.getNominees().clear();
        if (nominees == null) return;
        int order = 0;
        for (NomineeInput input : nominees) {
            if (input == null || isBlank(input.fullName())) continue;
            ApplicationNominee nominee = new ApplicationNominee();
            nominee.setApplication(application);
            nominee.setFullName(input.fullName());
            nominee.setIcNumber(input.icNumber());
            nominee.setRelationship(input.relationship());
            nominee.setHomeAddress(input.homeAddress());
            nominee.setOccupation(input.occupation());
            nominee.setPhoneNumber(input.phoneNumber());
            application.getNominees().add(nominee);
            order++;
        }
    }

    private void attachFiles(ConsultationApplication application, Customer customer, MultipartFile icFront, MultipartFile icBack, String signatureDataUrl) {
        StoredFile front = fileStorageService.storeImage(icFront, customer, application, FilePurpose.IC_FRONT);
        if (front != null) {
            application.setIcFrontFile(front);
            application.getFiles().add(front);
        }
        StoredFile back = fileStorageService.storeImage(icBack, customer, application, FilePurpose.IC_BACK);
        if (back != null) {
            application.setIcBackFile(back);
            application.getFiles().add(back);
        }
        StoredFile signature = fileStorageService.storeSignatureDataUrl(signatureDataUrl, customer, application);
        if (signature != null) {
            application.setSignatureFile(signature);
            application.getFiles().add(signature);
        }
    }

    private void validateSubmission(ConsultationApplication application) {
        List<String> missing = new ArrayList<>();
        if (application.getIcFrontFile() == null) missing.add("IC front image");
        if (application.getIcBackFile() == null) missing.add("IC back image");
        if (application.getSignatureFile() == null) missing.add("signature");
        if (isBlank(application.getApplicantFullName())) missing.add("full name");
        if (isBlank(application.getEmail())) missing.add("email address");
        if (isBlank(application.getPhoneNumber())) missing.add("phone number");
        if (isBlank(application.getHomeAddress())) missing.add("home address");
        if (isBlank(application.getOccupation())) missing.add("occupation");
        if (isBlank(application.getPositionTitle())) missing.add("position");
        if (isBlank(application.getEmployerName())) missing.add("employer name");
        if (isBlank(application.getWorkplaceAddress())) missing.add("workplace address");
        if (application.getAnnualIncome() == null || application.getAnnualIncome().signum() <= 0) missing.add("annual income");
        if (isBlank(application.getBankName())) missing.add("bank name");
        if (isBlank(application.getBankAccountNumber())) missing.add("bank account number");
        if (application.getHeightCm() == null || application.getHeightCm().signum() <= 0) missing.add("height");
        if (application.getWeightKg() == null || application.getWeightKg().signum() <= 0) missing.add("weight");
        if (application.getNominees().isEmpty()) missing.add("at least one nominee");
        application.getNominees().forEach(nominee -> {
            if (isBlank(nominee.getFullName())
                    || isBlank(nominee.getIcNumber())
                    || isBlank(nominee.getRelationship())
                    || isBlank(nominee.getHomeAddress())
                    || isBlank(nominee.getOccupation())
                    || isBlank(nominee.getPhoneNumber())) {
                missing.add("complete nominee details");
            }
        });
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required submission data: " + String.join(", ", missing));
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private ConsultationApplication initializeDetails(ConsultationApplication application) {
        Hibernate.initialize(application.getCustomer());
        Hibernate.initialize(application.getProduct());
        Hibernate.initialize(application.getNominees());
        Hibernate.initialize(application.getFiles());
        if (application.getQuotation() != null) {
            Hibernate.initialize(application.getQuotation());
            Hibernate.initialize(application.getQuotation().getItems());
            Hibernate.initialize(application.getQuotation().getPayment());
        }
        return application;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ApplicationInput(
            String applicantFullName,
            String email,
            String phoneNumber,
            String homeAddress,
            String occupation,
            String positionTitle,
            String employerName,
            String workplaceAddress,
            BigDecimal annualIncome,
            String bankName,
            String bankAccountNumber,
            BigDecimal heightCm,
            BigDecimal weightKg,
            List<NomineeInput> nominees
    ) {}

    public record NomineeInput(
            String fullName,
            String icNumber,
            String relationship,
            String homeAddress,
            String occupation,
            String phoneNumber
    ) {}
}
