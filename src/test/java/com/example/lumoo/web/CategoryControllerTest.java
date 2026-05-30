package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.product.CategoryController;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = CategoryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class CategoryControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService productService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @Test
    void categoryPage_returns200_forKnownCategory() throws Exception {
        when(productService.getApprovedByCategoryPaged(eq("roofing"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));
        mvc.perform(get("/category/roofing"))
                .andExpect(status().isOk());
    }
    @Test
    void categoryPage_returns200_forUnknownCategory() throws Exception {
        when(productService.getApprovedByCategoryPaged(eq("custom"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));
        mvc.perform(get("/category/custom"))
                .andExpect(status().isOk());
    }
    @Test
    void categoryPage_withProducts_returns200() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setName("Iron Sheet");
        when(productService.getApprovedByCategoryPaged(eq("roofing"), any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 8), 1));
        mvc.perform(get("/category/roofing"))
                .andExpect(status().isOk());
    }
    @Test
    void allCategories_returns200() throws Exception {
        mvc.perform(get("/category"))
                .andExpect(status().isOk());
    }
}
