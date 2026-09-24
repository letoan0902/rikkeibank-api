package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.CacheStatsResponse;
import com.rikkeibank.customer.dto.CustomerRequest;
import com.rikkeibank.customer.dto.CustomerResponse;
import com.rikkeibank.customer.security.AuthUser;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public List<CustomerResponse> findAll(@AuthenticationPrincipal AuthUser user) {
        return customerService.findAll(user);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse me(@AuthenticationPrincipal AuthUser user) {
        return customerService.findMe(user);
    }

    @GetMapping("/cache-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public CacheStatsResponse cacheStats() {
        return new CacheStatsResponse(customerService.getDbQueryCount());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public CustomerResponse findById(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return customerService.findById(id, user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return customerService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
