package com.rikkeibank.transaction.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.transaction.config.KafkaTopicConfig;
import com.rikkeibank.transaction.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TransactionEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Lỗi Kafka chỉ log, không làm hỏng giao dịch đã lưu
    public void publish(Transaction tx, String eventType) {
        String code = tx.getTransactionCode();
        try {
            TransactionEvent event = new TransactionEvent(UUID.randomUUID().toString(), eventType, code,
                    tx.getFromAccountNumber(), tx.getToAccountNumber(), tx.getCustomerId(), tx.getToCustomerId(),
                    tx.getAmount(), tx.getFailureReason(), LocalDateTime.now().format(FMT));
            String json = objectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(KafkaTopicConfig.TRANSACTION_EVENTS, code, json);
            if (future != null) {
                future.whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("[SAGA][{}] Phát sự kiện {} thất bại: {}", code, eventType, ex.getMessage());
                    } else {
                        log.info("[SAGA][{}] Đã phát sự kiện {} lên {}", code, eventType, KafkaTopicConfig.TRANSACTION_EVENTS);
                    }
                });
            }
        } catch (Exception e) {
            log.error("[SAGA][{}] Phát sự kiện {} thất bại: {}", code, eventType, e.getMessage());
        }
    }
}
