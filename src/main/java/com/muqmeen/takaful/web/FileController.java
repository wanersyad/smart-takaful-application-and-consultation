package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.FilePurpose;
import com.muqmeen.takaful.domain.StoredFile;
import com.muqmeen.takaful.repository.StoredFileRepository;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FileController {

    private final StoredFileRepository storedFileRepository;
    private final FileStorageService fileStorageService;
    private final CustomerService customerService;

    public FileController(StoredFileRepository storedFileRepository,
                          FileStorageService fileStorageService,
                          CustomerService customerService) {
        this.storedFileRepository = storedFileRepository;
        this.fileStorageService = fileStorageService;
        this.customerService = customerService;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<?> download(@PathVariable Long id, Authentication authentication) {
        StoredFile storedFile = storedFileRepository.findById(id).orElse(null);
        if (storedFile == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccess(storedFile, authentication)) {
            return ResponseEntity.status(403).build();
        }
        // Public assets (product images/brochures) are redirected straight to a short-lived
        // Supabase signed URL so the browser fetches the bytes directly from storage. The app
        // thread is released immediately instead of proxying potentially-large files — this is
        // what prevents the Tomcat thread pool from being exhausted by image/PDF traffic.
        if (isPublic(storedFile)) {
            String signedUrl = fileStorageService.createSignedUrl(storedFile, 3600);
            if (signedUrl != null) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, signedUrl)
                        // Allow the browser/CDN to reuse the redirect for a while (well within the
                        // 1h signed-URL lifetime) so we don't re-sign on every image request.
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=600")
                        .build();
            }
            // Fall through to byte-proxying when signing is unavailable (e.g. local storage mode).
        }
        byte[] bytes;
        try {
            bytes = fileStorageService.readBytes(storedFile);
        } catch (RuntimeException ex) {
            // Storage backend unavailable or object missing — return a clean 404 instead of
            // a Whitelabel 500 error page.
            return ResponseEntity.notFound().build();
        }
        String contentType = storedFile.getContentType() == null ? "application/octet-stream" : storedFile.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedFile.getOriginalFilename() + "\"")
                .body(bytes);
    }

    private boolean isPublic(StoredFile storedFile) {
        return storedFile.getPurpose() == FilePurpose.PRODUCT_IMAGE
                || storedFile.getPurpose() == FilePurpose.PRODUCT_DOCUMENT;
    }

    private boolean canAccess(StoredFile storedFile, Authentication authentication) {
        if (isPublic(storedFile)) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (admin) {
            return true;
        }
        Customer customer = customerService.currentCustomer(authentication).orElse(null);
        return customer != null
                && storedFile.getCustomer() != null
                && storedFile.getCustomer().getId().equals(customer.getId());
    }
}
