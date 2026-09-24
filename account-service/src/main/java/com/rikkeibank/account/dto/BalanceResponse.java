package com.rikkeibank.account.dto;

import com.rikkeibank.account.entity.OperationType;

public record BalanceResponse(String transactionCode, String accountNumber, OperationType operationType,
                              long amount, long balanceAfter, boolean alreadyProcessed, String servedBy) {
}
