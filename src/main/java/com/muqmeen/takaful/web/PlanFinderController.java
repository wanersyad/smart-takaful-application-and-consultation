package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.PlanFinderResponse;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.PlanFinderService;
import com.muqmeen.takaful.service.PlanFinderService.ResultView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Public Plan Finder: questionnaire -> recommended products -> apply or request a consultation.
 * No login needed; the result is linked to the customer's application or consultation later.
 */
@Controller
@RequestMapping("/plan-finder")
public class PlanFinderController {

    /** Session key that remembers the visitor's latest result across login/registration. */
    public static final String SESSION_TOKEN = "planFinderToken";

    private final PlanFinderService planFinderService;
    private final CustomerService customerService;
    private final IpRateLimiter rateLimiter = new IpRateLimiter(5, Duration.ofMinutes(10));

    public PlanFinderController(PlanFinderService planFinderService, CustomerService customerService) {
        this.planFinderService = planFinderService;
        this.customerService = customerService;
    }

    @GetMapping
    public String form(@RequestParam(value = "lang", required = false) String lang, Model model) {
        model.addAttribute("lang", "ms".equals(lang) ? "ms" : "en");
        model.addAttribute("questions", planFinderService.questions());
        return "plan_finder";
    }

    @PostMapping
    public String submit(@ModelAttribute PlanFinderForm form,
                         Authentication authentication,
                         HttpServletRequest request,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        String lang = "ms".equals(form.lang) ? "ms" : "en";
        if (!rateLimiter.allow(request)) {
            redirectAttributes.addFlashAttribute("planFinderError", "ms".equals(lang)
                    ? "Terlalu banyak percubaan. Sila cuba lagi dalam beberapa minit."
                    : "Too many attempts. Please try again in a few minutes.");
            return "redirect:/plan-finder?lang=" + lang;
        }
        Customer customer = customerService.currentCustomer(authentication).orElse(null);
        try {
            PlanFinderResponse response = planFinderService.submit(new PlanFinderService.Answers(
                    lang, form.ageBand, form.maritalStatus, form.childrenBand, form.goals,
                    form.existingCoverage, form.budgetBand, form.occupationClass, form.healthCondition), customer);
            session.setAttribute(SESSION_TOKEN, response.getToken());
            return "redirect:/plan-finder/result/" + response.getToken();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("planFinderError", ex.getMessage());
            return "redirect:/plan-finder?lang=" + lang;
        }
    }

    @GetMapping("/result/{token}")
    public String result(@PathVariable String token, Authentication authentication, HttpSession session, Model model) {
        return planFinderService.findByToken(token)
                .map(response -> {
                    session.setAttribute(SESSION_TOKEN, response.getToken());
                    ResultView view = planFinderService.resultView(response);
                    Customer customer = customerService.currentCustomer(authentication).orElse(null);
                    model.addAttribute("result", view);
                    model.addAttribute("lang", view.language());
                    model.addAttribute("token", response.getToken());
                    model.addAttribute("consultTopic", consultTopic(view));
                    model.addAttribute("consultMessage", consultMessage(view));
                    model.addAttribute("prefillName", customer == null ? "" : customer.getFullName());
                    model.addAttribute("prefillEmail", customer == null ? "" : customer.getEmail());
                    model.addAttribute("prefillPhone", customer == null ? "" : customer.getPhoneNumber());
                    return "plan_finder_result";
                })
                .orElse("redirect:/plan-finder");
    }

    private static String consultTopic(ResultView view) {
        String first = view.products().isEmpty() ? "" : ": " + view.products().get(0).product().getName();
        String topic = "Plan Finder" + first;
        return topic.length() <= 80 ? topic : topic.substring(0, 80);
    }

    private static String consultMessage(ResultView view) {
        String names = view.products().stream().map(p -> p.product().getName()).collect(Collectors.joining(", "));
        if ("ms".equals(view.language())) {
            return names.isEmpty()
                    ? "Saya telah melengkapkan Plan Finder dan ingin berbincang tentang pelan yang sesuai untuk saya."
                    : "Saya telah melengkapkan Plan Finder. Pelan yang dicadangkan: " + names + ". Saya ingin berbincang pelan mana yang paling sesuai untuk saya.";
        }
        return names.isEmpty()
                ? "I completed the Plan Finder and would like to discuss which plan suits me."
                : "I completed the Plan Finder. Suggested plans: " + names + ". I'd like to discuss which one suits me best.";
    }

    public static class PlanFinderForm {
        private String lang;
        private String ageBand;
        private String maritalStatus;
        private String childrenBand;
        private List<String> goals;
        private String existingCoverage;
        private String budgetBand;
        private String occupationClass;
        private String healthCondition;

        public String getLang() { return lang; }
        public void setLang(String lang) { this.lang = lang; }

        public String getAgeBand() { return ageBand; }
        public void setAgeBand(String ageBand) { this.ageBand = ageBand; }

        public String getMaritalStatus() { return maritalStatus; }
        public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

        public String getChildrenBand() { return childrenBand; }
        public void setChildrenBand(String childrenBand) { this.childrenBand = childrenBand; }

        public List<String> getGoals() { return goals; }
        public void setGoals(List<String> goals) { this.goals = goals; }

        public String getExistingCoverage() { return existingCoverage; }
        public void setExistingCoverage(String existingCoverage) { this.existingCoverage = existingCoverage; }

        public String getBudgetBand() { return budgetBand; }
        public void setBudgetBand(String budgetBand) { this.budgetBand = budgetBand; }

        public String getOccupationClass() { return occupationClass; }
        public void setOccupationClass(String occupationClass) { this.occupationClass = occupationClass; }

        public String getHealthCondition() { return healthCondition; }
        public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }
    }
}
