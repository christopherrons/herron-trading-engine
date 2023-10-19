package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic auditTrailTopic(@Value("${kafka.topic.audit-trail.nr-of-partitions:1}") int nrOfPartitions,
                                    @Value("${kafka.topic.audit-trail.max-message-bytes}") String maxMessageBytes) {
        return TopicBuilder
                .name(KafkaTopicEnum.AUDIT_TRAIL.getTopicName())
                .partitions(nrOfPartitions)
                .config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, maxMessageBytes)
                .build();
    }

    @Bean
    public NewTopic tradeDataTopic(@Value("${kafka.topic.trade-data.nr-of-partitions:1}") int nrOfPartitions,
                                   @Value("${kafka.topic.trade-data.max-message-bytes}") String maxMessageBytes) {
        return TopicBuilder
                .name(KafkaTopicEnum.TRADE_DATA.getTopicName())
                .partitions(nrOfPartitions)
                .config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, maxMessageBytes)
                .build();
    }

    @Bean
    public NewTopic topOfOrderBookDataTopic(@Value("${kafka.topic.top-of-book.nr-of-partitions:1}") int nrOfPartitions,
                                            @Value("${kafka.topic.top-of-book.max-message-bytes}") String maxMessageBytes) {
        return TopicBuilder
                .name(KafkaTopicEnum.TOP_OF_BOOK_QUOTE.getTopicName())
                .partitions(nrOfPartitions)
                .config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, maxMessageBytes)
                .build();
    }
}
