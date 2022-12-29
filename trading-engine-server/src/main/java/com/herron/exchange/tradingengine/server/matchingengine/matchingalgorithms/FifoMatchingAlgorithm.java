package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.api.Trade;
import com.herron.exchange.common.api.common.model.Participant;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;

import java.util.LinkedList;
import java.util.Queue;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.*;

public class FifoMatchingAlgorithm implements MatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;

    public FifoMatchingAlgorithm(ActiveOrderReadOnly activeOrders) {
        this.activeOrders = activeOrders;
    }

    public Queue<Message> runMatchingAlgorithm() {
        if (!activeOrders.hasBidAndAskOrders()) {
            return new LinkedList<>();
        }

        Order bestBid = activeOrders.getBestBidOrder().get();
        Order bestAsk = activeOrders.getBestAskOrder().get();

        if (isMatch(bestBid.price(), bestAsk.price())) {
            return runPostTrade(bestBid, bestAsk);
        }
        return new LinkedList<>();
    }

    private boolean isMatch(double bidPrice, double askPrice) {
        return bidPrice >= askPrice;
    }

    private Queue<Message> runPostTrade(Order bidOrder, Order askOrder) {
        final double tradeVolume = Math.min(bidOrder.currentVolume(), askOrder.currentVolume());

        Queue<Message> matchingMessages = new LinkedList<>();

        final Order updateBidOrder = buildUpdateOrder(bidOrder, tradeVolume);
        if (updateBidOrder.currentVolume() == 0) {
            matchingMessages.add(buildCancelOrder(updateBidOrder));
        } else {
            matchingMessages.add(updateBidOrder);
        }

        final Order updateAskOrder = buildUpdateOrder(askOrder, tradeVolume);
        if (updateAskOrder.currentVolume() == 0) {
            matchingMessages.add(buildCancelOrder(updateAskOrder));
        } else {
            matchingMessages.add(updateAskOrder);
        }

        if (!isSelfMatch(bidOrder.participant(), askOrder.participant())) {
            final Trade trade = buildTrade(bidOrder, askOrder, tradeVolume);
            matchingMessages.add(trade);
        }

        return matchingMessages;
    }

    private boolean isSelfMatch(Participant bidParticipant, Participant askParticipant) {
        return bidParticipant.equals(askParticipant);
    }
}
