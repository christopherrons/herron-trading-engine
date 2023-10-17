package com.herron.exchange.tradingengine.server.matchingengine.cache;

import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.factory.OrderbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderbookCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderbookCache.class);

    private final Map<String, Orderbook> orderbookIdToOrderBook = new ConcurrentHashMap<>();

    public Orderbook getOrCreateOrderbook(String orderbookId) {
        return orderbookIdToOrderBook.computeIfAbsent(orderbookId, obId -> OrderbookFactory.createOrderbook(ReferenceDataCache.getCache().getOrderbookData(obId)));
    }
}