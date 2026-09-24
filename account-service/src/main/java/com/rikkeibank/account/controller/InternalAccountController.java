package com.rikkeibank.account.controller;

import com.rikkeibank.account.config.InstanceInfo;
import com.rikkeibank.account.dto.BalanceRequest;
import com.rikkeibank.account.dto.BalanceResponse;
import com.rikkeibank.account.dto.CustomerAccountsResponse;
import com.rikkeibank.account.dto.InternalAccountResponse;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.OperationType;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.service.AccountService;
import com.rikkeibank.account.service.BalanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chỉ cho service gọi service, được bảo vệ bằng X-Internal-Key (InternalKeyInterceptor)
@RestController
@RequestMapping("/internal/accounts")
public class InternalAccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final BalanceService balanceService;
    private final InstanceInfo instanceInfo;

    public InternalAccountController(AccountService accountService, AccountRepository accountRepository,
                                     BalanceService balanceService, InstanceInfo instanceInfo) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.balanceService = balanceService;
        this.instanceInfo = instanceInfo;
    }

    @GetMapping("/{accountNumber}")
    public InternalAccountResponse get(@PathVariable String accountNumber) {
        Account a = accountService.getEntity(accountNumber);
        return new InternalAccountResponse(a.getAccountNumber(), a.getCustomerId(), a.getBalance(),
                a.getStatus(), a.getBranchCode(), instanceInfo.servedBy());
    }

    @GetMapping("/by-customer/{customerId}")
    public CustomerAccountsResponse byCustomer(@PathVariable Long customerId) {
        List<String> numbers = accountRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .map(Account::getAccountNumber).toList();
        return new CustomerAccountsResponse(customerId, numbers, instanceInfo.servedBy());
    }

    @PostMapping("/debit")
    public BalanceResponse debit(@Valid @RequestBody BalanceRequest req) {
        return balanceService.process(OperationType.DEBIT, req).toResponse(instanceInfo.servedBy());
    }

    @PostMapping("/credit")
    public BalanceResponse credit(@Valid @RequestBody BalanceRequest req) {
        return balanceService.process(OperationType.CREDIT, req).toResponse(instanceInfo.servedBy());
    }

    @PostMapping("/refund")
    public BalanceResponse refund(@Valid @RequestBody BalanceRequest req) {
        return balanceService.process(OperationType.REFUND, req).toResponse(instanceInfo.servedBy());
    }
}
