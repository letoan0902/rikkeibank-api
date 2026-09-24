package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.CustomerRequest;
import com.rikkeibank.customer.dto.CustomerResponse;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.CustomerStatus;
import com.rikkeibank.customer.exception.ConflictException;
import com.rikkeibank.customer.exception.NotFoundException;
import com.rikkeibank.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

// Bean riêng chứa các phương thức cache, tránh tự gọi trong cùng bean làm proxy bị bỏ qua
@Service
public class CustomerCacheService {

    private static final Logger log = LoggerFactory.getLogger(CustomerCacheService.class);

    private final CustomerRepository customerRepository;
    private final AtomicLong dbQueryCount = new AtomicLong();

    public CustomerCacheService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Cacheable(cacheNames = "customers", key = "#id")
    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        long count = dbQueryCount.incrementAndGet();
        log.info("[CACHE MISS] Đọc khách hàng id={} từ CSDL (lần thứ {})", id, count);
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng id=" + id));
    }

    @CachePut(cacheNames = "customers", key = "#id")
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng id=" + id));
        if (customerRepository.existsByCodeAndIdNot(req.code(), id)) {
            throw new ConflictException("Mã khách hàng " + req.code() + " đã tồn tại");
        }
        if (customerRepository.existsByIdNumberAndIdNot(req.idNumber(), id)) {
            throw new ConflictException("Số CCCD " + req.idNumber() + " đã được sử dụng");
        }
        if (customerRepository.existsByEmailAndIdNot(req.email(), id)) {
            throw new ConflictException("Email " + req.email() + " đã được sử dụng");
        }
        c.setCode(req.code());
        c.setFullName(req.fullName());
        c.setIdNumber(req.idNumber());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setAddress(req.address());
        c.setBranchCode(req.branchCode());
        if (req.status() != null) {
            c.setStatus(CustomerStatus.valueOf(req.status()));
        }
        return CustomerResponse.from(customerRepository.save(c));
    }

    @CacheEvict(cacheNames = "customers", key = "#id")
    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy khách hàng id=" + id);
        }
        customerRepository.deleteById(id);
    }

    public long getDbQueryCount() {
        return dbQueryCount.get();
    }
}
