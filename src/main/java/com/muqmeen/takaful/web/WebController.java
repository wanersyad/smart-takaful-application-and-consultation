package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.service.ProductService;
import com.muqmeen.takaful.service.TakafulService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class WebController {

    private final TakafulService takafulService;
    private final ProductService productService;

    public WebController(TakafulService takafulService, ProductService productService) {
        this.takafulService = takafulService;
        this.productService = productService;
    }

    @GetMapping("/")
    public String landingPage(Model model) {
        if (!model.containsAttribute("lead")) {
            model.addAttribute("lead", new Lead());
        }
        model.addAttribute("products", productService.listActiveForLanding());
        return "index";
    }

    @PostMapping("/submit-lead")
    public String submitLead(@Valid @ModelAttribute("lead") Lead lead,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", true);
            model.addAttribute("products", productService.listActiveForLanding());
            return "index";
        }

        Lead savedLead = takafulService.processNewLead(lead);

        if (savedLead.getBillCode() != null) {
            return "redirect:/payment/mock/" + savedLead.getBillCode();
        }
        return "redirect:/success?leadId=" + savedLead.getId();
    }

    @GetMapping("/payment/mock/{billCode}")
    public String mockPaymentPage(@PathVariable String billCode, Model model) {
        model.addAttribute("billCode", billCode);
        return "payment_mock";
    }

    @GetMapping("/payment/callback")
    public String paymentCallback(@RequestParam("billcode") String billCode,
                                  @RequestParam("status_id") String statusId) {
        if ("1".equals(statusId)) {
            takafulService.updatePaymentStatus(billCode, "PAID");
        }
        return takafulService.findLeadByBillCode(billCode)
                .map(lead -> "redirect:/success?leadId=" + lead.getId())
                .orElse("redirect:/success");
    }

    @GetMapping("/success")
    public String successPage(@RequestParam(value = "leadId", required = false) Long leadId,
                              Model model) {
        if (leadId != null) {
            takafulService.findLead(leadId).ifPresent(lead -> model.addAttribute("lead", lead));
        }
        return "success";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        List<Lead> leads = takafulService.getAllLeadsForAdmin();
        model.addAttribute("leads", leads);

        BigDecimal totalTips = leads.stream()
                .filter(l -> "PAID".equals(l.getPaymentStatus()))
                .map(Lead::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalTips", totalTips);
        return "admin/dashboard";
    }

    @PostMapping("/admin/lead/{id}/status")
    @ResponseBody
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        takafulService.updateLeadStatus(id, status);
        return "OK";
    }
}
