package com.rikkeibank.notification.security;

public record AuthUser(Long uid, String username, String role, Long customerId, Long staffId, String branchCode) {
}
