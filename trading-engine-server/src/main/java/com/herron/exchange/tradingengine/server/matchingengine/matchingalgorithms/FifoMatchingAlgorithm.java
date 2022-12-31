package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    public List<Message> runMatchingAlgorithm(Order order) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(order);
        if (opposingBestOptional.isEmpty()) {
            return Collections.emptyList();
        }

        Order opposingBest = opposingBestOptional.get();

        if (isMatch(order, opposingBest)) {
            return createMatchingMessages(order, opposingBest);
        }
        return Collections.emptyList();
    }

    private Optional<Order> getOpposingBestOrder(Order order) {
        return switch (order.orderSide()) {
            case BID -> activeOrders.getBestAskOrder();
            case ASK -> activeOrders.getBestBidOrder();
            default -> Optional.empty();
        };
    }

    private boolean isMatch(Order order, Order opposingBestOrder) {
        if (order.orderType().equals(OrderTypeEnum.MARKET)) {
            return true;
        }
        return switch (order.orderSide()) {
            case BID -> order.price() >= opposingBestOrder.price();
            case ASK -> order.price() <= opposingBestOrder.price();
            default -> false;
        };
    }

}
