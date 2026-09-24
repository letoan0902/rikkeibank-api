package com.rikkeibank.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank(message = "Số tài khoản nguồn không được để trống") String fromAccountNumber,
        @NotBlank(message = "Số tài khoản đích không được để trống") String toAccountNumber,
        @NotNull(message = "Số tiền không được để trống") @Positive(message = "Số tiền phải lớn hơn 0") Long amount,
        String description,
        Boolean simulateFailure) {
}
