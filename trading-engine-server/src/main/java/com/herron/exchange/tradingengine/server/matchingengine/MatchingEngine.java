package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;

import java.util.Collections;
import java.util.List;

public class MatchingEngine {
    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();

    public void addMessage(Message message) {
        if (message instanceof Order order) {
            var orderbook = orderbookCache.getOrderbook(order.orderbookId());
            if (orderbook != null) {
                orderbook.updateOrderbook(order);
            }
        } else if (message instanceof OrderbookData orderbookData) {
            addOrderbookData(orderbookData);
        } else if (message instanceof Instrument instrument) {
            addInstrument(instrument);
        } else if (message instanceof StateChange stateChange) {
            updateState(stateChange);
        }
    }

    private void addOrderbookData(OrderbookData orderbookData) {
        referanceDataCache.addOrderbookData(orderbookData);
        orderbookCache.createOrderbook(orderbookData);
    }

    private void addInstrument(Instrument instrument) {
        referanceDataCache.addInstrument(instrument);
    }

    private void updateState(StateChange stateChange) {
        orderbookCache.updateState(stateChange);
    }

    public List<Message> runMatchingAlgorithm(Order order) {
        final Orderbook orderbook = orderbookCache.getOrderbook(order.orderbookId());
        return orderbook != null ? orderbook.runMatchingAlgorithm(order) : Collections.emptyList();
    }

}
