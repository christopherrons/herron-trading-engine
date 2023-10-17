package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.broadcasts.BroadcastMessage;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingEngine.class);
    private final Map<PartitionKey, MatchingEngine> partitionKeyToMatchingEngine = new ConcurrentHashMap<>();
    private final KafkaBroadcastHandler broadcastHandler;

    public TradingEngine(KafkaBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
    }

    public void queueMessage(BroadcastMessage broadcastMessage) {
        partitionKeyToMatchingEngine.computeIfAbsent(broadcastMessage.partitionKey(), key -> {
                    var matchingEngine = new MatchingEngine(key, broadcastHandler);
                    matchingEngine.init();
                    return matchingEngine;
                })
                .queueMessage(broadcastMessage.message());
    }
}
