package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderUpdatedOperationTypeEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.model.PriceLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.*;

public class ProRataMatchingAlgorithm implements MatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;
    private final double minTradeVolume;


    public ProRataMatchingAlgorithm(ActiveOrderReadOnly activeOrders, double minTradeVolume) {
        this.activeOrders = activeOrders;
        this.minTradeVolume = minTradeVolume;
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

        if (isMatch(bestBidOrder.get(), bestAskOrder.get().price())) {
            return createAuctionMatchingMessages(bestBidOrder.get(), bestAskOrder.get(), price);
        }
        return Collections.emptyList();
    }

    private List<OrderbookEvent> matchNonActiveOrders(Order nonActiveOrder) {
        return switch (nonActiveOrder.orderExecutionType()) {
            case FOK -> matchFillOrKill(nonActiveOrder);
            case FAK -> matchFillAndKill(nonActiveOrder);
            default -> matchMarketOrder(nonActiveOrder);
        };
    }

    private List<OrderbookEvent> matchActiveOrder(Order incomingOrder) {
        Optional<PriceLevel> opposingBestOptional = getOpposingBestPriceLevel(incomingOrder);
        if (opposingBestOptional.isEmpty()) {
            return Collections.emptyList();
        }

        PriceLevel opposingBest = opposingBestOptional.get();

        if (isMatch(incomingOrder, opposingBest.getPrice())) {
            return matchProRata(incomingOrder, opposingBest);
        }

        return Collections.emptyList();
    }

    public List<OrderbookEvent> matchFillOrKill(Order fillOrKillOrder) {
        if (!activeOrders.isTotalFillPossible(fillOrKillOrder)) {
            return createKillMessage(fillOrKillOrder);
        }

        Optional<PriceLevel> opposingBestOptional = getOpposingBestPriceLevel(fillOrKillOrder);
        if (opposingBestOptional.isEmpty()) {
            return Collections.emptyList();
        }

        PriceLevel opposingBest = opposingBestOptional.get();

        return matchProRata(fillOrKillOrder, opposingBest);
    }

    public List<OrderbookEvent> matchFillAndKill(Order fillAndKillOrder) {
        Optional<PriceLevel> opposingBestOptional = getOpposingBestPriceLevel(fillAndKillOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(fillAndKillOrder);
        }

        PriceLevel opposingBest = opposingBestOptional.get();

        if (isMatch(fillAndKillOrder, opposingBest.getPrice())) {
            return matchProRata(fillAndKillOrder, opposingBest);
        }

        return createKillMessage(fillAndKillOrder);
    }

    public List<OrderbookEvent> matchMarketOrder(Order marketOrder) {
        Optional<PriceLevel> opposingBestOptional = getOpposingBestPriceLevel(marketOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(marketOrder);
        }

        PriceLevel opposingBest = opposingBestOptional.get();

        return matchProRata(marketOrder, opposingBest);
    }

    private List<OrderbookEvent> matchProRata(Order incomingOrder, PriceLevel opposingBest) {

        final List<OrderbookEvent> result = new ArrayList<>();
        final double volumeAtPriceLevel = opposingBest.volumeAtPriceLevel().getValue();
        final double tradeVolume = Math.min(incomingOrder.currentVolume().getValue(), volumeAtPriceLevel);

        double remainingTradeVolume = tradeVolume;
        for (var opposingOrder : opposingBest) {
            double tradeVolumeWeighted = tradeVolume * (opposingOrder.currentVolume().getValue() / volumeAtPriceLevel);
            double minTradeVolumeWeighted = minTradeVolume == 0 ? tradeVolumeWeighted :
                    Math.floor(tradeVolumeWeighted / minTradeVolume) + Math.max(tradeVolumeWeighted % minTradeVolume, minTradeVolume);

            if (remainingTradeVolume <= 0) {
                break;
            } else if (remainingTradeVolume - minTradeVolumeWeighted < 0) {
                result.addAll(createMatchingMessages(incomingOrder, opposingBest.first(), Volume.create(remainingTradeVolume)));
                break;
            }

            remainingTradeVolume -= minTradeVolumeWeighted;
            result.addAll(createMatchingMessages(incomingOrder, opposingOrder, Volume.create(minTradeVolumeWeighted)));
            incomingOrder = buildUpdateOrder(incomingOrder, Volume.create(minTradeVolumeWeighted), OrderUpdatedOperationTypeEnum.PARTIAL_FILL);
        }
        return result;
    }

    private Optional<PriceLevel> getOpposingBestPriceLevel(Order incomingOrder) {
        return switch (incomingOrder.orderSide()) {
            case BID -> activeOrders.getBestAskPriceLevel();
            case ASK -> activeOrders.getBestBidPriceLevel();
            default -> Optional.empty();
        };
    }

    private boolean isMatch(Order incomingOrder, Price opposingPrice) {
        if (incomingOrder.orderType() == OrderTypeEnum.MARKET) {
            return true;
        }
        return switch (incomingOrder.orderSide()) {
            case BID -> incomingOrder.price().geq(opposingPrice);
            case ASK -> incomingOrder.price().leq(opposingPrice);
            default -> false;
        };
    }

    private List<OrderbookEvent> createKillMessage(Order nonActiveOrder) {
        List<OrderbookEvent> matchingMessages = new ArrayList<>();
        matchingMessages.add(buildCancelOrder(nonActiveOrder, OrderCancelOperationTypeEnum.KILLED));
        return matchingMessages;
    }

}
