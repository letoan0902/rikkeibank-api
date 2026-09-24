package com.rikkeibank.notification.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "rikkeibank.notification.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final NotificationEventHandler handler;
    private final String bootstrapServers;
    private final String groupId;
    private final List<String> topics;
    private Disposable subscription;

    public NotificationKafkaConsumer(NotificationEventHandler handler,
                                     @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
                                     @Value("${rikkeibank.notification.kafka.group-id:notification-service-group}") String groupId,
                                     @Value("${rikkeibank.notification.kafka.topics:transaction-events,account-events}") List<String> topics) {
        this.handler = handler;
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topics = topics;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ReceiverOptions<String, String> options = ReceiverOptions.<String, String>create(props)
                .commitInterval(Duration.ofSeconds(1))
                .subscription(topics);

        subscription = KafkaReceiver.create(options)
                .receive()
                .concatMap(record -> handler.handleSafely(record.value())
                        // chỉ xác nhận offset sau khi xử lý xong
                        .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge())))
                // lỗi ở tầng kết nối Kafka: đăng ký lại, không để consumer dừng âm thầm
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(s -> log.warn("[NOTIFY] Consumer lỗi, thử lại lần {}: {}",
                                s.totalRetries() + 1, s.failure().getMessage())))
                .subscribe(
                        v -> { },
                        e -> log.error("[NOTIFY] Consumer dừng hẳn", e));
        log.info("[NOTIFY] Bắt đầu nghe topic {} với group {}", topics, groupId);
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }
}
