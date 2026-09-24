package com.rikkeibank.identity.config;

import com.rikkeibank.identity.entity.Role;
import com.rikkeibank.identity.entity.UserAccount;
import com.rikkeibank.identity.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// khởi tạo dữ liệu mẫu, chạy nhiều lần không bị trùng (kiểm tra theo username)
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // thứ tự chèn giữ đúng id 1..6 khi bảng trống
        create("admin", "Admin@123", Role.ADMIN, null, 3L, "HN01");
        create("teller.hn", "Teller@123", Role.TELLER, null, 1L, "HN01");
        create("teller.hcm", "Teller@123", Role.TELLER, null, 2L, "HCM01");
        create("an", "Customer@123", Role.CUSTOMER, 1L, null, "HN01");
        create("binh", "Customer@123", Role.CUSTOMER, 2L, null, "HCM01");
        create("cuong", "Customer@123", Role.CUSTOMER, 3L, null, "HN01");
    }

    private void create(String username, String password, Role role, Long customerId, Long staffId, String branch) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        u.setCustomerId(customerId);
        u.setStaffId(staffId);
        u.setBranchCode(branch);
        u.setEnabled(true);
        userRepository.save(u);
        log.info("Tạo tài khoản mẫu: {} ({})", username, role);
    }
}
