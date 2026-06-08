package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.service.ApplicationService;
import com.muqmeen.takaful.service.ApplicationService.ApplicationInput;
import com.muqmeen.takaful.service.ApplicationService.NomineeInput;
import com.muqmeen.takaful.service.CustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ApplicationController {

    private final CustomerService customerService;
    private final ApplicationService applicationService;

    public ApplicationController(CustomerService customerService, ApplicationService applicationService) {
        this.customerService = customerService;
        this.applicationService = applicationService;
    }

    @GetMapping("/applications/new")
    public String start(@RequestParam("productId") Long productId,
                        Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        ConsultationApplication application = applicationService.start(customer, productId);
        return "redirect:/applications/" + application.getId() + "/edit";
    }

    @GetMapping("/applications/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = currentCustomer(authentication);
        ConsultationApplication application = applicationService.findOwned(id, customer)
                .orElseThrow(ApplicationAccessDeniedException::new);
        model.addAttribute("application", application);
        return "application_detail";
    }

    @GetMapping("/applications/{id}/edit")
    public String edit(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = currentCustomer(authentication);
        ConsultationApplication application = applicationService.findOwned(id, customer)
                .orElseThrow(ApplicationAccessDeniedException::new);
        if (!application.isEditableByCustomer()) {
            return "redirect:/applications/" + id;
        }
        model.addAttribute("application", application);
        model.addAttribute("form", ApplicationForm.from(application));
        return "application_form";
    }

    @PostMapping("/applications/{id}")
    public String save(@PathVariable Long id,
                       @ModelAttribute ApplicationForm form,
                       @RequestParam(value = "icFront", required = false) MultipartFile icFront,
                       @RequestParam(value = "icBack", required = false) MultipartFile icBack,
                       @RequestParam(value = "signatureDataUrl", required = false) String signatureDataUrl,
                       @RequestParam(value = "action", defaultValue = "draft") String action,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(authentication);
        boolean submit = "submit".equals(action);
        try {
            ConsultationApplication saved = applicationService.save(customer, id, form.toInput(), icFront, icBack, signatureDataUrl, submit);
            redirectAttributes.addFlashAttribute("flashMessage", submit ? "Application submitted for agent review." : "Draft saved.");
            return "redirect:/applications/" + saved.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashMessage", ex.getMessage());
            return "redirect:/applications/" + id + "/edit";
        }
    }

    @PostMapping("/applications/{id}/delete")
    public String deleteDraft(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(authentication);
        try {
            applicationService.deleteDraft(customer, id);
            redirectAttributes.addFlashAttribute("flashMessage", "Draft application deleted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashMessage", ex.getMessage());
        }
        return "redirect:/account";
    }

    private Customer currentCustomer(Authentication authentication) {
        return customerService.currentCustomer(authentication)
                .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class ApplicationAccessDeniedException extends RuntimeException {
    }

    public static class ApplicationForm {
        public String applicantFullName;
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
        public String nominee1FullName;
        public String nominee1IcNumber;
        public String nominee1Relationship;
        public String nominee1HomeAddress;
        public String nominee1Occupation;
        public String nominee1PhoneNumber;

        static ApplicationForm from(ConsultationApplication application) {
            ApplicationForm form = new ApplicationForm();
            form.applicantFullName = application.getApplicantFullName();
            form.email = application.getEmail();
            form.phoneNumber = application.getPhoneNumber();
            form.homeAddress = application.getHomeAddress();
            form.occupation = application.getOccupation();
            form.positionTitle = application.getPositionTitle();
            form.employerName = application.getEmployerName();
            form.workplaceAddress = application.getWorkplaceAddress();
            form.annualIncome = application.getAnnualIncome();
            form.bankName = application.getBankName();
            form.bankAccountNumber = application.getBankAccountNumber();
            form.heightCm = application.getHeightCm();
            form.weightKg = application.getWeightKg();
            application.getNominees().stream().findFirst().ifPresent(nominee -> {
                form.nominee1FullName = nominee.getFullName();
                form.nominee1IcNumber = nominee.getIcNumber();
                form.nominee1Relationship = nominee.getRelationship();
                form.nominee1HomeAddress = nominee.getHomeAddress();
                form.nominee1Occupation = nominee.getOccupation();
                form.nominee1PhoneNumber = nominee.getPhoneNumber();
            });
            return form;
        }

        ApplicationInput toInput() {
            return new ApplicationInput(
                    applicantFullName,
                    email,
                    phoneNumber,
                    homeAddress,
                    occupation,
                    positionTitle,
                    employerName,
                    workplaceAddress,
                    annualIncome,
                    bankName,
                    bankAccountNumber,
                    heightCm,
                    weightKg,
                    List.of(new NomineeInput(
                            nominee1FullName,
                            nominee1IcNumber,
                            nominee1Relationship,
                            nominee1HomeAddress,
                            nominee1Occupation,
                            nominee1PhoneNumber
                    ))
            );
        }

        public void setApplicantFullName(String applicantFullName) { this.applicantFullName = applicantFullName; }
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
        public void setNominee1FullName(String nominee1FullName) { this.nominee1FullName = nominee1FullName; }
        public void setNominee1IcNumber(String nominee1IcNumber) { this.nominee1IcNumber = nominee1IcNumber; }
        public void setNominee1Relationship(String nominee1Relationship) { this.nominee1Relationship = nominee1Relationship; }
        public void setNominee1HomeAddress(String nominee1HomeAddress) { this.nominee1HomeAddress = nominee1HomeAddress; }
        public void setNominee1Occupation(String nominee1Occupation) { this.nominee1Occupation = nominee1Occupation; }
        public void setNominee1PhoneNumber(String nominee1PhoneNumber) { this.nominee1PhoneNumber = nominee1PhoneNumber; }
    }
}
