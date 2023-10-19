package com.herron.exchange.tradingengine.server.matchingengine.comparator;


import com.herron.exchange.common.api.common.api.trading.orders.Order;

import java.util.Comparator;

public class ProRataOrderBookComparator implements Comparator<Order> {
    private final FifoOrderBookComparator fifoOrderBookComparator = new FifoOrderBookComparator();

    @Override
    public int compare(Order order, Order otherEvent) {
        if (order.currentVolume().gt(otherEvent.currentVolume())) {
            return -1;

        } else if (order.currentVolume().lt(otherEvent.currentVolume())) {
            return 1;

        } else {
            if (order.currentVolume().equals(otherEvent.currentVolume())) {
                return fifoOrderBookComparator.compare(order, otherEvent);
            } else {
                return order.orderId().compareTo(otherEvent.orderId());
            }
        }

    }

}