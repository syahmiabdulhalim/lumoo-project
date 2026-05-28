package com.example.lumoo.domain.order;

import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
public class CartController {

    @Autowired private CartService cartService;
    @Autowired private ProductService productService;
    @Autowired private UserService userService;

    @GetMapping("/cart")
    public String viewCart(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        List<CartItem> items = cartService.getItems(user);
        model.addAttribute("cart", items);
        model.addAttribute("total", cartService.getTotal(items));
        return "cart";
    }

    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        Product product = productService.findById(id).orElse(null);
        if (product == null) return "redirect:/?error=product_not_found";
        if (!product.isApproved()) return "redirect:/?error=product_unavailable";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        cartService.addItem(user, product);
        return "redirect:/cart?success_add";
    }

    @GetMapping("/cart/remove/{id}")
    public String removeFromCart(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        boolean removed = cartService.removeItem(id, user);
        return removed ? "redirect:/cart?removed" : "redirect:/cart?error=unauthorized";
    }
}
