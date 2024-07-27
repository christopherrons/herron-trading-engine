package com.herron.exchange.tradingengine.server.matchingengine.utils;


import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.messages.common.Participant;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.ImmutableLimitOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableMarketOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableTrade;
import com.herron.exchange.common.api.common.messages.trading.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.*;
import static com.herron.exchange.common.api.common.enums.OrderTypeEnum.MARKET;
import static com.herron.exchange.common.api.common.enums.TradeType.AUTOMATCH;

public class MatchingEngineUtils {
    private static final AtomicLong CURRENT_TRADE_ID = new AtomicLong(1);

    public static Trade buildTrade(Order bidOrder,
                                   Order askOrder,
                                   Volume tradeVolume) {
        boolean isBidSideAggressor;
        if (bidOrder.orderType() == MARKET) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType() == MARKET) {
            isBidSideAggressor = false;
        } else {
            isBidSideAggressor = bidOrder.timeOfEvent().isAfterOrAt(askOrder.timeOfEvent());
        }
        return buildTrade(bidOrder, askOrder, isBidSideAggressor ? askOrder.price() : bidOrder.price(), tradeVolume);
    }

    public static Trade buildTrade(Order bidOrder,
                                   Order askOrder,
                                   Price price,
                                   Volume tradeVolume) {
        boolean isBidSideAggressor;
        if (bidOrder.orderType() == MARKET) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType() == MARKET) {
            isBidSideAggressor = false;
        } else {
            isBidSideAggressor = bidOrder.timeOfEvent().isAfterOrAt(askOrder.timeOfEvent());
        }
        return ImmutableTrade.builder()
                .bidParticipant(bidOrder.participant())
                .askParticipant(askOrder.participant())
                .tradeId(String.valueOf(CURRENT_TRADE_ID.getAndIncrement()))
                .bidOrderId(bidOrder.orderId())
                .askOrderId(askOrder.orderId())
                .isBidSideAggressor(isBidSideAggressor)
                .volume(tradeVolume)
                .price(price)
                .timeOfEvent(Timestamp.now())
                .instrumentId(bidOrder.instrumentId())
                .orderbookId(bidOrder.orderbookId())
                .eventType(SYSTEM)
                .tradeType(AUTOMATCH)
                .build();
    }

    public static Order buildUpdateOrder(Order order, Volume tradeVolume, OrderOperationCauseEnum orderOperationCauseEnum) {
        if (order.orderType() == MARKET) {
            return ImmutableMarketOrder.builder()
                    .from(order)
                    .orderOperationCause(orderOperationCauseEnum)
                    .currentVolume(order.currentVolume().subtract(tradeVolume))
                    .orderOperation(OrderOperationEnum.UPDATE)
                    .eventType(SYSTEM)
                    .build();
        }
        return ImmutableLimitOrder.builder()
                .from(order)
                .orderOperationCause(orderOperationCauseEnum)
                .currentVolume(order.currentVolume().subtract(tradeVolume))
                .orderOperation(OrderOperationEnum.UPDATE)
                .eventType(SYSTEM)
                .build();
    }

    public static Order buildCancelOrder(Order order, OrderOperationCauseEnum orderOperationCauseEnum) {
        if (order.orderType() == MARKET) {
            return ImmutableMarketOrder.builder()
                    .from(order)
                    .orderOperationCause(orderOperationCauseEnum)
                    .currentVolume(order.currentVolume())
                    .orderOperation(OrderOperationEnum.CANCEL)
                    .eventType(SYSTEM)
                    .build();
        }
        return ImmutableLimitOrder.builder()
                .from(order)
                .orderOperationCause(orderOperationCauseEnum)
                .currentVolume(order.currentVolume())
                .orderOperation(OrderOperationEnum.CANCEL)
                .eventType(SYSTEM)
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
            matchingMessages.add(buildCancelOrder(thisOrder, FILLED));
        }

        if (isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thatOrder, FILLED));
        }

        if (!isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thisOrder, tradeVolume, PARTIAL_FILL));
        }

        if (!isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thatOrder, tradeVolume, PARTIAL_FILL));
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
            matchingMessages.add(buildCancelOrder(thisOrder, FILLED));
        }

        if (isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thatOrder, FILLED));
        }

        if (!isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thisOrder, tradeVolume, PARTIAL_FILL));
        }

        if (!isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thatOrder, tradeVolume, PARTIAL_FILL));
        }

        final Trade trade;
        if (thisOrder.orderSide() == OrderSideEnum.BID) {
            trade = buildTrade(thisOrder, thatOrder, price, tradeVolume);
        } else {
            trade = buildTrade(thatOrder, thisOrder, price, tradeVolume);
        }
        matchingMessages.add(trade);

        return matchingMessages;
    }

    public static List<OrderbookEvent> createMatchingMessagesSelfMatched(Order thisOrder, Order thatOrder, Volume tradeVolume) {

        List<OrderbookEvent> matchingMessages = new ArrayList<>();

        if (isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thisOrder, SELF_MATCH));
        }

        if (isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildCancelOrder(thatOrder, SELF_MATCH));
        }

        if (!isFilled(thisOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thisOrder, tradeVolume, SELF_MATCH));
        }

        if (!isFilled(thatOrder, tradeVolume)) {
            matchingMessages.add(buildUpdateOrder(thatOrder, tradeVolume, SELF_MATCH));
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