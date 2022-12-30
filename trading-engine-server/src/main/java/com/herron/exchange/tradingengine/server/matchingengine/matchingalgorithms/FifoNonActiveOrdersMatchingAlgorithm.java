package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.buildCancelOrder;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.createMatchingMessages;

public class FifoNonActiveOrdersMatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;

    public FifoNonActiveOrdersMatchingAlgorithm(ActiveOrderReadOnly activeOrders) {
        this.activeOrders = activeOrders;
    }

    public List<Message> runMatchingAlgorithmNonActiveOrder(Order nonActiveOrder) {
        if (nonActiveOrder.currentVolume() <= 0) {
            return new ArrayList<>();
        }

        return switch (nonActiveOrder.orderExecutionType()) {
            case FOK -> handleFillOrKill(nonActiveOrder);
            case FAK -> handleFillAndKill(nonActiveOrder);
            default -> handleMarketOrder(nonActiveOrder);
        };
    }

    public List<Message> handleFillAndKill(Order fillAndKillOrder) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(fillAndKillOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(fillAndKillOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        if (isMatch(fillAndKillOrder, opposingBest)) {
            return switch (fillAndKillOrder.orderSide()) {
                case BID -> createMatchingMessages(fillAndKillOrder, opposingBest);
                case ASK -> createMatchingMessages(opposingBest, fillAndKillOrder);
                default -> createKillMessage(fillAndKillOrder);
            };
        }
        return createKillMessage(fillAndKillOrder);
    }

    public List<Message> handleMarketOrder(Order marketOrder) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(marketOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(marketOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        return switch (marketOrder.orderSide()) {
            case BID -> createMatchingMessages(marketOrder, opposingBest);
            case ASK -> createMatchingMessages(opposingBest, marketOrder);
            default -> createKillMessage(marketOrder);
        };
    }

    public List<Message> handleFillOrKill(Order fillOrKillOrder) {
        if (!activeOrders.isTotalFillPossible(fillOrKillOrder)) {
            return createKillMessage(fillOrKillOrder);
        }

        Optional<Order> opposingBestOptional = getOpposingBestOrder(fillOrKillOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(fillOrKillOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        return switch (fillOrKillOrder.orderSide()) {
            case BID -> createMatchingMessages(fillOrKillOrder, opposingBest);
            case ASK -> createMatchingMessages(opposingBest, fillOrKillOrder);
            default -> createKillMessage(fillOrKillOrder);
        };
    }

    private Optional<Order> getOpposingBestOrder(Order nonActiveOrder) {
        return switch (nonActiveOrder.orderSide()) {
            case BID -> activeOrders.getBestAskOrder();
            case ASK -> activeOrders.getBestBidOrder();
            default -> Optional.empty();
        };
    }

    private List<Message> createKillMessage(Order nonActiveOrder) {
        List<Message> matchingMessages = new ArrayList<>();
        matchingMessages.add(buildCancelOrder(nonActiveOrder, OrderCancelOperationTypeEnum.KILLED));
        return matchingMessages;
    }

    private boolean isMatch(Order nonActiveOrder, Order opposingBestOrder) {
        if (nonActiveOrder.orderType().equals(OrderTypeEnum.MARKET)) {
            return true;
        }
        return switch (nonActiveOrder.orderSide()) {
            case BID -> nonActiveOrder.price() >= opposingBestOrder.price();
            case ASK -> nonActiveOrder.price() <= opposingBestOrder.price();
            default -> false;
        };
    }

}
