package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    boolean existsByCode(String code);

    boolean existsByEmail(String email);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
