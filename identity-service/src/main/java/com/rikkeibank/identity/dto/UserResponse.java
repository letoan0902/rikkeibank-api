package com.rikkeibank.identity.dto;

import com.rikkeibank.identity.entity.UserAccount;

import java.time.LocalDateTime;

// không có trường mật khẩu
public record UserResponse(Long id, String username, String role, Long customerId, Long staffId,
                           String branchCode, boolean enabled, LocalDateTime createdAt) {

    public static UserResponse from(UserAccount u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole().name(), u.getCustomerId(),
                u.getStaffId(), u.getBranchCode(), u.isEnabled(), u.getCreatedAt());
    }
}
