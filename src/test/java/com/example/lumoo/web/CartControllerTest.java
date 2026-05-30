package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.CartController;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = CartController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class CartControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CartService cartService;
    @MockitoBean ProductService productService;
    @MockitoBean UserService userService;
    @MockitoBean SiteSettingsService siteSettingsService;
    private UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }
    private User user(long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }
    @Test
    void viewCart_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void viewCart_returns200_whenAuthenticated() throws Exception {
        User u = user(1L, "buyer@test.com");
        Product p = new Product();
        p.setId(1L);
        p.setName("Hat");
        p.setPrice(25.0);
        CartItem item = new CartItem();
        item.setProduct(p);
        item.setQuantity(1);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(cartService.getTotal(any())).thenReturn(25.0);
        mvc.perform(get("/cart").principal(principal("buyer@test.com")))
                .andExpect(status().isOk());
    }
    @Test
    void addToCart_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(post("/cart/add/1"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void addToCart_redirects_whenProductNotFound() throws Exception {
        when(productService.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(post("/cart/add/99").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?error=product_not_found"));
    }
    @Test
    void addToCart_redirects_whenProductNotApproved() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setApproved(false);
        when(productService.findById(1L)).thenReturn(Optional.of(p));
        mvc.perform(post("/cart/add/1").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?error=product_unavailable"));
    }
    @Test
    void addToCart_redirectsToCart_whenSuccessful() throws Exception {
        User u = user(1L, "buyer@test.com");
        Product p = new Product();
        p.setId(1L);
        p.setApproved(true);
        p.setStock(10);
        when(productService.findById(1L)).thenReturn(Optional.of(p));
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        mvc.perform(post("/cart/add/1").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/cart?success_add"));
        verify(cartService).addItem(u, p);
    }
    @Test
    void removeFromCart_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/cart/remove/1"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void removeFromCart_redirectsToCart_whenRemoved() throws Exception {
        User u = user(1L, "buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.removeItem(1L, u)).thenReturn(true);
        mvc.perform(get("/cart/remove/1").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/cart?removed"));
    }
    @Test
    void removeFromCart_redirectsWithError_whenUnauthorized() throws Exception {
        User u = user(1L, "buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.removeItem(1L, u)).thenReturn(false);
        mvc.perform(get("/cart/remove/1").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/cart?error=unauthorized"));
    }
}
