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
        if (productService.count() > 0) return;

        log.info("Products table is empty — seeding the 3 default Takaful products.");

        List<Product> defaults = List.of(
                build("PruBSN Medical",
                        "Comprehensive medical coverage with unlimited lifetime limit and cashless hospital admission.",
                        "fa-heart-pulse",
                        "bg-yellow-100 text-zinc-950",
                        false),
                build("Hibah Al-Wasiyyah",
                        "Absolute gift to your loved ones. Ensure they are financially protected without inheritance complexities.",
                        "fa-hand-holding-dollar",
                        "bg-zinc-950 text-yellow-300",
                        true),
                build("PruBSN EduSmart",
                        "Secure your child's education fund early with guaranteed savings and high potential returns.",
                        "fa-user-graduate",
                        "bg-amber-100 text-zinc-950",
                        false)
        );

        defaults.forEach(productService::save);
    }

    private Product build(String name, String description, String iconClass, String accentClass, boolean featured) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setIconClass(iconClass);
        p.setAccentClass(accentClass);
        p.setFeatured(featured);
        p.setActive(true);
        return p;
    }
}
