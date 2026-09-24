package com.rikkeibank.transaction.client;

import com.rikkeibank.transaction.dto.AccountInfo;
import com.rikkeibank.transaction.dto.BalanceOperationRequest;
import com.rikkeibank.transaction.dto.BalanceOperationResponse;
import com.rikkeibank.transaction.dto.CustomerAccounts;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "account-service", configuration = AccountClientConfig.class)
public interface AccountClient {

    @GetMapping("/internal/accounts/{accountNumber}")
    AccountInfo getAccount(@PathVariable("accountNumber") String accountNumber);

    @GetMapping("/internal/accounts/by-customer/{customerId}")
    CustomerAccounts getAccountNumbersByCustomer(@PathVariable("customerId") Long customerId);

    @PostMapping("/internal/accounts/debit")
    BalanceOperationResponse debit(@RequestBody BalanceOperationRequest request);

    @PostMapping("/internal/accounts/credit")
    BalanceOperationResponse credit(@RequestBody BalanceOperationRequest request);

    @PostMapping("/internal/accounts/refund")
    BalanceOperationResponse refund(@RequestBody BalanceOperationRequest request);
}
