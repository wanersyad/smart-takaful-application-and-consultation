package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.ConsultationStatus;
import com.muqmeen.takaful.domain.ContactInquiry;
import com.muqmeen.takaful.service.ContactInquiryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Agent-side management of general consultation requests (people who want to speak to an agent
 * without applying for a specific product). Provides the read/list, status funnel update,
 * agent-notes edit, and delete operations.
 */
@Controller
@RequestMapping("/admin/consultations")
public class AdminConsultationController {

    private final ContactInquiryService consultationService;

    public AdminConsultationController(ContactInquiryService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status, Model model) {
        List<ContactInquiry> all = consultationService.listAll();
        List<ContactInquiry> filtered = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? all
                : all.stream().filter(c -> status.equalsIgnoreCase(c.getStatus())).toList();
        model.addAttribute("consultations", filtered);
        model.addAttribute("statuses", ConsultationStatus.values());
        model.addAttribute("activeStatus", status == null ? "ALL" : status.toUpperCase());
        model.addAttribute("totalCount", all.size());
        return "admin/consultations";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return consultationService.findById(id)
                .map(consultation -> {
                    model.addAttribute("consultation", consultation);
                    model.addAttribute("statuses", ConsultationStatus.values());
                    return "admin/consultation-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("flashMessage", "Consultation not found.");
                    return "redirect:/admin/consultations";
                });
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ConsultationStatus status,
                               RedirectAttributes redirectAttributes) {
        consultationService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("flashMessage", "Status updated to " + status + ".");
        return "redirect:/admin/consultations/" + id;
    }

    @PostMapping("/{id}/notes")
    public String updateNotes(@PathVariable Long id,
                              @RequestParam(value = "agentNotes", required = false) String agentNotes,
                              RedirectAttributes redirectAttributes) {
        consultationService.updateNotes(id, agentNotes);
        redirectAttributes.addFlashAttribute("flashMessage", "Notes saved.");
        return "redirect:/admin/consultations/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        consultationService.delete(id);
        redirectAttributes.addFlashAttribute("flashMessage", "Consultation deleted.");
        return "redirect:/admin/consultations";
    }
}
