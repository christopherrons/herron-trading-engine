package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.statechange.StateChange;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TradingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingEngine.class);
    private final Map<String, MatchingEngine> partitionKeyToMatchingEngine = new ConcurrentHashMap<>();
    private final KafkaBroadcastHandler broadcastHandler;
    private final CountDownLatch stateChangeInitializedLatch;

    public TradingEngine(KafkaBroadcastHandler broadcastHandler, CountDownLatch stateChangeInitializedLatch) {
        this.broadcastHandler = broadcastHandler;
        this.stateChangeInitializedLatch = stateChangeInitializedLatch;
    }

    public void queueOrder(Order order) throws InterruptedException {
        stateChangeInitializedLatch.await();
        queueMessage(order);
    }

    public void queueStateChange(StateChange stateChange) {
        queueMessage(stateChange);
    }

    private void queueMessage(OrderbookEvent orderbookEvent) {
        var id = ReferenceDataCache.getCache().getOrderbookData(orderbookEvent.orderbookId()).instrument().product().market().marketId();
        partitionKeyToMatchingEngine.computeIfAbsent(id, key -> {
                    var matchingEngine = new MatchingEngine(key, broadcastHandler);
                    matchingEngine.init();
                    return matchingEngine;
                })
                .queueMessage(orderbookEvent);
    }
}
