package com.rikkeibank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BalanceRequest(
        @NotBlank(message = "transactionCode không được trống") @Size(max = 40) String transactionCode,
        @NotBlank(message = "accountNumber không được trống") String accountNumber,
        @NotNull(message = "amount không được trống") @Positive(message = "amount phải lớn hơn 0") Long amount) {
}
