package com.herron.exchange.tradingengine.server.matchingengine.utils;

import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.messages.herron.HerronOrder;
import com.herron.exchange.common.api.common.messages.herron.HerronTrade;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngineUtils {
    private static final AtomicLong CURRENT_TRADE_ID = new AtomicLong(1);

    public static Trade buildTrade(Order bidOrder,
                                   Order askOrder,
                                   double tradeVolume) {
        boolean isBidSideAggressor = bidOrder.timeStampInMs() >= askOrder.timeStampInMs();
        return new HerronTrade(bidOrder.participant(),
                askOrder.participant(),
                String.valueOf(CURRENT_TRADE_ID.getAndIncrement()),
                bidOrder.orderId(),
                askOrder.orderId(),
                isBidSideAggressor,
                tradeVolume,
                isBidSideAggressor ? askOrder.price() : bidOrder.price(),
                Instant.now().toEpochMilli(),
                bidOrder.instrumentId(),
                bidOrder.orderbookId()
        );
    }

    public static Order buildUpdateOrder(Order order, double tradeVolume) {
        return new HerronOrder(OrderOperationEnum.UPDATE,
                order.participant(),
                order.orderId(),
                order.orderType(),
                order.initialVolume(),
                order.currentVolume() - tradeVolume,
                order.price(),
                order.timeStampInMs(),
                order.instrumentId(),
                order.orderbookId());
    }

    public static Order buildCancelOrder(Order order) {
        return new HerronOrder(OrderOperationEnum.DELETE,
                order.participant(),
                order.orderId(),
                order.orderType(),
                order.initialVolume(),
                order.currentVolume(),
                order.price(),
                order.timeStampInMs(),
                order.instrumentId(),
                order.orderbookId());
    }
}