package com.example.lumoo.controller;

import com.example.lumoo.model.BlogPost;
import com.example.lumoo.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/blog")
public class AdminBlogController {

    @Autowired private BlogService blogService;

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
    public String create(@ModelAttribute BlogPost post) {
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(blogService.slugify(post.getTitle()));
        }
        blogService.save(post);
        return "redirect:/admin/blog?created";
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
    public String update(@PathVariable Long id, @ModelAttribute BlogPost updated) {
        BlogPost existing = blogService.findById(id).orElse(null);
        if (existing == null) return "redirect:/admin/blog";
        existing.setTitle(updated.getTitle());
        existing.setSlug(updated.getSlug() != null && !updated.getSlug().isBlank()
                ? updated.getSlug() : blogService.slugify(updated.getTitle()));
        existing.setExcerpt(updated.getExcerpt());
        existing.setContent(updated.getContent());
        existing.setCategory(updated.getCategory());
        existing.setTags(updated.getTags());
        existing.setFeaturedImageUrl(updated.getFeaturedImageUrl());
        existing.setAuthor(updated.getAuthor());
        existing.setPublished(updated.isPublished());
        existing.setMetaTitle(updated.getMetaTitle());
        existing.setMetaDescription(updated.getMetaDescription());
        blogService.save(existing);
        return "redirect:/admin/blog?updated";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        blogService.delete(id);
        return "redirect:/admin/blog?deleted";
    }
}
