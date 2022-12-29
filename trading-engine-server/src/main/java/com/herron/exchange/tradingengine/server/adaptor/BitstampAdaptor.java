package com.herron.exchange.tradingengine.server.adaptor;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.messages.herron.HerronOrder;
import com.herron.exchange.common.api.common.messages.herron.HerronOrderbookData;
import com.herron.exchange.common.api.common.messages.herron.HerronStateChange;
import com.herron.exchange.common.api.common.messages.herron.HerronStockInstrument;
import com.herron.exchange.tradingengine.server.TradingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import static com.herron.exchange.common.api.common.enums.MessageTypesEnum.decodeMessage;

public class BitstampAdaptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitstampAdaptor.class);
    private final TradingEngine tradingEngine;

    public BitstampAdaptor(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    @KafkaListener(topics = "bitstamp-market-data", groupId = "bitstamp-market-data-1")
    public void listenBitstampMarketData(ConsumerRecord<String, String> messageRecord) {
        Message message = decodeMessage(messageRecord.key(), messageRecord.value());
        if (message == null) {
            LOGGER.warn("Unable to map message: {}", messageRecord);
        } else {
            publishMessage(message);
        }
    }

    private void publishMessage(Message message) {
        switch (message.messageType()) {
            case BITSTAMP_STOCK_INSTRUMENT -> tradingEngine.queueMessage(new HerronStockInstrument((Instrument) message));
            case BITSTAMP_ORDERBOOK_DATA -> tradingEngine.queueMessage(new HerronOrderbookData((OrderbookData) message));
            case BITSTAMP_STATE_CHANGE -> tradingEngine.queueMessage(new HerronStateChange((StateChange) message));
            case BITSTAMP_ORDER -> tradingEngine.queueMessage(new HerronOrder((Order) message));
        }
    }
}
