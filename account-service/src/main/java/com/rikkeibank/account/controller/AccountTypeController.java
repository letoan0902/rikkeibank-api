package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.AccountTypeCache;
import com.rikkeibank.account.dto.AccountTypeRequest;
import com.rikkeibank.account.service.AccountTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-types")
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    public AccountTypeController(AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    @GetMapping
    public List<AccountTypeCache> list() {
        return accountTypeService.findAll();
    }

    @GetMapping("/{id}")
    public AccountTypeCache get(@PathVariable Long id) {
        return accountTypeService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountTypeCache> create(@Valid @RequestBody AccountTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountTypeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountTypeCache update(@PathVariable Long id, @Valid @RequestBody AccountTypeRequest req) {
        return accountTypeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
