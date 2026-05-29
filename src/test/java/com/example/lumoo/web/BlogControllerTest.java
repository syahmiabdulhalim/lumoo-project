package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.blog.BlogController;
import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BlogController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class BlogControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean BlogService blogService;
    @MockitoBean SiteSettingsService siteSettingsService;

    @Test
    void index_returns200() throws Exception {
        when(blogService.getPublished()).thenReturn(List.of());
        when(blogService.getPublishedCategories()).thenReturn(List.of());

        mvc.perform(get("/blog"))
                .andExpect(status().isOk());
    }

    @Test
    void index_withPosts_setsFeatured() throws Exception {
        BlogPost post = new BlogPost();
        post.setId(1L);
        post.setSlug("featured-post");
        post.setTitle("Featured");
        post.setCategory("news");
        when(blogService.getPublished()).thenReturn(List.of(post));
        when(blogService.getPublishedCategories()).thenReturn(List.of("news"));

        mvc.perform(get("/blog"))
                .andExpect(status().isOk());
    }

    @Test
    void index_withCategory_returns200() throws Exception {
        when(blogService.getPublishedByCategory("tips")).thenReturn(List.of());
        when(blogService.getPublishedCategories()).thenReturn(List.of("tips"));

        mvc.perform(get("/blog").param("category", "tips"))
                .andExpect(status().isOk());
    }

    @Test
    void post_returns200_whenFound() throws Exception {
        BlogPost post = new BlogPost();
        post.setId(1L);
        post.setSlug("my-post");
        post.setCategory("news");
        when(blogService.getBySlug("my-post")).thenReturn(Optional.of(post));
        when(blogService.getRelated("news", 1L)).thenReturn(List.of());

        mvc.perform(get("/blog/my-post"))
                .andExpect(status().isOk());
    }

    @Test
    void post_redirects_whenNotFound() throws Exception {
        when(blogService.getBySlug("ghost")).thenReturn(Optional.empty());

        mvc.perform(get("/blog/ghost"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/blog"));
    }
}
