package com.muqmeen.takaful.config;

import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);

    private final ProductService productService;

    public ProductSeeder(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void run(String... args) {
        log.info("Syncing landing product catalogue from official brochure assets.");

        List<Product> brochureProducts = List.of(
                build("PruBSN AnugerahMax",
                        "Family takaful protection focused on long-term legacy planning, protection value, and flexible brochure review.",
                        "Family Protection",
                        "fa-shield-halved",
                        "bg-yellow-400/10 text-yellow-300",
                        "/brochures/anugerahmax-bm.pdf",
                        "/brochures/anugerahmax-en.pdf",
                        "/images/products/anugerahmax.svg",
                        true),
                build("PruBSN Anggun",
                        "A women-focused takaful plan for protection, savings discipline, and life-stage planning support.",
                        "Women & Family",
                        "fa-person-dress",
                        "bg-yellow-400/10 text-yellow-300",
                        "/brochures/anggun-bm.pdf",
                        "/brochures/anggun-en.pdf",
                        "/images/products/anggun.svg",
                        false),
                build("PruBSN Asas360",
                        "Essential protection built for straightforward coverage conversations and a practical first review.",
                        "Essential Cover",
                        "fa-circle-check",
                        "bg-yellow-400/10 text-yellow-300",
                        "/brochures/asas360-en.pdf",
                        null,
                        "/images/products/asas360.svg",
                        false),
                build("PruBSN Kritikal Care360",
                        "Critical illness focused protection for income continuity, recovery support, and family preparedness.",
                        "Critical Illness",
                        "fa-heart-pulse",
                        "bg-yellow-400/10 text-yellow-300",
                        "/brochures/kritikal-care360-en.pdf",
                        null,
                        "/images/products/kritikal-care360.svg",
                        false),
                build("PruBSN WarisanGold",
                        "Legacy and wealth transfer planning support for families who want clearer inheritance preparation.",
                        "Legacy Planning",
                        "fa-hand-holding-heart",
                        "bg-yellow-400/10 text-yellow-300",
                        "/brochures/warisan-gold-bm.pdf",
                        "/brochures/warisan-gold-en.pdf",
                        "/images/products/warisan-gold.svg",
                        false),
                build("PruBSN Aspirasi",
                        "Savings-led takaful planning for future goals, education conversations, and structured family priorities.",
                        "Savings Goals",
                        "fa-seedling",
                        "bg-yellow-400/10 text-yellow-300",
                        "/brochures/aspirasi-bm.pdf",
                        null,
                        "/images/products/aspirasi.svg",
                        false)
        );

        brochureProducts.forEach(this::insertIfMissing);
    }

    private void insertIfMissing(Product source) {
        if (productService.findByName(source.getName()).isPresent()) {
            return;
        }
        productService.save(source);
    }

    private Product build(String name,
                          String description,
                          String categoryLabel,
                          String iconClass,
                          String accentClass,
                          String brochureUrl,
                          String altBrochureUrl,
                          String imageUrl,
                          boolean featured) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setCategoryLabel(categoryLabel);
        p.setIconClass(iconClass);
        p.setAccentClass(accentClass);
        p.setBrochureUrl(brochureUrl);
        p.setAltBrochureUrl(altBrochureUrl);
        p.setImageUrl(imageUrl);
        p.setFeatured(featured);
        p.setActive(true);
        return p;
    }
}
