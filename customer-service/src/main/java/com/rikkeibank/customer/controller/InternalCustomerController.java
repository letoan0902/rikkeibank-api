package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.CustomerResponse;
import com.rikkeibank.customer.exception.ForbiddenException;
import com.rikkeibank.customer.service.CustomerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

// Endpoint nội bộ cho account-service, bảo vệ bằng header X-Internal-Key
@RestController
@RequestMapping("/internal/customers")
public class InternalCustomerController {

    private final CustomerService customerService;

    @Value("${rikkeibank.internal-key}")
    private String internalKey;

    public InternalCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id,
                                     @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        if (key == null || !key.equals(internalKey)) {
            throw new ForbiddenException("Sai hoặc thiếu khóa nội bộ X-Internal-Key");
        }
        return customerService.findInternal(id);
    }
}
