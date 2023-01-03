package com.herron.exchange.tradingengine.server.adaptor;

import com.herron.exchange.common.api.common.api.BroadcastMessage;
import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.tradingengine.server.TradingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.herron.exchange.common.api.common.enums.MessageTypesEnum.deserializeMessage;

public class BitstampAdaptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitstampAdaptor.class);
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.HERRON_AUDIT_TRAIL, 0);
    private static final PartitionKey PARTITION_ONE_KEY = new PartitionKey(KafkaTopicEnum.HERRON_AUDIT_TRAIL, 1);
    private final TradingEngine tradingEngine;
    private final Map<PartitionKey, AtomicLong> partitionToSequenceNumberHandler = new ConcurrentHashMap<>();

    public BitstampAdaptor(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    @KafkaListener(id = "bistamp-adaptor-listener-one", topicPartitions = {@TopicPartition(topic = "bitstamp-market-data",
            partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))})
    public void listenBitstampMarketDataOne(ConsumerRecord<String, String> consumerRecord) {
        queueMessage(consumerRecord, PARTITION_ZERO_KEY);
    }

    @KafkaListener(id = "bistamp-adaptor-listener-two", topicPartitions = {@TopicPartition(topic = "bitstamp-market-data",
            partitionOffsets = @PartitionOffset(partition = "1", initialOffset = "0"))})
    public void listenBitstampMarketDataTwo(ConsumerRecord<String, String> consumerRecord) {
        queueMessage(consumerRecord, PARTITION_ONE_KEY);
    }

    private void queueMessage(ConsumerRecord<String, String> consumerRecord, PartitionKey partitionKey) {
        BroadcastMessage broadcastMessage = (BroadcastMessage) deserializeMessage(consumerRecord.key(), consumerRecord.value());
        if (broadcastMessage == null || broadcastMessage.serialize().isEmpty()) {
            LOGGER.warn("Unable to map message: {}", consumerRecord);
            return;
        }

        long expected = getSequenceNumber(partitionKey);
        if (broadcastMessage.sequenceNumber() != expected) {
            LOGGER.warn("GAP detected: Expected={}, Incoming={}", expected, broadcastMessage.sequenceNumber());
        }

        try {
            Message message = broadcastMessage.message();
            tradingEngine.queueMessage(partitionKey, message);
        } catch (Exception e) {
            LOGGER.warn("Unhandled exception for record: {}, decoded-message: {}, {}", consumerRecord, broadcastMessage, e);
        }
    }

    private long getSequenceNumber(PartitionKey partitionKey) {
        return partitionToSequenceNumberHandler.computeIfAbsent(partitionKey, k -> new AtomicLong(1)).getAndIncrement();
    }
}
