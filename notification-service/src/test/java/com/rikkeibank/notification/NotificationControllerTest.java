package com.rikkeibank.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void khongCoTokenTra401() {
        webTestClient.get().uri("/api/notifications/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.path").isEqualTo("/api/notifications/me");
    }

    @Test
    void tokenSaiTra401() {
        webTestClient.get().uri("/api/notifications")
                .header("Authorization", "Bearer abc.def.ghi")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
