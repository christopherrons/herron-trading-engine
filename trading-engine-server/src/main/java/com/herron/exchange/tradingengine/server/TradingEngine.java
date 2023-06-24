package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Response;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingEngine.class);
    private static final PartitionKey DEFAULT_PARTITION_KEY = new PartitionKey(KafkaTopicEnum.HERRON_AUDIT_TRAIL, 1);
    private final Map<PartitionKey, MatchingEngine> partitionKeyToMatchingEngine = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradingEngine(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Response addMessage(Message message) {
        return addMessage(DEFAULT_PARTITION_KEY, message);
    }

    public Response addMessage(PartitionKey partitionKey, Message message) {
        return partitionKeyToMatchingEngine.computeIfAbsent(partitionKey, k -> new MatchingEngine(partitionKey, kafkaTemplate)).handleMessage(message);
    }
}
