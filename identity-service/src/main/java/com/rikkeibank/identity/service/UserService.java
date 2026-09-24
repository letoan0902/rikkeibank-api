package com.rikkeibank.identity.service;

import com.rikkeibank.identity.dto.CreateUserRequest;
import com.rikkeibank.identity.dto.UpdateUserRequest;
import com.rikkeibank.identity.dto.UserResponse;
import com.rikkeibank.identity.entity.UserAccount;
import com.rikkeibank.identity.exception.ApiException;
import com.rikkeibank.identity.repository.RefreshTokenRepository;
import com.rikkeibank.identity.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại: " + req.username());
        }
        UserAccount u = new UserAccount();
        u.setUsername(req.username());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(req.role());
        u.setCustomerId(req.customerId());
        u.setStaffId(req.staffId());
        u.setBranchCode(req.branchCode());
        u.setEnabled(true);
        u = userRepository.save(u);
        log.info("Tạo tài khoản: username={}, role={}", u.getUsername(), u.getRole());
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        UserAccount u = getOrThrow(id);
        if (req.role() != null) u.setRole(req.role());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        if (req.customerId() != null) u.setCustomerId(req.customerId());
        if (req.staffId() != null) u.setStaffId(req.staffId());
        if (req.branchCode() != null) u.setBranchCode(req.branchCode());
        return UserResponse.from(userRepository.save(u));
    }

    @Transactional
    public void delete(Long id) {
        UserAccount u = getOrThrow(id);
        refreshTokenRepository.deleteAllByUserId(id);
        userRepository.delete(u);
        log.info("Xóa tài khoản: username={}", u.getUsername());
    }

    private UserAccount getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản id=" + id));
    }
}
