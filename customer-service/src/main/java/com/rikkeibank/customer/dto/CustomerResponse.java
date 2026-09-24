package com.rikkeibank.customer.dto;

import com.rikkeibank.customer.entity.Customer;

import java.time.LocalDateTime;

// Lớp thường, không final, có hàm tạo rỗng để Redis serializer ghi được @class
public class CustomerResponse {

    private Long id;
    private String code;
    private String fullName;
    private String idNumber;
    private String phone;
    private String email;
    private String address;
    private String branchCode;
    private String status;
    private LocalDateTime createdAt;

    public CustomerResponse() {
    }

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.code = c.getCode();
        r.fullName = c.getFullName();
        r.idNumber = c.getIdNumber();
        r.phone = c.getPhone();
        r.email = c.getEmail();
        r.address = c.getAddress();
        r.branchCode = c.getBranchCode();
        r.status = c.getStatus() == null ? null : c.getStatus().name();
        r.createdAt = c.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
