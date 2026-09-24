package com.rikkeibank.account.service;

import com.rikkeibank.account.client.CustomerGateway;
import com.rikkeibank.account.client.CustomerInfo;
import com.rikkeibank.account.dto.AccountResponse;
import com.rikkeibank.account.dto.AccountTypeCache;
import com.rikkeibank.account.dto.CreateAccountRequest;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountStatus;
import com.rikkeibank.account.exception.BadRequestException;
import com.rikkeibank.account.exception.ForbiddenException;
import com.rikkeibank.account.exception.NotFoundException;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTypeService accountTypeService;
    private final CustomerGateway customerGateway;

    public AccountService(AccountRepository accountRepository, AccountTypeService accountTypeService,
                          CustomerGateway customerGateway) {
        this.accountRepository = accountRepository;
        this.accountTypeService = accountTypeService;
        this.customerGateway = customerGateway;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(AuthUser user) {
        List<Account> accounts = user.isAdmin()
                ? accountRepository.findAllByOrderByIdAsc()
                : accountRepository.findByBranchCodeOrderByIdAsc(user.branchCode());
        return accounts.stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> myAccounts(AuthUser user) {
        return accountRepository.findByCustomerIdOrderByIdAsc(user.customerId()).stream()
                .map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountNumber, AuthUser user) {
        Account a = getEntity(accountNumber);
        checkAccess(a, user);
        return AccountResponse.from(a);
    }

    // Không đặt @Transactional ở đây để không giữ kết nối CSDL khi gọi Feign
    public AccountResponse create(CreateAccountRequest req) {
        AccountTypeCache type = accountTypeService.findById(req.accountTypeId());
        if (req.initialBalance() < type.getMinBalance()) {
            throw new BadRequestException("Số dư ban đầu phải từ " + type.getMinBalance() + " đồng trở lên");
        }
        CustomerInfo customer = customerGateway.getCustomer(req.customerId());

        Account a = new Account();
        a.setAccountNumber(generateAccountNumber());
        a.setCustomerId(customer.id() != null ? customer.id() : req.customerId());
        a.setAccountTypeId(type.getId());
        a.setBalance(req.initialBalance());
        a.setBranchCode(req.branchCode() != null && !req.branchCode().isBlank()
                ? req.branchCode() : customer.branchCode());
        a.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(a);
        log.info("Tạo tài khoản {} cho khách {}", saved.getAccountNumber(), saved.getCustomerId());
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse updateStatus(String accountNumber, AccountStatus status) {
        Account a = getEntity(accountNumber);
        a.setStatus(status);
        return AccountResponse.from(a);
    }

    @Transactional
    public void close(String accountNumber) {
        Account a = getEntity(accountNumber);
        a.setStatus(AccountStatus.CLOSED);
    }

    public Account getEntity(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản " + accountNumber));
    }

    private void checkAccess(Account a, AuthUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (user.isTeller() && a.getBranchCode() != null && a.getBranchCode().equals(user.branchCode())) {
            return;
        }
        if (user.isCustomer() && a.getCustomerId().equals(user.customerId())) {
            return;
        }
        throw new ForbiddenException("Bạn không có quyền xem tài khoản " + a.getAccountNumber());
    }

    // Số tài khoản 12 chữ số, bắt đầu bằng 1, lặp tới khi chưa bị dùng
    private String generateAccountNumber() {
        String number;
        do {
            number = "1" + String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
