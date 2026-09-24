package com.rikkeibank.account.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Mỗi cặp (transactionCode, operationType) chỉ được ghi một lần -> lũy đẳng
@Entity
@Table(name = "balance_operations",
        uniqueConstraints = @UniqueConstraint(name = "uk_balance_op_code_type",
                columnNames = {"transaction_code", "operation_type"}))
public class BalanceOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", nullable = false, length = 40)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 10)
    private OperationType operationType;

    @Column(name = "account_number", nullable = false, length = 12)
    private String accountNumber;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public BalanceOperation() {
    }

    public BalanceOperation(String transactionCode, OperationType operationType, String accountNumber, long amount) {
        this.transactionCode = transactionCode;
        this.operationType = operationType;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTransactionCode() { return transactionCode; }
    public OperationType getOperationType() { return operationType; }
    public String getAccountNumber() { return accountNumber; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(long balanceAfter) { this.balanceAfter = balanceAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
