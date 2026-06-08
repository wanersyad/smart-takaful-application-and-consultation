package com.muqmeen.takaful.web;

import com.muqmeen.takaful.service.ContactEmailService;
import com.muqmeen.takaful.service.ContactInquiryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    private final ContactEmailService contactEmailService;
    private final ContactInquiryService contactInquiryService;

    public ContactController(ContactEmailService contactEmailService,
                             ContactInquiryService contactInquiryService) {
        this.contactEmailService = contactEmailService;
        this.contactInquiryService = contactInquiryService;
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute ContactForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("contactError", "Please complete the contact form with a valid email.");
            return "redirect:/#contact";
        }

        var inquiry = contactInquiryService.create(new ContactInquiryService.ContactInput(
                form.fullName,
                form.email,
                form.phoneNumber,
                form.subject,
                form.message
        ));
        try {
            contactEmailService.send(new ContactEmailService.ContactMessage(
                    form.fullName,
                    form.email,
                    form.phoneNumber,
                    form.subject,
                    form.message
            ));
            contactInquiryService.markDelivered(inquiry.getId(), "Email delivery accepted");
            redirectAttributes.addFlashAttribute("contactSent", true);
        } catch (ContactEmailService.ContactEmailException ex) {
            contactInquiryService.markFailed(inquiry.getId(), ex.getMessage());
            redirectAttributes.addFlashAttribute("contactError", "We could not send the message right now. Please try again shortly.");
        }

        return "redirect:/#contact";
    }

    public static class ContactForm {
        @NotBlank(message = "Name is required")
        @Size(max = 120)
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 160)
        private String email;

        @Size(max = 40)
        private String phoneNumber;

        @NotBlank(message = "Interest is required")
        @Size(max = 120)
        private String subject;

        @NotBlank(message = "Message is required")
        @Size(max = 1000)
        private String message;

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
    }
}
