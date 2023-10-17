package com.herron.exchange.tradingengine.server.matchingengine.utils;


import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.common.*;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultAddOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultCancelOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultTrade;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultUpdateOrder;

import java.time.Instant;

public class MessageCreatorTestUtils {
    private static final Participant PARTICIPANT = new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString()));

    public static Order buildOrderUpdate(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId) {
        return ImmutableDefaultUpdateOrder.builder()
                .participant(PARTICIPANT)
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .orderExecutionType(OrderExecutionTypeEnum.FILL)
                .orderType(OrderTypeEnum.LIMIT)
                .updateOperationType(OrderUpdatedOperationTypeEnum.EXTERNAL_UPDATE)
                .build();
    }

    public static Order buildOrderDelete(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId) {
        return ImmutableDefaultCancelOrder.builder()
                .participant(PARTICIPANT)
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .orderExecutionType(OrderExecutionTypeEnum.FILL)
                .orderType(OrderTypeEnum.LIMIT)
                .cancelOperationType(OrderCancelOperationTypeEnum.FILLED)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId,
                                      Participant participant) {
        return ImmutableDefaultAddOrder.builder()
                .participant(participant)
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .orderExecutionType(OrderExecutionTypeEnum.FILL)
                .orderType(OrderTypeEnum.LIMIT)
                .addOperationType(OrderAddOperationTypeEnum.NEW_ORDER)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId,
                                      OrderExecutionTypeEnum orderExecutionTypeEnum,
                                      OrderTypeEnum orderTypeEnum) {
        return ImmutableDefaultAddOrder.builder()
                .participant(new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())))
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .orderExecutionType(orderExecutionTypeEnum)
                .orderType(OrderTypeEnum.LIMIT)
                .addOperationType(OrderAddOperationTypeEnum.NEW_ORDER)
                .orderType(orderTypeEnum)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId) {
        return ImmutableDefaultAddOrder.builder()
                .participant(new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())))
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .orderExecutionType(OrderExecutionTypeEnum.FILL)
                .orderType(OrderTypeEnum.LIMIT)
                .addOperationType(OrderAddOperationTypeEnum.NEW_ORDER)
                .build();
    }

    public static Trade buildTrade(long timeStampInMs,
                                   double price,
                                   double volume,
                                   boolean isBidSideAggressor,
                                   String tradeId,
                                   Participant bidParticipant,
                                   Participant askParticipant) {
        return ImmutableDefaultTrade.builder()
                .bidParticipant(bidParticipant)
                .askParticipant(askParticipant)
                .tradeId(tradeId)
                .bidOrderId(tradeId)
                .askOrderId(tradeId)
                .isBidSideAggressor(isBidSideAggressor)
                .volume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(Instant.now().toEpochMilli())
                .instrumentId("instrumentId")
                .orderbookId("orderbookId")
                .build();
    }
}
