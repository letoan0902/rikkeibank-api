package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.CustomerRequest;
import com.rikkeibank.customer.dto.CustomerResponse;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.CustomerStatus;
import com.rikkeibank.customer.exception.ConflictException;
import com.rikkeibank.customer.exception.ForbiddenException;
import com.rikkeibank.customer.exception.NotFoundException;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerCacheService cacheService;

    public CustomerService(CustomerRepository customerRepository, CustomerCacheService cacheService) {
        this.customerRepository = customerRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(AuthUser user) {
        List<Customer> list = user.isAdmin()
                ? customerRepository.findAll()
                : customerRepository.findByBranchCode(user.branchCode());
        return list.stream().map(CustomerResponse::from).toList();
    }

    public CustomerResponse findById(Long id, AuthUser user) {
        // CUSTOMER chỉ xem chính mình
        if (user.isCustomer() && !id.equals(user.customerId())) {
            throw new ForbiddenException("Bạn chỉ được xem thông tin của chính mình");
        }
        CustomerResponse c = cacheService.findById(id);
        // TELLER chỉ xem khách cùng chi nhánh
        if (user.isTeller() && !c.getBranchCode().equals(user.branchCode())) {
            throw new ForbiddenException("Bạn chỉ được xem khách hàng thuộc chi nhánh " + user.branchCode());
        }
        return c;
    }

    public CustomerResponse findMe(AuthUser user) {
        if (user.customerId() == null) {
            throw new NotFoundException("Tài khoản chưa gắn với khách hàng nào");
        }
        return cacheService.findById(user.customerId());
    }

    public CustomerResponse findInternal(Long id) {
        return cacheService.findById(id);
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        if (customerRepository.existsByCode(req.code())) {
            throw new ConflictException("Mã khách hàng " + req.code() + " đã tồn tại");
        }
        if (customerRepository.existsByIdNumber(req.idNumber())) {
            throw new ConflictException("Số CCCD " + req.idNumber() + " đã được sử dụng");
        }
        if (customerRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email " + req.email() + " đã được sử dụng");
        }
        Customer c = new Customer();
        c.setCode(req.code());
        c.setFullName(req.fullName());
        c.setIdNumber(req.idNumber());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setAddress(req.address());
        c.setBranchCode(req.branchCode());
        c.setStatus(req.status() == null ? CustomerStatus.ACTIVE : CustomerStatus.valueOf(req.status()));
        return CustomerResponse.from(customerRepository.save(c));
    }

    public CustomerResponse update(Long id, CustomerRequest req) {
        return cacheService.update(id, req);
    }

    public void delete(Long id) {
        cacheService.delete(id);
    }

    public long getDbQueryCount() {
        return cacheService.getDbQueryCount();
    }
}
