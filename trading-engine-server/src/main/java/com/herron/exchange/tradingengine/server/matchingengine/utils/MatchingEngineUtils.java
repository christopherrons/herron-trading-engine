package com.herron.exchange.tradingengine.server.matchingengine.utils;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.HerronCancelOrder;
import com.herron.exchange.common.api.common.messages.HerronTrade;
import com.herron.exchange.common.api.common.messages.HerronUpdateOrder;
import com.herron.exchange.common.api.common.model.MonetaryAmount;
import com.herron.exchange.common.api.common.model.Participant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngineUtils {
    private static final AtomicLong CURRENT_TRADE_ID = new AtomicLong(1);

    public static Trade buildTrade(Order bidOrder,
                                   Order askOrder,
                                   double tradeVolume) {
        boolean isBidSideAggressor;
        if (bidOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = false;
        } else {
            isBidSideAggressor = bidOrder.timeStampInMs() >= askOrder.timeStampInMs();
        }
        return new HerronTrade(bidOrder.participant(),
                askOrder.participant(),
                String.valueOf(CURRENT_TRADE_ID.getAndIncrement()),
                bidOrder.orderId(),
                askOrder.orderId(),
                isBidSideAggressor,
                tradeVolume,
                isBidSideAggressor ? askOrder.monetaryAmount() : bidOrder.monetaryAmount(),
                Instant.now().toEpochMilli(),
                bidOrder.instrumentId(),
                bidOrder.orderbookId()
        );
    }

    public static Trade buildAuctionTrade(Order bidOrder,
                                          Order askOrder,
                                          double price,
                                          double tradeVolume) {
        boolean isBidSideAggressor;
        if (bidOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType() == OrderTypeEnum.MARKET) {
            isBidSideAggressor = false;
        } else {
            isBidSideAggressor = bidOrder.timeStampInMs() >= askOrder.timeStampInMs();
        }
        return new HerronTrade(bidOrder.participant(),
                askOrder.participant(),
                String.valueOf(CURRENT_TRADE_ID.getAndIncrement()),
                bidOrder.orderId(),
                askOrder.orderId(),
                isBidSideAggressor,
                tradeVolume,
                new MonetaryAmount(price, bidOrder.monetaryAmount().currency()),
                Instant.now().toEpochMilli(),
                bidOrder.instrumentId(),
                bidOrder.orderbookId()
        );
    }

    public static Order buildUpdateOrder(Order order, double tradeVolume, OrderUpdatedOperationTypeEnum orderUpdatedOperationTypeEnum) {
        return new HerronUpdateOrder(OrderOperationEnum.UPDATE,
                order.participant(),
                order.orderId(),
                order.orderSide(),
                order.initialVolume(),
                order.currentVolume() - tradeVolume,
                order.monetaryAmount(),
                order.timeStampInMs(),
                order.instrumentId(),
                order.orderbookId(),
                order.orderExecutionType(),
                order.orderType(),
                orderUpdatedOperationTypeEnum);
    }

    public static Order buildCancelOrder(Order order, OrderCancelOperationTypeEnum orderCancelOperationTypeEnum) {
        return new HerronCancelOrder(OrderOperationEnum.DELETE,
                order.participant(),
                order.orderId(),
                order.orderSide(),
                order.initialVolume(),
                orderCancelOperationTypeEnum == OrderCancelOperationTypeEnum.FILLED ? 0 : order.currentVolume(),
                order.monetaryAmount(),
                order.timeStampInMs(),
                order.instrumentId(),
                order.orderbookId(),
                order.orderExecutionType(),
                order.orderType(),
                orderCancelOperationTypeEnum);
    }

    public static List<Message> createAuctionMatchingMessages(Order thisOrder, Order thatOrder, double price) {
        final double tradeVolume = Math.min(thisOrder.currentVolume(), thatOrder.currentVolume());
        return createAuctionMatchingMessages(thisOrder, thatOrder, price, tradeVolume);
    }

    public static List<Message> createMatchingMessages(Order thisOrder, Order thatOrder) {
        final double tradeVolume = Math.min(thisOrder.currentVolume(), thatOrder.currentVolume());
        return createMatchingMessages(thisOrder, thatOrder, tradeVolume);
    }

    public static List<Message> createMatchingMessages(Order thisOrder, Order thatOrder, double tradeVolume) {
        if (isSelfMatch(thisOrder.participant(), thatOrder.participant())) {
            return createMatchingMessagesSelfMatched(thisOrder, thatOrder, tradeVolume);
        }

        final List<Message> matchingMessages = new ArrayList<>();

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

    public static List<Message> createAuctionMatchingMessages(Order thisOrder, Order thatOrder, double price, double tradeVolume) {
        if (isSelfMatch(thisOrder.participant(), thatOrder.participant())) {
            return createMatchingMessagesSelfMatched(thisOrder, thatOrder, tradeVolume);
        }

        final List<Message> matchingMessages = new ArrayList<>();

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

    public static List<Message> createMatchingMessagesSelfMatched(Order thisOrder, Order thatOrder, double tradeVolume) {

        List<Message> matchingMessages = new ArrayList<>();

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

    private static boolean isFilled(Order order, double tradeVolume) {
        return order.currentVolume() - tradeVolume <= 0;
    }

    private static boolean isSelfMatch(Participant bidParticipant, Participant askParticipant) {
        return bidParticipant.equals(askParticipant);
    }
}