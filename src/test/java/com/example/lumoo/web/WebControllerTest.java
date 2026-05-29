package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.blog.BlogService;
import com.example.lumoo.infrastructure.security.RateLimitInterceptor;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.product.ReviewService;
import com.example.lumoo.domain.subscriber.SubscriberService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.domain.vendor.VendorApplicationService;
import com.example.lumoo.shared.WebController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.payment.ModemPayService;
import com.example.lumoo.domain.product.Product;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = WebController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class WebControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ProductService productService;
    @MockitoBean UserService userService;
    @MockitoBean OrderService orderService;
    @MockitoBean ReviewService reviewService;
    @MockitoBean VendorApplicationService vendorApplicationService;
    @MockitoBean BlogService blogService;
    @MockitoBean SubscriberService subscriberService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @MockitoBean ModemPayService modemPayService;
    @MockitoBean RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void allowRequests() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // ── public routes ─────────────────────────────────────────────────────────

    @Test
    void home_returns200() throws Exception {
        when(productService.getAllApprovedList()).thenReturn(List.of());
        when(blogService.getPublished()).thenReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void home_withKeyword_returns200() throws Exception {
        when(productService.searchApproved("hat")).thenReturn(List.of());
        when(blogService.getPublished()).thenReturn(List.of());

        mvc.perform(get("/").param("keyword", "hat"))
                .andExpect(status().isOk());
    }

    @Test
    void home_withCategory_returns200() throws Exception {
        when(productService.getApprovedByCategory("Clothing")).thenReturn(List.of());
        when(blogService.getPublished()).thenReturn(List.of());

        mvc.perform(get("/").param("category", "Clothing"))
                .andExpect(status().isOk());
    }

    @Test
    void productDetail_returns200_whenProductExists() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setName("Hat");
        when(productService.findById(1L)).thenReturn(Optional.of(p));
        when(reviewService.getByProduct(p)).thenReturn(List.of());
        when(reviewService.getAverageRating(List.of())).thenReturn(0.0);

        mvc.perform(get("/product/1"))
                .andExpect(status().isOk());
    }

    @Test
    void productDetail_redirects_whenProductNotFound() throws Exception {
        when(productService.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/product/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?error=not_found"));
    }

    @Test
    void stores_returns200() throws Exception {
        when(productService.getVendorsWithProducts()).thenReturn(List.of());

        mvc.perform(get("/stores"))
                .andExpect(status().isOk());
    }

    @Test
    void store_redirects_whenVendorNotFound() throws Exception {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/store/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/stores"));
    }

    @Test
    void store_returns200_whenVendorExists() throws Exception {
        User vendor = new User();
        vendor.setId(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(vendor));
        when(productService.getApprovedByVendor(vendor)).thenReturn(List.of());

        mvc.perform(get("/store/1"))
                .andExpect(status().isOk());
    }

    @Test
    void privacyPolicy_returns200() throws Exception {
        when(siteSettingsService.get()).thenReturn(null);

        mvc.perform(get("/privacy-policy"))
                .andExpect(status().isOk());
    }

    @Test
    void terms_returns200() throws Exception {
        when(siteSettingsService.get()).thenReturn(null);

        mvc.perform(get("/terms"))
                .andExpect(status().isOk());
    }

    @Test
    void cookiePolicy_returns200() throws Exception {
        when(siteSettingsService.get()).thenReturn(null);

        mvc.perform(get("/cookie-policy"))
                .andExpect(status().isOk());
    }

    @Test
    void returns_returns200() throws Exception {
        when(siteSettingsService.get()).thenReturn(null);

        mvc.perform(get("/returns"))
                .andExpect(status().isOk());
    }

    // ── protected routes — redirect to login when unauthenticated ─────────────

    @Test
    void buyerDashboard_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/buyer/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void settings_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/settings"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void orderDetails_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/buyer/order/1"))
                .andExpect(status().is3xxRedirection());
    }

    // ── authenticated routes ──────────────────────────────────────────────────

    @Test
    void buyerDashboard_returns200_whenAuthenticated() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderService.getUserOrders(user)).thenReturn(new java.util.ArrayList<>());
        when(vendorApplicationService.hasAlreadyApplied(user)).thenReturn(false);
        when(userService.getAndMarkNotificationsRead(user)).thenReturn(List.of());

        mvc.perform(get("/buyer/dashboard")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().isOk());
    }

    @Test
    void settings_returns200_whenAuthenticated() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        mvc.perform(get("/settings")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().isOk());
    }

    @Test
    void orderDetails_redirects_whenOrderBelongsToDifferentUser() throws Exception {
        User buyer = new User();
        buyer.setId(1L);
        User otherUser = new User();
        otherUser.setId(99L);
        Order order = new Order();
        order.setUser(otherUser);

        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
        when(orderService.findById(5L)).thenReturn(Optional.of(order));

        mvc.perform(get("/buyer/order/5")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().is3xxRedirection());
    }

    // ── POST /subscribe ───────────────────────────────────────────────────────

    @Test
    void subscribe_returnsOk_whenSubscribed() throws Exception {
        when(subscriberService.subscribe("test@test.com"))
                .thenReturn(com.example.lumoo.domain.subscriber.SubscriberService.Result.SUBSCRIBED);

        mvc.perform(post("/subscribe").param("email", "test@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void subscribe_returnsOk_whenAlreadySubscribed() throws Exception {
        when(subscriberService.subscribe("existing@test.com"))
                .thenReturn(com.example.lumoo.domain.subscriber.SubscriberService.Result.ALREADY_SUBSCRIBED);

        mvc.perform(post("/subscribe").param("email", "existing@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void subscribe_returnsBadRequest_whenInvalidEmail() throws Exception {
        when(subscriberService.subscribe("bad"))
                .thenReturn(com.example.lumoo.domain.subscriber.SubscriberService.Result.INVALID);

        mvc.perform(post("/subscribe").param("email", "bad"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /product/{id}/review ─────────────────────────────────────────────

    @Test
    void addReview_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(post("/product/1/review")
                        .param("rating", "5")
                        .param("comment", "Great"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void addReview_redirectsWithError_whenRatingOutOfRange() throws Exception {
        mvc.perform(post("/product/1/review")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of()))
                        .param("rating", "6")
                        .param("comment", "OK"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/product/1?error=invalid_rating"));
    }

    @Test
    void addReview_succeeds_whenEligible() throws Exception {
        User user = new User();
        user.setId(1L);
        Product p = new Product();
        p.setId(1L);

        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productService.findById(1L)).thenReturn(Optional.of(p));
        when(reviewService.canReview(user, p)).thenReturn(true);
        when(reviewService.hasAlreadyReviewed(user, p)).thenReturn(false);

        mvc.perform(post("/product/1/review")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of()))
                        .param("rating", "4")
                        .param("comment", "Good product"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/product/1?review_success"));
        verify(reviewService).addReview(p, user, 4, "Good product");
    }

    @Test
    void addReview_redirectsWithError_whenNotPurchased() throws Exception {
        User user = new User();
        user.setId(1L);
        Product p = new Product();
        p.setId(1L);

        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productService.findById(1L)).thenReturn(Optional.of(p));
        when(reviewService.canReview(user, p)).thenReturn(false);

        mvc.perform(post("/product/1/review")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of()))
                        .param("rating", "5")
                        .param("comment", "Nice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/product/1?error=not_purchased"));
    }

    // ── POST /buyer/order/{id}/received ──────────────────────────────────────

    @Test
    void markReceived_delivered_redirectsWithReceived() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderService.markDelivered(1L, 1L)).thenReturn(OrderService.DeliverResult.DELIVERED);

        mvc.perform(post("/buyer/order/1/received")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/buyer/order/1?received"));
    }

    @Test
    void markReceived_unauthorized_redirectsToDashboard() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderService.markDelivered(1L, 1L)).thenReturn(OrderService.DeliverResult.UNAUTHORIZED);

        mvc.perform(post("/buyer/order/1/received")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().is3xxRedirection());
    }

    // ── POST /buyer/order/{id}/return ─────────────────────────────────────────

    @Test
    void requestReturn_redirectsWithRequested() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderService.requestReturn(1L, 1L, "Damaged")).thenReturn(OrderService.ReturnResult.REQUESTED);

        mvc.perform(post("/buyer/order/1/return")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of()))
                        .param("returnReason", "Damaged"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/buyer/order/1?return_requested"));
    }

    // ── POST /buyer/order/delete/{id} ─────────────────────────────────────────

    @Test
    void cancelOrder_cancelled_redirectsToDashboard() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderService.cancelOrder(eq(1L), any())).thenReturn(OrderService.CancelResult.CANCELLED);

        mvc.perform(post("/buyer/order/delete/1")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/buyer/dashboard?order_cancelled"));
    }
}
