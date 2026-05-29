package com.example.lumoo.domain.blog;

import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/blog")
public class BlogController {

    private static final int PAGE_SIZE = 9;

    @Autowired private BlogService blogService;

    @GetMapping({"", "/"})
    public String index(@RequestParam(required = false) String category,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<BlogPost> postsPage = (category != null && !category.isBlank())
                ? blogService.getPublishedByCategoryPaged(category, page, PAGE_SIZE)
                : blogService.getPublishedPaged(page, PAGE_SIZE);

        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", postsPage.getNumber());
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("categories", blogService.getPublishedCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("featured", postsPage.getNumber() == 0 && !postsPage.isEmpty()
                ? postsPage.getContent().get(0) : null);
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
