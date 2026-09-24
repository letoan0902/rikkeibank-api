package com.rikkeibank.account.repository;

import com.rikkeibank.account.entity.BalanceOperation;
import com.rikkeibank.account.entity.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BalanceOperationRepository extends JpaRepository<BalanceOperation, Long> {

    Optional<BalanceOperation> findByTransactionCodeAndOperationType(String transactionCode, OperationType operationType);

    long countByTransactionCode(String transactionCode);
}
