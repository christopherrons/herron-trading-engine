package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.trading.StateChange;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingEngine.class);
    private final Map<String, MatchingEngine> partitionKeyToMatchingEngine = new ConcurrentHashMap<>();
    private final KafkaBroadcastHandler broadcastHandler;

    public TradingEngine(KafkaBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
    }

    public void queueOrder(Order order) {
        queueMessage(order);
    }

    public void queueStateChange(StateChange stateChange) {
        queueMessage(stateChange);
    }

    private void queueMessage(OrderbookEvent orderbookEvent) {
        var id = ReferenceDataCache.getCache().getOrderbookData(orderbookEvent.orderbookId()).instrument().product().productName();
        partitionKeyToMatchingEngine.computeIfAbsent(id, key -> {
                    var matchingEngine = new MatchingEngine(key, broadcastHandler);
                    matchingEngine.init();
                    return matchingEngine;
                })
                .queueMessage(orderbookEvent);
    }
}
