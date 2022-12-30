package com.herron.exchange.tradingengine.server.matchingengine.utils;

import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.enums.*;
import com.herron.exchange.common.api.common.messages.herron.HerronAddOrder;
import com.herron.exchange.common.api.common.messages.herron.HerronTrade;
import com.herron.exchange.common.api.common.model.Member;
import com.herron.exchange.common.api.common.model.Participant;
import com.herron.exchange.common.api.common.model.User;

import java.time.Instant;

public class EventCreatorUtils {
    public static Order buildOrderCreate(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId) {
        return buildOrder(OrderOperationEnum.CREATE, timeStampInMs, price, volume, orderSideEnum, orderId);
    }

    public static Order buildOrderCreate(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderSideEnum orderSideEnum,
                                         String orderId,
                                         Participant participant) {
        return buildOrder(OrderOperationEnum.CREATE, timeStampInMs, price, volume, orderSideEnum, orderId, participant);
    }

    public static Order buildOrder(OrderOperationEnum orderOperationEnum,
                                   long timeStampInMs,
                                   double price,
                                   double volume,
                                   OrderSideEnum orderSideEnum,
                                   String orderId,
                                   Participant participant) {
        return new HerronAddOrder(orderOperationEnum,
                participant,
                orderId,
                orderSideEnum,
                volume,
                volume,
                price,
                timeStampInMs,
                "inststrumenId",
                "orderbookId",
                OrderExecutionTypeEnum.FILL,
                OrderTypeEnum.LIMIT,
                OrderAddOperationTypeEnum.NEW_ORDER);
    }

    public static Order buildOrder(OrderOperationEnum orderOperationEnum,
                                   long timeStampInMs,
                                   double price,
                                   double volume,
                                   OrderSideEnum orderSideEnum,
                                   String orderId) {
        return new HerronAddOrder(orderOperationEnum,
                new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())),
                orderId,
                orderSideEnum,
                volume,
                volume,
                price,
                timeStampInMs,
                "inststrumenId",
                "orderbookId",
                OrderExecutionTypeEnum.FILL,
                OrderTypeEnum.LIMIT,
                OrderAddOperationTypeEnum.NEW_ORDER);
    }

    public static Trade buildTrade(long timeStampInMs,
                                   double price,
                                   double volume,
                                   boolean isBidSideAggressor,
                                   String tradeId,
                                   Participant bidParticipant,
                                   Participant askParticipant) {
        return new HerronTrade(
                bidParticipant,
                askParticipant,
                tradeId,
                "buyOrderid",
                "askOrderid",
                isBidSideAggressor,
                volume,
                price,
                timeStampInMs,
                "instrumentId",
                "ordebookId"
        );
    }
}
