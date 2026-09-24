package com.rikkeibank.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerAccounts(Long customerId, List<String> accountNumbers, String servedBy) {
}
