package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.springframework.context.annotation.Bean;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TradingEngine {

    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    private final MatchingEngine matchingEngine;
    private final AuditTrail auditTrail;

    public TradingEngine(MatchingEngine matchingEngine, AuditTrail auditTrail) {
        this.matchingEngine = matchingEngine;
        this.auditTrail = auditTrail;
    }

    public void queueMessage(Message message) {
        messageQueue.add(message);
    }

    @Bean(initMethod = "init")
    public void init() {
        Queue<Message> matchingMessages = new LinkedList<>();
        while (true) {
            Message message;
            if (matchingMessages.isEmpty()) {
                message = messageQueue.poll();
            } else {
                message = matchingMessages.poll();
            }

            if (message != null) {
                auditTrail.handleMessage(message);
                matchingMessages = matchingEngine.addAndMatch(message);
            }
        }
    }
}
