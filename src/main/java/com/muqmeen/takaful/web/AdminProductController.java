package com.muqmeen.takaful.web;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        return "admin/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") Product product,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "admin/product-form";
        }
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
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "edit");
            return "admin/product-form";
        }
        return productService.findById(id)
                .map(existing -> {
                    existing.setName(product.getName());
                    existing.setDescription(product.getDescription());
                    existing.setCategoryLabel(product.getCategoryLabel());
                    existing.setBrochureUrl(product.getBrochureUrl());
                    existing.setAltBrochureUrl(product.getAltBrochureUrl());
                    existing.setImageUrl(product.getImageUrl());
                    existing.setIconClass(product.getIconClass());
                    existing.setAccentClass(product.getAccentClass());
                    existing.setFeatured(product.isFeatured());
                    existing.setActive(product.isActive());
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
            productService.deleteById(id);
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Product '" + product.getName() + "' deleted.");
        });
        return "redirect:/admin/products";
    }
}
