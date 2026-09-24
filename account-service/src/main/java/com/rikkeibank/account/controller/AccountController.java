package com.rikkeibank.account.controller;

import com.rikkeibank.account.config.ChaosState;
import com.rikkeibank.account.config.InstanceInfo;
import com.rikkeibank.account.dto.AccountResponse;
import com.rikkeibank.account.dto.CreateAccountRequest;
import com.rikkeibank.account.dto.UpdateStatusRequest;
import com.rikkeibank.account.exception.BadRequestException;
import com.rikkeibank.account.security.AuthUser;
import com.rikkeibank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final ChaosState chaosState;
    private final InstanceInfo instanceInfo;

    public AccountController(AccountService accountService, ChaosState chaosState, InstanceInfo instanceInfo) {
        this.accountService = accountService;
        this.chaosState = chaosState;
        this.instanceInfo = instanceInfo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public List<AccountResponse> list(@AuthenticationPrincipal AuthUser user) {
        return accountService.list(user);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<AccountResponse> me(@AuthenticationPrincipal AuthUser user) {
        return accountService.myAccounts(user);
    }

    @GetMapping("/instance")
    public Map<String, String> instance() {
        return Map.of("servedBy", instanceInfo.servedBy());
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber, @AuthenticationPrincipal AuthUser user) {
        return accountService.get(accountNumber, user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(req));
    }

    @PutMapping("/{accountNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse updateStatus(@PathVariable String accountNumber, @Valid @RequestBody UpdateStatusRequest req) {
        return accountService.updateStatus(accountNumber, req.status());
    }

    @DeleteMapping("/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> close(@PathVariable String accountNumber) {
        accountService.close(accountNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/chaos")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> chaos(@RequestParam String mode) {
        if (!"normal".equalsIgnoreCase(mode) && !"down".equalsIgnoreCase(mode)) {
            throw new BadRequestException("mode chỉ nhận normal hoặc down");
        }
        chaosState.setDown("down".equalsIgnoreCase(mode));
        return Map.of("mode", mode.toLowerCase(), "servedBy", instanceInfo.servedBy());
    }
}
