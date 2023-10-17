package com.herron.exchange.tradingengine.server.matchingengine.utils;


import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderUpdatedOperationTypeEnum;
import com.herron.exchange.common.api.common.messages.common.Participant;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultCancelOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultTrade;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultUpdateOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngineUtils {
    private static final AtomicLong CURRENT_TRADE_ID = new AtomicLong(1);

    public static Trade buildTrade(Order bidOrder,
                                   Order askOrder,
                                   Volume tradeVolume) {
        boolean isBidSideAggressor;
        if (bidOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = false;
        } else {
            isBidSideAggressor = bidOrder.timeOfEventMs() >= askOrder.timeOfEventMs();
        }
        return ImmutableDefaultTrade.builder()
                .bidParticipant(bidOrder.participant())
                .askParticipant(askOrder.participant())
                .tradeId(String.valueOf(CURRENT_TRADE_ID.getAndIncrement()))
                .bidOrderId(bidOrder.orderId())
                .askOrderId(askOrder.orderId())
                .isBidSideAggressor(isBidSideAggressor)
                .volume(tradeVolume)
                .price(isBidSideAggressor ? askOrder.price() : bidOrder.price())
                .timeOfEventMs(Instant.now().toEpochMilli())
                .instrumentId(bidOrder.instrumentId())
                .orderbookId(bidOrder.orderbookId())
                .build();
    }

    public static Trade buildAuctionTrade(Order bidOrder,
                                          Order askOrder,
                                          Price price,
                                          Volume tradeVolume) {
        boolean isBidSideAggressor;
        if (bidOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = false;
        } else {
            isBidSideAggressor = bidOrder.timeOfEventMs() >= askOrder.timeOfEventMs();
        }
        return ImmutableDefaultTrade.builder()
                .bidParticipant(bidOrder.participant())
                .askParticipant(askOrder.participant())
                .tradeId(String.valueOf(CURRENT_TRADE_ID.getAndIncrement()))
                .bidOrderId(bidOrder.orderId())
                .askOrderId(askOrder.orderId())
                .isBidSideAggressor(isBidSideAggressor)
                .volume(tradeVolume)
                .price(price)
                .timeOfEventMs(Instant.now().toEpochMilli())
                .instrumentId(bidOrder.instrumentId())
                .orderbookId(bidOrder.orderbookId())
                .build();
    }

    public static Order buildUpdateOrder(Order order, Volume tradeVolume, OrderUpdatedOperationTypeEnum orderUpdatedOperationTypeEnum) {
        return ImmutableDefaultUpdateOrder.builder()
                .from(order)
                .updateOperationType(orderUpdatedOperationTypeEnum)
                .currentVolume(order.currentVolume().subtract(tradeVolume))
                .build();
    }

    public static Order buildCancelOrder(Order order, OrderCancelOperationTypeEnum orderCancelOperationTypeEnum) {
        return ImmutableDefaultCancelOrder.builder()
                .from(order)
                .cancelOperationType(orderCancelOperationTypeEnum)
                .currentVolume(orderCancelOperationTypeEnum == OrderCancelOperationTypeEnum.FILLED ? Volume.ZERO : order.currentVolume())
                .build();
    }

    public static List<OrderbookEvent> createAuctionMatchingMessages(Order thisOrder, Order thatOrder, Price price) {
        final Volume tradeVolume = thisOrder.currentVolume().min(thatOrder.currentVolume());
        return createAuctionMatchingMessages(thisOrder, thatOrder, price, tradeVolume);
    }

    public static List<OrderbookEvent> createMatchingMessages(Order thisOrder, Order thatOrder) {
        final Volume tradeVolume = thisOrder.currentVolume().min(thatOrder.currentVolume());
        return createMatchingMessages(thisOrder, thatOrder, tradeVolume);
    }

    public static List<OrderbookEvent> createMatchingMessages(Order thisOrder, Order thatOrder, Volume tradeVolume) {
        if (isSelfMatch(thisOrder.participant(), thatOrder.participant())) {
            return createMatchingMessagesSelfMatched(thisOrder, thatOrder, tradeVolume);
        }

        final List<OrderbookEvent> matchingMessages = new ArrayList<>();

        if (isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thisOrder, OrderCancelOperationTypeEnum.FILLED));
        }

        if (isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thatOrder, OrderCancelOperationTypeEnum.FILLED));
        }

        if (!isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thisOrder, tradeVolume, OrderUpdatedOperationTypeEnum.PARTIAL_FILL));
        }

        if (!isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thatOrder, tradeVolume, OrderUpdatedOperationTypeEnum.PARTIAL_FILL));
        }

        final Trade trade;
        if (thisOrder.orderSide() == OrderSideEnum.BID) {
            trade = buildTrade(thisOrder, thatOrder, tradeVolume);
        } else {
            trade = buildTrade(thatOrder, thisOrder, tradeVolume);
        }
        matchingMessages.add(trade);

        return matchingMessages;
    }

    public static List<OrderbookEvent> createAuctionMatchingMessages(Order thisOrder, Order thatOrder, Price price, Volume tradeVolume) {
        if (isSelfMatch(thisOrder.participant(), thatOrder.participant())) {
            return createMatchingMessagesSelfMatched(thisOrder, thatOrder, tradeVolume);
        }

        final List<OrderbookEvent> matchingMessages = new ArrayList<>();

        if (isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thisOrder, OrderCancelOperationTypeEnum.FILLED));
        }

        if (isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thatOrder, OrderCancelOperationTypeEnum.FILLED));
        }

        if (!isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thisOrder, tradeVolume, OrderUpdatedOperationTypeEnum.PARTIAL_FILL));
        }

        if (!isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thatOrder, tradeVolume, OrderUpdatedOperationTypeEnum.PARTIAL_FILL));
        }

        final Trade trade;
        if (thisOrder.orderSide() == OrderSideEnum.BID) {
            trade = buildAuctionTrade(thisOrder, thatOrder, price, tradeVolume);
        } else {
            trade = buildAuctionTrade(thatOrder, thisOrder, price, tradeVolume);
        }
        matchingMessages.add(trade);

        return matchingMessages;
    }

    public static List<OrderbookEvent> createMatchingMessagesSelfMatched(Order thisOrder, Order thatOrder, Volume tradeVolume) {

        List<OrderbookEvent> matchingMessages = new ArrayList<>();

        if (isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thisOrder, OrderCancelOperationTypeEnum.SELF_MATCH));
        }

        if (isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thatOrder, OrderCancelOperationTypeEnum.SELF_MATCH));
        }

        if (!isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thisOrder, tradeVolume, OrderUpdatedOperationTypeEnum.SELF_MATCH));
        }

        if (!isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thatOrder, tradeVolume, OrderUpdatedOperationTypeEnum.SELF_MATCH));
        }

        return matchingMessages;
    }

    private static boolean isFilled(Order order, Volume tradeVolume) {
        return order.currentVolume().subtract(tradeVolume).leq(0);
    }

    private static boolean isSelfMatch(Participant bidParticipant, Participant askParticipant) {
        return bidParticipant.equals(askParticipant);
    }
}