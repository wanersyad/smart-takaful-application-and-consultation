package com.muqmeen.takaful.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes authentication state to every view so the shared navigation fragment can render
 * the correct links on any page (not just the few controllers that set these explicitly).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("authState")
    public AuthState authState() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
        boolean admin = false;
        boolean customer = false;
        if (authenticated) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                    admin = true;
                } else if ("ROLE_USER".equals(authority.getAuthority())) {
                    customer = true;
                }
            }
        }
        return new AuthState(customer, admin);
    }

    public record AuthState(boolean customer, boolean admin) {
        public boolean signedIn() {
            return customer || admin;
        }
    }
}
