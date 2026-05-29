package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.blog.AdminBlogController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdminBlogController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AdminBlogControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean BlogService blogService;
    @MockitoBean SiteSettingsService siteSettingsService;

    @Test
    void list_returns200() throws Exception {
        when(blogService.getAll()).thenReturn(List.of());

        mvc.perform(get("/admin/blog"))
                .andExpect(status().isOk());
    }

    @Test
    void newForm_returns200() throws Exception {
        mvc.perform(get("/admin/blog/new"))
                .andExpect(status().isOk());
    }

    @Test
    void create_withSlug_redirects() throws Exception {
        BlogPost saved = new BlogPost();
        when(blogService.save(any())).thenReturn(saved);

        mvc.perform(post("/admin/blog/new")
                        .param("title", "Test Post")
                        .param("slug", "test-post")
                        .param("content", "Body"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/blog"));
        verify(blogService).save(any(BlogPost.class));
    }

    @Test
    void create_withoutSlug_slugifiesTitle() throws Exception {
        when(blogService.slugify("My Title")).thenReturn("my-title");
        when(blogService.save(any())).thenReturn(new BlogPost());

        mvc.perform(post("/admin/blog/new")
                        .param("title", "My Title")
                        .param("content", "Body"))
                .andExpect(status().is3xxRedirection());
        verify(blogService).slugify("My Title");
    }

    @Test
    void create_sanitizesUnsafeImageUrl() throws Exception {
        when(blogService.save(any())).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            assert p.getFeaturedImageUrl() == null : "Unsafe URL should be stripped";
            return p;
        });

        mvc.perform(post("/admin/blog/new")
                        .param("title", "X")
                        .param("slug", "x")
                        .param("featuredImageUrl", "javascript:alert(1)"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void editForm_returns200_whenFound() throws Exception {
        BlogPost post = new BlogPost();
        post.setId(1L);
        when(blogService.findById(1L)).thenReturn(Optional.of(post));

        mvc.perform(get("/admin/blog/edit/1"))
                .andExpect(status().isOk());
    }

    @Test
    void editForm_redirects_whenNotFound() throws Exception {
        when(blogService.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/admin/blog/edit/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/blog"));
    }

    @Test
    void update_whenFound_redirects() throws Exception {
        BlogPost existing = new BlogPost();
        existing.setId(1L);
        when(blogService.findById(1L)).thenReturn(Optional.of(existing));
        when(blogService.save(any())).thenReturn(existing);

        mvc.perform(post("/admin/blog/edit/1")
                        .param("title", "Updated Title")
                        .param("slug", "updated-slug")
                        .param("content", "New content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/blog"));
        verify(blogService).save(any(BlogPost.class));
    }

    @Test
    void update_whenNotFound_redirects() throws Exception {
        when(blogService.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(post("/admin/blog/edit/99")
                        .param("title", "X")
                        .param("slug", "x"))
                .andExpect(status().is3xxRedirection());
        verify(blogService, never()).save(any());
    }

    @Test
    void delete_redirects() throws Exception {
        mvc.perform(post("/admin/blog/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/blog"));
        verify(blogService).delete(1L);
    }
}
