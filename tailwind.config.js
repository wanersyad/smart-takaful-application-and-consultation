/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/java/**/*.java"
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
