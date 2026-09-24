package com.rikkeibank.identity.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                            long expiresIn, String role, String username) {
}
