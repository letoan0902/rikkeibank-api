package com.rikkeibank.transaction.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.transaction.exception.AccountUnavailableException;
import com.rikkeibank.transaction.exception.BusinessException;
import com.rikkeibank.transaction.exception.NotFoundException;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Cấu hình riêng cho AccountClient (không đánh @Configuration để không áp cho client khác)
public class AccountClientConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Bean
    public RequestInterceptor internalKeyInterceptor(@Value("${rikkeibank.internal-key}") String internalKey) {
        return template -> template.header("X-Internal-Key", internalKey);
    }

    @Bean
    public ErrorDecoder accountErrorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            String body = readBody(response);
            String message = extractMessage(body);
            if (status == 404) {
                return new NotFoundException(message != null ? message : "Không tìm thấy tài khoản");
            }
            if (status == 409) {
                return new BusinessException(HttpStatus.CONFLICT, extractCode(body),
                        message != null ? message : "Thao tác số dư bị từ chối");
            }
            if (status >= 500) {
                return new AccountUnavailableException("account-service không khả dụng (HTTP " + status + ")");
            }
            HttpStatus hs = HttpStatus.resolve(status);
            return new BusinessException(hs != null ? hs : HttpStatus.BAD_REQUEST, "ACCOUNT_ERROR",
                    message != null ? message : "Lỗi từ account-service (HTTP " + status + ")");
        };
    }

    private static String readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream in = response.body().asInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractMessage(String body) {
        try {
            JsonNode node = MAPPER.readTree(body);
            return node.hasNonNull("message") ? node.get("message").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // lấy mã lỗi từ trường code/error hoặc tìm trong thân JSON
    private static String extractCode(String body) {
        if (body == null) {
            return "CONFLICT";
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node.hasNonNull("code")) {
                return node.get("code").asText();
            }
        } catch (Exception ignored) {
        }
        if (body.contains("ACCOUNT_NOT_ACTIVE")) {
            return "ACCOUNT_NOT_ACTIVE";
        }
        if (body.contains("INSUFFICIENT_BALANCE")) {
            return "INSUFFICIENT_BALANCE";
        }
        return "CONFLICT";
    }
}
