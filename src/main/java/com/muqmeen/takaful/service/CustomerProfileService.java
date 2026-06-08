package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.CustomerProfile;
import com.muqmeen.takaful.domain.FilePurpose;
import com.muqmeen.takaful.domain.StoredFile;
import com.muqmeen.takaful.repository.CustomerProfileRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Service
@Transactional
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final FileStorageService fileStorageService;

    public CustomerProfileService(CustomerProfileRepository customerProfileRepository,
                                  FileStorageService fileStorageService) {
        this.customerProfileRepository = customerProfileRepository;
        this.fileStorageService = fileStorageService;
    }

    public CustomerProfile getOrCreate(Customer customer) {
        CustomerProfile profile = customerProfileRepository.findByCustomer(customer).orElseGet(() -> {
            CustomerProfile createdProfile = new CustomerProfile();
            createdProfile.setCustomer(customer);
            return customerProfileRepository.save(createdProfile);
        });
        Hibernate.initialize(profile.getProfilePicture());
        return profile;
    }

    public CustomerProfile update(Customer customer, ProfileUpdate update, MultipartFile profilePicture) {
        CustomerProfile profile = getOrCreate(customer);
        profile.setHomeAddress(update.homeAddress());
        profile.setOccupation(update.occupation());
        profile.setPositionTitle(update.positionTitle());
        profile.setEmployerName(update.employerName());
        profile.setWorkplaceAddress(update.workplaceAddress());
        profile.setAnnualIncome(update.annualIncome());
        profile.setBankName(update.bankName());
        profile.setBankAccountNumber(update.bankAccountNumber());
        profile.setHeightCm(update.heightCm());
        profile.setWeightKg(update.weightKg());
        StoredFile uploaded = fileStorageService.storeImage(profilePicture, customer, null, FilePurpose.PROFILE_PICTURE);
        if (uploaded != null) {
            profile.setProfilePicture(uploaded);
        }
        return customerProfileRepository.save(profile);
    }

    public record ProfileUpdate(
            String homeAddress,
            String occupation,
            String positionTitle,
            String employerName,
            String workplaceAddress,
            BigDecimal annualIncome,
            String bankName,
            String bankAccountNumber,
            BigDecimal heightCm,
            BigDecimal weightKg
    ) {}
}
