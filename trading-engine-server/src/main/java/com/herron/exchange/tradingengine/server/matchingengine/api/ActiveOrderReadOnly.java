package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.model.PriceLevel;

import java.util.Optional;

public interface ActiveOrderReadOnly {

    Optional<Order> getBestBidOrder();

    Optional<Order> getBestAskOrder();

    boolean hasBidAndAskOrders();

    boolean isTotalFillPossible(Order order);

    Optional<PriceLevel> getBestBidPriceLevel();

    Optional<PriceLevel> getBestAskPriceLevel();
}
