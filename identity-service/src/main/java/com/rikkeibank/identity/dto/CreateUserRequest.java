package com.rikkeibank.identity.dto;

import com.rikkeibank.identity.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(min = 3, max = 50, message = "Tên đăng nhập dài từ 3 tới 50 ký tự") String username,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự") String password,
        @NotNull(message = "Vai trò không được để trống") Role role,
        Long customerId,
        Long staffId,
        String branchCode) {
}
