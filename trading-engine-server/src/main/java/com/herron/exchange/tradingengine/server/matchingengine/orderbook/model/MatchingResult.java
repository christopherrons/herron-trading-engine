package com.herron.exchange.tradingengine.server.matchingengine.orderbook.model;

import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.trades.TradeExecution;

public record MatchingResult(TradeExecution tradeExecution, Order newBidTopOfBook, Order newAskTopOfBook) {
}
