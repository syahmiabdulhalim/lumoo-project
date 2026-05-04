package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@Controller
public class CartController {

    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/cart")
    public String viewCart(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<CartItem> cartItems = cartRepository.findByUser(user);
        
        double total = cartItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        
        model.addAttribute("cart", cartItems);
        model.addAttribute("total", total);
        return "cart";
    }
@GetMapping("/cart/remove/{id}")
public String removeFromCart(@PathVariable Long id, Principal principal) {
    if (principal == null) return "redirect:/login";

    User user = userRepository.findByEmail(principal.getName()).orElse(null);
    if (user == null) return "redirect:/login";

    CartItem item = cartRepository.findById(id).orElse(null);
    if (item == null) return "redirect:/cart?error=item_not_found";

    // Make sure the item belongs to this user
    if (!item.getUser().getId().equals(user.getId()))
        return "redirect:/cart?error=unauthorized";

    cartRepository.delete(item);
    return "redirect:/cart?removed";
}
   @PostMapping("/cart/add/{id}")
public String addToCart(@PathVariable Long id, Principal principal) {
    if (principal == null) return "redirect:/login";

    Product product = productRepository.findById(id).orElse(null);
    if (product == null) return "redirect:/?error=product_not_found";

    // Must be approved before it can be added
    if (!product.isApproved())
    return "redirect:/?error=product_unavailable";

    User user = userRepository.findByEmail(principal.getName()).orElse(null);
if (user == null) return "redirect:/login";

    // Only USER role can add to cart
    if (user.getRole() != Role.USER) return "redirect:/?error=not_a_buyer";

    CartItem cartItem = cartRepository.findByUserAndProduct(user, product).orElse(null);
    if (cartItem != null) {
        cartItem.setQuantity(cartItem.getQuantity() + 1);
    } else {
        cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setName(product.getName());
        cartItem.setPrice(product.getPrice());
    }

    cartRepository.save(cartItem);
    return "redirect:/cart?success_add";
}


}