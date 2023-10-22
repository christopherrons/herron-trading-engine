package com.herron.exchange.tradingengine.server.matchingengine.orderbook.model;


import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.messages.trading.TradeExecution;

public record MatchingResult(TradeExecution tradeExecution, Order newBidTopOfBook, Order newAskTopOfBook) {
}
