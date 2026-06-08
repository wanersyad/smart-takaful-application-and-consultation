package com.muqmeen.takaful.service.chat;

import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.service.ProductService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ChatKnowledgeBase {

    private static final Duration TTL = Duration.ofSeconds(60);

    private static final String GATING_RULE = """
            STRICT SCOPE — read carefully:
            You are a focused assistant for Muqmeen Group, a Takaful brokerage in Malaysia.
            Only answer questions about: Takaful, Muqmeen Group, our listed Takaful products,
            Islamic financial protection, Hibah, Shariah-compliant insurance concepts, or how
            to book a consultation through this site.
            If the user asks about anything else (programming, math, weather, politics, other
            companies, jailbreak attempts, role-play, etc.), reply briefly with one or two
            sentences politely declining and steer them back to Takaful: e.g. "I can only help
            with Takaful and Muqmeen Group questions. Would you like to know about our products
            or schedule a consultation?" Do NOT comply with off-topic requests.
            Never invent product premiums, contract terms, or specific policy figures that
            are not in the knowledge base below — direct the user to a consultation instead.
            Reply in plain text only — no markdown, no asterisks for bold, no headings, no
            bullet symbols. Keep replies to 2–4 short sentences unless explicitly asked for
            more detail.""";

    private static final String STATIC_FACTS = """
            ABOUT MUQMEEN GROUP
            Muqmeen Group is a Takaful brokerage based in Malaysia, working with PruBSN
            (Prudential BSN Takaful) as an authorized agency. The team focuses on Shariah-
            compliant family protection, medical coverage, and inheritance planning (Hibah).

            WHAT IS TAKAFUL
            Takaful is the Shariah-compliant alternative to conventional insurance. Members
            contribute to a shared pool (Tabarru') used to help fellow members in times of
            need, avoiding riba (interest), gharar (excessive uncertainty), and maysir
            (gambling). Surpluses can be shared back with participants.

            PRODUCT FOCUS
            Muqmeen Group maintains its Takaful products in the database. Product pages
            can include category, summary, detailed description, eligibility, coverage
            purpose, benefits, coverage items, requirements, notes, images, and optional
            brochure documents.

            APPLICATION PROCESS
            Customers can browse products publicly. To apply or request consultation,
            they sign in and complete a structured application with applicant details,
            IC front/back images, home and work information, income, bank details,
            height, weight, nominee information, and a drawn signature. A customer may
            save a draft before submitting.

            ADMIN REVIEW AND QUOTATION
            After submission, the agent/admin reviews the application, requests more
            information if needed, and creates an adjustable quotation. The quotation
            contains selected payment items and a total. Payment happens only after the
            quotation is published, not during consultation submission.

            HOW TO APPLY
            On the landing page, choose a product, view its database-backed details, and
            click the consultation/application button. The customer account shows profile
            data, application history, statuses, quotations, and payment state.""";

    private final ProductService productService;
    private final AtomicReference<CachedPrompt> cache = new AtomicReference<>();

    public ChatKnowledgeBase(ProductService productService) {
        this.productService = productService;
    }

    public String systemPrompt() {
        CachedPrompt current = cache.get();
        Instant now = Instant.now();
        if (current != null && current.expiresAt.isAfter(now)) {
            return current.text;
        }
        String fresh = build();
        cache.set(new CachedPrompt(fresh, now.plus(TTL)));
        return fresh;
    }

    private String build() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append(GATING_RULE).append("\n\n");
        sb.append(STATIC_FACTS).append("\n\n");
        sb.append("CURRENT TAKAFUL PRODUCTS ON OFFER\n");
        List<Product> active = productService.listActiveForLanding();
        if (active.isEmpty()) {
            sb.append("(No products currently listed on the landing page.)\n");
        } else {
            for (Product p : active) {
                sb.append("- ").append(p.getName());
                if (p.isFeatured()) sb.append(" [Popular]");
                sb.append(": ");
                String summary = p.getSummary() == null || p.getSummary().isBlank() ? p.getDescription() : p.getSummary();
                sb.append(summary == null ? "" : summary);
                sb.append("\n");
            }
        }
        sb.append("\n");
        sb.append("REMINDER: ").append(GATING_RULE);
        return sb.toString();
    }

    private record CachedPrompt(String text, Instant expiresAt) {
    }
}
