package com.rikkeibank.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.kafka.NotificationEventHandler;
import com.rikkeibank.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationEventHandlerTest {

    private NotificationEventHandler handler;

    private static final String BALANCE_JSON = "{\"eventId\":\"e1\",\"eventType\":\"BALANCE_CHANGED\","
            + "\"transactionCode\":\"TX1A2B3C4D5E\",\"accountNumber\":\"100000000001\",\"customerId\":1,"
            + "\"changeType\":\"DEBIT\",\"amount\":1000000,\"balanceAfter\":49000000,\"occurredAt\":\"2026-09-24T10:00:00\"}";

    @BeforeEach
    void setUp() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        handler = new NotificationEventHandler(repository, new ObjectMapper());
    }

    @Test
    void balanceChangedTaoDungThongBao() {
        StepVerifier.create(handler.handle(BALANCE_JSON))
                .assertNext(list -> {
                    assertEquals(1, list.size());
                    Notification n = list.get(0);
                    assertEquals(1L, n.getCustomerId());
                    assertEquals(Notification.Channel.SMS, n.getChannel());
                    assertEquals("BALANCE_CHANGED", n.getEventType());
                    assertEquals("TX1A2B3C4D5E", n.getTransactionCode());
                    assertEquals("TK 100000000001 -1.000.000đ, số dư 49.000.000đ, GD TX1A2B3C4D5E", n.getContent());
                })
                .verifyComplete();
    }

    @Test
    void transferCompletedThongBaoChoCaHaiBen() {
        String json = "{\"eventType\":\"TRANSFER_COMPLETED\",\"transactionCode\":\"TX0000000001\","
                + "\"fromAccountNumber\":\"100000000001\",\"toAccountNumber\":\"100000000002\","
                + "\"fromCustomerId\":1,\"toCustomerId\":2,\"amount\":500000,\"reason\":null}";
        StepVerifier.create(handler.handle(json))
                .assertNext(list -> {
                    assertEquals(2, list.size());
                    assertEquals(1L, list.get(0).getCustomerId());
                    assertEquals(2L, list.get(1).getCustomerId());
                })
                .verifyComplete();
    }

    @Test
    void jsonHongKhongLamDungLuong() {
        Flux<List<Notification>> flow = Flux.just("{json hong", BALANCE_JSON)
                .concatMap(handler::handleSafely);
        StepVerifier.create(flow)
                .assertNext(list -> assertEquals(0, list.size()))
                .assertNext(list -> assertEquals(1, list.size()))
                .verifyComplete();
    }
}
