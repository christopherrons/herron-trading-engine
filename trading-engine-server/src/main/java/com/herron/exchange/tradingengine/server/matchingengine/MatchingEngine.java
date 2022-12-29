package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.Instrument;
import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.OrderbookData;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;

public class MatchingEngine {

    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();

    public void handleMessage(Message message) {
        if (message instanceof Order order) {
            handleOrder(order);
        } else if (message instanceof OrderbookData orderbookData) {
            handleOrderbookData(orderbookData);
        } else if (message instanceof Instrument instrument) {
            handleInstrument(instrument);
        }
    }

    private void handleOrderbookData(OrderbookData orderbookData) {
        referanceDataCache.addOrderbookData(orderbookData);
    }

    private void handleInstrument(Instrument instrument) {
        referanceDataCache.addInstrument(instrument);
    }

    private void handleOrder(Order order) {
        final OrderbookData orderbookData = referanceDataCache.getOrderbookData(order.orderbookId());
        final Orderbook orderbook = orderbookCache.findOrCreate(orderbookData);
    }
}
