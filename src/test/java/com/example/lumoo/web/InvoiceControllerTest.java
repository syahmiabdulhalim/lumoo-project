package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.order.InvoiceController;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = InvoiceController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class InvoiceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderRepository orderRepository;
    @MockitoBean SiteSettingsService siteSettingsService;
    private Order orderFor(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername("testuser");
        Order order = new Order();
        order.setUser(user);
        order.setItems(new ArrayList<>());
        order.setTotalAmount(250.0);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }
    @Test
    void generateInvoice_returnsForbidden_whenUnauthenticated() throws Exception {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(orderFor("buyer@test.com")));
        mvc.perform(get("/buyer/invoice/1"))
                .andExpect(status().isForbidden());
    }
    @Test
    void generateInvoice_returnsForbidden_whenWrongUser() throws Exception {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(orderFor("buyer@test.com")));
        mvc.perform(get("/buyer/invoice/1")
                        .principal(new UsernamePasswordAuthenticationToken("other@test.com", null, List.of())))
                .andExpect(status().isForbidden());
    }
    @Test
    void generateInvoice_returns200_whenCorrectUser() throws Exception {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(orderFor("buyer@test.com")));
        mvc.perform(get("/buyer/invoice/1")
                        .principal(new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
