package com.herron.exchange.tradingengine.server.matchingengine.orderbook;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.OrderbookData;
import com.herron.exchange.common.api.common.api.TradeExecution;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.StateChangeTypeEnum;
import com.herron.exchange.common.api.common.messages.HerronTradeExecution;
import com.herron.exchange.tradingengine.server.matchingengine.api.AuctionAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderbookImpl implements Orderbook {
    private final Logger logger = LoggerFactory.getLogger(OrderbookImpl.class);
    private final OrderbookData orderbookData;
    private final ActiveOrders activeOrders;
    private final MatchingAlgorithm matchingAlgorithm;
    private final AuctionAlgorithm auctionAlgorithm;
    private StateChangeTypeEnum currentState = StateChangeTypeEnum.CLOSED;


    public OrderbookImpl(OrderbookData orderbookData,
                         ActiveOrders activeOrders,
                         MatchingAlgorithm matchingAlgorithm,
                         AuctionAlgorithm auctionAlgorithm) {
        this.orderbookData = orderbookData;
        this.activeOrders = activeOrders;
        this.matchingAlgorithm = matchingAlgorithm;
        this.auctionAlgorithm = auctionAlgorithm;
    }

    @Override
    public synchronized void updateOrderbook(Order order) {
        if (order.isActiveOrder()) {
            switch (order.orderOperation()) {
                case CREATE -> addOrder(order);
                case UPDATE -> updateOrder(order);
                case DELETE -> removeOrder(order);
            }
        }
    }

    @Override
    public boolean isAccepting() {
        if (currentState == null) {
            return false;
        }
        if (currentState == StateChangeTypeEnum.TRADE_STOP) {
            return false;
        }

        if (currentState == StateChangeTypeEnum.CLOSED) {
            return false;
        }
        return true;
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
    public boolean updateState(StateChangeTypeEnum toState) {
        if (currentState == null || currentState.isValidStateChange(toState)) {
            logger.info("Successfully updated orderbook: {} from state: {} to state: {}", getOrderbookId(), currentState, toState);
            currentState = toState;
            return true;
        }
        logger.error("Could not updated orderbook: {} from state: {} to state: {}", getOrderbookId(), currentState, toState);
        return false;
    }

    @Override
    public StateChangeTypeEnum getState() {
        return currentState;
    }

    @Override
    public TradeExecution runMatchingAlgorithm(final Order matchingOrder) {
        if (matchingOrder.currentVolume() <= 0 || currentState != StateChangeTypeEnum.CONTINUOUS_TRADING) {
            return new HerronTradeExecution(matchingOrder, List.of(), Instant.now().toEpochMilli());
        }

        final List<Message> messages = new ArrayList<>();
        List<Message> matchingMessages;
        Order updatedMatchingOrder = (Order) matchingOrder.getCopy();
        do {
            matchingMessages = matchingAlgorithm.matchOrder(updatedMatchingOrder);
            for (var message : matchingMessages) {
                messages.add(message);
                if (message instanceof Order order) {
                    updateOrderbook(order);
                    if (order.orderId().equals(updatedMatchingOrder.orderId())) {
                        updatedMatchingOrder = order;
                    }
                }
            }
        } while (!matchingMessages.isEmpty() && updatedMatchingOrder.orderOperation() != OrderOperationEnum.DELETE);

        return new HerronTradeExecution(matchingOrder, messages, Instant.now().toEpochMilli());
    }

    @Override
    public TradeExecution runAuctionAlgorithm() {
        if (currentState != StateChangeTypeEnum.AUCTION_RUN) {
            return new HerronTradeExecution(null, List.of(), Instant.now().toEpochMilli());
        }

        var equilibriumPrice = auctionAlgorithm.calculateEquilibriumPrice();
        final List<Message> messages = new ArrayList<>();
        List<Message> matchingMessages;
        do {
            matchingMessages = matchingAlgorithm.matchAtPrice(equilibriumPrice.optimalPrice().equilibriumPrice());
            for (var message : matchingMessages) {
                messages.add(message);
                if (message instanceof Order order) {
                    updateOrderbook(order);
                }
            }
        } while (!matchingMessages.isEmpty());

        return new HerronTradeExecution(null, messages, Instant.now().toEpochMilli());
    }
}
