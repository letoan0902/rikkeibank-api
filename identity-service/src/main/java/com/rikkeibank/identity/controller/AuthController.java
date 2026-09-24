package com.rikkeibank.identity.controller;

import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RefreshRequest;
import com.rikkeibank.identity.dto.TokenResponse;
import com.rikkeibank.identity.dto.UserResponse;
import com.rikkeibank.identity.security.AuthUser;
import com.rikkeibank.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req,
                                       @AuthenticationPrincipal AuthUser user) {
        authService.logout(req.refreshToken(), user.uid());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user.uid());
    }
}
