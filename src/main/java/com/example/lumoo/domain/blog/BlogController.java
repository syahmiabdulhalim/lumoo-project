package com.example.lumoo.domain.blog;

import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/blog")
public class BlogController {

    @Autowired private BlogService blogService;

    @GetMapping({"", "/"})
    public String index(@RequestParam(required = false) String category, Model model) {
        List<BlogPost> posts = (category != null && !category.isBlank())
                ? blogService.getPublishedByCategory(category)
                : blogService.getPublished();

        model.addAttribute("posts", posts);
        model.addAttribute("categories", blogService.getPublishedCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("featured", posts.isEmpty() ? null : posts.get(0));
        return "blog/index";
    }

    @GetMapping("/{slug}")
    public String post(@PathVariable String slug, Model model) {
        BlogPost post = blogService.getBySlug(slug).orElse(null);
        if (post == null) return "redirect:/blog";
        model.addAttribute("post", post);
        model.addAttribute("related", blogService.getRelated(post.getCategory(), post.getId()));
        return "blog/post";
    }
}
