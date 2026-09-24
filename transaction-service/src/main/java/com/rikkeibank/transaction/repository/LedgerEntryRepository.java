package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionCode(String transactionCode);
}
