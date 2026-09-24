package com.rikkeibank.account.config;

import org.springframework.stereotype.Component;

// Bật "down" để /internal/accounts/** trả 503 (demo Circuit Breaker)
@Component
public class ChaosState {

    private volatile boolean down;

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }
}
