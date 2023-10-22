package com.herron.exchange.tradingengine.server.matchingengine.matchingalgorithms;

import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.enums.OrderTypeEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.tradingengine.server.matchingengine.api.ActiveOrderReadOnly;
import com.herron.exchange.tradingengine.server.matchingengine.api.MatchingAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.model.PriceLevel;

import java.util.*;

import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.KILLED;
import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.PARTIAL_FILL;
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
        return switch (nonActiveOrder.timeInForce()) {
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

    private List<OrderbookEvent> matchProRata(final Order incomingOrder, final PriceLevel opposingBest) {

        final Volume volumeAtPriceLevel = opposingBest.volumeAtPriceLevel();
        final Volume tradeVolume = incomingOrder.currentVolume().min(volumeAtPriceLevel);

        Volume remainingTradeVolume = tradeVolume;
        Map<OrderKey, Volume> keyToVolume = new LinkedHashMap<>();
        Order updatedIncomingKey = incomingOrder;
        for (var opposingOrder : opposingBest) {
            Volume tradeVolumeWeighted = tradeVolume.multiply(opposingOrder.currentVolume().divide(volumeAtPriceLevel));
            tradeVolumeWeighted = remainingTradeVolume.min(tradeVolumeWeighted);

            if (tradeVolumeWeighted.leq(0)) {
                remainingTradeVolume = Volume.ZERO;
                break;
            } else if (tradeVolumeWeighted.subtract(minTradeVolume).leq(0)) {
                break;
            }

            remainingTradeVolume = remainingTradeVolume.subtract(tradeVolumeWeighted);

            var key = new OrderKey(updatedIncomingKey, opposingOrder);
            var currentVolumeTrade = keyToVolume.computeIfAbsent(key, k -> Volume.ZERO);
            keyToVolume.put(key, currentVolumeTrade.add(tradeVolumeWeighted));

            updatedIncomingKey = buildUpdateOrder(updatedIncomingKey, tradeVolumeWeighted, PARTIAL_FILL);
        }

        final List<OrderbookEvent> result = new ArrayList<>();
        for (var entry : keyToVolume.entrySet()) {
            var key = entry.getKey();
            var volume = entry.getValue();
            if (remainingTradeVolume.gt(0) && key.incomingOrder.equals(incomingOrder)) {
                result.addAll(createMatchingMessages(key.incomingOrder, key.opposingOrder, volume.add(remainingTradeVolume)));
            } else if (remainingTradeVolume.gt(0)) {
                result.addAll(createMatchingMessages(buildUpdateOrder(key.incomingOrder, remainingTradeVolume, PARTIAL_FILL), key.opposingOrder, volume));

            } else {
                result.addAll(createMatchingMessages(key.incomingOrder, key.opposingOrder, volume));
            }
        }

        return result;
    }

    private Optional<PriceLevel> getOpposingBestPriceLevel(Order incomingOrder) {
        return switch (incomingOrder.orderSide()) {
            case BID -> activeOrders.getBestAskPriceLevel();
            case ASK -> activeOrders.getBestBidPriceLevel();
        };
    }

    private boolean isMatch(Order incomingOrder, Price opposingPrice) {
        if (incomingOrder.orderType() == OrderTypeEnum.MARKET) {
            return true;
        }
        return switch (incomingOrder.orderSide()) {
            case BID -> incomingOrder.price().geq(opposingPrice);
            case ASK -> incomingOrder.price().leq(opposingPrice);
        };
    }

    private List<OrderbookEvent> createKillMessage(Order nonActiveOrder) {
        List<OrderbookEvent> matchingMessages = new ArrayList<>();
        matchingMessages.add(buildCancelOrder(nonActiveOrder, KILLED));
        return matchingMessages;
    }

    private record OrderKey(Order incomingOrder, Order opposingOrder) {

    }

}
