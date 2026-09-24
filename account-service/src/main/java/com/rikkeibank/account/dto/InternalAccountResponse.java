package com.rikkeibank.account.dto;

import com.rikkeibank.account.entity.AccountStatus;

public record InternalAccountResponse(String accountNumber, Long customerId, long balance,
                                      AccountStatus status, String branchCode, String servedBy) {
}
