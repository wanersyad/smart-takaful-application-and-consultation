package com.muqmeen.takaful.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.ContactInquiry;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.PlanFinderResponse;
import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.domain.ProductBenefit;
import com.muqmeen.takaful.domain.ProductCoverageItem;
import com.muqmeen.takaful.repository.PlanFinderResponseRepository;
import com.muqmeen.takaful.service.chat.GeminiClient;
import com.muqmeen.takaful.service.chat.GeminiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Plan Finder: a short needs questionnaire that suggests suitable Takaful products.
 *
 * Gemini ranks the active products against the visitor's coarse answers (bands and goal codes
 * only - no name, IC, contact details or health answer). The AI output is validated against the
 * real product list; if the AI is unavailable or returns anything unusable, a deterministic
 * keyword-matching engine is used instead so the feature always works. Pricing is never
 * suggested: the licensed agent confirms suitability and builds the quotation.
 */
@Service
@Transactional
public class PlanFinderService {

    private static final Logger log = LoggerFactory.getLogger(PlanFinderService.class);
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final int MAX_REASON_LENGTH = 400;

    // ---------- Questionnaire catalogue (codes are what we store; labels are bilingual) ----------

    public record Option(String code, String en, String ms) {
        public String label(String lang) { return "ms".equals(lang) ? ms : en; }
    }

    public record Question(String key, String en, String ms, boolean multiple, boolean optional, List<Option> options) {
        public String label(String lang) { return "ms".equals(lang) ? ms : en; }

        Optional<Option> option(String code) {
            return options.stream().filter(o -> o.code().equals(code)).findFirst();
        }
    }

    private static final List<Question> QUESTIONS = List.of(
            new Question("ageBand", "How old are you?", "Berapakah umur anda?", false, false, List.of(
                    new Option("18-25", "18 - 25", "18 - 25"),
                    new Option("26-35", "26 - 35", "26 - 35"),
                    new Option("36-45", "36 - 45", "36 - 45"),
                    new Option("46-55", "46 - 55", "46 - 55"),
                    new Option("56+", "56 and above", "56 dan ke atas"))),
            new Question("maritalStatus", "What is your family status?", "Apakah status keluarga anda?", false, false, List.of(
                    new Option("SINGLE", "Single", "Bujang"),
                    new Option("MARRIED", "Married", "Berkahwin"),
                    new Option("SINGLE_PARENT", "Single parent", "Ibu / bapa tunggal"))),
            new Question("childrenBand", "How many children depend on you?", "Berapa ramai anak di bawah tanggungan anda?", false, false, List.of(
                    new Option("0", "None", "Tiada"),
                    new Option("1", "1", "1"),
                    new Option("2", "2", "2"),
                    new Option("3+", "3 or more", "3 atau lebih"))),
            new Question("goals", "What do you want to protect or plan for? (choose all that apply)",
                    "Apakah yang ingin anda lindungi atau rancang? (pilih semua yang berkaitan)", true, false, List.of(
                    new Option("FAMILY_PROTECTION", "Protect my family's income if something happens to me", "Lindungi pendapatan keluarga jika berlaku sesuatu kepada saya"),
                    new Option("MEDICAL", "Medical and hospital bills", "Bil perubatan dan hospital"),
                    new Option("CRITICAL_ILLNESS", "Critical illness (e.g. cancer, heart attack)", "Penyakit kritikal (cth. kanser, serangan jantung)"),
                    new Option("EDUCATION", "My children's education", "Pendidikan anak-anak"),
                    new Option("HIBAH", "Hibah, inheritance and legacy", "Hibah, pewarisan dan legasi"),
                    new Option("SAVINGS", "Long-term savings", "Simpanan jangka panjang"))),
            new Question("existingCoverage", "What coverage do you have today?", "Apakah perlindungan yang anda ada sekarang?", false, false, List.of(
                    new Option("NONE", "None", "Tiada"),
                    new Option("EMPLOYER", "Only my employer's group cover", "Hanya perlindungan berkumpulan majikan"),
                    new Option("PERSONAL", "I have a personal policy", "Saya ada polisi peribadi"))),
            new Question("budgetBand", "What monthly contribution feels comfortable?", "Berapakah caruman bulanan yang selesa untuk anda?", false, false, List.of(
                    new Option("UNDER_100", "Below RM100", "Bawah RM100"),
                    new Option("100_200", "RM100 - RM200", "RM100 - RM200"),
                    new Option("200_400", "RM200 - RM400", "RM200 - RM400"),
                    new Option("OVER_400", "Above RM400", "Melebihi RM400"),
                    new Option("UNSURE", "Not sure yet", "Belum pasti"))),
            new Question("occupationClass", "What best describes your work?", "Apakah yang paling menggambarkan pekerjaan anda?", false, false, List.of(
                    new Option("OFFICE", "Office / indoor work", "Kerja pejabat / dalam bangunan"),
                    new Option("FIELD", "Field / outdoor work", "Kerja lapangan / luar"),
                    new Option("HIGH_RISK", "High-risk work (e.g. heavy machinery, offshore)", "Kerja berisiko tinggi (cth. jentera berat, luar pesisir)"))),
            new Question("healthCondition", "Do you have any existing medical condition? (optional - only our agent sees this)",
                    "Adakah anda mempunyai sebarang masalah kesihatan sedia ada? (pilihan - hanya ejen kami akan melihatnya)", false, true, List.of(
                    new Option("NO", "No", "Tidak"),
                    new Option("YES", "Yes", "Ya"),
                    new Option("PREFER_NOT", "Prefer not to say", "Tidak mahu menyatakan")))
    );

