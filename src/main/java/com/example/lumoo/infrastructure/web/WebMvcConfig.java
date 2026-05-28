package com.example.lumoo.infrastructure.web;

import com.example.lumoo.domain.admin.SiteSettingsInterceptor;
import com.example.lumoo.infrastructure.security.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:/app/uploads/products}")
    private String uploadDir;

    @Autowired private SiteSettingsInterceptor siteSettingsInterceptor;
    @Autowired private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
        registry.addInterceptor(siteSettingsInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + uploadDir + "/");
        String avatarDir = uploadDir.replace("/products", "/avatars");
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + avatarDir + "/");
        String kycDir = uploadDir.replace("/products", "/kyc");
        registry.addResourceHandler("/uploads/kyc/**")
                .addResourceLocations("file:" + kycDir + "/");
    }
}
