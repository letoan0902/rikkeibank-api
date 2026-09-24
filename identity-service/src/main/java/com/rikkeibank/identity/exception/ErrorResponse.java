package com.rikkeibank.identity.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ErrorResponse(String timestamp, int status, String error, String message, String path) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(LocalDateTime.now().format(FMT), status.value(),
                status.getReasonPhrase(), message, path);
    }
}
