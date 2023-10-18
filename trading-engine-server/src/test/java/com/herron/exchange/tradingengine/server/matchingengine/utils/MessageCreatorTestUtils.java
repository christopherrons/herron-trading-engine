package com.herron.exchange.tradingengine.server.matchingengine.utils;


import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.enums.TimeInForceEnum;
import com.herron.exchange.common.api.common.messages.common.*;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultLimitOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultMarketOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableDefaultTrade;

import java.time.Instant;

import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.*;
import static com.herron.exchange.common.api.common.enums.OrderOperationEnum.*;
import static com.herron.exchange.common.api.common.enums.OrderTypeEnum.MARKET;
import static com.herron.exchange.common.api.common.enums.TimeInForceEnum.SESSION;


public class MessageCreatorTestUtils {
    private static final Participant PARTICIPANT = new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString()));

    public static Order buildOrderUpdate(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId) {
        return ImmutableDefaultLimitOrder.builder()
                .participant(PARTICIPANT)
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .timeInForce(SESSION)
                .orderOperation(UPDATE)
                .orderOperationCause(EXTERNAL_UPDATE)
                .build();
    }

    public static Order buildOrderDelete(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId) {
        return ImmutableDefaultLimitOrder.builder()
                .participant(PARTICIPANT)
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .timeInForce(SESSION)
                .orderOperationCause(FILLED)
                .orderOperation(CANCEL)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId,
                                      Participant participant) {
        return ImmutableDefaultLimitOrder.builder()
                .participant(participant)
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .timeInForce(SESSION)
                .orderOperation(INSERT)
                .orderOperationCause(NEW_ORDER)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId,
                                      TimeInForceEnum timeInForceEnum,
                                      OrderTypeEnum orderTypeEnum) {
        if (orderTypeEnum == MARKET) {
            return ImmutableDefaultMarketOrder.builder()
                    .participant(new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())))
                    .orderId(orderId)
                    .orderSide(orderSideEnum)
                    .currentVolume(Volume.create(volume))
                    .initialVolume(Volume.create(volume))
                    .timeOfEventMs(timeStampInMs)
                    .instrumentId("inststrumenId")
                    .orderbookId("orderbookId")
                    .orderOperationCause(NEW_ORDER)
                    .orderOperation(INSERT)
                    .build();
        }
        return ImmutableDefaultLimitOrder.builder()
                .participant(new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())))
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .timeInForce(timeInForceEnum)
                .orderOperationCause(NEW_ORDER)
                .orderOperation(INSERT)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId) {
        return ImmutableDefaultLimitOrder.builder()
                .participant(new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())))
                .orderId(orderId)
                .orderSide(orderSideEnum)
                .currentVolume(Volume.create(volume))
                .initialVolume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEventMs(timeStampInMs)
                .instrumentId("inststrumenId")
                .orderbookId("orderbookId")
                .timeInForce(SESSION)
                .orderOperationCause(NEW_ORDER)
                .orderOperation(INSERT)
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
