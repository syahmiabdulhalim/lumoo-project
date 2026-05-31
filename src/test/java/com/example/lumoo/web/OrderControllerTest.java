package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderController;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.payment.ModemPayService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = OrderController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orderService;
    @MockitoBean CartService cartService;
    @MockitoBean UserService userService;
    @MockitoBean ModemPayService modemPayService;
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
    void checkout_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void checkout_redirectsToCart_whenCartEmpty() throws Exception {
        User u = user(1L, "buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of());
        mvc.perform(get("/checkout").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/cart"));
    }
    @Test
    void checkout_returns200_whenCartHasItems() throws Exception {
        User u = user(1L, "buyer@test.com");
        CartItem item = new CartItem();
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(cartService.getTotal(any())).thenReturn(50.0);
        mvc.perform(get("/checkout").principal(principal("buyer@test.com")))
                .andExpect(status().isOk());
    }
    @Test
    void placeOrder_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(post("/order/place")
                        .param("address", "123 Street")
                        .param("paymentMethod", "COD")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void placeOrder_redirectsToCheckout_whenNoConsent() throws Exception {
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "123 Street")
                        .param("paymentMethod", "COD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/checkout?error=consent_required"));
    }
    @Test
    void placeOrder_redirectsToCheckout_whenInvalidPayment() throws Exception {
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "123 Street")
                        .param("paymentMethod", "BITCOIN")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/checkout?error=invalid_payment"));
    }
    @Test
    void placeOrder_redirectsToCheckout_whenAddressBlank() throws Exception {
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "   ")
                        .param("paymentMethod", "COD")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/checkout?error=address_required"));
    }
    @Test
    void placeOrder_cod_redirectsToBuyerOrder() throws Exception {
        User u = user(1L, "buyer@test.com");
        CartItem item = new CartItem();
        Order order = new Order();
        order.setId(42L);
        order.setPaymentMethod("COD");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(orderService.placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), any()))
                .thenReturn(List.of(order));
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "123 Street")
                        .param("paymentMethod", "COD")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/buyer/order/42?success"));
    }
    @Test
    void placeOrder_transfer_redirectsToSuccess() throws Exception {
        User u = user(1L, "buyer@test.com");
        CartItem item = new CartItem();
        Order order = new Order();
        order.setId(55L);
        order.setPaymentMethod("TRANSFER");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(orderService.placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), any()))
                .thenReturn(List.of(order));
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "123 Street")
                        .param("paymentMethod", "TRANSFER")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/checkout/success/55"));
    }
    @Test
    void checkoutSuccess_returns200_whenOrderBelongsToUser() throws Exception {
        User u = user(1L, "buyer@test.com");
        Order order = new Order();
        order.setId(10L);
        order.setUser(u);
        order.setPaymentMethod("TRANSFER");
        order.setTotalAmount(100.0);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(orderService.findById(10L)).thenReturn(Optional.of(order));
        mvc.perform(get("/checkout/success/10").principal(principal("buyer@test.com")))
                .andExpect(status().isOk());
    }
    @Test
    void checkoutSuccess_redirects_whenOrderNotFound() throws Exception {
        User u = user(1L, "buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(orderService.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/checkout/success/99").principal(principal("buyer@test.com")))
                .andExpect(status().is3xxRedirection());
    }
}
