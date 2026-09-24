package com.rikkeibank.account.dto;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountStatus;

import java.time.LocalDateTime;

public record AccountResponse(Long id, String accountNumber, Long customerId, Long accountTypeId,
                              long balance, String branchCode, AccountStatus status, LocalDateTime createdAt) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getCustomerId(), a.getAccountTypeId(),
                a.getBalance(), a.getBranchCode(), a.getStatus(), a.getCreatedAt());
    }
}
