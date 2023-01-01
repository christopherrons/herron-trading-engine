package com.herron.exchange.tradingengine.server.matchingengine.factory;

import com.herron.exchange.common.api.common.api.OrderbookData;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.FifoOrderbook;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ProRataOrderbook;

public class OrderbookFactory {

    public static Orderbook createOrderbook(OrderbookData orderbookData) {
        return switch (orderbookData.matchingAlgorithm()) {
            case FIFO -> new FifoOrderbook(orderbookData);
            case PRO_RATA -> new ProRataOrderbook(orderbookData);
            default -> null;
        };
    }
}
