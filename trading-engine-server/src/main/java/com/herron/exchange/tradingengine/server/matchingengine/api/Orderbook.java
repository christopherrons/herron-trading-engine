package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.trades.TradeExecution;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.StateChangeTypeEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;

import java.util.Optional;

public interface Orderbook {

    Order getAskOrderIfPriceDoesNotMatch(Price preMatchAskPrice);

    Order getBidOrderIfPriceDoesNotMatch(Price preMatchBidPrice);

    Optional<Order> getBestBidOrder();

    Optional<Order> getBestAskOrder();

    TradeExecution runAuctionAlgorithm();

    boolean updateOrderbook(Order order);

    boolean isAccepting();

    boolean updateState(StateChangeTypeEnum toState);

    StateChangeTypeEnum getState();

    TradeExecution runMatchingAlgorithm(Order order);

    String getOrderbookId();

    MatchingAlgorithmEnum getMatchingAlgorithm();

    boolean hasBidAndAskOrders();

    Order getOrder(String orderId);

    Volume totalOrderVolume();

    Volume totalBidVolume();

    Volume totalAskVolume();

    Price getBestBidPrice();

    Price getBestAskPrice();

    long totalNumberOfBidOrders();

    long totalNumberOfAskOrders();

    long totalNumberOfActiveOrders();

    Volume totalVolumeAtPriceLevel(int priceLevel);

    Volume totalBidVolumeAtPriceLevel(int priceLevel);

    Volume totalAskVolumeAtPriceLevel(int priceLevel);

    int totalNumberOfPriceLevels();

    int totalNumberOfBidPriceLevels();

    int totalNumberOfAskPriceLevels();

    String getInstrumentId();

    boolean removeOrder(String orderId);

    Price getAskPriceAtPriceLevel(int priceLevel);

    Price getBidPriceAtPriceLevel(int priceLevel);
}
