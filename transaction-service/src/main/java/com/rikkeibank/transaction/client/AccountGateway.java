package com.rikkeibank.transaction.client;

import com.rikkeibank.transaction.dto.AccountInfo;
import com.rikkeibank.transaction.dto.CustomerAccounts;
import com.rikkeibank.transaction.dto.BalanceOperationRequest;
import com.rikkeibank.transaction.dto.BalanceOperationResponse;
import com.rikkeibank.transaction.exception.AccountUnavailableException;
import com.rikkeibank.transaction.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

// Bọc AccountClient bằng Circuit Breaker accountService
@Component
public class AccountGateway {

    private static final Logger log = LoggerFactory.getLogger(AccountGateway.class);
    private static final String CB = "accountService";

    private final AccountClient accountClient;

    public AccountGateway(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "getAccountFallback")
    public AccountInfo getAccount(String accountNumber) {
        return accountClient.getAccount(accountNumber);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "byCustomerFallback")
    public List<String> getAccountNumbersByCustomer(Long customerId) {
        CustomerAccounts kq = accountClient.getAccountNumbersByCustomer(customerId);
        return kq == null || kq.accountNumbers() == null ? List.of() : kq.accountNumbers();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "operationFallback")
    public BalanceOperationResponse debit(BalanceOperationRequest request) {
        return accountClient.debit(request);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "operationFallback")
    public BalanceOperationResponse credit(BalanceOperationRequest request) {
        return accountClient.credit(request);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "operationFallback")
    public BalanceOperationResponse refund(BalanceOperationRequest request) {
        return accountClient.refund(request);
    }

    // Fallback nạp chồng: Resilience4j chọn hàm có kiểu ngoại lệ khớp nhất.
    // Lỗi nghiệp vụ ném lại nguyên vẹn, còn lại chuyển thành AccountUnavailableException.

    public AccountInfo getAccountFallback(String accountNumber, BusinessException e) {
        throw e;
    }

    public AccountInfo getAccountFallback(String accountNumber, Throwable t) {
        throw unavailable(t);
    }

    public List<String> byCustomerFallback(Long customerId, BusinessException e) {
        throw e;
    }

    public List<String> byCustomerFallback(Long customerId, Throwable t) {
        throw unavailable(t);
    }

    public BalanceOperationResponse operationFallback(BalanceOperationRequest request, BusinessException e) {
        throw e;
    }

    public BalanceOperationResponse operationFallback(BalanceOperationRequest request, Throwable t) {
        throw unavailable(t);
    }

    private AccountUnavailableException unavailable(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker accountService đang MỞ, từ chối gọi account-service");
            return new AccountUnavailableException("account-service tạm thời không khả dụng (Circuit Breaker đang mở)");
        }
        if (t instanceof AccountUnavailableException aue) {
            return aue;
        }
        log.warn("Gọi account-service lỗi: {}", t.toString());
        return new AccountUnavailableException("account-service không khả dụng: " + t.getMessage());
    }
}
