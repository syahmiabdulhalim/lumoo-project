package com.example.lumoo.service;

import com.example.lumoo.model.BlogPost;
import com.example.lumoo.repository.BlogPostRepository;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    @Autowired private BlogPostRepository repo;

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    public List<BlogPost> getPublished() {
        return repo.findByPublishedTrueOrderByPublishedAtDesc();
    }

    public List<BlogPost> getPublishedByCategory(String category) {
        return repo.findByCategoryAndPublishedTrueOrderByPublishedAtDesc(category);
    }

    public List<String> getPublishedCategories() {
        return repo.findDistinctPublishedCategories();
    }

    public Optional<BlogPost> getBySlug(String slug) {
        return repo.findBySlugAndPublishedTrue(slug);
    }

    public List<BlogPost> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Optional<BlogPost> findById(Long id) {
        return repo.findById(id);
    }

    public List<BlogPost> getRelated(String category, Long excludeId) {
        return repo.findRelatedPosts(category, excludeId, PageRequest.of(0, 3));
    }

    public BlogPost save(BlogPost post) {
        post.setSlug(uniqueSlug(post.getSlug(), post.getId()));
        if (post.getAuthor() == null || post.getAuthor().isBlank()) post.setAuthor("LUMOO Team");
        if (post.isPublished() && post.getPublishedAt() == null) post.setPublishedAt(LocalDateTime.now());
        // Convert Markdown → HTML (only if content doesn't look like HTML already)
        if (post.getContent() != null && !post.getContent().trim().startsWith("<")) {
            Node document = MD_PARSER.parse(post.getContent());
            post.setContent(MD_RENDERER.render(document));
        }
        post.setReadingTimeMinutes(estimateReadingTime(post.getContent()));
        return repo.save(post);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public String slugify(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String uniqueSlug(String slug, Long existingId) {
        if (slug == null || slug.isBlank()) return "post-" + System.currentTimeMillis();
        String base = slug;
        int i = 2;
        while (true) {
            Optional<BlogPost> existing = repo.findBySlugAndPublishedTrue(slug);
            // treat as taken only if it belongs to a different post
            if (existing.isEmpty() || existing.get().getId().equals(existingId)) return slug;
            // also check drafts
            if (!repo.existsBySlug(slug)) return slug;
            slug = base + "-" + i++;
        }
    }

    private int estimateReadingTime(String html) {
        if (html == null || html.isBlank()) return 1;
        String text = html.replaceAll("<[^>]+>", " ");
        int words = text.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }
}
