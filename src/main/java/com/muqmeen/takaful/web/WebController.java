package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.Payment;
import com.muqmeen.takaful.domain.Quotation;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.LandingMetricsService;
import com.muqmeen.takaful.service.PaymentService;
import com.muqmeen.takaful.service.ProductService;
import com.muqmeen.takaful.service.QuotationService;
import com.muqmeen.takaful.service.SiteContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
public class WebController {

    private final ProductService productService;
    private final CustomerService customerService;
    private final PaymentService paymentService;
    private final QuotationService quotationService;
    private final LandingMetricsService landingMetricsService;
    private final SiteContentService siteContentService;

    public WebController(ProductService productService,
                         CustomerService customerService,
                         PaymentService paymentService,
                         QuotationService quotationService,
                         LandingMetricsService landingMetricsService,
                         SiteContentService siteContentService) {
        this.productService = productService;
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.quotationService = quotationService;
        this.landingMetricsService = landingMetricsService;
        this.siteContentService = siteContentService;
    }

    @GetMapping("/")
    public String landingPage(Authentication authentication, Model model) {
        Optional<Customer> customer = customerService.currentCustomer(authentication);
        SiteContentService.LandingContent landingContent = siteContentService.landingContent();
        model.addAttribute("products", productService.listActiveForLanding());
        model.addAttribute("metrics", landingMetricsService.current());
        model.addAttribute("content", landingContent);
        model.addAttribute("questionPrompts", landingContent.questionPrompts());
        model.addAttribute("faqItems", landingContent.faqItems());
        model.addAttribute("customerSignedIn", customer.isPresent());
        customer.ifPresent(c -> model.addAttribute("currentCustomer", c));
        return "index";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = customerService.currentCustomer(authentication).orElse(null);
        return productService.findActiveById(id)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("customerSignedIn", customer != null);
                    return "product_detail";
                })
                .orElse("redirect:/#products");
    }

    @GetMapping("/payment/mock/{billCode}")
    public String mockPaymentPage(@PathVariable String billCode, Model model) {
        model.addAttribute("billCode", billCode);
        return "payment_mock";
    }

    @GetMapping("/payment/callback")
    public String paymentCallback(@RequestParam("billcode") String billCode,
                                  @RequestParam("status_id") String statusId) {
        Payment payment = paymentService.updateMockStatus(billCode, statusId);
        if (payment.getQuotation() != null) {
            return "redirect:/applications/" + payment.getQuotation().getApplication().getId();
        }
        return "redirect:/account";
    }

    @PostMapping("/payment/callback")
    @ResponseBody
    public ResponseEntity<String> toyyibPayCallback(@RequestParam Map<String, String> params) {
        return paymentService.processCallback(params)
                ? ResponseEntity.ok("OK")
                : ResponseEntity.badRequest().body("INVALID");
    }

    @GetMapping("/payment/return")
    public String paymentReturn(@RequestParam(value = "billcode", required = false) String billCode,
                                @RequestParam(value = "status_id", required = false) String statusId,
                                Authentication authentication,
                                Model model) {
        model.addAttribute("billCode", billCode);
        model.addAttribute("statusId", statusId);
        if (billCode != null) {
            Optional<Customer> customer = customerService.currentCustomer(authentication);
            paymentService.findByBillCode(billCode)
                    .filter(payment -> customer.isPresent()
                            && payment.getCustomer() != null
                            && payment.getCustomer().getId().equals(customer.get().getId()))
                    .ifPresent(payment -> model.addAttribute("payment", payment));
        }
        return "payment_return";
    }

    @PostMapping("/quotations/{id}/pay")
    public String payQuotation(@PathVariable Long id, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Customer customer = customerService.currentCustomer(authentication)
                .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
        Optional<Quotation> owned = quotationService.findById(id)
                .filter(quotation -> quotation.getApplication().getCustomer().getId().equals(customer.getId()));
        if (owned.isEmpty()) {
            return "redirect:/account";
        }
        try {
            String redirectUrl = paymentService.prepareQuotationPayment(owned.get()).redirectUrl();
            return "redirect:" + redirectUrl;
        } catch (IllegalStateException ex) {
            // e.g. a zero-amount quotation that cannot be paid — show the reason instead of a 500.
            redirectAttributes.addFlashAttribute("paymentError", ex.getMessage());
            return "redirect:/applications/" + owned.get().getApplication().getId();
        }
    }

    @GetMapping("/success")
    public String successPage() {
        return "success";
    }

    @GetMapping("/admin")
    public String adminEntry() {
        return "redirect:/admin/dashboard";
    }
}
