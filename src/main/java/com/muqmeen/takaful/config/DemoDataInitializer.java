package com.muqmeen.takaful.config;

import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.domain.ProductBenefit;
import com.muqmeen.takaful.domain.ProductCoverageItem;
import com.muqmeen.takaful.domain.ProductDocument;
import com.muqmeen.takaful.domain.ProductRequirement;
import com.muqmeen.takaful.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "demo-seed", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;

    public DemoDataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }
        productRepository.save(sampleProduct(
                "PruBSN AnugerahMax",
                "Family Protection",
                "Long-term family protection with legacy planning support.",
                "Helps families prepare financial continuity, basic protection, and structured legacy planning before a full agent review.",
                "fa-hand-holding-heart",
                List.of("Family-focused protection planning", "Suitable for legacy and continuity conversations", "Admin can adjust quoted payable items after review"),
                List.of(
                        coverage("Protection review", "Agent reviews coverage needs before preparing quotation items."),
                        coverage("Legacy planning support", "Structured details help the agent decide suitable plan components.")
                ),
                List.of("Completed application form", "IC front and back images", "Nominee information", "Applicant signature"),
                List.of(document("Product brochure", "/brochures/anugerahmax.pdf", "PDF"))
        ));
        productRepository.save(sampleProduct(
                "PruBSN Kritikal Care360",
                "Critical Illness",
                "Critical illness protection for recovery and income continuity planning.",
                "Supports a structured review of recovery needs, income continuity concerns, eligibility notes, and required application documents.",
                "fa-heart-pulse",
                List.of("Critical illness planning discussion", "Recovery support focus", "Post-review quotation payment flow"),
                List.of(
                        coverage("Critical illness review", "Supports an agent-led review of illness protection needs."),
                        coverage("Income continuity concern", "Useful for customers worried about loss of income during recovery.")
                ),
                List.of("Medical and occupation details", "Annual income", "Height and weight", "Bank details for claims"),
                List.of(document("English brochure", "/brochures/kritikal-care360.pdf", "PDF"))
        ));
    }

    private Product sampleProduct(String name,
                                  String category,
                                  String summary,
                                  String detail,
                                  String iconClass,
                                  List<String> benefits,
                                  List<ProductCoverageItem> coverageItems,
                                  List<String> requirements,
                                  List<ProductDocument> documents) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryLabel(category);
        product.setSummary(summary);
        product.setDescription(summary);
        product.setDetailedDescription(detail);
        product.setEligibility("Subject to customer age, health, occupation, and underwriting review.");
        product.setCoveragePurpose("The agent reviews the customer profile and application snapshot before creating a suitable quotation.");
        product.setTermsNotes("Final eligibility, contribution amount, and selectable coverage items are confirmed by the agent after application review.");
        product.setIconClass(iconClass);
        product.setActive(true);
        product.setFeatured(true);
        product.replaceBenefits(benefits.stream().map(DemoDataInitializer::benefit).toList());
        product.replaceCoverageItems(coverageItems);
        product.replaceRequirements(requirements.stream().map(DemoDataInitializer::requirement).toList());
        product.replaceDocuments(documents);
        return product;
    }

    private static ProductBenefit benefit(String text) {
        ProductBenefit benefit = new ProductBenefit();
        benefit.setBenefitText(text);
        return benefit;
    }

    private static ProductCoverageItem coverage(String name, String description) {
        ProductCoverageItem item = new ProductCoverageItem();
        item.setItemName(name);
        item.setItemDescription(description);
        return item;
    }

    private static ProductRequirement requirement(String text) {
        ProductRequirement requirement = new ProductRequirement();
        requirement.setRequirementText(text);
        return requirement;
    }

    private static ProductDocument document(String label, String url, String type) {
        ProductDocument document = new ProductDocument();
        document.setLabel(label);
        document.setUrl(url);
        document.setDocumentType(type);
        return document;
    }
}