    /** Keywords used by the rule-based engine to match a goal to product text. */
    private static final Map<String, List<String>> GOAL_KEYWORDS = Map.of(
            "FAMILY_PROTECTION", List.of("family", "protection", "life", "income", "keluarga", "continuity"),
            "MEDICAL", List.of("medical", "hospital", "medic", "health", "perubatan", "admission"),
            "CRITICAL_ILLNESS", List.of("critical", "illness", "kritikal", "cancer", "recovery"),
            "EDUCATION", List.of("education", "child", "children", "pendidikan", "anak", "study"),
            "HIBAH", List.of("hibah", "legacy", "inheritance", "estate", "warisan", "wasiat"),
            "SAVINGS", List.of("saving", "savings", "investment", "simpanan", "wealth", "gold")
    );

    public record Answers(String language, String ageBand, String maritalStatus, String childrenBand,
                          List<String> goals, String existingCoverage, String budgetBand,
                          String occupationClass, String healthCondition) {
    }

    public record Recommendation(Long productId, String productName, String fit, String reason) {
    }

    public record RecommendedProduct(Recommendation recommendation, Product product) {
    }

    public record AnswerLine(String question, String answer) {
    }

    /** What the visitor sees on the result page. */
    public record ResultView(PlanFinderResponse response, String language, String summary,
                             List<RecommendedProduct> products, List<AnswerLine> answers) {
    }

    /** What the agent sees beside an application or consultation. */
    public record AgentView(PlanFinderResponse response, List<AnswerLine> answers, List<Recommendation> recommendations,
                            List<String> agentChecks, String summary, String source, LocalDateTime createdAt) {
    }

