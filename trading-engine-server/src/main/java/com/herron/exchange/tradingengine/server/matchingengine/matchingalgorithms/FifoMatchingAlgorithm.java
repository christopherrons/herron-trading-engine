package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.buildCancelOrder;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.createMatchingMessages;

public class FifoMatchingAlgorithm implements MatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;

    public FifoMatchingAlgorithm(ActiveOrderReadOnly activeOrders) {
        this.activeOrders = activeOrders;
    }

    public List<Message> matchOrder(Order order) {
        return order.isActiveOrder() ? matchActiveOrder(order) : matchNonActiveOrders(order);
    }

    private List<Message> matchNonActiveOrders(Order nonActiveOrder) {
        return switch (nonActiveOrder.orderExecutionType()) {
            case FOK -> matchFillOrKill(nonActiveOrder);
            case FAK -> matchFillAndKill(nonActiveOrder);
            default -> matchMarketOrder(nonActiveOrder);
        };
    }

    private List<Message> matchActiveOrder(Order order) {
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

    public List<Message> matchFillOrKill(Order fillOrKillOrder) {
        if (!activeOrders.isTotalFillPossible(fillOrKillOrder)) {
            return createKillMessage(fillOrKillOrder);
        }

        Optional<Order> opposingBestOptional = getOpposingBestOrder(fillOrKillOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(fillOrKillOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        return createMatchingMessages(fillOrKillOrder, opposingBest);
    }

    public List<Message> matchFillAndKill(Order fillAndKillOrder) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(fillAndKillOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(fillAndKillOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        if (isMatch(fillAndKillOrder, opposingBest)) {
            return createMatchingMessages(fillAndKillOrder, opposingBest);
        }
        return createKillMessage(fillAndKillOrder);
    }

    public List<Message> matchMarketOrder(Order marketOrder) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(marketOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(marketOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        return createMatchingMessages(marketOrder, opposingBest);
    }

    private Optional<Order> getOpposingBestOrder(Order order) {
        return switch (order.orderSide()) {
            case BID -> activeOrders.getBestAskOrder();
            case ASK -> activeOrders.getBestBidOrder();
            default -> Optional.empty();
        };
    }

    private boolean isMatch(Order order, Order opposingBestOrder) {
        if (order.orderType() == OrderTypeEnum.MARKET) {
            return true;
        }
        return switch (order.orderSide()) {
            case BID -> order.price() >= opposingBestOrder.price();
            case ASK -> order.price() <= opposingBestOrder.price();
            default -> false;
        };
    }

    private List<Message> createKillMessage(Order nonActiveOrder) {
        List<Message> matchingMessages = new ArrayList<>();
        matchingMessages.add(buildCancelOrder(nonActiveOrder, OrderCancelOperationTypeEnum.KILLED));
        return matchingMessages;
    }

}
