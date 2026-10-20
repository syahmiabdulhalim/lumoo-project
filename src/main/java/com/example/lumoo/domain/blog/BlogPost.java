package com.example.lumoo.domain.blog;
import com.example.lumoo.infrastructure.web.HtmlSanitizer;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "blog_posts", indexes = {
    @Index(name = "idx_blog_published_at", columnList = "published,publishedAt"),
    @Index(name = "idx_blog_category", columnList = "category,published")
})
@Getter
@Setter
public class BlogPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(unique = true)
    private String slug;
    @Column(columnDefinition = "TEXT")
    private String excerpt;
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    private String category;
    private String tags;
    private String featuredImageUrl;
    private String author;
    private boolean published = false;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String metaTitle;
    @Column(columnDefinition = "TEXT")
    private String metaDescription;
    private Integer readingTimeMinutes;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    /** Sanitised HTML for th:utext; also covers posts saved before sanitising on save existed. */
    public String getSafeContent() { return HtmlSanitizer.sanitize(content); }
    public String getEffectiveMetaTitle() {
        return (metaTitle != null && !metaTitle.isBlank()) ? metaTitle : title;
    }
    public String getEffectiveMetaDescription() {
        return (metaDescription != null && !metaDescription.isBlank()) ? metaDescription : excerpt;
    }
}
