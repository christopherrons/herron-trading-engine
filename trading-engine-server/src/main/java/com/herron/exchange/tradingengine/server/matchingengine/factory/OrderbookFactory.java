package com.herron.exchange.tradingengine.server.matchingengine.factory;

import com.herron.exchange.common.api.common.api.OrderbookData;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.FifoOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.ProRataOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms.FifoMatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms.ProRataMatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ActiveOrders;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.OrderbookImpl;

public class OrderbookFactory {

    public static Orderbook createOrderbook(OrderbookData orderbookData) {
        return switch (orderbookData.matchingAlgorithm()) {
            case FIFO -> {
                var activeOrders = new ActiveOrders(new FifoOrderBookComparator());
                var matchingAlgorithm = new FifoMatchingAlgorithm(activeOrders);
                yield new OrderbookImpl(orderbookData, activeOrders, matchingAlgorithm);
            }
            case PRO_RATA -> {
                var activeOrders = new ActiveOrders(new ProRataOrderBookComparator());
                var matchingAlgorithm = new ProRataMatchingAlgorithm(activeOrders, orderbookData.minTradeVolume());
                yield new OrderbookImpl(orderbookData, activeOrders, matchingAlgorithm);
            }
            default -> null;
        };
    }
}
