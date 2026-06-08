package com.muqmeen.takaful.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageProperties {

    private String mode = "local";
    private String localUploadDir = "uploads";
    private String supabaseUrl = "";
    private String supabaseServiceRoleKey = "";
    private String supabaseBucket = "takaful-private";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getLocalUploadDir() { return localUploadDir; }
    public void setLocalUploadDir(String localUploadDir) { this.localUploadDir = localUploadDir; }

    public String getSupabaseUrl() { return supabaseUrl; }
    public void setSupabaseUrl(String supabaseUrl) { this.supabaseUrl = supabaseUrl; }

    public String getSupabaseServiceRoleKey() { return supabaseServiceRoleKey; }
    public void setSupabaseServiceRoleKey(String supabaseServiceRoleKey) { this.supabaseServiceRoleKey = supabaseServiceRoleKey; }

    public String getSupabaseBucket() { return supabaseBucket; }
    public void setSupabaseBucket(String supabaseBucket) { this.supabaseBucket = supabaseBucket; }

    public boolean isSupabaseMode() {
        return "supabase".equalsIgnoreCase(mode);
    }
}
