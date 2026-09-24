package com.rikkeibank.account.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    // Gắn internal key cho mọi lời gọi Feign sang service khác
    @Bean
    public RequestInterceptor feignInternalKeyInterceptor(@Value("${rikkeibank.internal-key}") String internalKey) {
        return template -> template.header("X-Internal-Key", internalKey);
    }
}
