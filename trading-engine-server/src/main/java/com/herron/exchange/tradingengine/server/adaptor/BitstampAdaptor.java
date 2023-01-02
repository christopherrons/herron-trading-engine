package com.herron.exchange.tradingengine.server.adaptor;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.TopicEnum;
import com.herron.exchange.common.api.common.messages.HerronAddOrder;
import com.herron.exchange.common.api.common.messages.HerronOrderbookData;
import com.herron.exchange.common.api.common.messages.HerronStateChange;
import com.herron.exchange.common.api.common.messages.HerronStockInstrument;
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
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(TopicEnum.HERRON_AUDIT_TRAIL, 0);
    private static final PartitionKey PARTITION_ONE_KEY = new PartitionKey(TopicEnum.HERRON_AUDIT_TRAIL, 1);
    private final TradingEngine tradingEngine;
    private final Map<Integer, AtomicLong> partitionToSequenceNumberHandler = new ConcurrentHashMap<>();

    public BitstampAdaptor(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    @KafkaListener(id = "bistamp-adaptor-listener-one", topicPartitions = {@TopicPartition(topic = "bitstamp-market-data",
            partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))})
    public void listenBitstampMarketDataOne(ConsumerRecord<String, String> consumerRecord) {
        queueMessage(consumerRecord);
    }

    @KafkaListener(id = "bistamp-adaptor-listener-two", topicPartitions = {@TopicPartition(topic = "bitstamp-market-data",
            partitionOffsets = @PartitionOffset(partition = "1", initialOffset = "0"))})
    public void listenBitstampMarketDataTwo(ConsumerRecord<String, String> consumerRecord) {
        queueMessage(consumerRecord);
    }

    private void queueMessage(ConsumerRecord<String, String> consumerRecord) {
        BroadcastMessage broadcastMessage = (BroadcastMessage) deserializeMessage(consumerRecord.key(), consumerRecord.value());
        if (broadcastMessage == null || broadcastMessage.serialize().isEmpty()) {
            LOGGER.warn("Unable to map message: {}", consumerRecord);
            return;
        }

        long expected = getSequenceNumber(consumerRecord.partition());
        if (broadcastMessage.sequenceNumber() != expected) {
            LOGGER.warn("GAP detected: Expected={}, Incoming={}", expected, broadcastMessage.sequenceNumber());
        }

        try {
            Message message = broadcastMessage.message();
            message = mapToHerronMessage(message);
            tradingEngine.queueMessage(getPartitionKey(consumerRecord.partition()), message);
        } catch (Exception e) {
            LOGGER.warn("Unhandled exception for record: {}, decoded-message: {}, {}", consumerRecord, broadcastMessage, e);
        }
    }

    private Message mapToHerronMessage(Message message) {
        return switch (message.messageType()) {
            case BITSTAMP_STOCK_INSTRUMENT -> new HerronStockInstrument((Instrument) message);
            case BITSTAMP_ORDERBOOK_DATA -> new HerronOrderbookData((OrderbookData) message);
            case BITSTAMP_STATE_CHANGE -> new HerronStateChange((StateChange) message);
            case BITSTAMP_ADD_ORDER -> new HerronAddOrder((AddOrder) message);
            default -> null;
        };
    }

    private long getSequenceNumber(int partition) {
        return partitionToSequenceNumberHandler.computeIfAbsent(partition, k -> new AtomicLong(1)).getAndIncrement();
    }

    private PartitionKey getPartitionKey(int partition) {
        if (partition == 0) {
            return PARTITION_ZERO_KEY;
        } else {
            return PARTITION_ONE_KEY;
        }
    }

}
