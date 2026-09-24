package com.rikkeibank.identity.dto;

import com.rikkeibank.identity.entity.Role;

// trường nào null thì giữ nguyên
public record UpdateUserRequest(Role role, Boolean enabled, Long customerId, Long staffId, String branchCode) {
}
