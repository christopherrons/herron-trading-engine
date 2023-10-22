package com.herron.exchange.tradingengine.server.matchingengine.orderbook.model;


import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;

import java.util.Comparator;
import java.util.TreeSet;

public class PriceLevel extends TreeSet<Order> {

    private final Price price;
    private Volume volume = Volume.create(0);

    public PriceLevel(Price price, Comparator<? super Order> comparator) {
        super(comparator);
        this.price = price;
    }

    @Override
    public boolean add(Order order) {
        volume = volume.add(order.currentVolume());
        return super.add(order);
    }

    public boolean remove(Order order) {
        if (super.remove(order)) {
            volume = volume.subtract(order.currentVolume());
            return true;
        }
        return false;
    }

    public Price getPrice() {
        return price;
    }

    public long nrOfOrdersAtPriceLevel() {
        return size();
    }

    public Volume volumeAtPriceLevel() {
        return volume;
    }

}
