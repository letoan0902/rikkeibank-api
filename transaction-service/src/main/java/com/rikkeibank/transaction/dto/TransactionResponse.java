package com.rikkeibank.transaction.dto;

import com.rikkeibank.transaction.entity.Transaction;

import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String transactionCode,
        String fromAccountNumber,
        String toAccountNumber,
        Long amount,
        String description,
        String status,
        String failureReason,
        Long customerId,
        Long toCustomerId,
        String branchCode,
        Long assignedStaffId,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getTransactionCode(), t.getFromAccountNumber(),
                t.getToAccountNumber(), t.getAmount(), t.getDescription(), t.getStatus().name(),
                t.getFailureReason(), t.getCustomerId(), t.getToCustomerId(), t.getBranchCode(),
                t.getAssignedStaffId(), t.getReviewNote(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
