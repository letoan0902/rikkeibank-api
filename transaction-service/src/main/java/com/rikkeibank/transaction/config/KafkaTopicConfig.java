package com.rikkeibank.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "rikkeibank.kafka.create-topics", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    public static final String TRANSACTION_EVENTS = "transaction-events";

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS).partitions(3).replicas(1).build();
    }
}
