package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.KILLED;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.*;

public class FifoMatchingAlgorithm implements MatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;

    public FifoMatchingAlgorithm(ActiveOrderReadOnly activeOrders) {
        this.activeOrders = activeOrders;
    }

    @Override
    public List<OrderbookEvent> matchOrder(Order incomingOrder) {
        return incomingOrder.isActiveOrder() ? matchActiveOrder(incomingOrder) : matchNonActiveOrders(incomingOrder);
    }

    @Override
    public List<OrderbookEvent> matchAtPrice(Price price) {
        Optional<Order> bestBidOrder = activeOrders.getBestBidOrder();
        Optional<Order> bestAskOrder = activeOrders.getBestAskOrder();
        if (bestBidOrder.isEmpty() || bestAskOrder.isEmpty()) {
            return Collections.emptyList();
        }

        if (isMatch(bestBidOrder.get(), bestAskOrder.get())) {
            return createAuctionMatchingMessages(bestBidOrder.get(), bestAskOrder.get(), price);

        }
        return Collections.emptyList();
    }

    private List<OrderbookEvent> matchNonActiveOrders(Order nonActiveOrder) {
        return switch (nonActiveOrder.timeInForce()) {
            case FOK -> matchFillOrKill(nonActiveOrder);
            case FAK -> matchFillAndKill(nonActiveOrder);
            default -> matchMarketOrder(nonActiveOrder);
        };
    }

    private List<OrderbookEvent> matchActiveOrder(Order incomingOrder) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(incomingOrder);
        if (opposingBestOptional.isEmpty()) {
            return Collections.emptyList();
        }

        Order opposingBest = opposingBestOptional.get();

        if (isMatch(incomingOrder, opposingBest)) {
            return createMatchingMessages(incomingOrder, opposingBest);
        }
        return Collections.emptyList();
    }

    public List<OrderbookEvent> matchFillOrKill(Order fillOrKillOrder) {
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

    public List<OrderbookEvent> matchFillAndKill(Order fillAndKillOrder) {
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

    public List<OrderbookEvent> matchMarketOrder(Order marketOrder) {
        Optional<Order> opposingBestOptional = getOpposingBestOrder(marketOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(marketOrder);
        }

        Order opposingBest = opposingBestOptional.get();

        return createMatchingMessages(marketOrder, opposingBest);
    }

    private Optional<Order> getOpposingBestOrder(Order incomingOrder) {
        return switch (incomingOrder.orderSide()) {
            case BID -> activeOrders.getBestAskOrder();
            case ASK -> activeOrders.getBestBidOrder();
        };
    }

    private boolean isMatch(Order incomingOrder, Order opposingBestOrder) {
        if (incomingOrder.orderType() == OrderTypeEnum.MARKET) {
            return true;
        }
        return switch (incomingOrder.orderSide()) {
            case BID -> incomingOrder.price().geq(opposingBestOrder.price());
            case ASK -> incomingOrder.price().leq(opposingBestOrder.price());
        };
    }

    private List<OrderbookEvent> createKillMessage(Order nonActiveOrder) {
        List<OrderbookEvent> matchingMessages = new ArrayList<>();
        matchingMessages.add(buildCancelOrder(nonActiveOrder, KILLED));
        return matchingMessages;
    }

}
