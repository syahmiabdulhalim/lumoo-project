package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.user.CustomUserDetailsService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.domain.vendor.VendorController;
import com.example.lumoo.infrastructure.security.CustomSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorController.class)
class VendorControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ProductService productService;
    @MockitoBean OrderService orderService;
    @MockitoBean UserService userService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean CustomSuccessHandler customSuccessHandler;
    @MockitoBean PasswordEncoder passwordEncoder;
    @MockitoBean SiteSettingsService siteSettingsService;

    private User vendor(long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setUsername("vendor");
        return u;
    }

    // ── GET /vendor/dashboard ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void dashboard_returns200() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(productService.getByVendor(v)).thenReturn(List.of());
        when(orderService.getVendorOrders(1L)).thenReturn(List.of());

        mvc.perform(get("/vendor/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboard_blocksAccess_whenUnauthenticated() throws Exception {
        mvc.perform(get("/vendor/dashboard"))
                .andExpect(status().is(org.hamcrest.Matchers.not(200)));
    }

    // ── GET /vendor/add-product ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void addProductPage_returns200() throws Exception {
        mvc.perform(get("/vendor/add-product"))
                .andExpect(status().isOk());
    }

    // ── POST /vendor/add-product ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void saveProduct_redirectsToDashboard() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));

        mvc.perform(post("/vendor/add-product")
                        .with(csrf())
                        .param("name", "Iron Sheet")
                        .param("price", "50.0"))
                .andExpect(status().is3xxRedirection());
        verify(productService).addProduct(any(Product.class), eq(v));
    }

    // ── GET /vendor/edit-product/{id} ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void editProductPage_returns200_whenOwned() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        Product p = new Product();
        p.setId(1L);
        p.setVendor(v);
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(productService.findById(1L)).thenReturn(Optional.of(p));

        mvc.perform(get("/vendor/edit-product/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void editProductPage_redirects_whenNotOwner() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        User other = vendor(2L, "other@test.com");
        Product p = new Product();
        p.setId(1L);
        p.setVendor(other);
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(productService.findById(1L)).thenReturn(Optional.of(p));

        mvc.perform(get("/vendor/edit-product/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void editProductPage_redirects_whenProductNotFound() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(productService.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/vendor/edit-product/99"))
                .andExpect(status().is3xxRedirection());
    }

    // ── POST /vendor/edit-product/{id} ────────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void updateProduct_redirectsToDashboard_whenSuccessful() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(productService.updateProduct(eq(1L), any(), eq(v), any())).thenReturn(true);

        mvc.perform(post("/vendor/edit-product/1")
                        .with(csrf())
                        .param("name", "Updated Name"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void updateProduct_redirectsToDashboard_whenUnauthorized() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(productService.updateProduct(eq(1L), any(), eq(v), any())).thenReturn(false);

        mvc.perform(post("/vendor/edit-product/1")
                        .with(csrf())
                        .param("name", "Updated Name"))
                .andExpect(status().is3xxRedirection());
    }

    // ── POST /vendor/order/{id}/ship ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void shipOrder_shipped_redirectsToDashboard() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(orderService.markShipped(eq(1L), eq(1L), any())).thenReturn(OrderService.ShipResult.SHIPPED);

        mvc.perform(post("/vendor/order/1/ship")
                        .with(csrf())
                        .param("trackingNumber", "TRK123"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void shipOrder_notFound_redirectsToDashboard() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(orderService.markShipped(eq(1L), eq(1L), any())).thenReturn(OrderService.ShipResult.NOT_FOUND);

        mvc.perform(post("/vendor/order/1/ship").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void shipOrder_unauthorized_redirectsToDashboard() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(orderService.markShipped(eq(1L), eq(1L), any())).thenReturn(OrderService.ShipResult.UNAUTHORIZED);

        mvc.perform(post("/vendor/order/1/ship").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void shipOrder_invalidStatus_redirectsToDashboard() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));
        when(orderService.markShipped(eq(1L), eq(1L), any())).thenReturn(OrderService.ShipResult.INVALID_STATUS);

        mvc.perform(post("/vendor/order/1/ship").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // ── GET /vendor/delete-product/{id} ──────────────────────────────────────

    @Test
    @WithMockUser(username = "vendor@test.com", roles = "VENDOR")
    void deleteProduct_redirectsToDashboard() throws Exception {
        User v = vendor(1L, "vendor@test.com");
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(v));

        mvc.perform(get("/vendor/delete-product/1"))
                .andExpect(status().is3xxRedirection());
        verify(productService).deleteByVendor(1L, v);
    }
}
