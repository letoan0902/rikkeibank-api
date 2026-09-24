package com.rikkeibank.transaction.dto;

public record BalanceOperationRequest(String transactionCode, String accountNumber, Long amount) {
}
