package com.rikkeibank.transaction.dto;

public record AccountInfo(String accountNumber, Long customerId, Long balance, String status,
                          String branchCode, String servedBy) {
}
