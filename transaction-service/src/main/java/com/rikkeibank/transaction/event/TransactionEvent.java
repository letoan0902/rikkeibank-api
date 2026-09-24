package com.rikkeibank.transaction.event;

public record TransactionEvent(
        String eventId,
        String eventType,
        String transactionCode,
        String fromAccountNumber,
        String toAccountNumber,
        Long fromCustomerId,
        Long toCustomerId,
        Long amount,
        String reason,
        String occurredAt) {
}
