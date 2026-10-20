package com.example.lumoo.security;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Runs the real SecurityConfig against the H2 test context to check URL access rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesIntegrationTest {
    @Autowired MockMvc mvc;
    @Test
    void customerRightsErasure_requiresLogin() throws Exception {
        mvc.perform(post("/api/customer-rights/erasure-request").with(csrf())
                        .param("email", "victim@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
    @Test
    void customerRightsDataAccess_requiresLogin() throws Exception {
        mvc.perform(post("/api/customer-rights/data-access-request").with(csrf())
                        .param("email", "victim@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
    /** State-changing admin endpoints: GET is not allowed, POST without a CSRF token is refused. */
    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/approve-product/1", "/admin/reject-product/1", "/admin/delete-product/1",
            "/admin/delete-user/1", "/admin/delete-order/1", "/admin/delete-inquiry/1",
            "/admin/approve-image/1", "/admin/reject-image/1", "/admin/approve-vendor/1"})
    @WithMockUser(roles = "ADMIN")
    void adminActions_rejectGet_andRequireCsrf(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(url)).andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(roles = "VENDOR")
    void vendorDeleteProduct_rejectsGet_andRequiresCsrf() throws Exception {
        mvc.perform(get("/vendor/delete-product/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/vendor/delete-product/1")).andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser
    void cartRemove_rejectsGet_andRequiresCsrf() throws Exception {
        mvc.perform(get("/cart/remove/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/cart/remove/1")).andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(roles = "VENDOR")
    void breachRegister_isAdminOnly() throws Exception {
        mvc.perform(get("/admin/breaches")).andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void breachNotify_requiresCsrf() throws Exception {
        mvc.perform(post("/admin/breaches/1/notify-regulator")).andExpect(status().isForbidden());
    }
}
