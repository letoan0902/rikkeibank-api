package com.rikkeibank.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

// chạy cho mọi request (kể cả không khớp route): gắn header và chặn /internal/**
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayHeaderWebFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Gateway";
    public static final String HEADER_VALUE = "rikkeibank-gateway";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().getHeaders().set(HEADER_NAME, HEADER_VALUE);
        String path = exchange.getRequest().getURI().getPath();
        if (path.equals("/internal") || path.startsWith("/internal/") || path.contains("/internal/")) {
            return ErrorResponseWriter.write(exchange, HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn " + path);
        }
        return chain.filter(exchange);
    }
}
