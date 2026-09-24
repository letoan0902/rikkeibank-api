package com.rikkeibank.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(@NotBlank(message = "Ghi chú không được để trống") String note) {
}
