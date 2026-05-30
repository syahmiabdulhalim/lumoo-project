package com.example.lumoo.domain.blog;
import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogPostRepository;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
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
    public Page<BlogPost> getPublishedPaged(int page, int size) {
        return repo.findByPublishedTrueOrderByPublishedAtDesc(PageRequest.of(page, size));
    }
    public Page<BlogPost> getPublishedByCategoryPaged(String category, int page, int size) {
        return repo.findByCategoryAndPublishedTrueOrderByPublishedAtDesc(category, PageRequest.of(page, size));
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
    public Page<BlogPost> getAllPage(int page, int size) {
        return repo.findAll(PageRequest.of(page, size,
            org.springframework.data.domain.Sort.by("createdAt").descending()));
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
            if (existing.isEmpty() || existing.get().getId().equals(existingId)) return slug;
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
