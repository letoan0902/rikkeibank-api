package com.rikkeibank.account.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final InternalKeyInterceptor internalKeyInterceptor;

    public WebMvcConfig(InternalKeyInterceptor internalKeyInterceptor) {
        this.internalKeyInterceptor = internalKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalKeyInterceptor).addPathPatterns("/internal/**");
    }
}
