package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.pdpp.CustomerRightsController;
import com.example.lumoo.domain.pdpp.CustomerRightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = CustomerRightsController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class CustomerRightsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomerRightsService customerRightsService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @Test
    void requestErasure_returns200_whenEmailValid() throws Exception {
        when(customerRightsService.processErasureRequest(eq("user@test.com"), any()))
                .thenReturn(Map.of("status", "anonymised"));
        mvc.perform(post("/api/customer-rights/erasure-request")
                        .param("email", "user@test.com"))
                .andExpect(status().isOk());
        verify(customerRightsService).processErasureRequest(eq("user@test.com"), any());
    }
    @Test
    void requestErasure_returns400_whenEmailInvalid() throws Exception {
        mvc.perform(post("/api/customer-rights/erasure-request")
                        .param("email", "not-an-email"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void requestErasure_returns400_whenEmailBlank() throws Exception {
        mvc.perform(post("/api/customer-rights/erasure-request")
                        .param("email", ""))
                .andExpect(status().isBadRequest());
    }
    @Test
    void requestDataAccess_returns200_whenEmailValid() throws Exception {
        when(customerRightsService.processDataAccessRequest(eq("user@test.com"), any()))
                .thenReturn(Map.of("orders", 3));
        mvc.perform(post("/api/customer-rights/data-access-request")
                        .param("email", "user@test.com"))
                .andExpect(status().isOk());
        verify(customerRightsService).processDataAccessRequest(eq("user@test.com"), any());
    }
    @Test
    void requestDataAccess_returns400_whenEmailInvalid() throws Exception {
        mvc.perform(post("/api/customer-rights/data-access-request")
                        .param("email", "bad"))
                .andExpect(status().isBadRequest());
    }
}
