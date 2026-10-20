package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderController;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.payment.ModemPayService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.shipping.ShippingService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
    @MockitoBean ShippingService shippingService;
    @MockitoBean com.example.lumoo.domain.promo.PromoService promoService;
    private UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }
    private User user(long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setEmailVerified(true);
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
        when(orderService.placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyDouble(), any(), any()))
                .thenReturn(List.of(order));
        when(shippingService.quoteFor(any(), anyDouble())).thenReturn(Optional.of(0.0));
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
        when(orderService.placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyDouble(), any(), any()))
                .thenReturn(List.of(order));
        when(shippingService.quoteFor(any(), anyDouble())).thenReturn(Optional.of(0.0));
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
    @Test
    void checkout_passesCartWeightsToJs_withoutUserData() throws Exception {
        User u = user(1L, "buyer@test.com");
        u.setPassword("BUYER_PASSWORD_HASH");
        User vendor = user(2L, "vendor@test.com");
        vendor.setPassword("VENDOR_PASSWORD_HASH");
        Product p = new Product();
        p.setId(7L);
        p.setVendor(vendor);
        p.setWeightKg(2.5);
        CartItem item = new CartItem();
        item.setUser(u);
        item.setProduct(p);
        item.setName("Cement");
        item.setQuantity(2);
        item.setPrice(100.0);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(cartService.getTotal(any())).thenReturn(200.0);
        mvc.perform(get("/checkout").principal(principal("buyer@test.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"weightKg\":2.5")))
                .andExpect(content().string(containsString("var subtotal  = 200.0;")))
                .andExpect(content().string(not(containsString("BUYER_PASSWORD_HASH"))))
                .andExpect(content().string(not(containsString("VENDOR_PASSWORD_HASH"))));
    }
    @Test
    void placeOrder_ignoresClientShippingCost_andUsesServerQuote() throws Exception {
        User u = user(1L, "buyer@test.com");
        CartItem item = new CartItem();
        item.setQuantity(3);
        Order order = new Order();
        order.setId(42L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(shippingService.quoteFor("Gamcel Express", 3.0)).thenReturn(Optional.of(150.0));
        when(orderService.placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyDouble(), any(), any()))
                .thenReturn(List.of(order));
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "123 Street")
                        .param("paymentMethod", "COD")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true")
                        .param("courierName", "Gamcel Express")
                        .param("shippingCost", "-500"))
                .andExpect(status().is3xxRedirection());
        verify(orderService).placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(),
                eq("Gamcel Express"), eq(150.0), any(), any());
    }
    @Test
    void placeOrder_rejectsUnknownCourier() throws Exception {
        User u = user(1L, "buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(new CartItem()));
        when(shippingService.quoteFor(any(), anyDouble())).thenReturn(Optional.empty());
        mvc.perform(post("/order/place")
                        .principal(principal("buyer@test.com"))
                        .param("address", "123 Street")
                        .param("paymentMethod", "COD")
                        .param("privacyAccepted", "true")
                        .param("termsAccepted", "true")
                        .param("courierName", "Fake Courier"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/checkout?error=invalid_shipping"));
        verify(orderService, never()).placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyDouble(), any(), any());
    }
    @Test
    void checkout_showsPromoDiscount_andPayableTotal() throws Exception {
        User u = user(1L, "buyer@test.com");
        CartItem item = new CartItem(); item.setName("Cement"); item.setQuantity(1); item.setPrice(200.0);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(item));
        when(cartService.getTotal(any())).thenReturn(200.0);
        when(promoService.quote("save", 200.0)).thenReturn(Optional.of(20.0));
        mvc.perform(get("/checkout").param("promo", "save").principal(principal("buyer@test.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Discount (SAVE)")))
                .andExpect(content().string(containsString("name=\"promoCode\" value=\"SAVE\"")))
                .andExpect(content().string(containsString("GMD 180.00")));
    }
    @Test
    void placeOrder_promoRunOut_redirectsWithError() throws Exception {
        User u = user(1L, "buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(cartService.getItems(u)).thenReturn(List.of(new CartItem()));
        when(shippingService.quoteFor(any(), anyDouble())).thenReturn(Optional.of(0.0));
        when(orderService.placeOrders(any(), anyString(), anyString(), anyList(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyDouble(), any(), eq("SAVE")))
                .thenThrow(new OrderService.PromoUnavailableException("SAVE"));
        mvc.perform(post("/order/place").principal(principal("buyer@test.com"))
                        .param("address", "1 Street").param("paymentMethod", "COD")
                        .param("privacyAccepted", "true").param("termsAccepted", "true")
                        .param("promoCode", "SAVE"))
                .andExpect(header().string("Location", "/checkout?error=promo_invalid"));
    }
}
