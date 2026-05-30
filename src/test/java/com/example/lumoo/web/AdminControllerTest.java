package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettings;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.admin.AdminController;
import com.example.lumoo.domain.inquiry.InquiryService;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.subscriber.SubscriberService;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.vendor.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = AdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AdminControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orderService;
    @MockitoBean ProductService productService;
    @MockitoBean UserService userService;
    @MockitoBean InquiryService inquiryService;
    @MockitoBean VendorApplicationService vendorApplicationService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @MockitoBean SubscriberService subscriberService;
    @Test
    void dashboard_returns200() throws Exception {
        when(orderService.getAll()).thenReturn(List.of());
        when(productService.getAll()).thenReturn(List.of());
        when(userService.getAll()).thenReturn(List.of());
        when(inquiryService.getAll()).thenReturn(List.of());
        when(productService.getPendingApproval()).thenReturn(List.of());
        when(vendorApplicationService.getPending()).thenReturn(List.of());
        when(productService.getPendingImageApproval()).thenReturn(List.of());
        mvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }
    @Test
    void approveProduct_redirects() throws Exception {
        mvc.perform(get("/admin/approve-product/1"))
                .andExpect(status().is3xxRedirection());
        verify(productService).approve(1L);
    }
    @Test
    void rejectProduct_redirects() throws Exception {
        mvc.perform(get("/admin/reject-product/2"))
                .andExpect(status().is3xxRedirection());
        verify(productService).delete(2L);
    }
    @Test
    void deleteProduct_redirects() throws Exception {
        mvc.perform(get("/admin/delete-product/3"))
                .andExpect(status().is3xxRedirection());
        verify(productService).delete(3L);
    }
    @Test
    void deleteUser_redirects() throws Exception {
        mvc.perform(get("/admin/delete-user/4"))
                .andExpect(status().is3xxRedirection());
        verify(userService).delete(4L);
    }
    @Test
    void verifyUser_redirects() throws Exception {
        when(userService.verifyUser(5L)).thenReturn(true);
        mvc.perform(post("/admin/verify-user/5"))
                .andExpect(status().is3xxRedirection());
        verify(userService).verifyUser(5L);
    }
    @Test
    void verifyUser_alreadyVerified_redirects() throws Exception {
        when(userService.verifyUser(6L)).thenReturn(false);
        mvc.perform(post("/admin/verify-user/6"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void upgradeToVendor_redirects() throws Exception {
        when(userService.upgradeToVendor(7L)).thenReturn(true);
        mvc.perform(post("/admin/upgrade-vendor/7"))
                .andExpect(status().is3xxRedirection());
        verify(userService).upgradeToVendor(7L);
    }
    @Test
    void upgradeToVendor_alreadyVendor_redirects() throws Exception {
        when(userService.upgradeToVendor(8L)).thenReturn(false);
        mvc.perform(post("/admin/upgrade-vendor/8"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void updateOrderStatus_redirects() throws Exception {
        mvc.perform(post("/admin/update-order-status")
                        .param("orderId", "1")
                        .param("status", "SHIPPED"))
                .andExpect(status().is3xxRedirection());
        verify(orderService).updateStatus(1L, "SHIPPED");
    }
    @Test
    void deleteOrder_redirects() throws Exception {
        mvc.perform(get("/admin/delete-order/9"))
                .andExpect(status().is3xxRedirection());
        verify(orderService).delete(9L);
    }
    @Test
    void deleteInquiry_redirects() throws Exception {
        mvc.perform(get("/admin/delete-inquiry/10"))
                .andExpect(status().is3xxRedirection());
        verify(inquiryService).delete(10L);
    }
    @Test
    void resolveReturn_redirects() throws Exception {
        mvc.perform(post("/admin/order/11/resolve-return"))
                .andExpect(status().is3xxRedirection());
        verify(orderService).resolveReturn(11L);
    }
    @Test
    void verifyPayment_redirects() throws Exception {
        mvc.perform(post("/admin/order/12/verify-payment"))
                .andExpect(status().is3xxRedirection());
        verify(orderService).verifyPayment(12L);
    }
    @Test
    void approveImage_redirects() throws Exception {
        mvc.perform(get("/admin/approve-image/13"))
                .andExpect(status().is3xxRedirection());
        verify(productService).approveImage(13L);
    }
    @Test
    void rejectImage_redirects() throws Exception {
        mvc.perform(get("/admin/reject-image/14"))
                .andExpect(status().is3xxRedirection());
        verify(productService).rejectImage(14L);
    }
    @Test
    void applicationDetail_returns200_whenFound() throws Exception {
        com.example.lumoo.domain.user.User user = new com.example.lumoo.domain.user.User();
        user.setId(1L);
        user.setEmail("vendor@test.com");
        VendorApplication app = new VendorApplication();
        app.setUser(user);
        when(vendorApplicationService.findById(1L)).thenReturn(Optional.of(app));
        mvc.perform(get("/admin/application/1"))
                .andExpect(status().isOk());
    }
    @Test
    void applicationDetail_redirects_whenNotFound() throws Exception {
        when(vendorApplicationService.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/admin/application/99"))
                .andExpect(status().is3xxRedirection());
    }
    @Test
    void approveVendor_redirects() throws Exception {
        mvc.perform(get("/admin/approve-vendor/15"))
                .andExpect(status().is3xxRedirection());
        verify(vendorApplicationService).approve(15L);
    }
    @Test
    void rejectVendor_redirects() throws Exception {
        mvc.perform(post("/admin/reject-vendor/16").param("note", "Incomplete docs"))
                .andExpect(status().is3xxRedirection());
        verify(vendorApplicationService).reject(16L, "Incomplete docs");
    }
    @Test
    void settings_returns200() throws Exception {
        when(siteSettingsService.get()).thenReturn(new SiteSettings());
        when(subscriberService.getAll()).thenReturn(List.of());
        mvc.perform(get("/admin/settings"))
                .andExpect(status().isOk());
    }
    @Test
    void deleteSubscriber_redirects() throws Exception {
        mvc.perform(post("/admin/subscriber/17/delete"))
                .andExpect(status().is3xxRedirection());
        verify(subscriberService).delete(17L);
    }
    @Test
    void saveSettings_redirects() throws Exception {
        when(siteSettingsService.save(any())).thenReturn(new SiteSettings());
        mvc.perform(post("/admin/settings")
                        .param("businessName", "LUMOO"))
                .andExpect(status().is3xxRedirection());
        verify(siteSettingsService).save(any());
    }
}
