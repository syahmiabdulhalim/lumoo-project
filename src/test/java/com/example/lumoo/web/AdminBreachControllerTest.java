package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.pdpp.AdminBreachController;
import com.example.lumoo.domain.pdpp.BreachIncident;
import com.example.lumoo.domain.pdpp.DataBreachService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = AdminBreachController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AdminBreachControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean DataBreachService dataBreachService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @Test
    void list_rendersWithIncidents() throws Exception {
        BreachIncident b = new BreachIncident();
        ReflectionTestUtils.setField(b, "id", 3L);
        b.setDescription("test");
        b.setDiscoveredAt(java.time.LocalDateTime.now());
        when(dataBreachService.getAll()).thenReturn(List.of(b));
        when(dataBreachService.findById(3L)).thenReturn(java.util.Optional.of(b));
        when(dataBreachService.buildRegulatorEmail(b)).thenReturn("<p>preview</p>");
        mvc.perform(get("/admin/breaches").param("view", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Send to Information Commission")));
    }
    @Test
    void report_recordsBreach_andRedirectsToIt() throws Exception {
        BreachIncident saved = new BreachIncident();
        ReflectionTestUtils.setField(saved, "id", 9L);
        when(dataBreachService.reportBreach(eq("Laptop stolen"), any(), eq(5), any(), any(), any(), any())).thenReturn(saved);
        mvc.perform(post("/admin/breaches").param("description", "Laptop stolen").param("affectedUsers", "5")
                        .param("discoveredAt", "2026-10-21T08:30"))
                .andExpect(redirectedUrl("/admin/breaches?view=9"));
        verify(dataBreachService, never()).notifyRegulator(anyLong(), any(), any());
    }
    @Test
    void notifyRegulator_isExplicitAction() throws Exception {
        when(dataBreachService.notifyRegulator(eq(9L), any(), any())).thenReturn(DataBreachService.NotifyResult.SENT);
        mvc.perform(post("/admin/breaches/9/notify-regulator"))
                .andExpect(redirectedUrl("/admin/breaches?view=9"))
                .andExpect(flash().attribute("flashType", "green"));
    }
}
