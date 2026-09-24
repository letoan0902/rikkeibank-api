package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.client.AccountGateway;
import com.rikkeibank.transaction.dto.TransactionResponse;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.exception.ForbiddenException;
import com.rikkeibank.transaction.exception.NotFoundException;
import com.rikkeibank.transaction.repository.TransactionRepository;
import com.rikkeibank.transaction.security.AuthUser;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountGateway accountGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public TransactionService(TransactionRepository transactionRepository, AccountGateway accountGateway,
                              CircuitBreakerRegistry circuitBreakerRegistry) {
        this.transactionRepository = transactionRepository;
        this.accountGateway = accountGateway;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public List<TransactionResponse> findMine(AuthUser user) {
        List<String> accounts = accountGateway.getAccountNumbersByCustomer(user.customerId());
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByFromAccountNumberInOrToAccountNumberInOrderByCreatedAtDesc(accounts, accounts)
                .stream().map(TransactionResponse::from).toList();
    }

    public List<TransactionResponse> findToday(AuthUser user) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Transaction> list = user.hasRole("TELLER")
                ? transactionRepository.findByBranchCodeAndCreatedAtBetweenOrderByCreatedAtDesc(user.branchCode(), start, end)
                : transactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        return list.stream().map(TransactionResponse::from).toList();
    }

    public TransactionResponse findByCode(String code, AuthUser user) {
        Transaction tx = getOrThrow(code);
        boolean allowed = switch (user.role()) {
            case "ADMIN" -> true;
            case "TELLER" -> Objects.equals(user.branchCode(), tx.getBranchCode());
            case "CUSTOMER" -> user.customerId() != null
                    && (user.customerId().equals(tx.getCustomerId()) || user.customerId().equals(tx.getToCustomerId()));
            default -> false;
        };
        if (!allowed) {
            throw new ForbiddenException("Bạn không có quyền xem giao dịch " + code);
        }
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse assign(String code, Long staffId) {
        Transaction tx = getOrThrow(code);
        tx.setAssignedStaffId(staffId);
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse review(String code, String note, AuthUser user) {
        Transaction tx = getOrThrow(code);
        if (tx.getAssignedStaffId() == null || !tx.getAssignedStaffId().equals(user.staffId())) {
            throw new ForbiddenException("Giao dịch " + code + " không được phân công cho bạn");
        }
        tx.setReviewNote(note);
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    public Map<String, Object> circuitBreakerStatus() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("accountService");
        CircuitBreaker.Metrics m = cb.getMetrics();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", cb.getName());
        result.put("state", cb.getState().name());
        result.put("failureRate", m.getFailureRate());
        result.put("slowCallRate", m.getSlowCallRate());
        result.put("bufferedCalls", m.getNumberOfBufferedCalls());
        result.put("failedCalls", m.getNumberOfFailedCalls());
        result.put("successfulCalls", m.getNumberOfSuccessfulCalls());
        result.put("notPermittedCalls", m.getNumberOfNotPermittedCalls());
        result.put("failureRateThreshold", cb.getCircuitBreakerConfig().getFailureRateThreshold());
        result.put("slidingWindowSize", cb.getCircuitBreakerConfig().getSlidingWindowSize());
        result.put("minimumNumberOfCalls", cb.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        return result;
    }

    private Transaction getOrThrow(String code) {
        return transactionRepository.findByTransactionCode(code)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch " + code));
    }
}
