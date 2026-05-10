package com.muqmeen.takaful.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.muqmeen.takaful.repository.CustomerRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/login", "/admin/login", "/register", "/success", "/payment/**", "/api/chat", "/brochures/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/contact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/submit-lead").hasRole("USER")
                        .requestMatchers("/account/**").hasRole("USER")
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            String redirect = request.getParameter("redirect");
                            String loginPath = isSafeAdminRedirect(redirect) ? "/admin/login?error" : "/login?error";
                            response.sendRedirect(request.getContextPath() + loginPath);
                        })
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getRequestURI().substring(request.getContextPath().length());
                            String loginPath = isAdminPath(path) ? "/admin/login" : "/login";
                            response.sendRedirect(request.getContextPath() + loginPath);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String path = request.getRequestURI().substring(request.getContextPath().length());
                            if (HttpMethod.GET.matches(request.getMethod()) && isAdminPath(path)) {
                                response.sendRedirect(request.getContextPath() + "/admin/login");
                                return;
                            }
                            response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/payment/callback")
                )
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AdminSecurityProperties properties,
                                          CustomerRepository customerRepository,
                                          PasswordEncoder passwordEncoder) {
        if (isBlank(properties.getUsername()) || isBlank(properties.getPassword())) {
            throw new IllegalStateException("ADMIN_USERNAME and ADMIN_PASSWORD must be configured.");
        }

        String adminUsername = properties.getUsername();
        String encodedAdminPassword = passwordEncoder.encode(properties.getPassword());

        return username -> {
            if (adminUsername.equals(username)) {
                return User.withUsername(adminUsername)
                        .password(encodedAdminPassword)
                        .roles("ADMIN")
                        .build();
            }

            return customerRepository.findByEmailIgnoreCase(username)
                    .map(customer -> User.withUsername(customer.getEmail())
                            .password(customer.getPasswordHash())
                            .roles("USER")
                            .build())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        };
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            String redirect = request.getParameter("redirect");
            if (admin && isSafeAdminRedirect(redirect)) {
                response.sendRedirect(redirect);
                return;
            }
            if (!admin && isSafeUserRedirect(redirect)) {
                response.sendRedirect(redirect);
                return;
            }

            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (admin && savedRequest != null && isSafeAdminRedirect(savedRequest.getRedirectUrl())) {
                response.sendRedirect(savedRequest.getRedirectUrl());
                return;
            }
            if (!admin && savedRequest != null && isSafeUserRedirect(savedRequest.getRedirectUrl())) {
                response.sendRedirect(savedRequest.getRedirectUrl());
                return;
            }

            response.sendRedirect(admin ? "/admin/dashboard" : "/account");
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isSafeRedirect(String redirect) {
        return redirect != null && redirect.startsWith("/") && !redirect.startsWith("//");
    }

    private boolean isSafeAdminRedirect(String redirect) {
        return isSafeRedirect(redirect) && ("/admin".equals(redirect) || redirect.startsWith("/admin/"));
    }

    private boolean isSafeUserRedirect(String redirect) {
        return isSafeRedirect(redirect) && !"/admin".equals(redirect) && !redirect.startsWith("/admin/");
    }

    private boolean isAdminPath(String path) {
        return "/admin".equals(path) || path.startsWith("/admin/");
    }
}
