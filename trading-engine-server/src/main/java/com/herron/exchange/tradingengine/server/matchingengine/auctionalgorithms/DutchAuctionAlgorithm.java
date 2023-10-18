package com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms;

import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.tradingengine.server.matchingengine.api.AuctionAlgorithm;
import com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms.model.EquilibriumPriceResult;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.ActiveOrders;
import com.herron.exchange.tradingengine.server.matchingengine.orderbook.model.PriceLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DutchAuctionAlgorithm implements AuctionAlgorithm {

    private final ActiveOrders activeOrders;

    public DutchAuctionAlgorithm(ActiveOrders activeOrders) {
        this.activeOrders = activeOrders;
    }

    public EquilibriumPriceResult calculateEquilibriumPrice() {
        var bestBidPriceLevel = activeOrders.getBestBidPriceLevel();
        var bestAskPriceLevel = activeOrders.getBestAskPriceLevel();
        if (bestBidPriceLevel.isEmpty() || bestAskPriceLevel.isEmpty()) {
            return null;
        }

        List<PriceLevel> bidPriceLevels = activeOrders.getBidPriceLevelsHigherOrEqual(bestAskPriceLevel.get().getPrice());
        List<PriceLevel> askPriceLevels = activeOrders.getAskPriceLevelsLowerOrEqual(bestBidPriceLevel.get().getPrice());

        return calculateEquilibriumPrice(bidPriceLevels, askPriceLevels);
    }

    private EquilibriumPriceResult calculateEquilibriumPrice(List<PriceLevel> bidPriceLevels, List<PriceLevel> askPriceLevels) {
        Set<Price> possibleEquilibriumPrice = new HashSet<>();
        bidPriceLevels.forEach(pl -> possibleEquilibriumPrice.add(pl.getPrice()));
        askPriceLevels.forEach(pl -> possibleEquilibriumPrice.add(pl.getPrice()));

        if (possibleEquilibriumPrice.isEmpty()) {
            return null;
        }
        EquilibriumPriceResult.VolumeMatchAtPriceItem maxVolumeMatchAtPrice = new EquilibriumPriceResult.VolumeMatchAtPriceItem(Price.ZERO, Volume.ZERO, Volume.ZERO);
        List<EquilibriumPriceResult.VolumeMatchAtPriceItem> volumeMatchAtPriceItems = new ArrayList<>();
        for (var eqPrice : possibleEquilibriumPrice) {
            var bidVolume = bidPriceLevels.stream().filter(pl -> pl.getPrice().geq(eqPrice)).map(PriceLevel::volumeAtPriceLevel).reduce(Volume.ZERO, Volume::add);
            var askVolume = askPriceLevels.stream().filter(pl -> pl.getPrice().leq(eqPrice)).map(PriceLevel::volumeAtPriceLevel).reduce(Volume.ZERO, Volume::add);

            var matchingVolumeAtPrice = new EquilibriumPriceResult.VolumeMatchAtPriceItem(eqPrice, bidVolume, askVolume);
            volumeMatchAtPriceItems.add(matchingVolumeAtPrice);
            if (maxVolumeMatchAtPrice.matchedVolume().lt(matchingVolumeAtPrice.matchedVolume())) {
                maxVolumeMatchAtPrice = matchingVolumeAtPrice;
            }
        }

        return new EquilibriumPriceResult(maxVolumeMatchAtPrice, volumeMatchAtPriceItems);
    }
}
