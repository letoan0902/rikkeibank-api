package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    List<Transaction> findByFromAccountNumberInOrToAccountNumberInOrderByCreatedAtDesc(Collection<String> from, Collection<String> to);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    List<Transaction> findByBranchCodeAndCreatedAtBetweenOrderByCreatedAtDesc(String branchCode, LocalDateTime start, LocalDateTime end);
}
