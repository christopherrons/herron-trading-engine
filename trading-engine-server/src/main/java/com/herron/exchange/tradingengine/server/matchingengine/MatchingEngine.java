package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.StateChangeTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;

import java.util.LinkedList;
import java.util.Queue;

public class MatchingEngine {

    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();

    public void add(Message message) {
        if (message instanceof Order order) {
            handleOrder(order);
        } else if (message instanceof OrderbookData orderbookData) {
            handleOrderbookData(orderbookData);
        } else if (message instanceof Instrument instrument) {
            handleInstrument(instrument);
        } else if (message instanceof StateChange stateChange) {
            handleState(stateChange);
        }
    }

    private void handleOrderbookData(OrderbookData orderbookData) {
        referanceDataCache.addOrderbookData(orderbookData);
        orderbookCache.createOrderbook(orderbookData);
    }

    private void handleInstrument(Instrument instrument) {
        referanceDataCache.addInstrument(instrument);
    }

    private void handleState(StateChange stateChange) {
        orderbookCache.updateState(stateChange);
    }

    private void handleOrder(Order order) {
        final Orderbook orderbook = orderbookCache.getOrderbook(order.orderbookId());
        if (orderbook == null) {
            return;
        }

        switch (order.orderOperation()) {
            case CREATE -> orderbook.addOrder(order);
            case UPDATE -> orderbook.updateOrder(order);
            case DELETE -> orderbook.removeOrder(order);
        }
    }

    public Queue<Message> runMatchingAlgorithm(String orderbookId) {
        final Orderbook orderbook = orderbookCache.getOrderbook(orderbookId);
        if (orderbook.getState().equals(StateChangeTypeEnum.CONTINUOUS_TRADING)) {
            return orderbook.runMatchingAlgorithm();
        }
        return new LinkedList<>();
    }
}
