package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.StateChange;
import com.herron.exchange.common.api.common.api.TradeExecution;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.StateChangeTypeEnum;

public interface Orderbook {

    void updateOrderbook(Order order);

    void updateState(StateChange stateChange);

    StateChangeTypeEnum getState();

    TradeExecution runMatchingAlgorithm(Order order);

    String getOrderbookId();

    MatchingAlgorithmEnum getMatchingAlgorithm();

    boolean hasBidAndAskOrders();

    Order getOrder(String orderId);

    double totalOrderVolume();

    double totalBidVolume();

    double totalAskVolume();

    double getBestBidPrice();

    double getBestAskPrice();

    long totalNumberOfBidOrders();

    long totalNumberOfAskOrders();

    long totalNumberOfActiveOrders();

    double totalVolumeAtPriceLevel(int priceLevel);

    double totalBidVolumeAtPriceLevel(int priceLevel);

    double totalAskVolumeAtPriceLevel(int priceLevel);

    int totalNumberOfPriceLevels();

    int totalNumberOfBidPriceLevels();

    int totalNumberOfAskPriceLevels();

    String getInstrumentId();

    void removeOrder(String orderId);

    double getAskPriceAtPriceLevel(int priceLevel);

    double getBidPriceAtPriceLevel(int priceLevel);
}