    private final PlanFinderResponseRepository repository;
    private final ProductService productService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public PlanFinderService(PlanFinderResponseRepository repository, ProductService productService,
                             GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.repository = repository;
        this.productService = productService;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public List<Question> questions() {
        return QUESTIONS;
    }

    // ---------- Submit ----------

    public PlanFinderResponse submit(Answers answers, Customer customer) {
        Answers clean = validate(answers);
        List<Product> products = productService.listActiveForLanding();

        Outcome outcome = null;
        if (!products.isEmpty() && geminiClient.isConfigured()) {
            outcome = recommendWithAi(clean, products);
        }
        if (outcome == null) {
            outcome = recommendWithRules(clean, products);
        }

        // Rule-based agent checks always apply (the AI never sees the health answer).
        LinkedHashSet<String> checks = new LinkedHashSet<>(ruleAgentChecks(clean));
        checks.addAll(outcome.agentChecks());

        PlanFinderResponse response = new PlanFinderResponse();
        response.setToken(UUID.randomUUID().toString());
        response.setLanguage(clean.language());
        response.setAgeBand(clean.ageBand());
        response.setMaritalStatus(clean.maritalStatus());
        response.setChildrenBand(clean.childrenBand());
        response.setGoals(String.join(",", clean.goals()));
        response.setExistingCoverage(clean.existingCoverage());
        response.setBudgetBand(clean.budgetBand());
        response.setOccupationClass(clean.occupationClass());
        response.setHealthCondition(clean.healthCondition());
        response.setRecommendations(toJson(outcome.recommendations()));
        response.setAgentChecks(toJson(new ArrayList<>(checks)));
        response.setSummary(outcome.summary());
        response.setSource(outcome.source());
        response.setCustomer(customer);
        return repository.save(response);
    }

    private Answers validate(Answers a) {
        if (a == null) throw new IllegalArgumentException("Please answer the questions.");
        String lang = "ms".equals(a.language()) ? "ms" : "en";
        List<String> goals = a.goals() == null ? List.of() : a.goals().stream()
                .filter(g -> question("goals").option(g).isPresent())
                .distinct()
                .toList();
        if (goals.isEmpty()) {
            throw new IllegalArgumentException("ms".equals(lang) ? "Sila pilih sekurang-kurangnya satu matlamat." : "Please choose at least one goal.");
        }
        String health = a.healthCondition() == null || a.healthCondition().isBlank() ? null : a.healthCondition();
        if (health != null && question("healthCondition").option(health).isEmpty()) health = null;
        return new Answers(lang,
                require("ageBand", a.ageBand(), lang),
                require("maritalStatus", a.maritalStatus(), lang),
                require("childrenBand", a.childrenBand(), lang),
                goals,
                require("existingCoverage", a.existingCoverage(), lang),
                require("budgetBand", a.budgetBand(), lang),
                require("occupationClass", a.occupationClass(), lang),
                health);
    }

    private String require(String key, String value, String lang) {
        Question q = question(key);
        if (value == null || q.option(value).isEmpty()) {
            throw new IllegalArgumentException(("ms".equals(lang) ? "Sila jawab: " : "Please answer: ") + q.label(lang));
        }
        return value;
    }

    private static Question question(String key) {
        return QUESTIONS.stream().filter(q -> q.key().equals(key)).findFirst().orElseThrow();
    }

    private record Outcome(List<Recommendation> recommendations, List<String> agentChecks, String summary, String source) {
    }

    // ---------- AI engine ----------

    private Outcome recommendWithAi(Answers a, List<Product> products) {
        try {
            String raw = geminiClient.generateJson(aiInstruction(a.language()), aiPrompt(a, products));
            Outcome outcome = parseAi(raw, products);
            if (outcome == null) {
                log.warn("Plan Finder: AI answer unusable, using rule-based engine");
            }
            return outcome;
        } catch (GeminiException e) {
            log.warn("Plan Finder: Gemini unavailable, using rule-based engine. cause={}", e.getMessage());
            return null;
        }
    }

    private String aiInstruction(String lang) {
        String language = "ms".equals(lang) ? "Bahasa Melayu (Malaysian Malay)" : "English";
        return """
                You are the Plan Finder for Muqmeen Group, a Malaysian Takaful agency. Recommend which of the
                listed Takaful products best suit a prospective customer, based only on their questionnaire
                answers and the product information provided.

                Rules:
                - Recommend between 1 and 3 products, best match first. Use ONLY productId values from the list.
                - Write "summary" and every "reason" in %s, in a warm, plain, non-salesy tone, 1-2 sentences each.
                - Explain how the product relates to the customer's stated goals and situation.
                - Never state or estimate prices, contribution amounts, returns or guarantees.
                - Never claim the customer is eligible or approved; a licensed agent confirms suitability.
                - "fit" must be HIGH or MEDIUM.
                - "agentChecks" are short notes IN ENGLISH for the agent: things to confirm before quoting
                  (for example coverage gaps, budget fit, age or occupation considerations). 0-4 items.

                Answer with JSON only, exactly in this shape:
                {"summary": "...", "recommendations": [{"productId": 1, "fit": "HIGH", "reason": "..."}], "agentChecks": ["..."]}
                """.formatted(language);
    }

    /** Only coarse answer bands and public product information go to the AI - never identity or health data. */
    private String aiPrompt(Answers a, List<Product> products) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("CUSTOMER ANSWERS\n");
        for (Question q : QUESTIONS) {
            if (q.key().equals("healthCondition")) continue;
            sb.append("- ").append(q.en()).append(": ").append(answerLabel(q, a, "en")).append('\n');
        }
        sb.append("\nPRODUCTS\n");
        for (Product p : products) {
            sb.append("productId=").append(p.getId()).append(" | ").append(p.getName());
            if (notBlank(p.getCategoryLabel())) sb.append(" | category: ").append(p.getCategoryLabel());
            sb.append('\n');
            appendField(sb, "summary", notBlank(p.getSummary()) ? p.getSummary() : p.getDescription());
            appendField(sb, "eligibility", p.getEligibility());
            appendField(sb, "coverage purpose", p.getCoveragePurpose());
            String benefits = p.getBenefits().stream().map(ProductBenefit::getBenefitText).limit(6).collect(Collectors.joining("; "));
            appendField(sb, "benefits", benefits);
            String coverage = p.getCoverageItems().stream().map(ProductCoverageItem::getItemName).limit(6).collect(Collectors.joining("; "));
            appendField(sb, "coverage items", coverage);
        }
        return sb.toString();
    }

