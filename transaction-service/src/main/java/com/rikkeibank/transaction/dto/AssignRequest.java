package com.rikkeibank.transaction.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull(message = "staffId không được để trống") Long staffId) {
}
