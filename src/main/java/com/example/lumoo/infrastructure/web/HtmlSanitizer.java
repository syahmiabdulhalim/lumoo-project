package com.example.lumoo.infrastructure.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Strips scripts, event handlers and javascript: URLs from admin-authored HTML before it is
 * rendered with th:utext. Entities expose sanitised getters (e.g. BlogPost#getSafeContent) so
 * content stored before this existed is covered too.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("hr", "section", "article", "header", "footer", "figure", "figcaption")
            .addAttributes(":all", "class", "id")
            .addAttributes("a", "target", "rel")
            .preserveRelativeLinks(true);

    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, "https://lumoo.my/", SAFELIST, OUTPUT);
    }
}
