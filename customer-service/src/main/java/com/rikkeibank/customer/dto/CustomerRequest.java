package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Mã khách hàng không được để trống")
        @Size(max = 20, message = "Mã khách hàng tối đa 20 ký tự")
        String code,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Số CCCD không được để trống")
        @Pattern(regexp = "^\\d{12}$", message = "Số CCCD phải gồm đúng 12 chữ số")
        String idNumber,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0")
        String phone,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String address,

        @NotBlank(message = "Mã chi nhánh không được để trống")
        String branchCode,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE")
        String status
) {
}
