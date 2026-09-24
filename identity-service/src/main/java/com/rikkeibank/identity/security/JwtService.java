package com.rikkeibank.identity.security;

import com.rikkeibank.identity.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(@Value("${rikkeibank.jwt.secret}") String secret,
                      @Value("${rikkeibank.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getAccessTokenSeconds() {
        return accessTokenMinutes * 60;
    }

    public String generateAccessToken(UserAccount user) {
        // iat lấy thời điểm hiện tại để so với rb:revoked:{uid} ở gateway
        Date now = new Date();
        Date exp = new Date(now.getTime() + getAccessTokenSeconds() * 1000);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("customerId", user.getCustomerId())
                .claim("staffId", user.getStaffId())
                .claim("branchCode", user.getBranchCode())
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    // trả null nếu token sai, hết hạn hoặc không phải access token
    public AuthUser validate(String token) {
        try {
            Claims c = parse(token);
            if (!"access".equals(c.get("type", String.class))) {
                return null;
            }
            return new AuthUser(toLong(c.get("uid")), c.getSubject(), c.get("role", String.class),
                    toLong(c.get("customerId")), toLong(c.get("staffId")), c.get("branchCode", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
