package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.Order;
import com.herron.exchange.common.api.common.logging.EventLogger;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.*;

public class TradingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingEngine.class);
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    private final MatchingEngine matchingEngine;
    private final EventLogger eventLogger = new EventLogger("Incoming");
    private final AuditTrail auditTrail;
    private final ScheduledExecutorService logExecutorService = Executors.newScheduledThreadPool(1);
    private final ExecutorService messagePollExecutorService = Executors.newSingleThreadExecutor();

    public TradingEngine(MatchingEngine matchingEngine, AuditTrail auditTrail) {
        this.matchingEngine = matchingEngine;
        this.auditTrail = auditTrail;
    }

    public void queueMessage(Message message) {
        messageQueue.add(message);
        eventLogger.logEvent();
    }

    public void init() {
        messagePollExecutorService.execute(this::pollMessages);
        logExecutorService.scheduleAtFixedRate(this::logMessageQueueSize, 30, 30, TimeUnit.SECONDS);
    }

    private void pollMessages() {
        while (true) {
            Message message = messageQueue.poll();

            if (message == null) {
                continue;
            }

            try {
                auditTrail.publish(message);
                matchingEngine.add(message);
                if (message instanceof Order order) {
                    var result = matchingEngine.runMatchingAlgorithm(order);
                    result.forEach(auditTrail::publish);
                }

            } catch (Exception e) {
                LOGGER.warn("Unhandled exception for message: {}, {}", message, e);
            }
        }

    }

    private void logMessageQueueSize() {
        LOGGER.info("Message Queue size: {}", messageQueue.size());
    }
}
