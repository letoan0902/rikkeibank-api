package com.rikkeibank.customer.dto;

import java.time.LocalDateTime;

public record ErrorResponse(String timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now().withNano(0).toString(), status, error, message, path);
    }
}
