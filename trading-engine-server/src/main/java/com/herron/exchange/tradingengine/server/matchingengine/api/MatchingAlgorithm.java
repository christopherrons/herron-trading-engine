package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;

import java.util.List;

public interface MatchingAlgorithm {

    List<Message> matchActiveOrder(Order order);

    List<Message> matchFillOrKill(Order order);

    List<Message> matchFillAndKill(Order order);

    List<Message> matchMarketOrder(Order order);

}
