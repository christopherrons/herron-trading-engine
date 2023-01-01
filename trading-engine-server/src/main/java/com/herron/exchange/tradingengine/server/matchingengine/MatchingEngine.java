package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MatchingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingEngine.class);
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();
    private final PartitionKey partitionKey;
    private final AuditTrail auditTrail;

    public MatchingEngine(PartitionKey partitionKey, KafkaTemplate<String, Object> kafkaTemplate) {
        this.partitionKey = partitionKey;
        this.auditTrail = new AuditTrail(kafkaTemplate, partitionKey);

        var messagePollExecutorService = Executors.newSingleThreadExecutor(new ThreadWrapper(partitionKey.description()));
        messagePollExecutorService.execute(this::pollMessages);
        var logExecutorService = Executors.newScheduledThreadPool(1, new ThreadWrapper(partitionKey.description()));
        logExecutorService.scheduleAtFixedRate(this::logMessageQueueSize, 0, 30, TimeUnit.SECONDS);
    }

    public void queueMessage(Message message) {
        messageQueue.add(message);
    }

    private void pollMessages() {
        while (true) {

            if (messageQueue.isEmpty()) {
                continue;
            }

            Message message = messageQueue.poll();

            if (message == null) {
                continue;
            }

            try {
                auditTrail.queueMessage(message);
                addMessage(message);
                if (message instanceof Order order) {
                    var result = runMatchingAlgorithm(order);
                    result.forEach(auditTrail::queueMessage);
                }

            } catch (Exception e) {
                LOGGER.warn("Unhandled exception for message: {}, {}", message, e);
            }
        }
    }

    private void logMessageQueueSize() {
        LOGGER.info("Message Queue size: {}", messageQueue.size());
    }

    private void addMessage(Message message) {
        if (message instanceof Order order) {
            var orderbook = orderbookCache.getOrderbook(order.orderbookId());
            if (orderbook != null) {
                orderbook.updateOrderbook(order);
            }
        } else if (message instanceof OrderbookData orderbookData) {
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
        orderbookCache.updateState(stateChange);
    }

    private List<Message> runMatchingAlgorithm(Order order) {
        final Orderbook orderbook = orderbookCache.getOrderbook(order.orderbookId());
        return orderbook != null ? orderbook.runMatchingAlgorithm(order) : Collections.emptyList();
    }

}
