package com.muqmeen.takaful.web;

import com.muqmeen.takaful.service.PasswordResetEmailService;
import com.muqmeen.takaful.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "forgot_password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "forgot_password";
        }

        try {
            passwordResetService.requestReset(form.email);
            model.addAttribute("resetRequested", true);
        } catch (PasswordResetEmailService.PasswordResetEmailException ex) {
            model.addAttribute("emailError", true);
        }

        return "forgot_password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(value = "token", required = false) String token,
                                Model model) {
        if (!model.containsAttribute("resetPasswordForm")) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token);
            model.addAttribute("resetPasswordForm", form);
        }
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String completeReset(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "reset_password";
        }

        try {
            passwordResetService.resetPassword(form.token, form.password);
        } catch (PasswordResetService.InvalidResetTokenException ex) {
            bindingResult.rejectValue("token", "invalid", ex.getMessage());
            return "reset_password";
        }

        redirectAttributes.addFlashAttribute("passwordReset", true);
        return "redirect:/login";
    }

    public static class ForgotPasswordForm {
        @Email(message = "Enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordForm {
        @NotBlank(message = "Reset token is required")
        private String token;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        private String password;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
