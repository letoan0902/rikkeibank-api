package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBranchCode(String branchCode);

    boolean existsByCode(String code);

    boolean existsByIdNumber(String idNumber);

    boolean existsByEmail(String email);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByIdNumberAndIdNot(String idNumber, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
