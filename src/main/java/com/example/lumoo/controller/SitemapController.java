package com.example.lumoo.controller;

import com.example.lumoo.model.BlogPost;
import com.example.lumoo.model.Product;
import com.example.lumoo.service.BlogService;
import com.example.lumoo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class SitemapController {

    @Autowired private BlogService blogService;
    @Autowired private ProductService productService;

    private static final String BASE = "https://lumoo.gm";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        for (String path : List.of("", "/blog", "/stores", "/login", "/register")) {
            sb.append(url(BASE + path, "weekly", "0.8"));
        }

        // Blog posts
        for (BlogPost post : blogService.getPublished()) {
            String date = post.getPublishedAt() != null ? post.getPublishedAt().format(FMT) : "";
            sb.append(url(BASE + "/blog/" + post.getSlug(), "monthly", "0.7", date));
        }

        // Products
        for (Product p : productService.getAll()) {
            if (p.isApproved()) {
                sb.append(url(BASE + "/product/" + p.getId(), "weekly", "0.6"));
            }
        }

        sb.append("</urlset>");
        return sb.toString();
    }

    private String url(String loc, String changefreq, String priority) {
        return url(loc, changefreq, priority, "");
    }

    private String url(String loc, String changefreq, String priority, String lastmod) {
        StringBuilder u = new StringBuilder("  <url>\n");
        u.append("    <loc>").append(loc).append("</loc>\n");
        if (!lastmod.isBlank()) u.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        u.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        u.append("    <priority>").append(priority).append("</priority>\n");
        u.append("  </url>\n");
        return u.toString();
    }
}
