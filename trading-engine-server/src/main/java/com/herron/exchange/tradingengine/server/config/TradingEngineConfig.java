package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.tradingengine.server.TradingEngine;
import com.herron.exchange.tradingengine.server.adaptor.BitstampAdaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class TradingEngineConfig {

    @Bean
    public BitstampAdaptor getBitstampAdaptor(TradingEngine tradingEngine) {
        return new BitstampAdaptor(tradingEngine);
    }

    @Bean
    public TradingEngine tradingEngine(KafkaTemplate<String, Object> kafkaTemplate) {
        return new TradingEngine(kafkaTemplate);
    }
}
