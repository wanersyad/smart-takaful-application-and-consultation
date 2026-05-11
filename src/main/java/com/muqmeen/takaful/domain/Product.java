package com.muqmeen.takaful.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Size(max = 60)
    @Column(length = 60)
    private String iconClass;

    @Size(max = 60)
    @Column(length = 60)
    private String accentClass;

    @Size(max = 80)
    @Column(length = 80)
    private String categoryLabel;

    @Size(max = 255)
    @Column(length = 255)
    private String brochureUrl;

    @Size(max = 255)
    @Column(length = 255)
    private String altBrochureUrl;

    @Size(max = 255)
    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalizeDisplayFields();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeDisplayFields();
    }

    private void normalizeDisplayFields() {
        if (this.iconClass == null || this.iconClass.isBlank()) {
            this.iconClass = "fa-shield-halved";
        } else {
            this.iconClass = this.iconClass.replace("fa-solid", "").trim();
        }
        if (this.accentClass == null || this.accentClass.isBlank()) {
            this.accentClass = "bg-yellow-400/10 text-yellow-300";
        }
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }

    public String getAccentClass() { return accentClass; }
    public void setAccentClass(String accentClass) { this.accentClass = accentClass; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    public String getBrochureUrl() { return brochureUrl; }
    public void setBrochureUrl(String brochureUrl) { this.brochureUrl = brochureUrl; }

    public String getAltBrochureUrl() { return altBrochureUrl; }
    public void setAltBrochureUrl(String altBrochureUrl) { this.altBrochureUrl = altBrochureUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
