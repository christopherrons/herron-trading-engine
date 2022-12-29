package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.Order;

import java.util.Optional;

public interface ActiveOrderReadOnly {

    Optional<Order> getBestBidOrder();

    Optional<Order> getBestAskOrder();

    boolean hasBidAndAskOrders();
}
