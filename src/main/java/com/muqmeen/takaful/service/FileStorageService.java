package com.muqmeen.takaful.service;

import com.muqmeen.takaful.config.FileStorageProperties;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.FilePurpose;
import com.muqmeen.takaful.domain.StoredFile;
import com.muqmeen.takaful.repository.StoredFileRepository;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class FileStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> DOCUMENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final FileStorageProperties properties;
    private final StoredFileRepository storedFileRepository;
    private final RestClient restClient;

    public FileStorageService(FileStorageProperties properties, StoredFileRepository storedFileRepository) {
        this.properties = properties;
        this.storedFileRepository = storedFileRepository;
        // Bounded timeouts so a slow/unresponsive Supabase Storage call can never hang a
        // Tomcat worker thread indefinitely (which would exhaust the pool and take the whole
        // app down). 5s to connect, 20s to read a file.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public StoredFile storeImage(MultipartFile file, Customer customer, ConsultationApplication application, FilePurpose purpose) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        validateType(file.getContentType(), IMAGE_TYPES, "Only JPG, PNG, or WebP images are allowed.");
        return store(file, customer, application, purpose);
    }

    public StoredFile storeDocument(MultipartFile file, Customer customer, ConsultationApplication application, FilePurpose purpose) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        validateType(file.getContentType(), DOCUMENT_TYPES, "Only JPG, PNG, WebP, or PDF files are allowed.");
        return store(file, customer, application, purpose);
    }

    public StoredFile storeProductImage(MultipartFile file) {
        return storeImage(file, null, null, FilePurpose.PRODUCT_IMAGE);
    }

    public StoredFile storeProductDocument(MultipartFile file) {
        return storeDocument(file, null, null, FilePurpose.PRODUCT_DOCUMENT);
    }

    public StoredFile storeSignatureDataUrl(String dataUrl, Customer customer, ConsultationApplication application) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }
        String[] parts = dataUrl.split(",", 2);
        if (parts.length != 2 || !parts[0].startsWith("data:image/png;base64")) {
            throw new IllegalArgumentException("Signature must be submitted as a PNG image.");
        }
        byte[] bytes = Base64.getDecoder().decode(parts[1]);
        String key = buildStorageKey(customer, "signature.png");
        writeBytes(key, bytes, "image/png");

        StoredFile storedFile = new StoredFile();
        storedFile.setCustomer(customer);
        storedFile.setApplication(application);
        storedFile.setPurpose(FilePurpose.SIGNATURE);
        storedFile.setOriginalFilename("signature.png");
        storedFile.setContentType("image/png");
        storedFile.setFileSize((long) bytes.length);
        storedFile.setStorageProvider(properties.isSupabaseMode() ? "supabase" : "local");
        storedFile.setStorageKey(key);
        return storedFileRepository.save(storedFile);
    }

    public void delete(StoredFile storedFile) {
        if (storedFile == null) {
            return;
        }
        deleteBytes(storedFile);
        storedFileRepository.delete(storedFile);
    }

    private void deleteBytes(StoredFile storedFile) {
        if (storedFile.getStorageKey() == null) {
            return;
        }
        if ("supabase".equals(storedFile.getStorageProvider())) {
            if (properties.getSupabaseUrl().isBlank() || properties.getSupabaseServiceRoleKey().isBlank()) {
                return;
            }
            try {
                restClient.delete()
                        .uri(properties.getSupabaseUrl() + "/storage/v1/object/" + properties.getSupabaseBucket() + "/" + storedFile.getStorageKey())
                        .header("Authorization", "Bearer " + properties.getSupabaseServiceRoleKey())
                        .header("apikey", properties.getSupabaseServiceRoleKey())
                        .retrieve()
                        .toBodilessEntity();
            } catch (RuntimeException ignored) {
                // Best effort: a missing object should not block record deletion.
            }
            return;
        }
        try {
            Files.deleteIfExists(Path.of(properties.getLocalUploadDir()).resolve(storedFile.getStorageKey()));
        } catch (IOException ignored) {
            // Best effort: leave orphaned bytes rather than fail the deletion.
        }
    }

    public byte[] readBytes(StoredFile storedFile) {
        if ("supabase".equals(storedFile.getStorageProvider())) {
            return restClient.get()
                    .uri(properties.getSupabaseUrl() + "/storage/v1/object/" + properties.getSupabaseBucket() + "/" + storedFile.getStorageKey())
                    .header("Authorization", "Bearer " + properties.getSupabaseServiceRoleKey())
                    .header("apikey", properties.getSupabaseServiceRoleKey())
                    .retrieve()
                    .body(byte[].class);
        }
        try {
            return Files.readAllBytes(Path.of(properties.getLocalUploadDir()).resolve(storedFile.getStorageKey()));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read stored file.", ex);
        }
    }

    /**
     * Returns a time-limited URL the browser can use to fetch this file directly from Supabase
     * Storage, so public assets (product images/brochures) never have to be proxied byte-by-byte
     * through the app. Returns null when not in supabase mode (caller falls back to /files/{id}).
     */
    public String createSignedUrl(StoredFile storedFile, int expirySeconds) {
        if (storedFile == null
                || !"supabase".equals(storedFile.getStorageProvider())
                || storedFile.getStorageKey() == null
                || properties.getSupabaseUrl().isBlank()
                || properties.getSupabaseServiceRoleKey().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> resp = restClient.post()
                    .uri(properties.getSupabaseUrl() + "/storage/v1/object/sign/"
                            + properties.getSupabaseBucket() + "/" + storedFile.getStorageKey())
                    .header("Authorization", "Bearer " + properties.getSupabaseServiceRoleKey())
                    .header("apikey", properties.getSupabaseServiceRoleKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", expirySeconds))
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Object signed = resp == null ? null : resp.get("signedURL");
            if (signed == null) {
                return null;
            }
            // signedURL is a path like "/object/sign/<bucket>/<key>?token=..."; prefix the storage base.
            return properties.getSupabaseUrl() + "/storage/v1" + signed;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private StoredFile store(MultipartFile file, Customer customer, ConsultationApplication application, FilePurpose purpose) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        String key = buildStorageKey(customer, filename);
        try {
            writeBytes(key, file.getBytes(), file.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store uploaded file.", ex);
        }

        StoredFile storedFile = new StoredFile();
        storedFile.setCustomer(customer);
        storedFile.setApplication(application);
        storedFile.setPurpose(purpose);
        storedFile.setOriginalFilename(filename);
        storedFile.setContentType(file.getContentType());
        storedFile.setFileSize(file.getSize());
        storedFile.setStorageProvider(properties.isSupabaseMode() ? "supabase" : "local");
        storedFile.setStorageKey(key);
        return storedFileRepository.save(storedFile);
    }

    private void writeBytes(String key, byte[] bytes, String contentType) {
        if (properties.isSupabaseMode()) {
            if (properties.getSupabaseUrl().isBlank() || properties.getSupabaseServiceRoleKey().isBlank()) {
                throw new IllegalStateException("Supabase file storage requires SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.");
            }
            restClient.put()
                    .uri(properties.getSupabaseUrl() + "/storage/v1/object/" + properties.getSupabaseBucket() + "/" + key)
                    .header("Authorization", "Bearer " + properties.getSupabaseServiceRoleKey())
                    .header("apikey", properties.getSupabaseServiceRoleKey())
                    .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
            return;
        }
        try {
            Path target = Path.of(properties.getLocalUploadDir()).resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write file to local storage.", ex);
        }
    }

    private String buildStorageKey(Customer customer, String filename) {
        String extension = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            extension = "." + filename.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }
        String ownerPath = customer == null ? "products" : "customers/" + customer.getId();
        return ownerPath + "/" + LocalDate.now() + "/" + UUID.randomUUID() + extension;
    }

    private void validateType(String contentType, Set<String> allowed, String message) {
        if (contentType == null || !allowed.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(message);
        }
    }
}
