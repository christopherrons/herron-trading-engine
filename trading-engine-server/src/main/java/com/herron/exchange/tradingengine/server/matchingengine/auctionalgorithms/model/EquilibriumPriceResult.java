package com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms.model;

import java.util.List;

public record EquilibriumPriceResult(VolumeMatchAtPriceItem optimalPrice, List<VolumeMatchAtPriceItem> items) {
    public record VolumeMatchAtPriceItem(double equilibriumPrice, double bidVolume, double askVolume) {
        public double matchedVolume() {
            return Math.min(bidVolume, askVolume);
        }
    }
}
