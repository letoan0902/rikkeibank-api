package com.rikkeibank.account.dto;

import com.rikkeibank.account.entity.OperationType;

// Kết quả nội bộ của thao tác số dư (có thêm customerId để phát sự kiện)
public record BalanceResult(String transactionCode, String accountNumber, Long customerId, OperationType operationType,
                            long amount, long balanceAfter, boolean alreadyProcessed) {

    public BalanceResponse toResponse(String servedBy) {
        return new BalanceResponse(transactionCode, accountNumber, operationType, amount, balanceAfter,
                alreadyProcessed, servedBy);
    }
}
