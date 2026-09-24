package com.rikkeibank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AccountTypeRequest(
        @NotBlank(message = "Mã loại tài khoản không được trống") @Size(max = 30) String code,
        @NotBlank(message = "Tên loại tài khoản không được trống") @Size(max = 100) String name,
        @NotNull(message = "Lãi suất không được trống") @PositiveOrZero Double interestRate,
        @NotNull(message = "Số dư tối thiểu không được trống") @PositiveOrZero Long minBalance) {
}
