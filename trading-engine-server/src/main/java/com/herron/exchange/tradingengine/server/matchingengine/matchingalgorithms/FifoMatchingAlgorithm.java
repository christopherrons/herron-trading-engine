package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;

import java.util.ArrayList;
import java.util.List;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.createMatchingMessages;

public class FifoMatchingAlgorithm implements MatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;
    private final FifoNonActiveOrdersMatchingAlgorithm nonActiveOrdersMatchingAlgorithm;

    public FifoMatchingAlgorithm(ActiveOrderReadOnly activeOrders) {
        this.activeOrders = activeOrders;
        this.nonActiveOrdersMatchingAlgorithm = new FifoNonActiveOrdersMatchingAlgorithm(activeOrders);
    }

    public List<Message> runMatchingAlgorithmNonActiveOrder(Order nonActiveOrder) {
        return nonActiveOrdersMatchingAlgorithm.runMatchingAlgorithmNonActiveOrder(nonActiveOrder);
    }

    public List<Message> runMatchingAlgorithmNonActiveOrder() {
        if (!activeOrders.hasBidAndAskOrders()) {
            return new ArrayList<>();
        }

        Order bestBid = activeOrders.getBestBidOrder().get();
        Order bestAsk = activeOrders.getBestAskOrder().get();

        if (isMatch(bestBid.price(), bestAsk.price())) {
            return createMatchingMessages(bestBid, bestAsk);
        }
        return new ArrayList<>();
    }

    private boolean isMatch(double bidPrice, double askPrice) {
        return bidPrice >= askPrice;
    }

}
