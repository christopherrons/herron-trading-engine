package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;

public interface Orderbook {
    String getOrderbookId();

    MatchingAlgorithmEnum getMatchingAlgorithm();

    boolean hasBidAndAskOrders();

    void addOrder(Order order);

    Order getOrder(String orderId);

    void updateOrder(Order order);

    void removeOrder(Order order);

    void removeOrder(String orderId);

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

    double getAskPriceAtPriceLevel(int priceLevel);

    double getBidPriceAtPriceLevel(int priceLevel);
}
