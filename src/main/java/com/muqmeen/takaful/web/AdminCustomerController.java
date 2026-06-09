package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.CustomerProfile;
import com.muqmeen.takaful.service.ApplicationService;
import com.muqmeen.takaful.service.CustomerProfileService;
import com.muqmeen.takaful.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final CustomerProfileService customerProfileService;
    private final ApplicationService applicationService;

    public AdminCustomerController(CustomerService customerService,
                                   CustomerProfileService customerProfileService,
                                   ApplicationService applicationService) {
        this.customerService = customerService;
        this.customerProfileService = customerProfileService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Customer> customers = customerService.listForAdmin(query);
        model.addAttribute("customers", customers);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("activeCustomers", customers.stream().filter(Customer::isActive).count());
        model.addAttribute("disabledCustomers", customers.stream().filter(customer -> !customer.isActive()).count());
        return "admin/customers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return customerService.findById(id)
                .map(customer -> {
                    CustomerProfile profile = customerProfileService.getOrCreate(customer);
                    List<ConsultationApplication> applications = applicationService.listForCustomer(customer);
                    model.addAttribute("customer", customer);
                    model.addAttribute("profile", profile);
                    model.addAttribute("applications", applications);
                    model.addAttribute("totalApplications", applications.size());
                    model.addAttribute("submittedApplications", applications.stream()
                            .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.UNDER_REVIEW)
                            .count());
                    model.addAttribute("quotedApplications", applications.stream()
                            .filter(app -> app.getStatus() == ApplicationStatus.QUOTED || app.getStatus() == ApplicationStatus.PAYMENT_PENDING)
                            .count());
                    model.addAttribute("paidApplications", applications.stream()
                            .filter(app -> app.getStatus() == ApplicationStatus.PAID)
                            .count());
                    return "admin/customer-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("flashMessage", "Customer #" + id + " no longer exists.");
                    return "redirect:/admin/customers";
                });
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("active") boolean active,
                               RedirectAttributes redirectAttributes) {
        customerService.findById(id).ifPresent(customer -> {
            customerService.setActive(customer, active);
            redirectAttributes.addFlashAttribute("flashMessage",
                    customer.getFullName() + (active ? " can now sign in." : " has been disabled."));
        });
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String name = customerService.findById(id).map(Customer::getFullName).orElse("Customer");
        customerService.deleteCustomer(id);
        redirectAttributes.addFlashAttribute("flashMessage",
                name + " and all related applications, files, and quotations were deleted.");
        return "redirect:/admin/customers";
    }
}
