package com.rikkeibank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);
    private static final Set<String> PUBLIC_PATHS = Set.of("/api/auth/login", "/api/auth/refresh");
    public static final String REVOKED_PREFIX = "rb:revoked:";

    private final SecretKey key;
    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthGlobalFilter(@Value("${rikkeibank.jwt.secret}") String secret,
                               ReactiveStringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            return ErrorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "Thiếu access token (Authorization: Bearer)");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7).trim())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return ErrorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "Access token không hợp lệ hoặc đã hết hạn");
        }

        if (!"access".equals(claims.get("type"))) {
            return ErrorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "Token không phải access token");
        }
        Object uidObj = claims.get("uid");
        if (!(uidObj instanceof Number)) {
            return ErrorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "Access token thiếu uid");
        }
        long uid = ((Number) uidObj).longValue();
        Date issuedAt = claims.getIssuedAt();
        long iat = issuedAt == null ? 0L : issuedAt.getTime() / 1000;

        // Redis lỗi thì cho qua, không làm sập gateway
        return Mono.defer(() -> redisTemplate.opsForValue().get(REVOKED_PREFIX + uid))
                .map(this::parseEpoch)
                .onErrorResume(e -> {
                    log.warn("Không kiểm tra được thu hồi token uid={} do Redis lỗi: {}", uid, e.getMessage());
                    return Mono.empty();
                })
                .defaultIfEmpty(-1L)
                .flatMap(revokedAt -> {
                    if (revokedAt >= 0 && iat <= revokedAt) {
                        return ErrorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã bị thu hồi");
                    }
                    return chain.filter(exchange);
                });
    }

    private long parseEpoch(String value) {
        try {
            return Long.parseLong(value.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            log.warn("Giá trị thu hồi không hợp lệ: {}", value);
            return -1L;
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
