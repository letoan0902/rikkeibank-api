package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.AccountTypeCache;
import com.rikkeibank.account.dto.AccountTypeRequest;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.exception.ConflictException;
import com.rikkeibank.account.exception.NotFoundException;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.AccountTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class AccountTypeService {

    private static final Logger log = LoggerFactory.getLogger(AccountTypeService.class);
    public static final String CACHE = "account-types";

    private final AccountTypeRepository typeRepository;
    private final AccountRepository accountRepository;

    public AccountTypeService(AccountTypeRepository typeRepository, AccountRepository accountRepository) {
        this.typeRepository = typeRepository;
        this.accountRepository = accountRepository;
    }

    // Trả ArrayList (không dùng List.of) để Jackson đọc lại được từ Redis
    @Cacheable(value = CACHE, key = "'all'")
    public ArrayList<AccountTypeCache> findAll() {
        log.info("Đọc danh sách loại tài khoản từ CSDL");
        ArrayList<AccountTypeCache> list = new ArrayList<>();
        typeRepository.findAll().forEach(t -> list.add(AccountTypeCache.from(t)));
        return list;
    }

    @Cacheable(value = CACHE, key = "#id")
    public AccountTypeCache findById(Long id) {
        log.info("Đọc loại tài khoản id={} từ CSDL", id);
        return AccountTypeCache.from(getEntity(id));
    }

    @Transactional
    @CacheEvict(value = CACHE, key = "'all'")
    public AccountTypeCache create(AccountTypeRequest req) {
        if (typeRepository.existsByCode(req.code())) {
            throw new ConflictException("Mã loại tài khoản " + req.code() + " đã tồn tại");
        }
        AccountType t = new AccountType();
        apply(t, req);
        return AccountTypeCache.from(typeRepository.save(t));
    }

    @Transactional
    @Caching(put = @CachePut(value = CACHE, key = "#id"),
            evict = @CacheEvict(value = CACHE, key = "'all'"))
    public AccountTypeCache update(Long id, AccountTypeRequest req) {
        AccountType t = getEntity(id);
        if (typeRepository.existsByCodeAndIdNot(req.code(), id)) {
            throw new ConflictException("Mã loại tài khoản " + req.code() + " đã tồn tại");
        }
        apply(t, req);
        return AccountTypeCache.from(typeRepository.save(t));
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = CACHE, key = "#id"), @CacheEvict(value = CACHE, key = "'all'")})
    public void delete(Long id) {
        AccountType t = getEntity(id);
        if (accountRepository.existsByAccountTypeId(id)) {
            throw new ConflictException("Loại tài khoản đang được sử dụng, không thể xóa");
        }
        typeRepository.delete(t);
    }

    private AccountType getEntity(Long id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại tài khoản id=" + id));
    }

    private void apply(AccountType t, AccountTypeRequest req) {
        t.setCode(req.code().trim().toUpperCase());
        t.setName(req.name().trim());
        t.setInterestRate(req.interestRate());
        t.setMinBalance(req.minBalance());
    }
}
