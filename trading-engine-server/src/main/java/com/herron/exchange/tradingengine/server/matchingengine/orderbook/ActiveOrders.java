package com.herron.exchange.tradingengine.server.matchingengine.orderbook;

import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.model.PriceLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveOrders implements ActiveOrderReadOnly {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveOrders.class);
    private final Map<String, Order> orderIdToOrder = new ConcurrentHashMap<>();
    private final TreeMap<Price, PriceLevel> bidPriceToPriceLevel = new TreeMap<>(Comparator.comparing(Price::getValue).reversed());
    private final TreeMap<Price, PriceLevel> askPriceToPriceLevel = new TreeMap<>(Comparator.comparing(Price::getValue));
    private final Comparator<? super Order> comparator;

    public ActiveOrders(Comparator<? super Order> comparator) {
        this.comparator = comparator;
    }

    public boolean updateOrder(Order order) {
        return removeOrder(order) && addOrder(order);
    }

    public boolean addOrder(Order order) {
        PriceLevel priceLevel = findOrCreatePriceLevel(order);
        if (priceLevel == null) {
            return false;
        }

        orderIdToOrder.put(order.orderId(), order);
        return priceLevel.add(order);
    }

    public boolean removeOrder(Order order) {
        return removeOrder(order.orderId());
    }

    public boolean removeOrder(String orderId) {
        if (!orderIdToOrder.containsKey(orderId)) {
            LOGGER.error("Cannot remove order id {}, order does not exist.", orderId);
            return false;
        }

        Order order = orderIdToOrder.remove(orderId);
        PriceLevel priceLevel = findOrCreatePriceLevel(order);

        if (priceLevel != null && priceLevel.remove(order)) {
            if (priceLevel.isEmpty()) {
                return removePriceLevel(order);
            }
        } else {
            LOGGER.error("Cannot remove order {}, price level does not exist.", order);
            return false;
        }

        return true;
    }

    private PriceLevel findOrCreatePriceLevel(Order order) {
        return switch (order.orderSide()) {
            case BID -> bidPriceToPriceLevel.computeIfAbsent(order.price(), key -> new PriceLevel(order.price(), comparator));
            case ASK -> askPriceToPriceLevel.computeIfAbsent(order.price(), key -> new PriceLevel(order.price(), comparator));
            case INVALID_ORDER_SIDE -> {
                LOGGER.error("Could not create or find price level, invalid order side for order {}.", order);
                yield null;
            }
        };
    }

    private boolean removePriceLevel(Order order) {
        switch (order.orderSide()) {
            case BID -> bidPriceToPriceLevel.remove(order.price());
            case ASK -> askPriceToPriceLevel.remove(order.price());
            case INVALID_ORDER_SIDE -> {
                LOGGER.error("Could not remove price level, invalid order side for order {}.", order);
                return false;
            }
        }
        return true;
    }

    public int totalNumberOfPriceLevels() {
        return totalNumberOfBidPriceLevels() + totalNumberOfAskPriceLevels();
    }

    public int totalNumberOfBidPriceLevels() {
        return bidPriceToPriceLevel.values().size();
    }

    public int totalNumberOfAskPriceLevels() {
        return askPriceToPriceLevel.values().size();
    }

    public Order getOrder(String orderId) {
        return orderIdToOrder.get(orderId);
    }

    public Price getBestBidPrice() {
        return getBestBidOrder().map(Order::price).orElse(Price.ZERO);
    }

    public Price getBestAskPrice() {
        return getBestAskOrder().map(Order::price).orElse(Price.ZERO);
    }

    public Optional<Order> getBestBidOrder() {
        return getBestOrder(OrderSideEnum.BID);
    }

    public Optional<Order> getBestAskOrder() {
        return getBestOrder(OrderSideEnum.ASK);
    }

    private Optional<Order> getBestOrder(OrderSideEnum orderSide) {
        return switch (orderSide) {
            case BID -> bidPriceToPriceLevel.values().stream().findFirst().map(PriceLevel::first);
            case ASK -> askPriceToPriceLevel.values().stream().findFirst().map(PriceLevel::first);
            case INVALID_ORDER_SIDE -> Optional.empty();
        };
    }

    public long totalNumberOfBidOrders() {
        return totalNumberOfSideOrders(OrderSideEnum.BID.getValue());
    }

    public long totalNumberOfAskOrders() {
        return totalNumberOfSideOrders(OrderSideEnum.ASK.getValue());
    }

    private long totalNumberOfSideOrders(int orderSide) {
        return switch (OrderSideEnum.fromValue(orderSide)) {
            case BID -> bidPriceToPriceLevel.values().stream().mapToLong(PriceLevel::nrOfOrdersAtPriceLevel).sum();
            case ASK -> askPriceToPriceLevel.values().stream().mapToLong(PriceLevel::nrOfOrdersAtPriceLevel).sum();
            case INVALID_ORDER_SIDE -> 0;
        };
    }

    public long totalNumberOfActiveOrders() {
        return orderIdToOrder.size();
    }

    public Volume totalOrderVolume() {
        return totalBidVolume().add(totalAskVolume());
    }

    public Volume totalBidVolume() {
        return bidPriceToPriceLevel.values().stream().map(PriceLevel::volumeAtPriceLevel).reduce(Volume.ZERO, Volume::add);
    }

    public Volume totalAskVolume() {
        return askPriceToPriceLevel.values().stream().map(PriceLevel::volumeAtPriceLevel).reduce(Volume.ZERO, Volume::add);
    }

    public Volume totalVolumeAtPriceLevel(int priceLevel) {
        return totalBidVolumeAtPriceLevel(priceLevel).add(totalAskVolumeAtPriceLevel(priceLevel));
    }

    public Volume totalBidVolumeAtPriceLevel(int priceLevel) {
        return bidPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst()
                .map(PriceLevel::volumeAtPriceLevel)
                .orElse(Volume.ZERO);
    }

    public Volume totalAskVolumeAtPriceLevel(int priceLevel) {
        return askPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst()
                .map(PriceLevel::volumeAtPriceLevel)
                .orElse(Volume.ZERO);
    }

    public Price getAskPriceAtPriceLevel(int priceLevel) {
        return askPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst()
                .map(PriceLevel::getPrice)
                .orElse(Price.ZERO);
    }

    public Price getBidPriceAtPriceLevel(int priceLevel) {
        return bidPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst()
                .map(PriceLevel::getPrice)
                .orElse(Price.ZERO);
    }

    public boolean hasBidAndAskOrders() {
        return !bidPriceToPriceLevel.isEmpty() && !askPriceToPriceLevel.isEmpty();
    }

    public boolean isTotalFillPossible(Order order) {
        return switch (order.orderSide()) {
            case BID -> isTotalBidFillPossible(order);
            case ASK -> isTotalAskFillPossible(order);
            default -> false;
        };
    }

    @Override
    public Optional<PriceLevel> getBestBidPriceLevel() {
        return getBestPriceLevel(OrderSideEnum.BID);
    }

    @Override
    public Optional<PriceLevel> getBestAskPriceLevel() {
        return getBestPriceLevel(OrderSideEnum.ASK);
    }

    private Optional<PriceLevel> getBestPriceLevel(OrderSideEnum orderSide) {
        return switch (orderSide) {
            case BID -> bidPriceToPriceLevel.values().stream().findFirst();
            case ASK -> askPriceToPriceLevel.values().stream().findFirst();
            case INVALID_ORDER_SIDE -> Optional.empty();
        };
    }

    private boolean isTotalAskFillPossible(Order order) {
        Volume availableVolume = Volume.ZERO;
        for (var level : bidPriceToPriceLevel.values()) {
            if (order.orderType() == OrderTypeEnum.MARKET || order.price().leq(level.getPrice())) {
                availableVolume = availableVolume.add(level.volumeAtPriceLevel());
            } else {
                return false;
            }

            if (order.currentVolume().leq(availableVolume)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTotalBidFillPossible(Order order) {
        Volume availableVolume = Volume.ZERO;
        for (var level : askPriceToPriceLevel.values()) {
            if (order.orderType() == OrderTypeEnum.MARKET || order.price().geq(level.getPrice())) {
                availableVolume = availableVolume.add(level.volumeAtPriceLevel());
            } else {
                return false;
            }

            if (order.currentVolume().leq(availableVolume)) {
                return true;
            }
        }
        return false;
    }

    public List<PriceLevel> getAskPriceLevelsLowerOrEqual(Price bidPrice) {
        List<PriceLevel> matchingPriceLevels = new ArrayList<>();
        for (var priceLevel : askPriceToPriceLevel.values()) {
            if (priceLevel.getPrice().gt(bidPrice)) {
                return matchingPriceLevels;
            }
            matchingPriceLevels.add(priceLevel);
        }
        return matchingPriceLevels;
    }

    public List<PriceLevel> getBidPriceLevelsHigherOrEqual(Price askPrice) {
        List<PriceLevel> matchingPriceLevels = new ArrayList<>();
        for (var priceLevel : bidPriceToPriceLevel.values()) {
            if (priceLevel.getPrice().lt(askPrice)) {
                return matchingPriceLevels;
            }
            matchingPriceLevels.add(priceLevel);
        }
        return matchingPriceLevels;
    }
}
