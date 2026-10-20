package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.AdminRefundController;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.order.Refund;
import com.example.lumoo.domain.order.RefundService;
import com.example.lumoo.domain.pdpp.AuditService;
import com.example.lumoo.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = AdminRefundController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AdminRefundControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RefundService refundService;
    @MockitoBean OrderService orderService;
    @MockitoBean AuditService auditService;
    @MockitoBean SiteSettingsService siteSettingsService;
    private static Order order() {
        Order o = new Order();
        o.setId(7L);
        o.setStatus("DELIVERED");
        o.setPaymentMethod("MODEMPAY");
        o.setTotalAmount(120);
        o.setShippingCost(20.0);
        User u = new User();
        u.setEmail("buyer@test.com");
        o.setUser(u);
        OrderItem i = new OrderItem();
        i.setId(3L);
        i.setProductName("Shirt");
        i.setPrice(50.0);
        i.setQuantity(2);
        o.setItems(List.of(i));
        return o;
    }
    @Test
    void list_rendersPendingRefund() throws Exception {
        Refund r = new Refund();
        r.setOrder(order());
        r.setAmount(50);
        r.setReason("Damaged");
        r.setItemsSummary("1× Shirt");
        r.setDueBy(LocalDate.now().plusDays(10));
        when(refundService.getPending()).thenReturn(List.of(r));
        mvc.perform(get("/admin/refunds"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("GMD 50.00")))
                .andExpect(content().string(containsString("Mark sent")));
    }
    @Test
    void preview_showsServerComputedAmounts() throws Exception {
        Order o = order();
        when(orderService.findByIdWithItems(7L)).thenReturn(Optional.of(o));
        when(refundService.isRefundable(o)).thenReturn(true);
        when(refundService.quote(eq(7L), eq(Map.of(3L, 1)), eq(true)))
                .thenReturn(new RefundService.Breakdown(50, 0, 20, 70, 65, 5, "1× Shirt, shipping"));
        mvc.perform(get("/admin/refunds/new").param("orderId", "7").param("qty_3", "1")
                        .param("shipping", "true").param("preview", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("GMD 70.00")))
                .andExpect(content().string(containsString("Approve refund")));
    }
    @Test
    void create_passesParsedQuantities_andAudits() throws Exception {
        Refund r = new Refund();
        r.setAmount(50);
        r.setDueBy(LocalDate.now());
        when(refundService.create(eq(7L), eq(Map.of(3L, 1)), eq(false), eq("Damaged"), any())).thenReturn(r);
        mvc.perform(post("/admin/refunds").param("orderId", "7").param("qty_3", "1").param("reason", "Damaged"))
                .andExpect(redirectedUrl("/admin/refunds"))
                .andExpect(flash().attribute("flashType", "green"));
        verify(auditService).log(eq("REFUND_APPROVED"), eq("Refund"), any(), isNull(), any(), any());
    }
    @Test
    void create_error_goesBackToForm() throws Exception {
        when(refundService.create(anyLong(), any(), anyBoolean(), any(), any()))
                .thenThrow(new RefundService.RefundException("Nothing to refund"));
        mvc.perform(post("/admin/refunds").param("orderId", "7").param("reason", "x"))
                .andExpect(redirectedUrl("/admin/refunds/new?orderId=7"))
                .andExpect(flash().attribute("flashType", "red"));
    }
    @Test
    void complete_withoutReference_isRefused() throws Exception {
        when(refundService.complete(eq(4L), any(), any(), any())).thenReturn(RefundService.CompleteResult.REFERENCE_REQUIRED);
        mvc.perform(post("/admin/refunds/4/complete").param("method", "Wave"))
                .andExpect(flash().attribute("flashType", "red"));
        verifyNoInteractions(auditService);
    }
}
