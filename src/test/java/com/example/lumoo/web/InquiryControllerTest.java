package com.example.lumoo.web;

import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.inquiry.InquiryController;
import com.example.lumoo.domain.inquiry.InquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = InquiryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class InquiryControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean InquiryService inquiryService;
    @MockitoBean SiteSettingsService siteSettingsService;

    @Test
    void sendInquiry_redirects_whenSent() throws Exception {
        mvc.perform(post("/inquiry/send/1")
                        .param("buyerName", "Ali")
                        .param("email", "ali@test.com")
                        .param("message", "Is this available?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?inquiry_sent"));
        verify(inquiryService).send(1L, "Ali", "ali@test.com", "Is this available?");
    }
}
