package com.rikkeibank.account.config;

import com.rikkeibank.account.exception.ForbiddenException;
import com.rikkeibank.account.exception.ServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Kiểm tra X-Internal-Key cho /internal/**, lỗi ném ra được RestControllerAdvice xử lý
@Component
public class InternalKeyInterceptor implements HandlerInterceptor {

    private final String internalKey;
    private final ChaosState chaosState;

    public InternalKeyInterceptor(@Value("${rikkeibank.internal-key}") String internalKey, ChaosState chaosState) {
        this.internalKey = internalKey;
        this.chaosState = chaosState;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = request.getHeader("X-Internal-Key");
        if (key == null || !key.equals(internalKey)) {
            throw new ForbiddenException("Thiếu hoặc sai X-Internal-Key");
        }
        if (chaosState.isDown() && request.getRequestURI().startsWith("/internal/accounts")) {
            throw new ServiceUnavailableException("account-service đang ở chế độ down (giả lập sự cố)");
        }
        return true;
    }
}
