package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffRequest(
        @NotBlank(message = "Mã nhân viên không được để trống")
        @Size(max = 20, message = "Mã nhân viên tối đa 20 ký tự")
        String code,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0")
        String phone,

        @NotBlank(message = "Mã chi nhánh không được để trống")
        String branchCode,

        @NotBlank(message = "Chức vụ không được để trống")
        @Pattern(regexp = "TELLER|ADMIN", message = "Chức vụ chỉ nhận TELLER hoặc ADMIN")
        String position,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE")
        String status
) {
}
