package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.messages.common.Price;

import java.util.List;

public interface MatchingAlgorithm {
    List<OrderbookEvent> matchOrder(Order incomingOrder);

    List<OrderbookEvent> matchAtPrice(Price price);
}
