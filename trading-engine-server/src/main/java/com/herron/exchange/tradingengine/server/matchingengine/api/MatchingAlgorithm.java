package com.herron.exchange.tradingengine.server.matchingengine.api;

import com.herron.exchange.common.api.common.api.Message;

import java.util.Queue;

public interface MatchingAlgorithm {

    Queue<Message> runMatchingAlgorithm();
}
