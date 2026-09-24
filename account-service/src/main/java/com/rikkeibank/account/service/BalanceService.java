package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.BalanceRequest;
import com.rikkeibank.account.dto.BalanceResult;
import com.rikkeibank.account.entity.OperationType;
import com.rikkeibank.account.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    private final BalanceTxService txService;
    private final BalanceEventPublisher eventPublisher;

    public BalanceService(BalanceTxService txService, BalanceEventPublisher eventPublisher) {
        this.txService = txService;
        this.eventPublisher = eventPublisher;
    }

    public BalanceResult process(OperationType type, BalanceRequest req) {
        String code = req.transactionCode();
        log.info("[SAGA][{}] Nhận yêu cầu {} tài khoản {} số tiền {}", code, type, req.accountNumber(), req.amount());
        BalanceResult result;
        try {
            result = txService.apply(type, req);
        } catch (DataIntegrityViolationException e) {
            // Hai yêu cầu trùng chạy song song: bản ghi đã có, trả lại kết quả cũ
            result = txService.findExisting(type, code);
        } catch (ApiException e) {
            log.warn("[SAGA][{}] {} thất bại: {}", code, type, e.getMessage());
            throw e;
        }

        if (result.alreadyProcessed()) {
            log.info("[SAGA][{}] {} đã xử lý trước đó, trả lại kết quả cũ (số dư sau {})", code, type, result.balanceAfter());
        } else {
            log.info("[SAGA][{}] {} thành công, số dư mới {}", code, type, result.balanceAfter());
            // Giao dịch đã commit khi apply() trả về -> phát sự kiện
            eventPublisher.publishBalanceChanged(result);
        }
        return result;
    }
}
