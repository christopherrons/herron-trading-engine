package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingEngine.class);
    private static final String DEFAULT_PARTITION_ID = "partition-default-1";
    private final Map<String, MatchingEngine> partitionIdToMatchingEngine = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradingEngine(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void queueMessage(Message message) {
        queueMessage(DEFAULT_PARTITION_ID, message);
    }

    public void queueMessage(String partitionId, Message message) {
        partitionIdToMatchingEngine.computeIfAbsent(partitionId, k -> new MatchingEngine(partitionId, new AuditTrail(kafkaTemplate, partitionId))).queueMessage(message);
    }
}
