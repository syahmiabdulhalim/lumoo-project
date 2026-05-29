package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.vendor.VendorApplicationController;
import com.example.lumoo.domain.vendor.VendorApplicationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = VendorApplicationController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class VendorApplicationControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean VendorApplicationService vendorApplicationService;
    @MockitoBean UserService userService;
    @MockitoBean SiteSettingsService siteSettingsService;

    private UsernamePasswordAuthenticationToken buyer(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }

    private User userWithRole(long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    // ── GET /buyer/vendorapply ────────────────────────────────────────────────

    @Test
    void applyPage_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(get("/buyer/vendorapply"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void applyPage_redirects_whenNotBuyer() throws Exception {
        User vendor = userWithRole(1L, "vendor@test.com", Role.VENDOR);
        when(userService.findByEmail("vendor@test.com")).thenReturn(Optional.of(vendor));

        mvc.perform(get("/buyer/vendorapply").principal(buyer("vendor@test.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?error=not_eligible"));
    }

    @Test
    void applyPage_returns200_forEligibleUser() throws Exception {
        User user = userWithRole(1L, "buyer@test.com", Role.USER);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(vendorApplicationService.getByUser(user)).thenReturn(List.of());

        mvc.perform(get("/buyer/vendorapply").principal(buyer("buyer@test.com")))
                .andExpect(status().isOk());
    }

    @Test
    void applyPage_showsPendingState_whenPendingApplicationExists() throws Exception {
        User user = userWithRole(1L, "buyer@test.com", Role.USER);
        VendorApplication pending = new VendorApplication();
        pending.setStatus("PENDING");

        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(vendorApplicationService.getByUser(user)).thenReturn(List.of(pending));

        mvc.perform(get("/buyer/vendorapply").principal(buyer("buyer@test.com")))
                .andExpect(status().isOk());
    }

    // ── POST /buyer/vendorapply ───────────────────────────────────────────────

    @Test
    void submitApplication_redirectsToLogin_whenUnauthenticated() throws Exception {
        mvc.perform(post("/buyer/vendorapply")
                        .param("businessName", "ACME Ltd")
                        .param("businessType", "Retail")
                        .param("phone", "+220111111")
                        .param("ownerFullName", "Ali Bah")
                        .param("ownerIdType", "NID")
                        .param("ownerIdNumber", "NID123")
                        .param("bankAccountNumber", "ACC-001")
                        .param("productsToSell", "Cement")
                        .param("reason", "Grow business")
                        .param("yearsInBusiness", "2")
                        .param("estimatedMonthlyTurnover", "5000")
                        .param("businessAddress", "Banjul")
                        .param("businessRegion", "Greater Banjul")
                        .param("ownerAddress", "12 St")
                        .param("bankName", "GTBank")
                        .param("bankAccountName", "Ali Bah"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void submitApplication_redirects_whenMissingRequiredFields() throws Exception {
        User user = userWithRole(1L, "buyer@test.com", Role.USER);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(vendorApplicationService.canReapply(user)).thenReturn(true);

        mvc.perform(post("/buyer/vendorapply")
                        .principal(buyer("buyer@test.com"))
                        .param("businessName", "")
                        .param("businessType", "Retail")
                        .param("phone", "+220111111")
                        .param("ownerFullName", "Ali")
                        .param("ownerIdType", "NID")
                        .param("ownerIdNumber", "12345")
                        .param("bankAccountNumber", "111")
                        .param("productsToSell", "Cement")
                        .param("reason", "To sell")
                        .param("yearsInBusiness", "2")
                        .param("estimatedMonthlyTurnover", "5000")
                        .param("businessAddress", "Banjul")
                        .param("businessRegion", "Greater Banjul")
                        .param("ownerAddress", "123 St")
                        .param("bankName", "GTBANK")
                        .param("bankAccountName", "Ali Trading"))
                .andExpect(status().is3xxRedirection());
        verify(vendorApplicationService, never()).apply(any(), any());
    }

    @Test
    void submitApplication_succeeds_whenAllFieldsFilled() throws Exception {
        User user = userWithRole(1L, "buyer@test.com", Role.USER);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(vendorApplicationService.canReapply(user)).thenReturn(true);

        mvc.perform(post("/buyer/vendorapply")
                        .principal(buyer("buyer@test.com"))
                        .param("businessName", "ACME Ltd")
                        .param("businessType", "Retail")
                        .param("phone", "+220111111")
                        .param("ownerFullName", "Ali Bah")
                        .param("ownerIdType", "NID")
                        .param("ownerIdNumber", "NID123")
                        .param("bankAccountNumber", "ACC-001")
                        .param("productsToSell", "Cement, Sand")
                        .param("reason", "To grow my business")
                        .param("yearsInBusiness", "3")
                        .param("estimatedMonthlyTurnover", "10000")
                        .param("businessAddress", "12 Banjul St")
                        .param("businessRegion", "Greater Banjul")
                        .param("ownerAddress", "12 Banjul St")
                        .param("bankName", "GTBank")
                        .param("bankAccountName", "Ali Bah"))
                .andExpect(status().is3xxRedirection());
        verify(vendorApplicationService).apply(eq(user), any(VendorApplication.class));
    }

    @Test
    void submitApplication_redirects_whenCannotReapply() throws Exception {
        User user = userWithRole(1L, "buyer@test.com", Role.USER);
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(vendorApplicationService.canReapply(user)).thenReturn(false);

        mvc.perform(post("/buyer/vendorapply")
                        .principal(buyer("buyer@test.com"))
                        .param("businessName", "ACME Ltd")
                        .param("businessType", "Retail")
                        .param("phone", "+220111111")
                        .param("ownerFullName", "Ali Bah")
                        .param("ownerIdType", "NID")
                        .param("ownerIdNumber", "NID123")
                        .param("bankAccountNumber", "ACC-001")
                        .param("productsToSell", "Cement")
                        .param("reason", "Grow business")
                        .param("yearsInBusiness", "2")
                        .param("estimatedMonthlyTurnover", "5000")
                        .param("businessAddress", "Banjul")
                        .param("businessRegion", "Greater Banjul")
                        .param("ownerAddress", "12 St")
                        .param("bankName", "GTBank")
                        .param("bankAccountName", "Ali Bah"))
                .andExpect(status().is3xxRedirection());
        verify(vendorApplicationService, never()).apply(any(), any());
    }
}
