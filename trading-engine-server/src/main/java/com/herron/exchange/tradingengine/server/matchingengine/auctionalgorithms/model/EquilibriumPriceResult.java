package com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms.model;

import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;

import java.util.List;

public record EquilibriumPriceResult(VolumeMatchAtPriceItem optimalPrice, List<VolumeMatchAtPriceItem> items) {
    public record VolumeMatchAtPriceItem(Price equilibriumPrice, Volume bidVolume, Volume askVolume) {
        public Volume matchedVolume() {
            return bidVolume.min(askVolume);
        }
    }
}
