package com.rikkeibank.account.dto;

import java.util.List;

public record CustomerAccountsResponse(Long customerId, List<String> accountNumbers, String servedBy) {
}
