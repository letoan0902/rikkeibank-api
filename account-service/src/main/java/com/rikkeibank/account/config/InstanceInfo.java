package com.rikkeibank.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

// Cho biết bản nào đang phục vụ (demo cân bằng tải)
@Component
public class InstanceInfo implements ApplicationListener<WebServerInitializedEvent> {

    private final String appName;
    private volatile int port;

    public InstanceInfo(@Value("${spring.application.name}") String appName,
                        @Value("${server.port:8083}") int port) {
        this.appName = appName;
        this.port = port;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    public String servedBy() {
        return appName + ":" + port;
    }
}
