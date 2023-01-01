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

import static com.herron.exchange.common.api.common.enums.MessageTypesEnum.deserializeMessage;

public class BitstampAdaptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitstampAdaptor.class);
    private final TradingEngine tradingEngine;

    public BitstampAdaptor(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    @KafkaListener(topics = "bitstamp-market-data", groupId = "bitstamp-market-data-1")
    public void listenBitstampMarketData(ConsumerRecord<String, String> consumerRecord) {
        Message message = deserializeMessage(consumerRecord.key(), consumerRecord.value());
        if (message == null) {
            LOGGER.warn("Unable to map message: {}", consumerRecord);
        } else {
            try {
                message = mapToHerronMessage(message);
                tradingEngine.queueMessage(new PartitionKey(TopicEnum.HERRON_AUDIT_TRAIL, 1), message);
            } catch (Exception e) {
                LOGGER.warn("Unhandled exception for record: {], decoded-message: {}, {}", consumerRecord, message, e);
            }
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
