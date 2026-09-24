package com.rikkeibank.transaction.controller;

import com.rikkeibank.transaction.dto.AssignRequest;
import com.rikkeibank.transaction.dto.ReviewRequest;
import com.rikkeibank.transaction.dto.TransactionResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.entity.TransactionStatus;
import com.rikkeibank.transaction.saga.TransferSagaOrchestrator;
import com.rikkeibank.transaction.security.AuthUser;
import com.rikkeibank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransferSagaOrchestrator sagaOrchestrator;
    private final TransactionService transactionService;

    public TransactionController(TransferSagaOrchestrator sagaOrchestrator, TransactionService transactionService) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionService = transactionService;
    }

    // 201 khi COMPLETED, 422 khi FAILED/COMPENSATED
    @PostMapping("/transfers")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                        @AuthenticationPrincipal AuthUser user) {
        Transaction tx = sagaOrchestrator.transfer(user.customerId(), request);
        HttpStatus status = tx.getStatus() == TransactionStatus.COMPLETED
                ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(TransactionResponse.from(tx));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<TransactionResponse> me(@AuthenticationPrincipal AuthUser user) {
        return transactionService.findMine(user);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public List<TransactionResponse> today(@AuthenticationPrincipal AuthUser user) {
        return transactionService.findToday(user);
    }

    @GetMapping("/circuit-breaker")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> circuitBreaker() {
        return transactionService.circuitBreakerStatus();
    }

    @GetMapping("/{transactionCode}")
    public TransactionResponse get(@PathVariable String transactionCode, @AuthenticationPrincipal AuthUser user) {
        return transactionService.findByCode(transactionCode, user);
    }

    @PutMapping("/{transactionCode}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionResponse assign(@PathVariable String transactionCode, @Valid @RequestBody AssignRequest request) {
        return transactionService.assign(transactionCode, request.staffId());
    }

    @PutMapping("/{transactionCode}/review")
    @PreAuthorize("hasRole('TELLER')")
    public TransactionResponse review(@PathVariable String transactionCode, @Valid @RequestBody ReviewRequest request,
                                      @AuthenticationPrincipal AuthUser user) {
        return transactionService.review(transactionCode, request.note(), user);
    }
}
