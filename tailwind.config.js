/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
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
    "status-draft", "status-submitted", "status-under_review"
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          black: "#09090b",
          gold: "#facc15",
          amber: "#f59e0b"
        }
      },
      fontFamily: {
        sans: ["Poppins", "ui-sans-serif", "system-ui", "sans-serif"]
      },
      boxShadow: {
        premium: "0 18px 60px -30px rgba(9, 9, 11, 0.55)"
      }
    }
  },
  plugins: []
};
