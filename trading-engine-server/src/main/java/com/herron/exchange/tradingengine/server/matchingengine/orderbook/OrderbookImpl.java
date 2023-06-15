package com.herron.exchange.tradingengine.server.matchingengine.orderbook;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.StateChangeTypeEnum;
import com.herron.exchange.common.api.common.messages.HerronTradeExecution;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.FifoOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms.FifoMatchingAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderbookImpl implements Orderbook {
    private final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);

    private StateChangeTypeEnum stateChangeTypeEnum = StateChangeTypeEnum.INVALID_STATE_CHANGE;
    private final OrderbookData orderbookData;
    private final ActiveOrders activeOrders;
    private final MatchingAlgorithm matchingAlgorithm;

    public OrderbookImpl(OrderbookData orderbookData, ActiveOrders activeOrders, MatchingAlgorithm matchingAlgorithm) {
        this.orderbookData = orderbookData;
        this.activeOrders = activeOrders;
        this.matchingAlgorithm = matchingAlgorithm;
    }

    public synchronized void updateOrderbook(Order order) {
        if (order.isActiveOrder()) {
            switch (order.orderOperation()) {
                case CREATE -> addOrder(order);
                case UPDATE -> updateOrder(order);
                case DELETE -> removeOrder(order);

            }
        }
    }

    private void updateOrder(Order order) {
        activeOrders.updateOrder(order);
    }

    private void addOrder(Order order) {
        activeOrders.addOrder(order);
    }

    public void removeOrder(String orderId) {
        activeOrders.removeOrder(orderId);
    }

    private void removeOrder(Order order) {
        activeOrders.removeOrder(order);
    }

    @Override
    public double getBestBidPrice() {
        return activeOrders.getBestBidPrice();
    }

    @Override
    public double getBestAskPrice() {
        return activeOrders.getBestAskPrice();
    }

    @Override
    public boolean hasBidAndAskOrders() {
        return activeOrders.hasBidAndAskOrders();
    }

    @Override
    public long totalNumberOfBidOrders() {
        return activeOrders.totalNumberOfBidOrders();
    }

    @Override
    public long totalNumberOfAskOrders() {
        return activeOrders.totalNumberOfAskOrders();
    }

    @Override
    public long totalNumberOfActiveOrders() {
        return activeOrders.totalNumberOfActiveOrders();
    }

    @Override
    public double totalOrderVolume() {
        return activeOrders.totalOrderVolume();
    }

    @Override
    public double totalBidVolume() {
        return activeOrders.totalBidVolume();
    }

    @Override
    public double totalAskVolume() {
        return activeOrders.totalAskVolume();
    }

    @Override
    public double totalVolumeAtPriceLevel(int priceLevel) {
        return activeOrders.totalVolumeAtPriceLevel(priceLevel);
    }

    @Override
    public double totalBidVolumeAtPriceLevel(int priceLevel) {
        return activeOrders.totalBidVolumeAtPriceLevel(priceLevel);
    }

    @Override
    public double totalAskVolumeAtPriceLevel(int priceLevel) {
        return activeOrders.totalAskVolumeAtPriceLevel(priceLevel);
    }

    @Override
    public int totalNumberOfPriceLevels() {
        return activeOrders.totalNumberOfPriceLevels();
    }

    @Override
    public int totalNumberOfBidPriceLevels() {
        return activeOrders.totalNumberOfBidPriceLevels();
    }

    @Override
    public int totalNumberOfAskPriceLevels() {
        return activeOrders.totalNumberOfAskPriceLevels();
    }

    @Override
    public Order getOrder(String orderId) {
        return activeOrders.getOrder(orderId);
    }

    @Override
    public MatchingAlgorithmEnum getMatchingAlgorithm() {
        return orderbookData.matchingAlgorithm();
    }

    @Override
    public String getOrderbookId() {
        return orderbookData.orderbookId();
    }

    @Override
    public String getInstrumentId() {
        return orderbookData.instrumentId();
    }

    @Override
    public double getAskPriceAtPriceLevel(int priceLevel) {
        return activeOrders.getAskPriceAtPriceLevel(priceLevel);
    }

    @Override
    public double getBidPriceAtPriceLevel(int priceLevel) {
        return activeOrders.getBidPriceAtPriceLevel(priceLevel);
    }

    @Override
    public void updateState(StateChange stateChange) {
        stateChangeTypeEnum = stateChange.stateChangeType();
    }

    @Override
    public StateChangeTypeEnum getState() {
        return stateChangeTypeEnum;
    }

    @Override
    public TradeExecution runMatchingAlgorithm(Order matchingOrder) {
        if (matchingOrder.currentVolume() <= 0 || getState() != StateChangeTypeEnum.CONTINUOUS_TRADING) {
            return null;
        }

        final List<Message> messages = new ArrayList<>();
        List<Message> matchingMessages;
        do {
            matchingMessages = matchingAlgorithm.matchOrder(matchingOrder);
            for (var message : matchingMessages) {
                messages.add(message);
                if (message instanceof Order order) {
                    updateOrderbook(order);
                    if (order.orderId().equals(matchingOrder.orderId())) {
                        matchingOrder = order;
                    }
                }
            }
        } while (!matchingMessages.isEmpty() && matchingOrder.orderOperation() != OrderOperationEnum.DELETE);

        return new HerronTradeExecution(messages, Instant.now().toEpochMilli());
    }
}
