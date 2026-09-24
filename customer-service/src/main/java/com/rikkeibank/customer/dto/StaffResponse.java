package com.rikkeibank.customer.dto;

import com.rikkeibank.customer.entity.Staff;

public record StaffResponse(Long id, String code, String fullName, String email, String phone,
                            String branchCode, String position, String status) {

    public static StaffResponse from(Staff s) {
        return new StaffResponse(s.getId(), s.getCode(), s.getFullName(), s.getEmail(), s.getPhone(),
                s.getBranchCode(), s.getPosition().name(), s.getStatus().name());
    }
}
