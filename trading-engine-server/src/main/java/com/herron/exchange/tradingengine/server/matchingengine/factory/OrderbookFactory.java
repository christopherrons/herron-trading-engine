package com.herron.exchange.tradingengine.server.matchingengine.factory;

import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.enums.AuctionAlgorithmEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.AuctionAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms.DutchAuctionAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.FifoOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.comparator.ProRataOrderBookComparator;
import com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms.FifoMatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms.ProRataMatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ActiveOrders;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.OrderbookImpl;

public class OrderbookFactory {

    public static Orderbook createOrderbook(OrderbookData orderbookData) {
        return switch (orderbookData.matchingAlgorithm()) {
            case FIFO -> {
                var activeOrders = new ActiveOrders(new FifoOrderBookComparator());
                var matchingAlgorithm = new FifoMatchingAlgorithm(activeOrders);
                var auctionAlgorithm = createAuctionAlgorithm(orderbookData.auctionAlgorithm(), activeOrders);
                if (auctionAlgorithm == null) {
                    yield null;
                }
                yield new OrderbookImpl(orderbookData, activeOrders, matchingAlgorithm, auctionAlgorithm);
            }
            case PRO_RATA -> {
                var activeOrders = new ActiveOrders(new ProRataOrderBookComparator());
                var matchingAlgorithm = new ProRataMatchingAlgorithm(activeOrders, orderbookData.minTradeVolume());
                var auctionAlgorithm = createAuctionAlgorithm(orderbookData.auctionAlgorithm(), activeOrders);
                if (auctionAlgorithm == null) {
                    yield null;
                }
                yield new OrderbookImpl(orderbookData, activeOrders, matchingAlgorithm, auctionAlgorithm);
            }
        };
    }

    private static AuctionAlgorithm createAuctionAlgorithm(AuctionAlgorithmEnum auctionAlgorithmEnum, ActiveOrders activeOrders) {
        return switch (auctionAlgorithmEnum) {
            case DUTCH -> new DutchAuctionAlgorithm(activeOrders);
            default -> null;
        };
    }
}
