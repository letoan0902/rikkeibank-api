package com.rikkeibank.account.client;

import com.rikkeibank.account.exception.ApiException;
import com.rikkeibank.account.exception.NotFoundException;
import com.rikkeibank.account.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerGateway {

    private static final Logger log = LoggerFactory.getLogger(CustomerGateway.class);

    private final CustomerClient customerClient;

    public CustomerGateway(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "fallback")
    public CustomerInfo getCustomer(Long id) {
        try {
            return customerClient.getCustomer(id);
        } catch (FeignException.NotFound e) {
            // lỗi nghiệp vụ, không tính là lỗi của mạch (ignore-exceptions)
            throw new NotFoundException("Không tìm thấy khách hàng id=" + id);
        }
    }

    // Fallback được gọi cho MỌI ngoại lệ: lỗi nghiệp vụ thì ném lại nguyên vẹn
    public CustomerInfo fallback(Long id, Throwable t) {
        if (t instanceof ApiException apiException) {
            throw apiException;
        }
        log.warn("customer-service không khả dụng khi kiểm tra khách id={}: {}", id, t.toString());
        throw new ServiceUnavailableException("Dịch vụ khách hàng tạm thời không khả dụng, vui lòng thử lại sau");
    }
}
