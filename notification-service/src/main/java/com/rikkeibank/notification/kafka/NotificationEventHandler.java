package com.rikkeibank.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

// Chuyển chuỗi JSON sự kiện thành Notification và lưu lại
@Component
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public NotificationEventHandler(NotificationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // Xử lý một bản tin, lỗi được phát ra dưới dạng tín hiệu error
    public Mono<List<Notification>> handle(String json) {
        return Mono.fromCallable(() -> build(objectMapper.readTree(json)))
                .flatMap(list -> list.isEmpty() ? Mono.just(list) : save(list));
    }

    // Bản an toàn: lỗi của một bản tin không làm chết cả luồng
    public Mono<List<Notification>> handleSafely(String json) {
        return handle(json)
                .onErrorResume(e -> {
                    log.error("[NOTIFY] Bỏ qua bản tin lỗi: {} - {}", json, e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private Mono<List<Notification>> save(List<Notification> list) {
        // JPA chặn luồng nên đẩy sang boundedElastic
        return Mono.fromCallable(() -> {
                    List<Notification> saved = repository.saveAll(list);
                    for (Notification n : saved) {
                        log.info("[NOTIFY] Gửi SMS tới khách {}: {} (luồng {})",
                                n.getCustomerId(), n.getContent(), Thread.currentThread().getName());
                    }
                    return saved;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    List<Notification> build(JsonNode node) {
        String type = node.path("eventType").asText("");
        String tx = node.path("transactionCode").asText(null);
        List<Notification> result = new ArrayList<>();
        switch (type) {
            case "BALANCE_CHANGED" -> {
                Long customerId = requireLong(node, "customerId");
                String sign = "DEBIT".equals(node.path("changeType").asText()) ? "-" : "+";
                String content = "TK " + node.path("accountNumber").asText() + " " + sign
                        + money(node.path("amount").asLong()) + ", số dư " + money(node.path("balanceAfter").asLong())
                        + ", GD " + tx;
                result.add(sms(customerId, "Biến động số dư", content, type, tx));
            }
            case "TRANSFER_COMPLETED" -> {
                String amount = money(node.path("amount").asLong());
                String from = node.path("fromAccountNumber").asText();
                String to = node.path("toAccountNumber").asText();
                result.add(sms(requireLong(node, "fromCustomerId"), "Chuyển khoản thành công",
                        "Bạn đã chuyển " + amount + " từ TK " + from + " tới TK " + to + ", GD " + tx, type, tx));
                Long toCustomerId = optLong(node, "toCustomerId");
                if (toCustomerId != null) {
                    result.add(sms(toCustomerId, "Nhận tiền",
                            "TK " + to + " nhận " + amount + " từ TK " + from + ", GD " + tx, type, tx));
                }
            }
            case "TRANSFER_COMPENSATED" -> result.add(sms(requireLong(node, "fromCustomerId"), "Giao dịch thất bại",
                    "Giao dịch " + tx + " thất bại, đã hoàn tiền " + money(node.path("amount").asLong())
                            + " về TK " + node.path("fromAccountNumber").asText() + reason(node), type, tx));
            case "TRANSFER_FAILED" -> result.add(sms(requireLong(node, "fromCustomerId"), "Giao dịch thất bại",
                    "Giao dịch " + tx + " chuyển " + money(node.path("amount").asLong()) + " từ TK "
                            + node.path("fromAccountNumber").asText() + " thất bại" + reason(node), type, tx));
            default -> log.warn("[NOTIFY] Bỏ qua sự kiện không hỗ trợ: {}", type);
        }
        return result;
    }

    private Notification sms(Long customerId, String title, String content, String type, String tx) {
        return new Notification(customerId, Notification.Channel.SMS, title, content, type, tx);
    }

    private String reason(JsonNode node) {
        String r = node.path("reason").asText(null);
        return r == null || r.isBlank() ? "" : ". Lý do: " + r;
    }

    private Long optLong(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asLong();
    }

    private Long requireLong(JsonNode node, String field) {
        Long v = optLong(node, field);
        if (v == null) {
            throw new IllegalArgumentException("Thiếu trường " + field);
        }
        return v;
    }

    static String money(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "đ";
    }
}
