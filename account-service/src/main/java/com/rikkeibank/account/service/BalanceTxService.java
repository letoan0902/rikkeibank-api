package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.BalanceRequest;
import com.rikkeibank.account.dto.BalanceResult;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountStatus;
import com.rikkeibank.account.entity.BalanceOperation;
import com.rikkeibank.account.entity.OperationType;
import com.rikkeibank.account.exception.ApiException;
import com.rikkeibank.account.exception.ConflictException;
import com.rikkeibank.account.exception.NotFoundException;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.BalanceOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Phần chạy trong MỘT giao dịch CSDL của debit/credit/refund
@Service
public class BalanceTxService {

    private final AccountRepository accountRepository;
    private final BalanceOperationRepository operationRepository;

    public BalanceTxService(AccountRepository accountRepository, BalanceOperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    @Transactional
    public BalanceResult apply(OperationType type, BalanceRequest req) {
        Optional<BalanceOperation> old = operationRepository
                .findByTransactionCodeAndOperationType(req.transactionCode(), type);
        if (old.isPresent()) {
            return toResult(old.get(), true);
        }

        // 1. Chèn bản ghi thao tác trước, trùng khóa duy nhất sẽ ném DataIntegrityViolationException
        BalanceOperation op = new BalanceOperation(req.transactionCode(), type, req.accountNumber(), req.amount());
        operationRepository.saveAndFlush(op);

        // 2. Cập nhật số dư nguyên tử có điều kiện
        int rows = switch (type) {
            case DEBIT -> accountRepository.debit(req.accountNumber(), req.amount());
            case CREDIT -> accountRepository.credit(req.accountNumber(), req.amount());
            case REFUND -> accountRepository.refund(req.accountNumber(), req.amount());
        };
        if (rows == 0) {
            // ném lỗi -> quay lui cả bản ghi thao tác
            throw failureReason(type, req);
        }

        Account account = accountRepository.findByAccountNumber(req.accountNumber())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản " + req.accountNumber()));
        op.setBalanceAfter(account.getBalance());
        operationRepository.save(op);
        return new BalanceResult(op.getTransactionCode(), op.getAccountNumber(), account.getCustomerId(),
                type, op.getAmount(), op.getBalanceAfter(), false);
    }

    @Transactional(readOnly = true)
    public BalanceResult findExisting(OperationType type, String transactionCode) {
        BalanceOperation op = operationRepository.findByTransactionCodeAndOperationType(transactionCode, type)
                .orElseThrow(() -> new ConflictException("Thao tác " + type + " của " + transactionCode + " đang được xử lý"));
        return toResult(op, true);
    }

    private BalanceResult toResult(BalanceOperation op, boolean alreadyProcessed) {
        Long customerId = accountRepository.findByAccountNumber(op.getAccountNumber())
                .map(Account::getCustomerId).orElse(null);
        return new BalanceResult(op.getTransactionCode(), op.getAccountNumber(), customerId,
                op.getOperationType(), op.getAmount(), op.getBalanceAfter(), alreadyProcessed);
    }

    private ApiException failureReason(OperationType type, BalanceRequest req) {
        Account account = accountRepository.findByAccountNumber(req.accountNumber()).orElse(null);
        if (account == null) {
            return new NotFoundException("Không tìm thấy tài khoản " + req.accountNumber());
        }
        if (type != OperationType.REFUND && account.getStatus() != AccountStatus.ACTIVE) {
            return new ConflictException("ACCOUNT_NOT_ACTIVE: Tài khoản " + req.accountNumber()
                    + " đang ở trạng thái " + account.getStatus());
        }
        return new ConflictException("INSUFFICIENT_BALANCE: Số dư tài khoản " + req.accountNumber() + " không đủ");
    }
}
