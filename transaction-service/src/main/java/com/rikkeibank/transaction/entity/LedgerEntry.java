package com.rikkeibank.transaction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String transactionCode;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false)
    private Long amount;

    private Long balanceAfter;

    private LocalDateTime createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(String transactionCode, String accountNumber, EntryType entryType, Long amount, Long balanceAfter) {
        this.transactionCode = transactionCode;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTransactionCode() { return transactionCode; }
    public String getAccountNumber() { return accountNumber; }
    public EntryType getEntryType() { return entryType; }
    public Long getAmount() { return amount; }
    public Long getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
