package com.herron.exchange.tradingengine.server.matchingengine.orderbook;

import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.enums.MatchingAlgorithmEnum;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.TradingStatesEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.ImmutableTradeExecution;
import com.herron.exchange.common.api.common.messages.trading.TradeExecution;
import com.herron.exchange.tradingengine.server.matchingengine.api.AuctionAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.TradingStatesEnum.*;


public class OrderbookImpl implements Orderbook {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderbookImpl.class);
    private final OrderbookData orderbookData;
    private final ActiveOrders activeOrders;
    private final MatchingAlgorithm matchingAlgorithm;
    private final AuctionAlgorithm auctionAlgorithm;
    private TradingStatesEnum currentState = CLOSED;

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
    public synchronized boolean updateOrderbook(Order order) {
        if (!isAccepting()) {
            LOGGER.error("Is not accepting {}.", currentState);
            return false;
        }
        if (order.isActiveOrder()) {
            return switch (order.orderOperation()) {
                case INSERT -> addOrder(order);
                case UPDATE -> updateOrder(order);
                case CANCEL -> removeOrder(order);
            };
        }
        return true;
    }

    @Override
    public boolean isAccepting() {
        //FIXME: Add acceptable operations based on the current state.
        if (currentState == null) {
            return false;
        }
        if (currentState == TRADE_HALT) {
            return false;
        }

        if (currentState == CLOSED) {
            return false;
        }
        return true;
    }

    private boolean updateOrder(Order order) {
        return activeOrders.updateOrder(order);
    }

    private boolean addOrder(Order order) {
        return activeOrders.addOrder(order);
    }

    public boolean removeOrder(String orderId) {
        return activeOrders.removeOrder(orderId);
    }

    private boolean removeOrder(Order order) {
        return activeOrders.removeOrder(order);
    }

    @Override
    public Price getBestBidPrice() {
        return activeOrders.getBestBidPrice();
    }

    @Override
    public Price getBestAskPrice() {
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
    public Volume totalOrderVolume() {
        return activeOrders.totalOrderVolume();
    }

    @Override
    public Volume totalBidVolume() {
        return activeOrders.totalBidVolume();
    }

    @Override
    public Volume totalAskVolume() {
        return activeOrders.totalAskVolume();
    }

    @Override
    public Volume totalVolumeAtPriceLevel(int priceLevel) {
        return activeOrders.totalVolumeAtPriceLevel(priceLevel);
    }

    @Override
    public Volume totalBidVolumeAtPriceLevel(int priceLevel) {
        return activeOrders.totalBidVolumeAtPriceLevel(priceLevel);
    }

    @Override
    public Volume totalAskVolumeAtPriceLevel(int priceLevel) {
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
        return orderbookData.instrument().instrumentId();
    }

    @Override
    public Price getAskPriceAtPriceLevel(int priceLevel) {
        return activeOrders.getAskPriceAtPriceLevel(priceLevel);
    }

    @Override
    public Price getBidPriceAtPriceLevel(int priceLevel) {
        return activeOrders.getBidPriceAtPriceLevel(priceLevel);
    }

    @Override
    public Optional<Order> getBestBidOrder() {
        return activeOrders.getBestBidOrder();
    }

    @Override
    public Optional<Order> getBestAskOrder() {
        return activeOrders.getBestAskOrder();
    }

    @Override
    public boolean updateState(TradingStatesEnum toState) {
        if (toState == currentState) {
            return true;
        }

        if (currentState == null || currentState.isValidStateChange(toState)) {
            LOGGER.info("Successfully updated orderbook {} from state {} to state {}.", getOrderbookId(), currentState, toState);
            currentState = toState;
            return true;
        }
        LOGGER.error("Could not updated orderbook {} from state {} to state {}.", getOrderbookId(), currentState, toState);
        return false;
    }

    @Override
    public TradingStatesEnum getState() {
        return currentState;
    }

    @Override
    public TradeExecution runMatchingAlgorithm(final Order incomingOrder) {
        if (incomingOrder.currentVolume().leq(0) || currentState != CONTINUOUS_TRADING) {
            return null;
        }
        final List<OrderbookEvent> events = new ArrayList<>();
        List<OrderbookEvent> matchingEvents;
        Order updatedMatchingOrder = incomingOrder;
        do {
            matchingEvents = matchingAlgorithm.matchOrder(updatedMatchingOrder);
            for (var message : matchingEvents) {
                events.add(message);
                if (message instanceof Order order) {
                    updateOrderbook(order);
                    if (order.orderId().equals(updatedMatchingOrder.orderId())) {
                        updatedMatchingOrder = order;
                    }
                }
            }
        } while (!matchingEvents.isEmpty() && updatedMatchingOrder.orderOperation() != OrderOperationEnum.CANCEL);

        return ImmutableTradeExecution.builder()
                .timeOfEvent(Timestamp.now())
                .messages(events)
                .orderbookId(getOrderbookId())
                .eventType(SYSTEM)
                .build();
    }

    @Override
    public TradeExecution runAuctionAlgorithm() {
        if (currentState != OPEN_AUCTION_RUN && currentState != CLOSING_AUCTION_RUN) {
            LOGGER.error("Attempted auction run triggered in {} event to current state {}. Required state is {}/{} ", getOrderbookId(), currentState, OPEN_AUCTION_RUN, CLOSING_AUCTION_RUN);
            return null;
        }

        var equilibriumPrice = auctionAlgorithm.calculateEquilibriumPrice();
        if (equilibriumPrice == null) {
            return null;
        }
        final List<OrderbookEvent> events = new ArrayList<>();
        List<OrderbookEvent> matchingEvents;
        do {
            matchingEvents = matchingAlgorithm.matchAtPrice(equilibriumPrice.optimalPrice().equilibriumPrice());
            for (var message : matchingEvents) {
                events.add(message);
                if (message instanceof Order order) {
                    updateOrderbook(order);
                }
            }
        } while (!matchingEvents.isEmpty());

        return ImmutableTradeExecution.builder()
                .timeOfEvent(Timestamp.now())
                .messages(events)
                .orderbookId(getOrderbookId())
                .eventType(SYSTEM)
                .build();
    }

    @Override
    public Order getAskOrderIfPriceDoesNotMatch(Price preMatchAskPrice) {
        return getPreMatchBestOrder(preMatchAskPrice, getBestAskOrder());
    }

    @Override
    public Order getBidOrderIfPriceDoesNotMatch(Price preMatchBidPrice) {
        return getPreMatchBestOrder(preMatchBidPrice, getBestBidOrder());
    }

    private Order getPreMatchBestOrder(Price preMatchPrice, Optional<Order> postMatchBestOrder) {
        if (postMatchBestOrder.isPresent() && !postMatchBestOrder.get().price().equals(preMatchPrice)) {
            return postMatchBestOrder.get();
        }
        return null;
    }
}
