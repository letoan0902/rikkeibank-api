package com.rikkeibank.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthGlobalFilterTest {

    private static final String SECRET = "RikkeiBankApiSecretKeyDungChungChoGatewayVaCacMicroservice2026!!";

    private ReactiveValueOperations<String, String> ops;
    private JwtAuthGlobalFilter filter;
    private AtomicBoolean passed;
    private GatewayFilterChain chain;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(Mono.empty());
        filter = new JwtAuthGlobalFilter(SECRET, redis);
        passed = new AtomicBoolean(false);
        chain = exchange -> {
            passed.set(true);
            return Mono.empty();
        };
    }

    private String token(String type, long iatSeconds) {
        return Jwts.builder()
                .subject("an")
                .claim("uid", 4L)
                .claim("role", "CUSTOMER")
                .claim("customerId", 1L)
                .claim("type", type)
                .issuedAt(new Date(iatSeconds * 1000))
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .id(UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private MockServerWebExchange exchange(String path, String bearer) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (bearer != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        return MockServerWebExchange.from(builder);
    }

    @Test
    void tokenHopLeThiChoQua() {
        MockServerWebExchange ex = exchange("/api/accounts/me", token("access", System.currentTimeMillis() / 1000));
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertTrue(passed.get());
    }

    @Test
    void thieuTokenTra401() {
        MockServerWebExchange ex = exchange("/api/accounts/me", null);
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertFalse(passed.get());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
        String body = ex.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("\"path\":\"/api/accounts/me\""));
    }

    @Test
    void tokenSaiChuKyTra401() {
        MockServerWebExchange ex = exchange("/api/accounts/me", token("access", 1000) + "x");
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertFalse(passed.get());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void refreshTypeTra401() {
        MockServerWebExchange ex = exchange("/api/accounts/me", token("refresh", System.currentTimeMillis() / 1000));
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertFalse(passed.get());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void duongCongKhaiKhongCanToken() {
        MockServerWebExchange ex = exchange("/api/auth/login", null);
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertTrue(passed.get());
    }

    @Test
    void tokenDaBiThuHoiTra401() {
        long now = System.currentTimeMillis() / 1000;
        when(ops.get("rb:revoked:4")).thenReturn(Mono.just(String.valueOf(now)));
        MockServerWebExchange ex = exchange("/api/accounts/me", token("access", now));
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertFalse(passed.get());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
        assertTrue(ex.getResponse().getBodyAsString().block().contains("Phiên đăng nhập đã bị thu hồi"));
    }

    @Test
    void tokenCapSauThuHoiThiChoQua() {
        long now = System.currentTimeMillis() / 1000;
        when(ops.get("rb:revoked:4")).thenReturn(Mono.just(String.valueOf(now - 10)));
        MockServerWebExchange ex = exchange("/api/accounts/me", token("access", now));
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertTrue(passed.get());
    }

    @Test
    void redisLoiThiVanChoQua() {
        when(ops.get(anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));
        MockServerWebExchange ex = exchange("/api/accounts/me", token("access", System.currentTimeMillis() / 1000));
        StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        assertTrue(passed.get());
    }
}
