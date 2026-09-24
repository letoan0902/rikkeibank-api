package com.rikkeibank.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.account.dto.BalanceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class BalanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BalanceEventPublisher.class);
    public static final String TOPIC = "account-events";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BalanceEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Kafka lỗi chỉ log ERROR, không ảnh hưởng thao tác số dư đã commit
    public void publishBalanceChanged(BalanceResult r) {
        String code = r.transactionCode();
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "BALANCE_CHANGED");
            event.put("transactionCode", code);
            event.put("accountNumber", r.accountNumber());
            event.put("customerId", r.customerId());
            event.put("changeType", r.operationType().name());
            event.put("amount", r.amount());
            event.put("balanceAfter", r.balanceAfter());
            event.put("occurredAt", LocalDateTime.now().format(FMT));
            String json = objectMapper.writeValueAsString(event);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, code, json);
            if (future != null) {
                future.whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("[SAGA][{}] Phát BALANCE_CHANGED thất bại: {}", code, ex.getMessage());
                    } else {
                        log.info("[SAGA][{}] Đã phát BALANCE_CHANGED ({}) lên {}", code, r.operationType(), TOPIC);
                    }
                });
            }
        } catch (Exception e) {
            log.error("[SAGA][{}] Phát BALANCE_CHANGED thất bại: {}", code, e.getMessage());
        }
    }
}
