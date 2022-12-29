package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.enums.TopicEnum;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;


@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    public String bootstrapServers;

    @Bean
    public NewTopic bitstampAuditTrailTopic() {
        return TopicBuilder
                .name(TopicEnum.HERRON_AUDIT_TRAIL.getTopicName())
                .partitions(1)
                .build();
    }
}
