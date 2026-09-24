package com.rikkeibank.notification.controller;

import com.rikkeibank.notification.dto.NotificationResponse;
import com.rikkeibank.notification.repository.NotificationRepository;
import com.rikkeibank.notification.security.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Flux<NotificationResponse> myNotifications(@AuthenticationPrincipal AuthUser user) {
        // JPA là chặn nên chạy trên boundedElastic
        return Mono.fromCallable(() -> repository.findByCustomerIdOrderByCreatedAtDesc(user.customerId()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(NotificationResponse::from);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<NotificationResponse> all() {
        return Mono.fromCallable(repository::findAllByOrderByCreatedAtDesc)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(NotificationResponse::from);
    }
}
