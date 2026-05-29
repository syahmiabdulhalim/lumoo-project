package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.shared.SitemapController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SitemapController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SitemapControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean BlogService blogService;
    @MockitoBean ProductService productService;
    @MockitoBean SiteSettingsService siteSettingsService;

    @Test
    void sitemap_returns200_andXmlContentType() throws Exception {
        when(blogService.getPublished()).thenReturn(List.of());
        when(productService.getAll()).thenReturn(List.of());

        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    void sitemap_includesBlogPosts() throws Exception {
        BlogPost post = new BlogPost();
        post.setSlug("my-post");
        post.setPublishedAt(LocalDateTime.of(2025, 6, 1, 12, 0));
        when(blogService.getPublished()).thenReturn(List.of(post));
        when(productService.getAll()).thenReturn(List.of());

        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("my-post")));
    }

    @Test
    void sitemap_includesApprovedProducts() throws Exception {
        Product p = new Product();
        p.setId(5L);
        p.setApproved(true);
        when(blogService.getPublished()).thenReturn(List.of());
        when(productService.getAll()).thenReturn(List.of(p));

        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/product/5")));
    }

    @Test
    void sitemap_excludesUnapprovedProducts() throws Exception {
        Product p = new Product();
        p.setId(6L);
        p.setApproved(false);
        when(blogService.getPublished()).thenReturn(List.of());
        when(productService.getAll()).thenReturn(List.of(p));

        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/product/6"))));
    }

    @Test
    void sitemap_includesBlogPostWithoutPublishedAt() throws Exception {
        BlogPost post = new BlogPost();
        post.setSlug("no-date-post");
        post.setPublishedAt(null);
        when(blogService.getPublished()).thenReturn(List.of(post));
        when(productService.getAll()).thenReturn(List.of());

        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no-date-post")));
    }
}
