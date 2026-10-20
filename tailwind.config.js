/** Tailwind v3 config — replaces the CDN's on-the-fly build. Classes are found by scanning these files. */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/java/**/*.java',
  ],
  theme: { extend: {} },
  plugins: [],
};
