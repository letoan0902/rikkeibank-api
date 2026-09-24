package com.rikkeibank.notification.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ErrorResponse(String timestamp, int status, String error, String message, String path) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now().format(FORMAT), status, error, message, path);
    }
}
