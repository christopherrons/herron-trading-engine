package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.mapping.DefaultMessageFactory;
import com.herron.exchange.tradingengine.server.TradingEngine;
import com.herron.exchange.tradingengine.server.consumers.OrderDataConsumer;
import com.herron.exchange.tradingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.tradingengine.server.matchingengine.StateChangeOrchestrator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CountDownLatch;

@Configuration
public class TradingEngineConfig {

    @Bean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean(initMethod = "init")
    public StateChangeOrchestrator stateChangeOrchestrator(TradingEngine tradingEngine,
                                                           @Qualifier("referenceDataLoadLatch") CountDownLatch referenceDataLoadLatch,
                                                           @Qualifier("stateChangeInitializedLatch") CountDownLatch stateChangeInitializedLatch) {
        return new StateChangeOrchestrator(tradingEngine, referenceDataLoadLatch, stateChangeInitializedLatch);
    }

    @Bean
    public OrderDataConsumer orderDataConsumer(TradingEngine tradingEngine,
                                               MessageFactory messageFactory) {
        return new OrderDataConsumer(tradingEngine, messageFactory);
    }

    @Bean
    public CountDownLatch referenceDataLoadLatch() {
        return new CountDownLatch(1);
    }

    @Bean
    public CountDownLatch stateChangeInitializedLatch() {
        return new CountDownLatch(1);
    }

    @Bean
    public ReferenceDataConsumer referenceDataConsumer(MessageFactory messageFactory,
                                                       @Qualifier("stateChangeInitializedLatch") CountDownLatch referenceDataLoadLatch) {
        return new ReferenceDataConsumer(referenceDataLoadLatch, messageFactory);
    }

    @Bean
    public KafkaBroadcastHandler kafkaBroadcastHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaBroadcastHandler(kafkaTemplate);
    }

    @Bean
    public TradingEngine tradingEngine(KafkaBroadcastHandler kafkaBroadcastHandler, CountDownLatch referenceDataLoadLatch) {
        return new TradingEngine(kafkaBroadcastHandler, referenceDataLoadLatch);
    }
}
