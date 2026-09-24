package com.rikkeibank.transaction.security;

public record AuthUser(Long uid, String username, String role, Long customerId, Long staffId, String branchCode) {

    public boolean hasRole(String r) {
        return r.equals(role);
    }
}
