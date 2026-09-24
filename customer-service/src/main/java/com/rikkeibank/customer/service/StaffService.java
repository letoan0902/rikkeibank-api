package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.StaffRequest;
import com.rikkeibank.customer.dto.StaffResponse;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.entity.StaffPosition;
import com.rikkeibank.customer.entity.StaffStatus;
import com.rikkeibank.customer.exception.ConflictException;
import com.rikkeibank.customer.exception.NotFoundException;
import com.rikkeibank.customer.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> findAll() {
        return staffRepository.findAll().stream().map(StaffResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StaffResponse findById(Long id) {
        return StaffResponse.from(getStaff(id));
    }

    @Transactional
    public StaffResponse create(StaffRequest req) {
        if (staffRepository.existsByCode(req.code())) {
            throw new ConflictException("Mã nhân viên " + req.code() + " đã tồn tại");
        }
        if (staffRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email " + req.email() + " đã được sử dụng");
        }
        Staff s = new Staff();
        apply(s, req);
        return StaffResponse.from(staffRepository.save(s));
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest req) {
        Staff s = getStaff(id);
        if (staffRepository.existsByCodeAndIdNot(req.code(), id)) {
            throw new ConflictException("Mã nhân viên " + req.code() + " đã tồn tại");
        }
        if (staffRepository.existsByEmailAndIdNot(req.email(), id)) {
            throw new ConflictException("Email " + req.email() + " đã được sử dụng");
        }
        apply(s, req);
        return StaffResponse.from(staffRepository.save(s));
    }

    @Transactional
    public void delete(Long id) {
        staffRepository.delete(getStaff(id));
    }

    private Staff getStaff(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên id=" + id));
    }

    private void apply(Staff s, StaffRequest req) {
        s.setCode(req.code());
        s.setFullName(req.fullName());
        s.setEmail(req.email());
        s.setPhone(req.phone());
        s.setBranchCode(req.branchCode());
        s.setPosition(StaffPosition.valueOf(req.position()));
        if (req.status() != null) {
            s.setStatus(StaffStatus.valueOf(req.status()));
        }
    }
}
