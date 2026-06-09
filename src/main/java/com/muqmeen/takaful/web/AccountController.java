package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.CustomerProfile;
import com.muqmeen.takaful.service.ApplicationService;
import com.muqmeen.takaful.service.CustomerProfileService;
import com.muqmeen.takaful.service.CustomerProfileService.ProfileUpdate;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.CustomerService.DuplicateCustomerException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class AccountController {

    private final CustomerService customerService;
    private final CustomerProfileService customerProfileService;
    private final ApplicationService applicationService;

    public AccountController(CustomerService customerService,
                             CustomerProfileService customerProfileService,
                             ApplicationService applicationService) {
        this.customerService = customerService;
        this.customerProfileService = customerProfileService;
        this.applicationService = applicationService;
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        Customer customer = currentCustomer(authentication);
        CustomerProfile profile = customerProfileService.getOrCreate(customer);
        List<ConsultationApplication> applications = applicationService.listForCustomer(customer);
        long drafts = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.DRAFT).count();
        long underReview = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
        long quoted = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.QUOTED || app.getStatus() == ApplicationStatus.PAYMENT_PENDING).count();
        long paid = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.PAID).count();

        model.addAttribute("customer", customer);
        model.addAttribute("profile", profile);
        model.addAttribute("applications", applications);
        model.addAttribute("totalApplications", applications.size());
        model.addAttribute("draftApplications", drafts);
        model.addAttribute("reviewApplications", underReview);
        model.addAttribute("quotedApplications", quoted);
        model.addAttribute("paidApplications", paid);
        return "account";
    }

    @GetMapping("/account/profile")
    public String profile(Authentication authentication, Model model) {
        Customer customer = currentCustomer(authentication);
        model.addAttribute("customer", customer);
        model.addAttribute("profile", customerProfileService.getOrCreate(customer));
        return "profile";
    }

    @PostMapping("/account/profile")
    public String updateProfile(@ModelAttribute ProfileForm form,
                                @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(authentication);
        try {
            customer = customerService.updateAccountDetails(customer, form.fullName, form.email, form.phoneNumber);
            customerProfileService.update(customer, form.toUpdate(), profilePicture);
            refreshAuthentication(authentication, customer);
        } catch (DuplicateCustomerException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashMessage", ex.getMessage());
            return "redirect:/account/profile";
        }
        redirectAttributes.addFlashAttribute("flashMessage", "Profile updated.");
        return "redirect:/account/profile";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(Authentication authentication,
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(authentication);
        customerService.deleteCustomer(customer.getId());
        // Log the now-deleted user out.
        SecurityContextHolder.clearContext();
        try {
            request.getSession().invalidate();
        } catch (IllegalStateException ignored) {
            // session already gone
        }
        redirectAttributes.addFlashAttribute("flashMessage", "Your account has been permanently deleted.");
        return "redirect:/";
    }

    private void refreshAuthentication(Authentication authentication, Customer customer) {
        UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
                customer.getEmail(),
                authentication.getCredentials(),
                authentication.getAuthorities()
        );
        refreshed.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }

    private Customer currentCustomer(Authentication authentication) {
        return customerService.currentCustomer(authentication)
                .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
    }

    public static class ProfileForm {
        public String fullName;
        public String email;
        public String phoneNumber;
        public String homeAddress;
        public String occupation;
        public String positionTitle;
        public String employerName;
        public String workplaceAddress;
        public BigDecimal annualIncome;
        public String bankName;
        public String bankAccountNumber;
        public BigDecimal heightCm;
        public BigDecimal weightKg;

        ProfileUpdate toUpdate() {
            return new ProfileUpdate(
                    homeAddress,
                    occupation,
                    positionTitle,
                    employerName,
                    workplaceAddress,
                    annualIncome,
                    bankName,
                    bankAccountNumber,
                    heightCm,
                    weightKg
            );
        }

        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setEmail(String email) { this.email = email; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setHomeAddress(String homeAddress) { this.homeAddress = homeAddress; }
        public void setOccupation(String occupation) { this.occupation = occupation; }
        public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
        public void setEmployerName(String employerName) { this.employerName = employerName; }
        public void setWorkplaceAddress(String workplaceAddress) { this.workplaceAddress = workplaceAddress; }
        public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
        public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    }
}
