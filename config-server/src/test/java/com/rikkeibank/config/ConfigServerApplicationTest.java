package com.rikkeibank.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"native", "test"})
class ConfigServerApplicationTest {

    @Test
    void contextLoads() {
    }
}
