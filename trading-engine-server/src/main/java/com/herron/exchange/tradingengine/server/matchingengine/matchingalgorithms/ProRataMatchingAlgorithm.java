package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.enums.OrderCancelOperationTypeEnum;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.model.PriceLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.buildCancelOrder;
import static com.herron.exchange.tradingengine.server.matchingengine.utils.MatchingEngineUtils.createMatchingMessages;

public class ProRataMatchingAlgorithm implements MatchingAlgorithm {

    private final ActiveOrderReadOnly activeOrders;
    private final double minTradeVolume;


    public ProRataMatchingAlgorithm(ActiveOrderReadOnly activeOrders, double minTradeVolume) {
        this.activeOrders = activeOrders;
        this.minTradeVolume = minTradeVolume;
    }

    public List<Message> matchActiveOrder(Order order) {
        Optional<PriceLevel> opposingBestOptional = getOpposingBestPriceLevel(order);
        if (opposingBestOptional.isEmpty()) {
            return Collections.emptyList();
        }

        PriceLevel opposingBest = opposingBestOptional.get();

        if (isMatch(order, opposingBest.getPrice())) {
            return matchProRata(order, opposingBest);
        }

        return Collections.emptyList();
    }

    public List<Message> matchFillOrKill(Order fillOrKillOrder) {
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

    public List<Message> matchFillAndKill(Order fillAndKillOrder) {
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

    public List<Message> matchMarketOrder(Order marketOrder) {
        Optional<PriceLevel> opposingBestOptional = getOpposingBestPriceLevel(marketOrder);
        if (opposingBestOptional.isEmpty()) {
            return createKillMessage(marketOrder);
        }

        PriceLevel opposingBest = opposingBestOptional.get();

        return matchProRata(marketOrder, opposingBest);
    }

    private List<Message> matchProRata(Order order, PriceLevel opposingBest) {

        final List<Message> result = new ArrayList<>();
        final double volumeAtPriceLevel = opposingBest.volumeAtPriceLevel();
        final double tradeVolume = Math.min(order.currentVolume(), volumeAtPriceLevel);

        double remainingTradeVolume = tradeVolume;
        for (var opposingOrder : opposingBest) {
            double tradeVolumeWeighted = tradeVolume * (opposingOrder.currentVolume() / volumeAtPriceLevel);
            double minTradeVolumeWeighted = minTradeVolume == 0 ? tradeVolumeWeighted :
                    Math.floor(tradeVolumeWeighted / minTradeVolume) + Math.max(tradeVolumeWeighted % minTradeVolume, minTradeVolume);

            if (remainingTradeVolume <= 0) {
                break;
            } else if (remainingTradeVolume - minTradeVolumeWeighted < 0) {
                result.addAll(createMatchingMessages(order, opposingBest.first(), remainingTradeVolume));
                break;
            }

            remainingTradeVolume -= minTradeVolumeWeighted;
            result.addAll(createMatchingMessages(order, opposingOrder, minTradeVolumeWeighted));
        }
        return result;
    }

    private Optional<PriceLevel> getOpposingBestPriceLevel(Order order) {
        return switch (order.orderSide()) {
            case BID -> activeOrders.getBestAskPriceLevel();
            case ASK -> activeOrders.getBestBidPriceLevel();
            default -> Optional.empty();
        };
    }

    private boolean isMatch(Order order, double opposingPrice) {
        if (order.orderType().equals(OrderTypeEnum.MARKET)) {
            return true;
        }
        return switch (order.orderSide()) {
            case BID -> order.price() >= opposingPrice;
            case ASK -> order.price() <= opposingPrice;
            default -> false;
        };
    }

    private List<Message> createKillMessage(Order nonActiveOrder) {
        List<Message> matchingMessages = new ArrayList<>();
        matchingMessages.add(buildCancelOrder(nonActiveOrder, OrderCancelOperationTypeEnum.KILLED));
        return matchingMessages;
    }

}
