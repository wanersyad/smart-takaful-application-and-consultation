package com.muqmeen.takaful.web;

import com.muqmeen.takaful.service.SiteContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/content")
public class AdminContentController {

    private final SiteContentService siteContentService;

    public AdminContentController(SiteContentService siteContentService) {
        this.siteContentService = siteContentService;
    }

    @GetMapping
    public String edit(Model model) {
        model.addAttribute("blocks", siteContentService.listForAdmin());
        return "admin/content";
    }

    @PostMapping
    public String update(@RequestParam Map<String, String> params,
                         RedirectAttributes redirectAttributes) {
        Map<String, String> contentValues = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("content_")) {
                contentValues.put(key.substring("content_".length()), value);
            }
        });
        siteContentService.saveContent(contentValues);
        redirectAttributes.addFlashAttribute("flashMessage", "Site content updated.");
        return "redirect:/admin/content";
    }
}
