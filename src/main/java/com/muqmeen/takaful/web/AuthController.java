package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final CustomerService customerService;
    private final UserDetailsService userDetailsService;

    public AuthController(CustomerService customerService, UserDetailsService userDetailsService) {
        this.customerService = customerService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "redirect", required = false) String redirect,
                        Model model) {
        model.addAttribute("adminLogin", false);
        model.addAttribute("redirect", safeUserRedirectOrDefault(redirect, "/#products"));
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin(@RequestParam(value = "redirect", required = false) String redirect, Model model) {
        model.addAttribute("adminLogin", true);
        model.addAttribute("redirect", safeAdminRedirectOrDefault(redirect, "/admin/dashboard"));
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(@RequestParam(value = "redirect", required = false) String redirect,
                               @RequestParam(value = "email", required = false) String email,
                               Model model) {
        if (!model.containsAttribute("registerForm")) {
            RegisterForm form = new RegisterForm();
            form.setEmail(email);
            model.addAttribute("registerForm", form);
        }
        model.addAttribute("redirect", safeUserRedirectOrDefault(redirect, "/#products"));
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           @RequestParam(value = "redirect", required = false) String redirect,
                           Model model,
                           HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("redirect", safeUserRedirectOrDefault(redirect, "/#products"));
            return "register";
        }

        try {
            Customer customer = customerService.register(form.fullName, form.email, form.phoneNumber, form.password);
            authenticate(customer, request);
            return "redirect:" + safeUserRedirectOrDefault(redirect, "/#products");
        } catch (CustomerService.DuplicateCustomerException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            model.addAttribute("redirect", safeUserRedirectOrDefault(redirect, "/#products"));
            return "register";
        }
    }

    private void authenticate(Customer customer, HttpServletRequest request) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(customer.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private String safeUserRedirectOrDefault(String redirect, String fallback) {
        if (isSafeRedirect(redirect) && !isAdminRedirect(redirect)) {
            return redirect;
        }
        return fallback;
    }

    private String safeAdminRedirectOrDefault(String redirect, String fallback) {
        if (isSafeRedirect(redirect) && isAdminRedirect(redirect)) {
            return redirect;
        }
        return fallback;
    }

    private boolean isSafeRedirect(String redirect) {
        return redirect != null && redirect.startsWith("/") && !redirect.startsWith("//");
    }

    private boolean isAdminRedirect(String redirect) {
        return "/admin".equals(redirect) || redirect.startsWith("/admin/");
    }

    public static class RegisterForm {
        @NotBlank(message = "Full name is required")
        @Size(max = 120)
        private String fullName;

        @Email(message = "Enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        private String email;

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[+0-9\\s\\-()]{10,20}$",
                message = "Phone number must contain 10-20 digits"
        )
        private String phoneNumber;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
