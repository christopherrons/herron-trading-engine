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
    public int nrOfPartitionsAuditTrail;

    @Value("${kafka.topic.trade-data.nr-of-partitions}")
    public int nrOfPartitionsTradeData;

    @Value("${kafka.topic.top-of-book.nr-of-partitions}")
    public int nrOfPartitionsTopOfBook;

    @Bean
    public NewTopic auditTrailTopic() {
        return TopicBuilder
                .name(KafkaTopicEnum.AUDIT_TRAIL.getTopicName())
                .partitions(nrOfPartitionsAuditTrail)
                .build();
    }

    @Bean
    public NewTopic tradeDataTopic() {
        return TopicBuilder
                .name(KafkaTopicEnum.TRADE_DATA.getTopicName())
                .partitions(nrOfPartitionsTradeData)
                .build();
    }

    @Bean
    public NewTopic topOfOrderBookDataTopic() {
        return TopicBuilder
                .name(KafkaTopicEnum.TOP_OF_BOOK_ORDER_DATA.getTopicName())
                .partitions(nrOfPartitionsTopOfBook)
                .build();
    }
}
