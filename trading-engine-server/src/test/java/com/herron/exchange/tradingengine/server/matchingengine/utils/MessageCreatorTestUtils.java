package com.herron.exchange.tradingengine.server.matchingengine.utils;


import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.enums.TimeInForceEnum;
import com.herron.exchange.common.api.common.messages.common.*;
import com.herron.exchange.common.api.common.messages.trading.ImmutableLimitOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableMarketOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableTrade;
import com.herron.exchange.common.api.common.messages.trading.Trade;

import java.time.Instant;

import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.EventType.USER;
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
        return ImmutableLimitOrder.builder()
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
                .eventType(USER)
                .build();
    }

    public static Order buildOrderDelete(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId) {
        return ImmutableLimitOrder.builder()
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
                .eventType(USER)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId,
                                      Participant participant) {
        return ImmutableLimitOrder.builder()
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
                .eventType(USER)
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
            return ImmutableMarketOrder.builder()
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
                    .eventType(USER)
                    .build();
        }
        return ImmutableLimitOrder.builder()
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
                .eventType(USER)
                .build();
    }

    public static Order buildOrderAdd(long timeStampInMs,
                                      double price,
                                      double volume,
                                      OrderSideEnum orderSideEnum,
                                      String orderId) {
        return ImmutableLimitOrder.builder()
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
                .eventType(SYSTEM)
                .build();
    }

    public static Trade buildTrade(long timeStampInMs,
                                   double price,
                                   double volume,
                                   boolean isBidSideAggressor,
                                   String tradeId,
                                   Participant bidParticipant,
                                   Participant askParticipant) {
        return ImmutableTrade.builder()
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
                .eventType(SYSTEM)
                .build();
    }
}
