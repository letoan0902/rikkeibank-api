package com.rikkeibank.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Dữ liệu mẫu id cố định, chèn nếu chưa có; 2 bản chạy cùng lúc thì bản sau bỏ qua lỗi trùng
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final JdbcTemplate jdbc;

    public DataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        String typeSql = "INSERT INTO account_types (id, code, name, interest_rate, min_balance, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        insertIfAbsent("account_types", 1L, typeSql, 1L, "CHECKING", "Tài khoản thanh toán", 0.1, 50000L, now);
        insertIfAbsent("account_types", 2L, typeSql, 2L, "SAVING", "Tài khoản tiết kiệm", 5.5, 0L, now);
        insertIfAbsent("account_types", 3L, typeSql, 3L, "BUSINESS", "Tài khoản doanh nghiệp", 0.2, 1000000L, now);

        String accSql = "INSERT INTO accounts (id, account_number, customer_id, account_type_id, balance, branch_code, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        insertIfAbsent("accounts", 1L, accSql, 1L, "100000000001", 1L, 1L, 50000000L, "HN01", "ACTIVE", now);
        insertIfAbsent("accounts", 2L, accSql, 2L, "100000000002", 2L, 1L, 20000000L, "HCM01", "ACTIVE", now);
        insertIfAbsent("accounts", 3L, accSql, 3L, "100000000003", 3L, 1L, 5000000L, "HN01", "FROZEN", now);
        insertIfAbsent("accounts", 4L, accSql, 4L, "100000000004", 1L, 2L, 100000000L, "HN01", "ACTIVE", now);
    }

    private void insertIfAbsent(String table, long id, String sql, Object... args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbc.update(sql, args);
            log.info("Khởi tạo dữ liệu mẫu {} id={}", table, id);
        } catch (DataIntegrityViolationException e) {
            log.info("Dữ liệu mẫu {} id={} đã được bản khác chèn, bỏ qua", table, id);
        }
    }
}
