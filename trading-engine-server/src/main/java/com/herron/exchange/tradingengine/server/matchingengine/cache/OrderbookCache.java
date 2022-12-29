package com.herron.exchange.tradingengine.server.matchingengine.cache;

import com.herron.exchange.common.api.common.api.OrderbookData;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.herron.exchange.tradingengine.server.matchingengine.factory.OrderbookFactory.createOrderbook;

public class OrderbookCache {

    private final Map<String, Orderbook> orderbookIdToOrderBook = new ConcurrentHashMap<>();

    public Orderbook findOrCreate(OrderbookData orderbookData) {
        return orderbookIdToOrderBook.computeIfAbsent(orderbookData.orderbookId(), k -> createOrderbook(orderbookData));
    }


}