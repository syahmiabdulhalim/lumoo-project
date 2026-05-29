package com.example.lumoo.domain.blog;

import com.example.lumoo.domain.blog.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);
    List<BlogPost> findByPublishedTrueOrderByPublishedAtDesc();
    List<BlogPost> findByCategoryAndPublishedTrueOrderByPublishedAtDesc(String category);

    org.springframework.data.domain.Page<BlogPost> findByPublishedTrueOrderByPublishedAtDesc(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<BlogPost> findByCategoryAndPublishedTrueOrderByPublishedAtDesc(String category, org.springframework.data.domain.Pageable pageable);
    List<BlogPost> findAllByOrderByCreatedAtDesc();
    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT b.category FROM BlogPost b WHERE b.published = true AND b.category IS NOT NULL")
    List<String> findDistinctPublishedCategories();

    @Query("SELECT b FROM BlogPost b WHERE b.published = true AND b.category = :category AND b.id <> :excludeId ORDER BY b.publishedAt DESC")
    List<BlogPost> findRelatedPosts(String category, Long excludeId, org.springframework.data.domain.Pageable pageable);
}
