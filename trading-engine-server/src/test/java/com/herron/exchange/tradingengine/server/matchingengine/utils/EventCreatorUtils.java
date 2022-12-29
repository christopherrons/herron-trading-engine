package com.herron.exchange.tradingengine.server.matchingengine.utils;

import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.enums.OrderOperationEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.messages.herron.HerronOrder;
import com.herron.exchange.common.api.common.messages.herron.HerronTrade;
import com.herron.exchange.common.api.common.model.Member;
import com.herron.exchange.common.api.common.model.Participant;
import com.herron.exchange.common.api.common.model.User;

import java.time.Instant;

public class EventCreatorUtils {
    public static Order buildOrderCreate(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderTypeEnum orderTypeEnum,
                                         String orderId) {
        return buildOrder(OrderOperationEnum.CREATE, timeStampInMs, price, volume, orderTypeEnum, orderId);
    }

    public static Order buildOrderCreate(long timeStampInMs,
                                         double price,
                                         double volume,
                                         OrderTypeEnum orderTypeEnum,
                                         String orderId,
                                         Participant participant) {
        return buildOrder(OrderOperationEnum.CREATE, timeStampInMs, price, volume, orderTypeEnum, orderId, participant);
    }

    public static Order buildOrder(OrderOperationEnum orderOperationEnum,
                                   long timeStampInMs,
                                   double price,
                                   double volume,
                                   OrderTypeEnum orderTypeEnum,
                                   String orderId,
                                   Participant participant) {
        return new HerronOrder(orderOperationEnum,
                participant,
                orderId,
                orderTypeEnum,
                volume,
                volume,
                price,
                timeStampInMs,
                "inststrumenId",
                "orderbookId");
    }

    public static Order buildOrder(OrderOperationEnum orderOperationEnum,
                                   long timeStampInMs,
                                   double price,
                                   double volume,
                                   OrderTypeEnum orderTypeEnum,
                                   String orderId) {
        return new HerronOrder(orderOperationEnum,
                new Participant(new Member(Instant.now().toString()), new User(Instant.now().toString())),
                orderId,
                orderTypeEnum,
                volume,
                volume,
                price,
                timeStampInMs,
                "inststrumenId",
                "orderbookId");
    }

    public static Trade buildTrade(long timeStampInMs,
                                   double price,
                                   double volume,
                                   boolean isBidSideAggressor,
                                   long tradeId,
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
