package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.payment.ModemPayService;
import com.example.lumoo.domain.payment.PaymentController;
import com.example.lumoo.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = PaymentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class PaymentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ModemPayService modemPayService;
    @MockitoBean OrderService orderService;
    @MockitoBean UserService userService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @Test
    void webhook_invalidSignature_returns401() throws Exception {
        when(modemPayService.verifySignature(anyString(), any())).thenReturn(false);
        mvc.perform(post("/api/payment/webhook").content("{}").header("x-modem-signature", "bad"))
                .andExpect(status().isUnauthorized());
        verify(modemPayService, never()).processWebhook(anyString());
    }
    @Test
    void webhook_processingError_returns500_soModemPayRetries() throws Exception {
        when(modemPayService.verifySignature(anyString(), any())).thenReturn(true);
        doThrow(new RuntimeException("db down")).when(modemPayService).processWebhook(anyString());
        mvc.perform(post("/api/payment/webhook").content("{}").header("x-modem-signature", "ok"))
                .andExpect(status().isInternalServerError());
    }
    @Test
    void webhook_success_returns200() throws Exception {
        when(modemPayService.verifySignature(anyString(), any())).thenReturn(true);
        mvc.perform(post("/api/payment/webhook").content("{}").header("x-modem-signature", "ok"))
                .andExpect(status().isOk());
    }
}
