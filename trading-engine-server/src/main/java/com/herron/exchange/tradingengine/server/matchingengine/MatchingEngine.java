package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.StateChangeTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;

import java.util.ArrayList;
import java.util.List;

public class MatchingEngine {
    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();

    public void addMessage(Message message) {
        if (message instanceof Order order) {
            addOrder(order);
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

    private boolean addOrder(Order order) {
        final Orderbook orderbook = orderbookCache.getOrderbook(order.orderbookId());
        if (orderbook == null || order.isNonActiveOrder()) {
            return false;
        }

        switch (order.orderOperation()) {
            case CREATE -> orderbook.addOrder(order);
            case UPDATE -> orderbook.updateOrder(order);
            case DELETE -> orderbook.removeOrder(order);
        }
        return true;
    }

    public List<Message> runMatchingAlgorithm(Order order) {
        if (order.isNonActiveOrder()) {
            return runMatchingAlgorithmNonActiveOrder(order);
        }
        return runMatchingAlgorithmActiveOrder(order);
    }

    private List<Message> runMatchingAlgorithmActiveOrder(Order activeOrder) {
        final Orderbook orderbook = orderbookCache.getOrderbook(activeOrder.orderbookId());
        final List<Message> result = new ArrayList<>();
        if (!orderbook.getState().equals(StateChangeTypeEnum.CONTINUOUS_TRADING)) {
            return result;
        }

        List<Message> matchingMessages;
        do {
            matchingMessages = orderbook.runMatchingAlgorithm(activeOrder);
            for (var message : matchingMessages) {
                result.add(message);
                addMessage(message);
                if (message instanceof Order order && order.orderId().equals(activeOrder.orderId())) {
                    activeOrder = order;
                }
            }
        } while (!matchingMessages.isEmpty() && !activeOrder.orderOperation().equals(OrderOperationEnum.DELETE));

        return result;
    }

    private List<Message> runMatchingAlgorithmNonActiveOrder(Order nonActiveOrder) {
        final Orderbook orderbook = orderbookCache.getOrderbook(nonActiveOrder.orderbookId());
        final List<Message> result = new ArrayList<>();
        if (!orderbook.getState().equals(StateChangeTypeEnum.CONTINUOUS_TRADING)) {
            return result;
        }

        List<Message> matchingMessages;
        do {
            matchingMessages = orderbook.runMatchingAlgorithmNonActiveOrder(nonActiveOrder);
            for (var message : matchingMessages) {
                result.add(message);
                if (message instanceof Order order && !addOrder(order)) {
                    nonActiveOrder = order;
                }
            }
        } while (!matchingMessages.isEmpty() && !nonActiveOrder.orderOperation().equals(OrderOperationEnum.DELETE));

        return result;
    }
}
