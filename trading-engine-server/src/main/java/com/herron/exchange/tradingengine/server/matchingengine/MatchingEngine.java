package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.common.api.common.wrappers.ThreadWrapper;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class MatchingEngine {
    private final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();
    private final PartitionKey partitionKey;
    private final AuditTrail auditTrail;
    private final Thread pollThread;
    private final ScheduledExecutorService queueLoggerThread;

    public MatchingEngine(PartitionKey partitionKey, KafkaTemplate<String, Object> kafkaTemplate) {
        this.partitionKey = partitionKey;
        this.auditTrail = new AuditTrail(kafkaTemplate, partitionKey);

        pollThread = new Thread(this::pollMessages, partitionKey.description());
        queueLoggerThread = newScheduledThreadPool(1, new ThreadWrapper(partitionKey.description()));

        initMatching();
    }

    public void queueMessage(Message message) {
        messageQueue.add(message);
    }

    public void initMatching() {
        logger.info("Starting matching engine.");
        pollThread.start();
        queueLoggerThread.scheduleAtFixedRate(() -> logger.info("Message Queue size: {}", messageQueue.size()), 0, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        logger.info("Stopping Matching.");
        pollThread.interrupt();
        try {
            pollThread.join();
        } catch (InterruptedException e) {
            logger.error("Issue while shutting down matching thread");
        }
        queueLoggerThread.shutdown();
    }

    private void pollMessages() {
        while (pollThread.isAlive() && !pollThread.isInterrupted()) {

            if (messageQueue.isEmpty()) {
                continue;
            }

            Message message = messageQueue.poll();

            if (message == null) {
                continue;
            }

            auditTrail.broadcastMessage(message);

            try {
                if (message instanceof Order order) {
                    var result = runMatchingAlgorithm(order);
                    auditTrail.broadcastMessage(result);
                } else {
                    addMessage(message);
                }

            } catch (Exception e) {
                logger.warn("Unhandled exception for message: {}", message, e);
            }
        }
    }

    private void addMessage(Message message) {
        if (message instanceof OrderbookData orderbookData) {
            addOrderbookData(orderbookData);

        } else if (message instanceof Instrument instrument) {
            addInstrument(instrument);

        } else if (message instanceof StateChange stateChange) {
            updateState(stateChange);
        }
    }

    private void addOrderbookData(OrderbookData orderbookData) {
        referanceDataCache.addOrderbookData(orderbookData);
        orderbookCache.createOrderbook(orderbookData);
    }

    private void addInstrument(Instrument instrument) {
        referanceDataCache.addInstrument(instrument);
    }

    private void updateState(StateChange stateChange) {
        var orderbook = orderbookCache.getOrderbook(stateChange.orderbookId());
        if (orderbook == null) {
            logger.error("Cannot update state {} does not exist.", stateChange);
            return;
        }

        orderbook.updateState(stateChange);
    }

    private TradeExecution runMatchingAlgorithm(Order order) {
        var orderbook = orderbookCache.getOrderbook(order.orderbookId());
        if (orderbook == null) {
            logger.error("Order can not be added, orderbook {} does not exist.", order.orderbookId());
            return null;
        }

        orderbook.updateOrderbook(order);
        return orderbook.runMatchingAlgorithm(order);
    }

}
