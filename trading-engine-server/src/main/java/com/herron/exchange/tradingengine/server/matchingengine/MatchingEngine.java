package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.*;
import com.herron.exchange.common.api.common.enums.RequestStatus;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.common.api.common.response.InvalidRequestResponse;
import com.herron.exchange.common.api.common.wrappers.ThreadWrapper;
import com.herron.exchange.tradingengine.server.audittrail.AuditTrail;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import com.herron.exchange.tradingengine.server.matchingengine.cache.ReferanceDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class MatchingEngine {
    private final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    private final Queue<Order> orderQueue = new ConcurrentLinkedQueue<>();
    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final ReferanceDataCache referanceDataCache = new ReferanceDataCache();
    private final PartitionKey partitionKey;
    private final AuditTrail auditTrail;
    private final Thread pollThread;
    private final ScheduledExecutorService queueLoggerThread;
    private final AtomicBoolean isMatching = new AtomicBoolean(false);

    public MatchingEngine(PartitionKey partitionKey, KafkaTemplate<String, Object> kafkaTemplate) {
        this.partitionKey = partitionKey;
        this.auditTrail = new AuditTrail(kafkaTemplate, partitionKey);

        pollThread = new Thread(this::runMatching, partitionKey.description());
        queueLoggerThread = newScheduledThreadPool(1, new ThreadWrapper(partitionKey.description()));

        initMatching();
    }

    public Response handleMessage(Message message) {
        if (message instanceof OrderbookDataRequest orderbookDataRequest) {
            return handleOrderbookDataRequest(orderbookDataRequest);

        } else if (message instanceof InstrumentRequest instrumentRequest) {
            return handleInstrumentRequest(instrumentRequest);

        } else if (message instanceof StateChangeRequest stateChangeRequest) {
            return handleStateChangeRequest(stateChangeRequest);

        } else if (message instanceof OrderRequest orderRequest) {
            return handleOrderRequest(orderRequest);
        }

        if (message instanceof Request request) {
            return new InvalidRequestResponse(Instant.now().toEpochMilli(),
                    request.requestId(),
                    String.format("Unhandled request if type: %s and class: %s.", request.messageType(), request.getClass())
            );
        }

        return new InvalidRequestResponse(Instant.now().toEpochMilli(),
                Long.MIN_VALUE,
                String.format("Unhandled message if type: %s and class: %s.", message.messageType(), message.getClass())
        );
    }

    public void initMatching() {
        logger.info("Starting matching engine.");
        isMatching.set(true);
        pollThread.start();
        queueLoggerThread.scheduleAtFixedRate(() -> logger.info("Message Queue size: {}", orderQueue.size()), 0, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        logger.info("Stopping matching engine.");
        isMatching.set(false);
        queueLoggerThread.shutdown();
    }

    private void runMatching() {
        while (isMatching.get() || !orderQueue.isEmpty()) {

            if (orderQueue.isEmpty()) {
                continue;
            }

            Order order = orderQueue.poll();

            if (order == null) {
                continue;
            }

            try {
                var tradeExecution = runMatchingAlgorithm(order);
                auditTrail.broadcastMessage(Objects.requireNonNullElse(tradeExecution, order));
            } catch (Exception e) {
                logger.warn("Unhandled exception for order: {}", order, e);
            }
        }
    }

    private Response handleOrderbookDataRequest(OrderbookDataRequest orderbookDataRequest) {
        var isAdded = orderbookCache.createOrderbook(orderbookDataRequest.orderbookData());
        referanceDataCache.addOrderbookData(orderbookDataRequest.orderbookData());
        RequestStatus status = isAdded ? RequestStatus.OK : RequestStatus.ERROR;
        String message = isAdded ? "Successfully added created orderbook" : "Could not create orderbook";
        return orderbookDataRequest.createResponse(Instant.now().toEpochMilli(), orderbookDataRequest.requestId(), status, message);
    }

    private Response handleInstrumentRequest(InstrumentRequest instrumentRequest) {
        referanceDataCache.addInstrument(instrumentRequest.instrument());
        return instrumentRequest.createResponse(Instant.now().toEpochMilli(), instrumentRequest.requestId(), RequestStatus.OK, "");
    }

    private Response handleStateChangeRequest(StateChangeRequest stateChangeRequest) {
        var stateChange = stateChangeRequest.stateChange();
        var orderbook = orderbookCache.getOrderbook(stateChange.orderbookId());
        if (orderbook == null) {
            String errorMessage = String.format("Cannot update orderbook %s state to %s does not exist.", stateChange.orderbookId(), stateChange.stateChangeType());
            logger.error(errorMessage);
            return stateChangeRequest.createResponse(Instant.now().toEpochMilli(), stateChangeRequest.requestId(), RequestStatus.ERROR, errorMessage);
        }

        var isUpdated = orderbook.updateState(stateChange.stateChangeType());
        RequestStatus status = isUpdated ? RequestStatus.OK : RequestStatus.ERROR;
        String message = isUpdated ? "Successfully updated state" : String.format("Could not update state from %s to %s.", orderbook.getState(), stateChange.stateChangeType());
        return stateChangeRequest.createResponse(Instant.now().toEpochMilli(), stateChangeRequest.requestId(), status, message);
    }

    private Response handleOrderRequest(OrderRequest orderRequest) {
        var order = orderRequest.order();
        var orderbook = orderbookCache.getOrderbook(order.orderbookId());
        if (orderbook == null) {
            String errorMessage = String.format("Orderbook %s does not exist, cannot update with order id %s.", order.orderbookId(), order.orderId());
            logger.error(errorMessage);
            return orderRequest.createResponse(Instant.now().toEpochMilli(), orderRequest.requestId(), RequestStatus.ERROR, errorMessage);
        }

        var isQueued = orderbook.isUpdating() && orderQueue.add(orderRequest.order());
        RequestStatus status = isQueued ? RequestStatus.OK : RequestStatus.ERROR;
        String message = isQueued ? "Successfully queued order." : "Could not queue order.";
        return orderRequest.createResponse(Instant.now().toEpochMilli(), orderRequest.requestId(), status, message);
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

    public int getOrderQueueSize() {
        return orderQueue.size();
    }

}
