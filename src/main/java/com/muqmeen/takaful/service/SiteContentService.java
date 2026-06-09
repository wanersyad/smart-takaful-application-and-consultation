package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.SiteContentBlock;
import com.muqmeen.takaful.repository.SiteContentBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SiteContentService {

    private static final List<ContentDefinition> DEFINITIONS = List.of(
            def("hero.title", "Hero", "Main headline", "textarea", "Your Trusted Takaful Partner"),
            def("hero.subtitle", "Hero", "Hero subtitle", "textarea", "Compare protection, Hibah, medical, and education planning with Muqmeen Group. Apply through a structured, trackable system and let the agent review your real needs."),
            def("hero.questions", "Hero", "Rotating customer questions, one per line", "textarea", """
                    Which plan fits my family needs?
                    Do I need Hibah and medical cover?
                    Should I review my old policy first?
                    How much income protection is enough?
                    What documents should I prepare?"""),
            def("hero.support.value", "Hero", "Support badge value", "text", "24/7"),
            def("hero.support.label", "Hero", "Support badge label", "text", "Guide Support"),
            def("metrics.customers.label", "Metrics", "Customer metric label", "text", "Customers"),
            def("metrics.applications.label", "Metrics", "Application metric label", "text", "Applications"),
            def("metrics.products.label", "Metrics", "Product metric label", "text", "Active Products"),
            def("metrics.quoted.label", "Metrics", "Quotation/payment metric label", "text", "Quoted / Paid"),
            def("products.eyebrow", "Products", "Products eyebrow", "text", "Dynamic Product Records"),
            def("products.title", "Products", "Products heading", "text", "Protection plans built around your family"),
            def("products.subtitle", "Products", "Products intro", "textarea", "Every card below is read from the product database, including details, benefits, requirements, and documents."),
            def("products.empty.title", "Products", "Empty product title", "text", "No products yet"),
            def("products.empty.body", "Products", "Empty product body", "textarea", "An admin must create product records before customers can apply."),
            def("faq.eyebrow", "FAQ", "FAQ eyebrow", "text", "FAQ"),
            def("faq.title", "FAQ", "FAQ heading", "text", "Application flow questions"),
            def("faq.items", "FAQ", "FAQ rows, one question|answer per line", "textarea", """
                    What happens after I submit an application? | The admin reviews your documents, profile snapshot, bank details, measurements, and nominee information. If everything is clear, the admin creates a quotation for you.
                    Do I pay during the application? | No. Payment only happens after the agent reviews your application and publishes a quotation with selected payable items.
                    Can I save a draft? | Yes. You can save an application draft before uploading all documents. Once submitted, it can only be edited again if the admin requests corrections."""),
            def("contact.title", "Contact", "Contact heading", "text", "Get A Free Consultation Now"),
            def("contact.subtitle", "Contact", "Contact intro", "textarea", "Send a focused product question and the Muqmeen team will follow up by email."),
            def("chat.title", "Chat", "Chat title", "text", "Takaful Assistant"),
            def("chat.subtitle", "Chat", "Chat subtitle", "text", "Ask about our products"),
            def("chat.greeting", "Chat", "Chat opening message", "textarea", "Hi, I can explain the application process, product details, and quotation/payment flow."),
            def("chat.placeholder", "Chat", "Chat input placeholder", "text", "Ask about Takaful products")
    );

    private final SiteContentBlockRepository repository;

    public SiteContentService(SiteContentBlockRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<SiteContentBlock> listForAdmin() {
        ensureDefinitionsExist();
        return repository.findAllByOrderByDisplayOrderAscContentKeyAsc();
    }

    @Transactional
    public LandingContent landingContent() {
        ensureDefinitionsExist();
        Map<String, String> values = repository.findAllByOrderByDisplayOrderAscContentKeyAsc().stream()
                .filter(SiteContentBlock::isActive)
                .collect(Collectors.toMap(
                        SiteContentBlock::getContentKey,
                        block -> nullToBlank(block.getContentValue()),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        return new LandingContent(values, parseLines(values.get("hero.questions")), parseFaq(values.get("faq.items")));
    }

    @Transactional
    public void saveContent(Map<String, String> postedValues) {
        ensureDefinitionsExist();
        Map<String, SiteContentBlock> blocks = repository.findAllByContentKeyIn(DEFINITIONS.stream()
                        .map(ContentDefinition::key)
                        .toList())
                .stream()
                .collect(Collectors.toMap(SiteContentBlock::getContentKey, Function.identity()));
        postedValues.forEach((key, value) -> {
            SiteContentBlock block = blocks.get(key);
            if (block != null) {
                block.setContentValue(value == null ? "" : value.trim());
            }
        });
        repository.saveAll(blocks.values());
    }

    private void ensureDefinitionsExist() {
        Map<String, SiteContentBlock> existing = repository.findAllByContentKeyIn(DEFINITIONS.stream()
                        .map(ContentDefinition::key)
                        .toList())
                .stream()
                .collect(Collectors.toMap(SiteContentBlock::getContentKey, Function.identity()));
        List<SiteContentBlock> missing = new ArrayList<>();
        for (int i = 0; i < DEFINITIONS.size(); i++) {
            ContentDefinition definition = DEFINITIONS.get(i);
            if (!existing.containsKey(definition.key())) {
                SiteContentBlock block = new SiteContentBlock();
                block.setContentKey(definition.key());
                block.setSectionName(definition.section());
                block.setLabel(definition.label());
                block.setInputType(definition.inputType());
                block.setContentValue(definition.defaultValue());
                block.setDisplayOrder(i);
                block.setActive(true);
                missing.add(block);
            }
        }
        if (!missing.isEmpty()) {
            repository.saveAll(missing);
        }
    }

    private static ContentDefinition def(String key, String section, String label, String inputType, String defaultValue) {
        return new ContentDefinition(key, section, label, inputType, defaultValue);
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static List<String> parseLines(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return raw.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static List<FaqItem> parseFaq(String raw) {
        return parseLines(raw).stream()
                .map(line -> line.split("\\|", 2))
                .filter(parts -> parts.length == 2)
                .map(parts -> new FaqItem(parts[0].trim(), parts[1].trim()))
                .toList();
    }

    private record ContentDefinition(String key, String section, String label, String inputType, String defaultValue) {}

    public record LandingContent(Map<String, String> values, List<String> questionPrompts, List<FaqItem> faqItems) {
        public String value(String key) {
            return values.getOrDefault(key, "");
        }
    }

    public record FaqItem(String question, String answer) {}
}
