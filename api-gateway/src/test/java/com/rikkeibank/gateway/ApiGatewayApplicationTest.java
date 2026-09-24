package com.rikkeibank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.enabled=false")
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void thieuTokenTra401() {
        webTestClient.get().uri("/api/customers")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-Gateway", "rikkeibank-gateway")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.path").isEqualTo("/api/customers");
    }

    @Test
    void chanDuongInternal() {
        webTestClient.get().uri("/internal/accounts/100000000001")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }
}
