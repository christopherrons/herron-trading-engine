package com.herron.exchange.tradingengine.server.adaptor;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.TopicEnum;
import com.herron.exchange.common.api.common.messages.herron.HerronAddOrder;
import com.herron.exchange.common.api.common.messages.herron.HerronOrderbookData;
import com.herron.exchange.common.api.common.messages.herron.HerronStateChange;
import com.herron.exchange.common.api.common.messages.herron.HerronStockInstrument;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.tradingengine.server.TradingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.atomic.AtomicLong;

import static com.herron.exchange.common.api.common.enums.MessageTypesEnum.deserializeMessage;

public class BitstampAdaptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitstampAdaptor.class);
    private static final PartitionKey PARTITION_ONE_KEY = new PartitionKey(TopicEnum.HERRON_AUDIT_TRAIL, 1);
    private final TradingEngine tradingEngine;

    public BitstampAdaptor(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    private final AtomicLong sequenceNumberChecker = new AtomicLong();

    @KafkaListener(topics = "bitstamp-market-data", groupId = "bitstamp-market-data-1")
    public void listenBitstampMarketData(ConsumerRecord<String, String> consumerRecord) {
        BroadcastMessage broadcastMessage = (BroadcastMessage) deserializeMessage(consumerRecord.key(), consumerRecord.value());
        if (broadcastMessage == null || broadcastMessage.serializedMessage().isEmpty()) {
            LOGGER.warn("Unable to map message: {}", consumerRecord);
            return;
        }

        if (broadcastMessage.sequenceNumber() != sequenceNumberChecker.getAndIncrement()) {
            LOGGER.warn("GAP detected: Expected={}, Incoming={}", sequenceNumberChecker.get(), broadcastMessage.sequenceNumber());
        }

        try {
            Message message = broadcastMessage.message();
            message = mapToHerronMessage(message);
            tradingEngine.queueMessage(PARTITION_ONE_KEY, message);
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
}
