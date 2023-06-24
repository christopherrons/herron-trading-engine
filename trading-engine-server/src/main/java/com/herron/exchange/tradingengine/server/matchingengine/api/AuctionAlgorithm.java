package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.tradingengine.server.matchingengine.auctionalgorithms.model.EquilibriumPriceResult;

public interface AuctionAlgorithm {

    EquilibriumPriceResult calculateEquilibriumPrice();
}
