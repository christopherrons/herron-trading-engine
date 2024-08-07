package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.TradingStatesEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.MarketByLevel;
import com.herron.exchange.common.api.common.messages.trading.TopOfBook;
import com.herron.exchange.common.api.common.messages.trading.TradeExecution;

import java.util.Optional;

public interface Orderbook {
    MarketByLevel getMarketByLevel(int nrOfLevels);

    TopOfBook getTopOfBook();

    Optional<Order> getBestBidOrder();

    Optional<Order> getBestAskOrder();

    TradeExecution runAuctionAlgorithm();

    boolean updateOrderbook(Order order);

    boolean isAccepting();

    boolean updateState(TradingStatesEnum toState);

    TradingStatesEnum getState();

    TradeExecution runMatchingAlgorithm(Order order);

    String getOrderbookId();

    MatchingAlgorithmEnum getMatchingAlgorithm();

    boolean hasBidAndAskOrders();

    Order getOrder(String orderId);

    Volume totalOrderVolume();

    Volume totalBidVolume();

    Volume totalAskVolume();

    Optional<Price> getBestBidPrice();

    Optional<Price> getBestAskPrice();

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

    Optional<Price> getAskPriceAtPriceLevel(int priceLevel);

    Optional<Price> getBidPriceAtPriceLevel(int priceLevel);
}
