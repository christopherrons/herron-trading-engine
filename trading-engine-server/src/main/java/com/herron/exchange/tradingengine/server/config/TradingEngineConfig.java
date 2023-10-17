package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.mapping.DefaultMessageFactory;
import com.herron.exchange.tradingengine.server.TradingEngine;
import com.herron.exchange.tradingengine.server.consumers.OrderDataConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class TradingEngineConfig {

    @Bean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean
    public OrderDataConsumer orderDataConsumer(TradingEngine tradingEngine,
                                               MessageFactory messageFactory) {
        return new OrderDataConsumer(tradingEngine, messageFactory);
    }

    @Bean
    public KafkaBroadcastHandler kafkaBroadcastHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaBroadcastHandler(kafkaTemplate);
    }

    @Bean
    public TradingEngine tradingEngine(KafkaBroadcastHandler kafkaBroadcastHandler) {
        return new TradingEngine(kafkaBroadcastHandler);
    }
}
