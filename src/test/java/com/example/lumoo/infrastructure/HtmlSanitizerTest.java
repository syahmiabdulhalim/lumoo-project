package com.example.lumoo.infrastructure;
import com.example.lumoo.infrastructure.web.HtmlSanitizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HtmlSanitizerTest {
    @Test
    void stripsScriptsHandlersAndJavascriptUrls() {
        String dirty = "<h2 class=\"title\">Terms</h2><script>alert(1)</script>"
                + "<img src=\"x\" onerror=\"alert(1)\"><a href=\"javascript:alert(1)\">x</a>"
                + "<p style=\"x\" onclick=\"steal()\">ok</p><iframe src=\"https://evil\"></iframe>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertFalse(clean.contains("<script"));
        assertFalse(clean.contains("onerror"));
        assertFalse(clean.contains("onclick"));
        assertFalse(clean.contains("javascript:"));
        assertFalse(clean.contains("<iframe"));
        assertTrue(clean.contains("<h2 class=\"title\">Terms</h2>"));
        assertTrue(clean.contains("<p>ok</p>"));
    }
    @Test
    void keepsNormalContentAndRelativeLinks() {
        String html = "<p>See <a href=\"/returns\">returns</a> and <strong>bold</strong></p><ul><li>a</li></ul>";
        assertEquals(html, HtmlSanitizer.sanitize(html));
    }
    @Test
    void nullAndBlankPassThrough() {
        assertNull(HtmlSanitizer.sanitize(null));
        assertEquals("", HtmlSanitizer.sanitize(""));
    }
}
