package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;

import java.util.List;

public interface MatchingAlgorithm {
    List<Message> matchOrder(Order order);

    List<Message> matchAtPrice(double price);
}
