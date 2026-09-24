package com.rikkeibank.account.dto;

import com.rikkeibank.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "Trạng thái không được trống") AccountStatus status) {
}
