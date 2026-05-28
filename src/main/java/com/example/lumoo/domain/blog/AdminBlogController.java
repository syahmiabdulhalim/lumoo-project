package com.example.lumoo.domain.blog;

import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/blog")
public class AdminBlogController {

    @Autowired private BlogService blogService;

    private String sanitizeImageUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.trim();
        if (trimmed.startsWith("/uploads/") ||
            trimmed.startsWith("http://") ||
            trimmed.startsWith("https://")) {
            return trimmed;
        }
        return null;
    }

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("posts", blogService.getAll());
        return "admin/blog-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("post", new BlogPost());
        model.addAttribute("editing", false);
        return "admin/blog-form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute BlogPost post, RedirectAttributes ra) {
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(blogService.slugify(post.getTitle()));
        }
        post.setFeaturedImageUrl(sanitizeImageUrl(post.getFeaturedImageUrl()));
        blogService.save(post);
        ra.addFlashAttribute("flashMsg", "Post created successfully.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/blog";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        BlogPost post = blogService.findById(id).orElse(null);
        if (post == null) return "redirect:/admin/blog";
        model.addAttribute("post", post);
        model.addAttribute("editing", true);
        return "admin/blog-form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute BlogPost updated, RedirectAttributes ra) {
        BlogPost existing = blogService.findById(id).orElse(null);
        if (existing == null) return "redirect:/admin/blog";
        existing.setTitle(updated.getTitle());
        existing.setSlug(updated.getSlug() != null && !updated.getSlug().isBlank()
                ? updated.getSlug() : blogService.slugify(updated.getTitle()));
        existing.setExcerpt(updated.getExcerpt());
        existing.setContent(updated.getContent());
        existing.setCategory(updated.getCategory());
        existing.setTags(updated.getTags());
        existing.setFeaturedImageUrl(sanitizeImageUrl(updated.getFeaturedImageUrl()));
        existing.setAuthor(updated.getAuthor());
        existing.setPublished(updated.isPublished());
        existing.setMetaTitle(updated.getMetaTitle());
        existing.setMetaDescription(updated.getMetaDescription());
        blogService.save(existing);
        ra.addFlashAttribute("flashMsg", "Post updated successfully.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/blog";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        blogService.delete(id);
        ra.addFlashAttribute("flashMsg", "Post deleted.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/blog";
    }
}
