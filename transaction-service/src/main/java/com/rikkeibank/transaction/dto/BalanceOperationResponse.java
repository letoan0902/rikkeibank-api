package com.rikkeibank.transaction.dto;

public record BalanceOperationResponse(String transactionCode, String accountNumber, String operationType,
                                       Long amount, Long balanceAfter, Boolean alreadyProcessed, String servedBy) {
}
