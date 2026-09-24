package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.StaffRequest;
import com.rikkeibank.customer.dto.StaffResponse;
import com.rikkeibank.customer.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staffs")
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<StaffResponse> findAll() {
        return staffService.findAll();
    }

    @GetMapping("/{id}")
    public StaffResponse findById(@PathVariable Long id) {
        return staffService.findById(id);
    }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(req));
    }

    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest req) {
        return staffService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
