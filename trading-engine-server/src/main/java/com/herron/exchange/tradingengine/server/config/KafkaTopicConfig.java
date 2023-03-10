package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;


@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.audit-trail.nr-of-partitions}")
    public int nrOfPartitions;

    @Bean
    public NewTopic auditTrailTopic() {
        return TopicBuilder
                .name(KafkaTopicEnum.HERRON_AUDIT_TRAIL.getTopicName())
                .partitions(nrOfPartitions)
                .build();
    }
}
