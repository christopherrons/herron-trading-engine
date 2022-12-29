package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.tradingengine.server.TradingEngine;
import com.herron.exchange.tradingengine.server.adaptor.BitstampAdaptor;
import com.herron.exchange.tradingengine.server.audittrail.AuditLogger;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class TradingEngineConfig {


    @Bean
    public AuditTrail auditTrail(KafkaTemplate<String, Object> kafkaTemplate) {
        return new AuditTrail(kafkaTemplate);
    }

    @Bean
    public MatchingEngine matchingEngine() {
        return new MatchingEngine();
    }

    @Bean
    public BitstampAdaptor getBitstampAdaptor(TradingEngine tradingEngine) {
        return new BitstampAdaptor(tradingEngine);
    }

    @Bean
    public TradingEngine tradingEngine(AuditTrail auditTrail, MatchingEngine matchingEngine) {
        return new TradingEngine(matchingEngine, auditTrail);
    }
}
