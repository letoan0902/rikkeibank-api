package com.rikkeibank.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Đọc JWT từ header, token sai thì bỏ qua để entry point trả 401 (không đăng ký bean để tránh chạy 2 lần)
public class JwtAuthFilter implements WebFilter {

    private final SecretKey key;

    public JwtAuthFilter(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }
        AuthUser user = parse(header.substring(7));
        if (user == null) {
            return chain.filter(exchange);
        }
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    private AuthUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!"access".equals(c.get("type", String.class)) || c.get("role") == null) {
                return null;
            }
            return new AuthUser(toLong(c.get("uid")), c.getSubject(), c.get("role", String.class),
                    toLong(c.get("customerId")), toLong(c.get("staffId")), c.get("branchCode", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
