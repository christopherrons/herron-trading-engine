package com.herron.exchange.tradingengine.server.matchingengine.orderbook.model;

import com.herron.exchange.common.api.common.api.Order;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Predicate;

public class PriceLevel extends TreeSet<Order> {

    private final double price;

    public PriceLevel(double price, Comparator<? super Order> comparator) {
        super(comparator);
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public long nrOfOrdersAtPriceLevel() {
        return size();
    }

    public double volumeAtPriceLevel() {
        return stream().mapToDouble(Order::currentVolume).sum(); //TODO: Fix that this is updated at each insert / cencel
    }

    public double volumeAtPriceLevel(Predicate<Order> filter) {
        return stream()
                .filter(filter)
                .mapToDouble(Order::currentVolume)
                .sum();
    }

}
