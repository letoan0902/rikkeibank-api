package com.rikkeibank.account.repository;

import com.rikkeibank.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountTypeId(Long accountTypeId);

    List<Account> findByBranchCodeOrderByIdAsc(String branchCode);

    List<Account> findByCustomerIdOrderByIdAsc(Long customerId);

    List<Account> findAllByOrderByIdAsc();

    // Cập nhật nguyên tử, trả về số dòng bị ảnh hưởng
    @Modifying
    @Query(value = "UPDATE accounts SET balance = balance - :amount WHERE account_number = :n AND status = 'ACTIVE' AND balance >= :amount", nativeQuery = true)
    int debit(@Param("n") String accountNumber, @Param("amount") long amount);

    @Modifying
    @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE account_number = :n AND status = 'ACTIVE'", nativeQuery = true)
    int credit(@Param("n") String accountNumber, @Param("amount") long amount);

    // Bù trừ: hoàn tiền cả khi tài khoản đã bị khóa sau bước trừ tiền
    @Modifying
    @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE account_number = :n", nativeQuery = true)
    int refund(@Param("n") String accountNumber, @Param("amount") long amount);
}
