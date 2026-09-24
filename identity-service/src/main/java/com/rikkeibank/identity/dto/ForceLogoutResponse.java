package com.rikkeibank.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForceLogoutResponse(String message, long revokedAt, String warning) {
}
