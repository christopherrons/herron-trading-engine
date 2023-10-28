package com.herron.exchange.tradingengine.server.matchingengine.comparator;


import com.herron.exchange.common.api.common.api.trading.Order;

import java.util.Comparator;

public class FifoOrderBookComparator implements Comparator<Order> {
    @Override
    public int compare(Order order, Order otherEvent) {
        if (order.timeOfEvent().isBefore(otherEvent.timeOfEvent())) {
            return -1;

        } else if (order.timeOfEvent().isAfter(otherEvent.timeOfEvent())) {
            return 1;

        } else {
            return order.orderId().compareTo(otherEvent.orderId());
        }
    }
}