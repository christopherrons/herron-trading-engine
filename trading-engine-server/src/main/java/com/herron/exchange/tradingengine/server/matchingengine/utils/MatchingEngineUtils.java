package com.herron.exchange.tradingengine.server.matchingengine.utils;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderUpdatedOperationTypeEnum;
import com.herron.exchange.common.api.common.messages.herron.HerronCancelOrder;
import com.herron.exchange.common.api.common.messages.herron.HerronTrade;
import com.herron.exchange.common.api.common.messages.herron.HerronUpdateOrder;
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
        if (bidOrder.orderType().equals(OrderTypeEnum.MARKET)) {
            isBidSideAggressor = true;
        } else if (askOrder.orderType().equals(OrderTypeEnum.MARKET)) {
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
                isBidSideAggressor ? askOrder.price() : bidOrder.price(),
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
                order.price(),
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
                order.currentVolume(),
                order.price(),
                order.timeStampInMs(),
                order.instrumentId(),
                order.orderbookId(),
                order.orderExecutionType(),
                order.orderType(),
                orderCancelOperationTypeEnum);
    }

    public static List<Message> createMatchingMessages(Order bidOrder, Order askOrder) {
        if (isSelfMatch(bidOrder.participant(), askOrder.participant())) {
            return createMatchingMessagesSelfMatched(bidOrder, askOrder);
        }

        final double tradeVolume = Math.min(bidOrder.currentVolume(), askOrder.currentVolume());

        final List<Message> matchingMessages = new ArrayList<>();

        if (bidOrder.currentVolume() - tradeVolume <= 0) {
            matchingMessages.add(buildCancelOrder(bidOrder, OrderCancelOperationTypeEnum.FILLED));
        } else {
            matchingMessages.add(buildUpdateOrder(bidOrder, tradeVolume, OrderUpdatedOperationTypeEnum.PARTIAL_FILL));
        }

        if (askOrder.currentVolume() - tradeVolume <= 0) {
            matchingMessages.add(buildCancelOrder(askOrder, OrderCancelOperationTypeEnum.FILLED));
        } else {
            matchingMessages.add(buildUpdateOrder(askOrder, tradeVolume, OrderUpdatedOperationTypeEnum.PARTIAL_FILL));
        }

        final Trade trade = buildTrade(bidOrder, askOrder, tradeVolume);
        matchingMessages.add(trade);

        return matchingMessages;
    }

    public static List<Message> createMatchingMessagesSelfMatched(Order bidOrder, Order askOrder) {
        final double tradeVolume = Math.min(bidOrder.currentVolume(), askOrder.currentVolume());

        List<Message> matchingMessages = new ArrayList<>();

        if (bidOrder.currentVolume() - tradeVolume <= 0) {
            matchingMessages.add(buildCancelOrder(bidOrder, OrderCancelOperationTypeEnum.SELF_MATCH));
        } else {
            matchingMessages.add(buildUpdateOrder(bidOrder, tradeVolume, OrderUpdatedOperationTypeEnum.SELF_MATCH));
        }

        if (askOrder.currentVolume() - tradeVolume <= 0) {
            matchingMessages.add(buildCancelOrder(askOrder, OrderCancelOperationTypeEnum.SELF_MATCH));
        } else {
            matchingMessages.add(buildUpdateOrder(askOrder, tradeVolume, OrderUpdatedOperationTypeEnum.SELF_MATCH));
        }

        return matchingMessages;
    }

    private static boolean isSelfMatch(Participant bidParticipant, Participant askParticipant) {
        return bidParticipant.equals(askParticipant);
    }
}