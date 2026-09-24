package com.rikkeibank.transaction.saga;

import com.rikkeibank.transaction.client.AccountGateway;
import com.rikkeibank.transaction.dto.AccountInfo;
import com.rikkeibank.transaction.dto.BalanceOperationRequest;
import com.rikkeibank.transaction.dto.BalanceOperationResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.entity.EntryType;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.entity.TransactionStatus;
import com.rikkeibank.transaction.event.TransactionEventPublisher;
import com.rikkeibank.transaction.exception.AccountUnavailableException;
import com.rikkeibank.transaction.exception.BusinessException;
import com.rikkeibank.transaction.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Saga điều phối chuyển khoản. KHÔNG bọc @Transactional, mỗi bước tự lưu trạng thái.
@Component
public class TransferSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestrator.class);
    public static final String COMPENSATION_FAILED = "BÙ TRỪ THẤT BẠI, cần xử lý tay";

    private final AccountGateway accountGateway;
    private final TransactionStateService stateService;
    private final TransactionEventPublisher eventPublisher;

    public TransferSagaOrchestrator(AccountGateway accountGateway, TransactionStateService stateService,
                                    TransactionEventPublisher eventPublisher) {
        this.accountGateway = accountGateway;
        this.stateService = stateService;
        this.eventPublisher = eventPublisher;
    }

    public Transaction transfer(Long customerId, TransferRequest req) {
        if (req.amount() == null || req.amount() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Số tiền phải lớn hơn 0");
        }
        if (req.fromAccountNumber().equals(req.toAccountNumber())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT",
                    "Không thể chuyển khoản cho chính tài khoản nguồn");
        }

        // B1: tạo giao dịch PENDING
        Transaction tx = new Transaction();
        tx.setTransactionCode(newCode());
        tx.setFromAccountNumber(req.fromAccountNumber());
        tx.setToAccountNumber(req.toAccountNumber());
        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setCustomerId(customerId);
        tx.setStatus(TransactionStatus.PENDING);
        tx = stateService.save(tx);
        String code = tx.getTransactionCode();
        log.info("[SAGA][{}] B1: tạo giao dịch PENDING {} -> {}, số tiền {}", code,
                req.fromAccountNumber(), req.toAccountNumber(), req.amount());

        // B2: kiểm tra tài khoản nguồn (chủ sở hữu, chi nhánh) và tài khoản đích
        AccountInfo from;
        AccountInfo to;
        try {
            from = accountGateway.getAccount(req.fromAccountNumber());
            if (!customerId.equals(from.customerId())) {
                fail(tx, "Tài khoản nguồn không thuộc khách hàng khởi tạo");
                throw new ForbiddenException("Tài khoản nguồn " + req.fromAccountNumber() + " không thuộc quyền sở hữu của bạn");
            }
            to = accountGateway.getAccount(req.toAccountNumber());
        } catch (BusinessException e) {
            return fail(tx, "Kiểm tra tài khoản thất bại: " + e.getMessage());
        } catch (AccountUnavailableException e) {
            fail(tx, e.getMessage());
            throw e;
        }
        tx.setBranchCode(from.branchCode());
        tx.setToCustomerId(to.customerId());
        tx = stateService.save(tx);
        log.info("[SAGA][{}] B2: tài khoản hợp lệ, chi nhánh nguồn {}, khách đích {}", code,
                from.branchCode(), to.customerId());

        // B3: trừ tiền tài khoản nguồn, lỗi thì FAILED (chưa trừ nên không cần bù)
        BalanceOperationResponse debit;
        try {
            debit = accountGateway.debit(new BalanceOperationRequest(code, req.fromAccountNumber(), req.amount()));
        } catch (BusinessException e) {
            return fail(tx, "Trừ tiền thất bại: " + e.getMessage());
        } catch (AccountUnavailableException e) {
            fail(tx, "Trừ tiền thất bại: " + e.getMessage());
            throw e;
        }
        tx.setStatus(TransactionStatus.DEBITED);
        tx = stateService.save(tx);
        log.info("[SAGA][{}] B3: đã trừ {} từ {}, trạng thái DEBITED", code, req.amount(), req.fromAccountNumber());

        // B4: cộng tiền tài khoản đích
        BalanceOperationResponse credit;
        try {
            if (Boolean.TRUE.equals(req.simulateFailure())) {
                throw new IllegalStateException("Lỗi giả lập sau bước trừ tiền (simulateFailure=true)");
            }
            credit = accountGateway.credit(new BalanceOperationRequest(code, req.toAccountNumber(), req.amount()));
        } catch (RuntimeException e) {
            log.warn("[SAGA][{}] B4: cộng tiền thất bại: {}", code, e.getMessage());
            return compensate(tx, debit, e.getMessage());
        }
        log.info("[SAGA][{}] B4: đã cộng {} vào {}", code, req.amount(), req.toAccountNumber());

        // B6: hoàn tất, ghi 2 bút toán
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setFailureReason(null);
        tx = stateService.saveWithLedger(tx,
                req.fromAccountNumber(), EntryType.DEBIT, balanceAfter(debit),
                req.toAccountNumber(), EntryType.CREDIT, balanceAfter(credit));
        log.info("[SAGA][{}] B6: giao dịch COMPLETED, đã ghi 2 bút toán DEBIT/CREDIT", code);
        eventPublisher.publish(tx, "TRANSFER_COMPLETED");
        return tx;
    }

    // B5: bù trừ, hoàn lại tiền đã trừ ở tài khoản nguồn
    private Transaction compensate(Transaction tx, BalanceOperationResponse debit, String reason) {
        String code = tx.getTransactionCode();
        log.info("[SAGA][{}] B5: bắt đầu BÙ TRỪ, hoàn {} về {}", code, tx.getAmount(), tx.getFromAccountNumber());
        BalanceOperationResponse refund;
        try {
            refund = accountGateway.refund(new BalanceOperationRequest(code, tx.getFromAccountNumber(), tx.getAmount()));
        } catch (RuntimeException e) {
            log.error("[SAGA][{}] B5: BÙ TRỪ THẤT BẠI, cần xử lý tay. Lỗi cộng tiền: {}. Lỗi hoàn tiền: {}",
                    code, reason, e.getMessage());
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(COMPENSATION_FAILED);
            tx = stateService.save(tx);
            eventPublisher.publish(tx, "TRANSFER_FAILED");
            return tx;
        }
        tx.setStatus(TransactionStatus.COMPENSATED);
        tx.setFailureReason("Cộng tiền thất bại, đã hoàn tiền: " + reason);
        tx = stateService.saveWithLedger(tx,
                tx.getFromAccountNumber(), EntryType.DEBIT, balanceAfter(debit),
                tx.getFromAccountNumber(), EntryType.REFUND, balanceAfter(refund));
        log.info("[SAGA][{}] B5: đã hoàn tiền, trạng thái COMPENSATED", code);
        eventPublisher.publish(tx, "TRANSFER_COMPENSATED");
        return tx;
    }

    private Transaction fail(Transaction tx, String reason) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureReason(reason);
        Transaction saved = stateService.save(tx);
        log.warn("[SAGA][{}] Giao dịch FAILED: {}", saved.getTransactionCode(), reason);
        eventPublisher.publish(saved, "TRANSFER_FAILED");
        return saved;
    }

    private Long balanceAfter(BalanceOperationResponse r) {
        return r != null ? r.balanceAfter() : null;
    }

    private String newCode() {
        return "TX" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
