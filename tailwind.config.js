/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/resources/static/js/**/*.js",
    "./src/main/java/**/*.java"
  ],
  // Status classes are applied via Thymeleaf th:classappend with a dynamic value
  // ('status-' + status), so the literal names never appear in scanned source. Safelist
  // them so the production build keeps the rules even if scanning differs from local.
  safelist: [
    "status-badge",
    "status-paid", "status-quoted", "status-closed",
    "status-rejected",
    "status-needs_info", "status-payment_pending",
    "status-draft", "status-submitted", "status-under_review",
    "status-new", "status-contacted", "status-scheduled", "status-completed"
  ],
  theme: {
    extend: {
      // Design tokens: black = structure, logo gold = accent, cream/white = reading surfaces.
      colors: {
        ink: { DEFAULT: "#0B0B0C", soft: "#17171A", line: "#26262B" },
        cream: { DEFAULT: "#FAF8F3", deep: "#F3EFE6", line: "#E8E2D4" },
        // Sampled from the Muqmeen Group logo (metallic gold, not bright yellow).
        gold: { DEFAULT: "#C8962E", bright: "#E3B34F", deep: "#8C6115", soft: "#F5EAD2" },
        muted: "#6B675F",
        ok: { DEFAULT: "#15803D", soft: "#E7F5EC" },
        warn: { DEFAULT: "#B45309", soft: "#FDF1E3" },
        info: { DEFAULT: "#1D4ED8", soft: "#E8EEFD" }
      },
      fontFamily: {
        sans: ["Poppins", "ui-sans-serif", "system-ui", "sans-serif"],
        brand: ["Michroma", "sans-serif"]
      },
      boxShadow: {
        card: "0 1px 2px rgba(17,17,17,.04), 0 8px 24px -12px rgba(17,17,17,.12)",
        lift: "0 2px 4px rgba(17,17,17,.05), 0 24px 48px -16px rgba(17,17,17,.22)",
        glow: "0 0 0 1px rgba(200,150,46,.35), 0 12px 40px -8px rgba(200,150,46,.45)"
      }
    }
  },
  plugins: []
};
