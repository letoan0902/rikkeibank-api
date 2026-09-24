package com.rikkeibank.account.dto;

import com.rikkeibank.account.entity.AccountType;

import java.time.LocalDateTime;

// Đối tượng lưu cache Redis: lớp thường, không final, có hàm tạo rỗng để Jackson đọc lại
public class AccountTypeCache {

    private Long id;
    private String code;
    private String name;
    private double interestRate;
    private long minBalance;
    private LocalDateTime createdAt;

    public AccountTypeCache() {
    }

    public static AccountTypeCache from(AccountType t) {
        AccountTypeCache c = new AccountTypeCache();
        c.id = t.getId();
        c.code = t.getCode();
        c.name = t.getName();
        c.interestRate = t.getInterestRate();
        c.minBalance = t.getMinBalance();
        c.createdAt = t.getCreatedAt();
        return c;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public long getMinBalance() { return minBalance; }
    public void setMinBalance(long minBalance) { this.minBalance = minBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
