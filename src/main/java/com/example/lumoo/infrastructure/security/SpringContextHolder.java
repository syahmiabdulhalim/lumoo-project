package com.example.lumoo.infrastructure.security;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext context;
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }
    public static <T> T getBean(Class<T> type) {
        if (context == null) return null;
        return context.getBean(type);
    }
}
