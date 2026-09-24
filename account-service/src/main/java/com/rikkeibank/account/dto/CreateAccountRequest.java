package com.rikkeibank.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotNull(message = "customerId không được trống") Long customerId,
        @NotNull(message = "accountTypeId không được trống") Long accountTypeId,
        @NotNull(message = "Số dư ban đầu không được trống") @PositiveOrZero Long initialBalance,
        @Size(max = 20) String branchCode) {
}
