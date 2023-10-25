package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.mapping.DefaultMessageFactory;
import com.herron.exchange.tradingengine.server.TradingEngine;
import com.herron.exchange.tradingengine.server.TradingEngineBootloader;
import com.herron.exchange.tradingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.tradingengine.server.consumers.UserOrderDataConsumer;
import com.herron.exchange.tradingengine.server.matchingengine.StateChangeOrchestrator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.herron.exchange.common.api.common.enums.KafkaTopicEnum.REFERENCE_DATA;
import static com.herron.exchange.common.api.common.enums.KafkaTopicEnum.USER_ORDER_DATA;

@Configuration
public class TradingEngineConfig {

    @Bean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean
    public StateChangeOrchestrator stateChangeOrchestrator(TradingEngine tradingEngine) {
        return new StateChangeOrchestrator(tradingEngine);
    }

    @Bean
    public ReferenceDataConsumer referenceDataConsumer(KafkaConsumerClient kafkaConsumerClient, KafkaConfig.KafkaConsumerConfig config) {
        return new ReferenceDataConsumer(kafkaConsumerClient, config.getDetails(REFERENCE_DATA));
    }

    @Bean
    public UserOrderDataConsumer userOrderDataConsumer(TradingEngine tradingEngine, KafkaConsumerClient kafkaConsumerClient, KafkaConfig.KafkaConsumerConfig config) {
        return new UserOrderDataConsumer(tradingEngine, kafkaConsumerClient, config.getDetails(USER_ORDER_DATA));
    }

    @Bean
    public TradingEngine tradingEngine(KafkaBroadcastHandler kafkaBroadcastHandler) {
        return new TradingEngine(kafkaBroadcastHandler);
    }

    @Bean(initMethod = "init")
    public TradingEngineBootloader tradingEngineBootloader(ReferenceDataConsumer referenceDataConsumer,
                                                           StateChangeOrchestrator stateChangeOrchestrator,
                                                           UserOrderDataConsumer userOrderDataConsumer) {
        return new TradingEngineBootloader(referenceDataConsumer, stateChangeOrchestrator, userOrderDataConsumer);
    }
}
