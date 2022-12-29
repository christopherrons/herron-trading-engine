package com.herron.exchange.tradingengine.server.matchingengine.comparator;

import com.herron.exchange.common.api.common.api.Order;

import java.util.Comparator;

public class FifoOrderBookComparator implements Comparator<Order> {
    @Override
    public int compare(Order order, Order otherEvent) {
        if (order.timeStampInMs() < otherEvent.timeStampInMs()) {
            return -1;
        } else {
            return order.orderId().equals(otherEvent.orderId()) ? 0 : 1;
        }

    }

}