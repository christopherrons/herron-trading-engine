package com.herron.exchange.tradingengine.server.matchingengine.model;

import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


public class ActiveOrders {
    private final Map<String, Order> orderIdToOrder = new ConcurrentHashMap<>();
    private final TreeMap<Double, PriceLevel> bidPriceToPriceLevel = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Double, PriceLevel> askPriceToPriceLevel = new TreeMap<>();

    private final Comparator<? super Order> comparator;

    public ActiveOrders(Comparator<? super Order> comparator) {
        this.comparator = comparator;
    }

    public void updateOrder(Order order) {
        removeOrder(order.orderId());
        addOrder(order);
    }

    public void addOrder(Order order) {
        PriceLevel priceLevel = findOrCreatePriceLevel(order);
        priceLevel.add(order);
        orderIdToOrder.putIfAbsent(order.orderId(), order);
    }

    public void removeOrder(Order order) {
        removeOrder(order.orderId());
    }

    public void removeOrder(String orderId) {
        Order order = orderIdToOrder.remove(orderId);
        PriceLevel priceLevel = findOrCreatePriceLevel(order);
        priceLevel.remove(order);
        if (priceLevel.isEmpty()) {
            removePriceLevel(order);
        }
    }

    private PriceLevel findOrCreatePriceLevel(final Order order) {
        return switch (order.orderType()) {
            case BUY -> bidPriceToPriceLevel.computeIfAbsent(order.price(), key -> new PriceLevel(order.price(), comparator));
            case ASK -> askPriceToPriceLevel.computeIfAbsent(order.price(), key -> new PriceLevel(order.price(), comparator));
            case INVALID_ORDER_TYPE -> null;
        };
    }

    private void removePriceLevel(Order order) {
        switch (order.orderType()) {
            case BUY -> bidPriceToPriceLevel.remove(order.price());
            case ASK -> askPriceToPriceLevel.remove(order.price());
        }
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

    public double getBestBidPrice() {
        return getBestBidOrder().map(Order::price).orElse(0.0);
    }

    public double getBestAskPrice() {
        return getBestAskOrder().map(Order::price).orElse(0.0);
    }

    public Optional<Order> getBestBidOrder() {
        return getBestOrder(OrderTypeEnum.BUY);
    }

    public Optional<Order> getBestAskOrder() {
        return getBestOrder(OrderTypeEnum.ASK);
    }

    private Optional<Order> getBestOrder(OrderTypeEnum orderType) {
        return switch (orderType) {
            case BUY -> bidPriceToPriceLevel.values().stream().findFirst().map(PriceLevel::first);
            case ASK -> askPriceToPriceLevel.values().stream().findFirst().map(PriceLevel::first);
            case INVALID_ORDER_TYPE -> Optional.empty();
        };
    }

    public long totalNumberOfBidOrders() {
        return totalNumberOfSideOrders(OrderTypeEnum.BUY.getValue());
    }

    public long totalNumberOfAskOrders() {
        return totalNumberOfSideOrders(OrderTypeEnum.ASK.getValue());
    }

    private long totalNumberOfSideOrders(int orderType) {
        return switch (OrderTypeEnum.fromValue(orderType)) {
            case BUY -> bidPriceToPriceLevel.values().stream().mapToLong(PriceLevel::nrOfOrdersAtPriceLevel).sum();
            case ASK -> askPriceToPriceLevel.values().stream().mapToLong(PriceLevel::nrOfOrdersAtPriceLevel).sum();
            case INVALID_ORDER_TYPE -> 0;
        };
    }

    public long totalNumberOfActiveOrders() {
        return orderIdToOrder.size();
    }

    public double totalOrderVolume() {
        return totalBidVolume() + totalAskVolume();
    }

    public double totalBidVolume() {
        return bidPriceToPriceLevel.values().stream().mapToDouble(PriceLevel::volumeAtPriceLevel).sum();
    }

    public double totalAskVolume() {
        return askPriceToPriceLevel.values().stream().mapToDouble(PriceLevel::volumeAtPriceLevel).sum();
    }

    public double totalVolumeAtPriceLevel(int priceLevel) {
        return totalBidVolumeAtPriceLevel(priceLevel) + totalAskVolumeAtPriceLevel(priceLevel);
    }

    public double totalBidVolumeAtPriceLevel(int priceLevel) {
        return bidPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst().map(PriceLevel::volumeAtPriceLevel)
                .orElse(0.0);
    }

    public double totalAskVolumeAtPriceLevel(int priceLevel) {
        return askPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst().map(PriceLevel::volumeAtPriceLevel)
                .orElse(0.0);
    }

    public double getAskPriceAtPriceLevel(int priceLevel) {
        return askPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst().map(PriceLevel::getPrice)
                .orElse(0.0);
    }

    public double getBidPriceAtPriceLevel(int priceLevel) {
        return bidPriceToPriceLevel.values().stream()
                .skip(priceLevel - 1L)
                .findFirst().map(PriceLevel::getPrice)
                .orElse(0.0);
    }

    public boolean hasBidAndAskOrders() {
        return bidPriceToPriceLevel.size() != 0 && askPriceToPriceLevel.size() != 0;
    }


}
