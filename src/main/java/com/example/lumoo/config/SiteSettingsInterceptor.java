package com.example.lumoo.config;

import com.example.lumoo.service.SiteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class SiteSettingsInterceptor implements HandlerInterceptor {

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView mav) {
        if (mav == null) return;
        String view = mav.getViewName();
        if (view == null || view.startsWith("redirect:") || view.startsWith("forward:")) return;
        mav.addObject("siteSettings", siteSettingsService.get());
    }
}
