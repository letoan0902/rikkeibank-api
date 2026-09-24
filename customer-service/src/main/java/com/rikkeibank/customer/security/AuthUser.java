package com.rikkeibank.customer.security;

public record AuthUser(Long uid, String username, String role, Long customerId, Long staffId, String branchCode) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isTeller() {
        return "TELLER".equals(role);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(role);
    }
}
