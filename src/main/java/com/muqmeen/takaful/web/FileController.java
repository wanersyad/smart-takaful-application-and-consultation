package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
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
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication authentication) {
        StoredFile storedFile = storedFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (!canAccess(storedFile, authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedFile.getOriginalFilename() + "\"")
                .body(fileStorageService.readBytes(storedFile));
    }

    private boolean canAccess(StoredFile storedFile, Authentication authentication) {
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
