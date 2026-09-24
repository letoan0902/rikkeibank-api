package com.rikkeibank.customer.config;

import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.CustomerStatus;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.entity.StaffPosition;
import com.rikkeibank.customer.entity.StaffStatus;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Khởi tạo dữ liệu mẫu, chạy nhiều lần không bị trùng (kiểm tra theo mã)
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    public DataInitializer(CustomerRepository customerRepository, StaffRepository staffRepository) {
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
    }

    @Override
    public void run(String... args) {
        // thứ tự lưu giữ nguyên để id lần lượt là 1, 2, 3
        addCustomer("CUS001", "Nguyễn Văn An", "001090000001", "0901000001", "an@rikkeibank.vn", "Hà Nội", "HN01");
        addCustomer("CUS002", "Trần Thị Bình", "079090000002", "0901000002", "binh@rikkeibank.vn", "TP Hồ Chí Minh", "HCM01");
        addCustomer("CUS003", "Lê Hoàng Cường", "001090000003", "0901000003", "cuong@rikkeibank.vn", "Hà Nội", "HN01");

        addStaff("STF001", "Phạm Minh Tuấn", "tuan@rikkeibank.vn", "0912000001", "HN01", StaffPosition.TELLER);
        addStaff("STF002", "Đỗ Thu Hà", "ha@rikkeibank.vn", "0912000002", "HCM01", StaffPosition.TELLER);
        addStaff("STF003", "Quản trị hệ thống", "admin@rikkeibank.vn", "0912000003", "HN01", StaffPosition.ADMIN);

        log.info("Dữ liệu mẫu: {} khách hàng, {} nhân viên", customerRepository.count(), staffRepository.count());
    }

    private void addCustomer(String code, String name, String idNumber, String phone, String email,
                             String address, String branch) {
        if (customerRepository.existsByCode(code)) {
            return;
        }
        Customer c = new Customer();
        c.setCode(code);
        c.setFullName(name);
        c.setIdNumber(idNumber);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);
        c.setBranchCode(branch);
        c.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(c);
    }

    private void addStaff(String code, String name, String email, String phone, String branch, StaffPosition position) {
        if (staffRepository.existsByCode(code)) {
            return;
        }
        Staff s = new Staff();
        s.setCode(code);
        s.setFullName(name);
        s.setEmail(email);
        s.setPhone(phone);
        s.setBranchCode(branch);
        s.setPosition(position);
        s.setStatus(StaffStatus.ACTIVE);
        staffRepository.save(s);
    }
}
