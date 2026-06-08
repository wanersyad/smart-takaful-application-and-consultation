package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Quotation;
import com.muqmeen.takaful.service.ApplicationService;
import com.muqmeen.takaful.service.QuotationService;
import com.muqmeen.takaful.service.QuotationService.QuotationItemInput;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminApplicationController {

    private final ApplicationService applicationService;
    private final QuotationService quotationService;

    public AdminApplicationController(ApplicationService applicationService, QuotationService quotationService) {
        this.applicationService = applicationService;
        this.quotationService = quotationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("applications", applicationService.listForAdmin());
        model.addAttribute("statuses", ApplicationStatus.values());
        return "admin/dashboard";
    }

    @GetMapping("/applications/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ConsultationApplication application = applicationService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        model.addAttribute("application", application);
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("quotation", quotationService.getOrCreate(application));
        return "admin/application-detail";
    }

    @PostMapping("/applications/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ApplicationStatus status,
                               RedirectAttributes redirectAttributes) {
        applicationService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("flashMessage", "Application status updated.");
        return "redirect:/admin/applications/" + id;
    }

    @GetMapping("/applications/{id}/quotation")
    public String quotationForm(@PathVariable Long id, Model model) {
        ConsultationApplication application = applicationService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Quotation quotation = quotationService.getOrCreate(application);
        model.addAttribute("application", application);
        model.addAttribute("quotation", quotation);
        model.addAttribute("form", QuotationForm.from(quotation));
        return "admin/quotation-form";
    }

    @PostMapping("/applications/{id}/quotation")
    public String saveQuotation(@PathVariable Long id,
                                @ModelAttribute QuotationForm form,
                                RedirectAttributes redirectAttributes) {
        ConsultationApplication application = applicationService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        quotationService.save(application, form.adminNotes, form.toItems());
        redirectAttributes.addFlashAttribute("flashMessage", "Quotation saved.");
        return "redirect:/admin/applications/" + id + "/quotation";
    }

    @PostMapping("/quotations/{id}/publish")
    public String publishQuotation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Quotation quotation = quotationService.publish(id);
        redirectAttributes.addFlashAttribute("flashMessage", "Quotation published to customer.");
        return "redirect:/admin/applications/" + quotation.getApplication().getId();
    }

    public static class QuotationForm {
        public String adminNotes;
        public String itemsText;

        static QuotationForm from(Quotation quotation) {
            QuotationForm form = new QuotationForm();
            form.adminNotes = quotation.getAdminNotes();
            form.itemsText = quotation.getItems().stream()
                    .map(item -> item.getItemName() + " | "
                            + (item.getDescription() == null ? "" : item.getDescription()) + " | "
                            + (item.getAmount() == null ? "0.00" : item.getAmount()) + " | "
                            + (item.isSelected() ? "yes" : "no"))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            return form;
        }

        List<QuotationItemInput> toItems() {
            List<QuotationItemInput> rows = new ArrayList<>();
            if (itemsText == null || itemsText.isBlank()) return rows;
            for (String line : itemsText.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) continue;
                String[] parts = trimmed.split("\\|", 4);
                rows.add(new QuotationItemInput(
                        parts[0].trim(),
                        parts.length > 1 ? parts[1].trim() : "",
                        parts.length > 2 ? parseAmount(parts[2].trim()) : BigDecimal.ZERO,
                        parts.length <= 3 || !"no".equalsIgnoreCase(parts[3].trim())
                ));
            }
            return rows;
        }

        private BigDecimal parseAmount(String raw) {
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException ex) {
                return BigDecimal.ZERO;
            }
        }

        public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
        public void setItemsText(String itemsText) { this.itemsText = itemsText; }
    }
}
