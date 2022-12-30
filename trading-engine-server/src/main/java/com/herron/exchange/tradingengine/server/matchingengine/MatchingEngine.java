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

    public boolean add(Message message) {
        if (message instanceof Order order) {
            return handleOrder(order);
        } else if (message instanceof OrderbookData orderbookData) {
            return handleOrderbookData(orderbookData);
        } else if (message instanceof Instrument instrument) {
            return handleInstrument(instrument);
        } else if (message instanceof StateChange stateChange) {
            return handleState(stateChange);
        }

        return false;
    }

    private boolean handleOrderbookData(OrderbookData orderbookData) {
        referanceDataCache.addOrderbookData(orderbookData);
        orderbookCache.createOrderbook(orderbookData);
        return true;
    }

    private boolean handleInstrument(Instrument instrument) {
        referanceDataCache.addInstrument(instrument);
        return true;
    }

    private boolean handleState(StateChange stateChange) {
        orderbookCache.updateState(stateChange);
        return true;
    }

    private boolean handleOrder(Order order) {
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
        return runMatchingAlgorithm(order.orderbookId());
    }

    private List<Message> runMatchingAlgorithm(String orderbookId) {
        final Orderbook orderbook = orderbookCache.getOrderbook(orderbookId);
        final List<Message> result = new ArrayList<>();
        if (!orderbook.getState().equals(StateChangeTypeEnum.CONTINUOUS_TRADING)) {
            return result;
        }

        while (true) {
            var matchingMessages = orderbook.runMatchingAlgorithmNonActiveOrder();
            matchingMessages.forEach(message -> {
                result.add(message);
                add(message);
            });

            if (matchingMessages.isEmpty()) {
                break;

            }
        }
        return result;
    }

    private List<Message> runMatchingAlgorithmNonActiveOrder(Order nonActiveOrder) {
        final Orderbook orderbook = orderbookCache.getOrderbook(nonActiveOrder.orderbookId());
        final List<Message> result = new ArrayList<>();
        if (!orderbook.getState().equals(StateChangeTypeEnum.CONTINUOUS_TRADING)) {
            return result;
        }

        while (true) {
            var matchingMessages = orderbook.runMatchingAlgorithmNonActiveOrder(nonActiveOrder);
            for (var message : matchingMessages) {
                result.add(message);
                if (!add(message)) {
                    nonActiveOrder = (Order) message;
                }
            }

            if (matchingMessages.isEmpty() || nonActiveOrder.orderOperation().equals(OrderOperationEnum.DELETE)) {
                break;

            }
        }
        return result;
    }
}
