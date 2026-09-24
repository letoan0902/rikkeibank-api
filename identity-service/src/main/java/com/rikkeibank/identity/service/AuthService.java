package com.rikkeibank.identity.service;

import com.rikkeibank.identity.dto.ForceLogoutResponse;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.TokenResponse;
import com.rikkeibank.identity.dto.UserResponse;
import com.rikkeibank.identity.entity.RefreshToken;
import com.rikkeibank.identity.entity.UserAccount;
import com.rikkeibank.identity.exception.ApiException;
import com.rikkeibank.identity.repository.RefreshTokenRepository;
import com.rikkeibank.identity.repository.UserAccountRepository;
import com.rikkeibank.identity.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    public static final String REVOKED_KEY_PREFIX = "rb:revoked:";

    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenDays;

    public AuthService(UserAccountRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, StringRedisTemplate redisTemplate,
                       @Value("${rikkeibank.jwt.refresh-token-days:30}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        UserAccount user = userRepository.findByUsername(req.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.info("Đăng nhập thất bại: username={} (sai tên đăng nhập hoặc mật khẩu)", req.username());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu");
        }
        if (!user.isEnabled()) {
            log.info("Đăng nhập thất bại: username={} (tài khoản bị khóa)", req.username());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa");
        }
        log.info("Đăng nhập thành công: username={}, role={}", user.getUsername(), user.getRole());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawToken) {
        RefreshToken old = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElse(null);
        if (old == null || old.isRevoked() || old.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.info("Làm mới token thất bại: refresh token không hợp lệ, hết hạn hoặc đã thu hồi");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ, hết hạn hoặc đã bị thu hồi");
        }
        UserAccount user = userRepository.findById(old.getUserId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại hoặc đã bị khóa");
        }
        // xoay vòng: vô hiệu token cũ rồi cấp cặp mới
        old.setRevoked(true);
        refreshTokenRepository.save(old);
        log.info("Làm mới token: username={}", user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken, Long uid) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            if (t.getUserId().equals(uid) && !t.isRevoked()) {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            }
        });
        log.info("Đăng xuất: uid={}", uid);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long uid) {
        return userRepository.findById(uid).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản id=" + uid));
    }

    @Transactional
    public ForceLogoutResponse forceLogout(Long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản id=" + userId));
        int count = refreshTokenRepository.revokeAllByUserId(userId);
        long revokedAt = Instant.now().getEpochSecond();
        String warning = null;
        try {
            redisTemplate.opsForValue().set(REVOKED_KEY_PREFIX + userId, String.valueOf(revokedAt), Duration.ofDays(1));
        } catch (Exception e) {
            // Redis lỗi vẫn thu hồi refresh token, chỉ báo cảnh báo
            log.error("Không ghi được Redis khóa {}{}: {}", REVOKED_KEY_PREFIX, userId, e.getMessage());
            warning = "Không ghi được Redis, access token hiện tại vẫn dùng được tới khi hết hạn";
        }
        log.info("Ép đăng xuất: username={}, số refresh token bị thu hồi={}, revokedAt={}",
                user.getUsername(), count, revokedAt);
        return new ForceLogoutResponse("Đã thu hồi mọi phiên của " + user.getUsername(), revokedAt, warning);
    }

    private TokenResponse issueTokens(UserAccount user) {
        String raw = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(hash(raw));
        rt.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        refreshTokenRepository.save(rt);
        String access = jwtService.generateAccessToken(user);
        return new TokenResponse(access, raw, "Bearer", jwtService.getAccessTokenSeconds(),
                user.getRole().name(), user.getUsername());
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
