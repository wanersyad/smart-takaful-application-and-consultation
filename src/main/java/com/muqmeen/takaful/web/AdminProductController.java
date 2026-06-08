package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.ProductBenefit;
import com.muqmeen.takaful.domain.ProductCoverageItem;
import com.muqmeen.takaful.domain.ProductDocument;
import com.muqmeen.takaful.domain.ProductRequirement;
import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.listAllForAdmin());
        return "admin/products";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        Product blank = new Product();
        blank.setActive(true);
        model.addAttribute("product", blank);
        model.addAttribute("formMode", "create");
        addStructuredText(model, "", "", "", "");
        return "admin/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") Product product,
                         BindingResult bindingResult,
                         @RequestParam(value = "benefitsText", required = false) String benefitsText,
                         @RequestParam(value = "coverageText", required = false) String coverageText,
                         @RequestParam(value = "requirementsText", required = false) String requirementsText,
                         @RequestParam(value = "documentsText", required = false) String documentsText,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            addStructuredText(model, benefitsText, coverageText, requirementsText, documentsText);
            return "admin/product-form";
        }
        applyStructuredRows(product, benefitsText, coverageText, requirementsText, documentsText);
        Product saved = productService.save(product);
        redirectAttributes.addFlashAttribute("flashMessage",
                "Product '" + saved.getName() + "' created.");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return productService.findById(id)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("formMode", "edit");
                    model.addAttribute("benefitsText", joinBenefits(product));
                    model.addAttribute("coverageText", joinCoverage(product));
                    model.addAttribute("requirementsText", joinRequirements(product));
                    model.addAttribute("documentsText", joinDocuments(product));
                    return "admin/product-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("flashMessage",
                            "Product #" + id + " no longer exists.");
                    return "redirect:/admin/products";
                });
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") Product product,
                         BindingResult bindingResult,
                         @RequestParam(value = "benefitsText", required = false) String benefitsText,
                         @RequestParam(value = "coverageText", required = false) String coverageText,
                         @RequestParam(value = "requirementsText", required = false) String requirementsText,
                         @RequestParam(value = "documentsText", required = false) String documentsText,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "edit");
            addStructuredText(model, benefitsText, coverageText, requirementsText, documentsText);
            return "admin/product-form";
        }
        return productService.findById(id)
                .map(existing -> {
                    existing.setName(product.getName());
                    existing.setDescription(product.getDescription());
                    existing.setSummary(product.getSummary());
                    existing.setDetailedDescription(product.getDetailedDescription());
                    existing.setEligibility(product.getEligibility());
                    existing.setCoveragePurpose(product.getCoveragePurpose());
                    existing.setTermsNotes(product.getTermsNotes());
                    existing.setCategoryLabel(product.getCategoryLabel());
                    existing.setBrochureUrl(product.getBrochureUrl());
                    existing.setAltBrochureUrl(product.getAltBrochureUrl());
                    existing.setImageUrl(product.getImageUrl());
                    existing.setIconClass(product.getIconClass());
                    existing.setAccentClass(product.getAccentClass());
                    existing.setFeatured(product.isFeatured());
                    existing.setActive(product.isActive());
                    applyStructuredRows(existing, benefitsText, coverageText, requirementsText, documentsText);
                    Product saved = productService.save(existing);
                    redirectAttributes.addFlashAttribute("flashMessage",
                            "Product '" + saved.getName() + "' updated.");
                    return "redirect:/admin/products";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("flashMessage",
                            "Product #" + id + " no longer exists.");
                    return "redirect:/admin/products";
                });
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.findById(id).ifPresent(product -> {
            productService.archiveById(id);
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Product '" + product.getName() + "' archived.");
        });
        return "redirect:/admin/products";
    }

    private void addStructuredText(Model model, String benefitsText, String coverageText, String requirementsText, String documentsText) {
        model.addAttribute("benefitsText", benefitsText);
        model.addAttribute("coverageText", coverageText);
        model.addAttribute("requirementsText", requirementsText);
        model.addAttribute("documentsText", documentsText);
    }

    private void applyStructuredRows(Product product, String benefitsText, String coverageText, String requirementsText, String documentsText) {
        product.replaceBenefits(parseLines(benefitsText).stream().map(line -> {
            ProductBenefit benefit = new ProductBenefit();
            benefit.setBenefitText(line.text());
            benefit.setDisplayOrder(line.order());
            return benefit;
        }).toList());

        product.replaceCoverageItems(parseLines(coverageText).stream().map(line -> {
            String[] parts = line.text().split("\\|", 2);
            ProductCoverageItem item = new ProductCoverageItem();
            item.setItemName(parts[0].trim());
            item.setItemDescription(parts.length > 1 ? parts[1].trim() : "");
            item.setDisplayOrder(line.order());
            return item;
        }).toList());

        product.replaceRequirements(parseLines(requirementsText).stream().map(line -> {
            ProductRequirement requirement = new ProductRequirement();
            requirement.setRequirementText(line.text());
            requirement.setDisplayOrder(line.order());
            return requirement;
        }).toList());

        product.replaceDocuments(parseLines(documentsText).stream().map(line -> {
            String[] parts = line.text().split("\\|", 3);
            ProductDocument document = new ProductDocument();
            document.setLabel(parts[0].trim());
            document.setUrl(parts.length > 1 ? parts[1].trim() : "");
            document.setDocumentType(parts.length > 2 ? parts[2].trim() : "Brochure");
            document.setDisplayOrder(line.order());
            return document;
        }).filter(document -> !document.getUrl().isBlank()).toList());
    }

    private List<StructuredLine> parseLines(String raw) {
        List<StructuredLine> rows = new ArrayList<>();
        if (raw == null || raw.isBlank()) return rows;
        String[] lines = raw.split("\\R");
        int order = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                rows.add(new StructuredLine(trimmed, order++));
            }
        }
        return rows;
    }

    private String joinBenefits(Product product) {
        return product.getBenefits().stream()
                .map(ProductBenefit::getBenefitText)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String joinCoverage(Product product) {
        return product.getCoverageItems().stream()
                .map(item -> item.getItemName() + (item.getItemDescription() == null || item.getItemDescription().isBlank() ? "" : " | " + item.getItemDescription()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String joinRequirements(Product product) {
        return product.getRequirements().stream()
                .map(ProductRequirement::getRequirementText)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String joinDocuments(Product product) {
        return product.getDocuments().stream()
                .map(document -> document.getLabel() + " | " + document.getUrl() + (document.getDocumentType() == null || document.getDocumentType().isBlank() ? "" : " | " + document.getDocumentType()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private record StructuredLine(String text, int order) {}
}