    private Outcome parseAi(String raw, List<Product> products) {
        if (raw == null) return null;
        Map<Long, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, p -> p, (x, y) -> x, LinkedHashMap::new));
        try {
            JsonNode root = objectMapper.readTree(stripFences(raw));
            List<Recommendation> recs = new ArrayList<>();
            Set<Long> seen = new LinkedHashSet<>();
            for (JsonNode node : root.path("recommendations")) {
                if (!node.path("productId").canConvertToLong()) continue;
                long id = node.path("productId").asLong();
                Product product = byId.get(id);
                String reason = node.path("reason").asText("").trim();
                if (product == null || reason.isEmpty() || !seen.add(id)) continue;
                String fit = "HIGH".equalsIgnoreCase(node.path("fit").asText()) ? "HIGH" : "MEDIUM";
                recs.add(new Recommendation(id, product.getName(), fit, truncate(reason, MAX_REASON_LENGTH)));
                if (recs.size() == MAX_RECOMMENDATIONS) break;
            }
            if (recs.isEmpty()) return null;
            List<String> checks = new ArrayList<>();
            for (JsonNode c : root.path("agentChecks")) {
                String text = c.asText("").trim();
                if (!text.isEmpty() && checks.size() < 4) checks.add(truncate(text, 300));
            }
            String summary = truncate(root.path("summary").asText("").trim(), 600);
            return new Outcome(recs, checks, summary.isEmpty() ? null : summary, "AI");
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String stripFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(json)?", "").replaceFirst("```\\s*$", "").trim();
        }
        return s;
    }

    // ---------- Rule-based engine (fallback, deterministic) ----------

    private Outcome recommendWithRules(Answers a, List<Product> products) {
        String lang = a.language();
        record Scored(Product product, double score, List<String> matchedGoals) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Product p : products) {
            String text = productText(p);
            double score = 0;
            List<String> matched = new ArrayList<>();
            for (String goal : a.goals()) {
                boolean hit = GOAL_KEYWORDS.getOrDefault(goal, List.of()).stream().anyMatch(text::contains);
                if (hit) {
                    score += 2;
                    matched.add(goal);
                }
            }
            if (p.isFeatured()) score += 0.5;
            scored.add(new Scored(p, score, matched));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());

        List<Recommendation> recs = new ArrayList<>();
        for (Scored s : scored) {
            if (recs.size() == MAX_RECOMMENDATIONS) break;
            if (s.matchedGoals().isEmpty() && !recs.isEmpty()) break;
            String goals = s.matchedGoals().stream()
                    .map(g -> question("goals").option(g).map(o -> o.label(lang).toLowerCase(Locale.ROOT)).orElse(g))
                    .collect(Collectors.joining(", "));
            String reason = s.matchedGoals().isEmpty()
                    ? ("ms".equals(lang)
                        ? "Pelan popular kami. Ejen akan membincangkan sama ada ia sesuai dengan keperluan anda."
                        : "One of our popular plans. An agent will discuss whether it suits your needs.")
                    : ("ms".equals(lang)
                        ? "Sepadan dengan matlamat anda: " + goals + "."
                        : "Matches what you want to plan for: " + goals + ".");
            recs.add(new Recommendation(s.product().getId(), s.product().getName(),
                    s.matchedGoals().isEmpty() ? "MEDIUM" : "HIGH", reason));
        }
        String summary = recs.isEmpty() ? null : ("ms".equals(lang)
                ? "Berdasarkan jawapan anda, berikut ialah pelan yang paling berkaitan. Ejen berlesen kami akan mengesahkan kesesuaian dan caruman."
                : "Based on your answers, these are the most relevant plans. Our licensed agent will confirm suitability and contribution.");
        return new Outcome(recs, List.of(), summary, "RULES");
    }

    private List<String> ruleAgentChecks(Answers a) {
        List<String> checks = new ArrayList<>();
        if ("YES".equals(a.healthCondition())) checks.add("Customer disclosed an existing medical condition - request medical details before quoting.");
        if ("UNDER_100".equals(a.budgetBand())) checks.add("Budget is below RM100/month - confirm an entry-level plan or rider fits.");
        if ("EMPLOYER".equals(a.existingCoverage())) checks.add("Relies on employer group cover only - explain the gap if they change or lose their job.");
        if ("HIGH_RISK".equals(a.occupationClass())) checks.add("High-risk occupation - check underwriting loading or exclusions.");
        if ("56+".equals(a.ageBand())) checks.add("Aged 56 or above - confirm entry-age limits for the recommended plans.");
        if (a.goals().contains("EDUCATION") && "0".equals(a.childrenBand())) checks.add("Chose education planning but has no dependent children listed - clarify who it is for.");
        return checks;
    }

    private static String productText(Product p) {
        return String.join(" ",
                nullToEmpty(p.getName()), nullToEmpty(p.getCategoryLabel()), nullToEmpty(p.getSummary()),
                nullToEmpty(p.getDescription()), nullToEmpty(p.getCoveragePurpose()),
                p.getBenefits().stream().map(ProductBenefit::getBenefitText).map(PlanFinderService::nullToEmpty).collect(Collectors.joining(" ")),
                p.getCoverageItems().stream().map(ProductCoverageItem::getItemName).map(PlanFinderService::nullToEmpty).collect(Collectors.joining(" "))
        ).toLowerCase(Locale.ROOT);
    }

    // ---------- Read side ----------

    @Transactional(readOnly = true)
    public Optional<PlanFinderResponse> findByToken(String token) {
        return token == null || token.isBlank() ? Optional.empty() : repository.findByToken(token);
    }

    @Transactional(readOnly = true)
    public ResultView resultView(PlanFinderResponse response) {
        String lang = response.getLanguage();
        List<RecommendedProduct> products = new ArrayList<>();
        for (Recommendation r : recommendations(response)) {
            productService.findActiveById(r.productId()).ifPresent(p -> products.add(new RecommendedProduct(r, p)));
        }
        return new ResultView(response, lang, response.getSummary(), products, answerLines(response, lang, false));
    }

    @Transactional(readOnly = true)
    public Optional<AgentView> forApplication(ConsultationApplication application) {
        return repository.findFirstByApplicationOrderByCreatedAtDesc(application).map(this::agentView);
    }

    @Transactional(readOnly = true)
    public Optional<AgentView> forInquiry(ContactInquiry inquiry) {
        return repository.findFirstByInquiryOrderByCreatedAtDesc(inquiry).map(this::agentView);
    }

    private AgentView agentView(PlanFinderResponse r) {
        return new AgentView(r, answerLines(r, "en", true), recommendations(r), agentChecks(r),
                r.getSummary(), r.getSource(), r.getCreatedAt());
    }

    private List<AnswerLine> answerLines(PlanFinderResponse r, String lang, boolean includeHealth) {
        Answers a = new Answers(r.getLanguage(), r.getAgeBand(), r.getMaritalStatus(), r.getChildrenBand(),
                List.of(r.getGoals().split(",")), r.getExistingCoverage(), r.getBudgetBand(),
                r.getOccupationClass(), r.getHealthCondition());
        List<AnswerLine> lines = new ArrayList<>();
        for (Question q : QUESTIONS) {
            if (q.key().equals("healthCondition") && !includeHealth) continue;
            String label = answerLabel(q, a, lang);
            lines.add(new AnswerLine(shortQuestion(q.key(), lang), label.isEmpty() ? "-" : label));
        }
        return lines;
    }

    private static String shortQuestion(String key, String lang) {
        boolean ms = "ms".equals(lang);
        return switch (key) {
            case "ageBand" -> ms ? "Umur" : "Age";
            case "maritalStatus" -> ms ? "Status keluarga" : "Family status";
            case "childrenBand" -> ms ? "Anak tanggungan" : "Dependent children";
            case "goals" -> ms ? "Matlamat" : "Goals";
            case "existingCoverage" -> ms ? "Perlindungan sedia ada" : "Existing coverage";
            case "budgetBand" -> ms ? "Bajet bulanan" : "Monthly budget";
            case "occupationClass" -> ms ? "Pekerjaan" : "Occupation";
            case "healthCondition" -> ms ? "Masalah kesihatan" : "Existing medical condition";
            default -> key;
        };
    }

    private static String answerLabel(Question q, Answers a, String lang) {
        return switch (q.key()) {
            case "goals" -> a.goals().stream().map(g -> q.option(g).map(o -> o.label(lang)).orElse(g)).collect(Collectors.joining("; "));
            default -> {
                String code = switch (q.key()) {
                    case "ageBand" -> a.ageBand();
                    case "maritalStatus" -> a.maritalStatus();
                    case "childrenBand" -> a.childrenBand();
                    case "existingCoverage" -> a.existingCoverage();
                    case "budgetBand" -> a.budgetBand();
                    case "occupationClass" -> a.occupationClass();
                    case "healthCondition" -> a.healthCondition();
                    default -> null;
                };
                yield code == null ? "" : q.option(code).map(o -> o.label(lang)).orElse(code);
            }
        };
    }

    public List<Recommendation> recommendations(PlanFinderResponse r) {
        return fromJson(r.getRecommendations(), new TypeReference<List<Recommendation>>() {});
    }

    public List<String> agentChecks(PlanFinderResponse r) {
        return fromJson(r.getAgentChecks(), new TypeReference<List<String>>() {});
    }

    // ---------- Linking to the customer's next step ----------

    /** Attaches a questionnaire to the application the visitor started from its result page. */
    public void linkApplication(String token, ConsultationApplication application, Customer customer) {
        findByToken(token).ifPresent(r -> {
            if (r.getApplication() != null) return;
            if (r.getCustomer() != null && customer != null && !r.getCustomer().getId().equals(customer.getId())) return;
            r.setApplication(application);
            if (r.getCustomer() == null) r.setCustomer(customer);
            repository.save(r);
        });
    }

    /** Attaches a questionnaire to the consultation request sent from its result page. */
    public void linkInquiry(String token, ContactInquiry inquiry) {
        findByToken(token).ifPresent(r -> {
            if (r.getInquiry() != null) return;
            r.setInquiry(inquiry);
            repository.save(r);
        });
    }

    // ---------- Helpers ----------

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise Plan Finder data", e);
        }
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (notBlank(value)) sb.append("  ").append(label).append(": ").append(truncate(value.replaceAll("\\s+", " ").trim(), 500)).append('\n');
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
