package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.trading.OrderbookEvent;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.api.trading.statechange.StateChange;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.api.trading.trades.TradeExecution;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.wrappers.ThreadWrapper;
import com.herron.exchange.tradingengine.server.matchingengine.api.Orderbook;
import com.herron.exchange.tradingengine.server.matchingengine.cache.OrderbookCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.herron.exchange.common.api.common.enums.StateChangeTypeEnum.*;
import static java.util.concurrent.Executors.newScheduledThreadPool;

public class MatchingEngine {
    private static final PartitionKey AUDIT_TRAIL_KEY = new PartitionKey(KafkaTopicEnum.AUDIT_TRAIL, 0);
    private static final PartitionKey TRADE_DATA_KEY = new PartitionKey(KafkaTopicEnum.TRADE_DATA, 0);
    private static final PartitionKey TOP_OF_BOOK_DATA_KEY = new PartitionKey(KafkaTopicEnum.TOP_OF_BOOK_ORDER_DATA, 0);
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingEngine.class);
    private final BlockingQueue<OrderbookEvent> eventQueue = new PriorityBlockingQueue<>();
    private final OrderbookCache orderbookCache = new OrderbookCache();
    private final Thread pollThread;
    private final ScheduledExecutorService queueLoggerThread;
    private final AtomicBoolean isMatching = new AtomicBoolean(false);
    private final KafkaBroadcastHandler broadcastHandler;

    public MatchingEngine(String id, KafkaBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
        pollThread = new Thread(this::runMatching, id);
        queueLoggerThread = newScheduledThreadPool(1, new ThreadWrapper(id));
    }

    public void init() {
        isMatching.set(true);
        pollThread.start();
        queueLoggerThread.scheduleAtFixedRate(() -> LOGGER.info("Message Queue size: {}", eventQueue.size()), 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        LOGGER.info("Stopping matching engine.");
        isMatching.set(false);
        queueLoggerThread.shutdown();
    }

    public void queueMessage(OrderbookEvent orderbookEvent) {
        var orderbook = orderbookCache.getOrCreateOrderbook(orderbookEvent.orderbookId());
        if (orderbook == null) {
            LOGGER.error("Orderbook {} does not exist, queue orderbook event {}.", orderbookEvent.orderbookId(), orderbookEvent);
            return;
        }
        eventQueue.add(orderbookEvent);
    }

    private void runMatching() {
        LOGGER.info("Starting matching engine.");
        OrderbookEvent orderbookEvent;
        while (isMatching.get() || !eventQueue.isEmpty()) {

            orderbookEvent = poll();
            if (orderbookEvent == null) {
                continue;
            }

            try {
                updateOrderbook(orderbookEvent);
            } catch (Exception e) {
                LOGGER.warn("Unhandled exception for orderbookEvent: {}", orderbookEvent, e);
            }
        }
    }

    private OrderbookEvent poll() {
        try {
            return eventQueue.poll(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    private void updateOrderbook(OrderbookEvent orderbookEvent) {
        if (orderbookEvent instanceof StateChange stateChange) {
            updateState(stateChange);

        } else if (orderbookEvent instanceof Order order) {
            runMatchingAlgorithm(order);
        }
    }

    private void runMatchingAlgorithm(Order order) {
        var orderbook = orderbookCache.getOrCreateOrderbook(order.orderbookId());
        if (orderbook == null) {
            LOGGER.error("Order {} can not be added, orderbook {} does not exist.", order, order.orderbookId());
            return;
        }

        var preMatchAskPrice = orderbook.getBestAskPrice();
        var preMatchBidPrice = orderbook.getBestBidPrice();

        if (orderbook.updateOrderbook(order)) {
            broadcast(AUDIT_TRAIL_KEY, order);
            var tradeExecution = orderbook.runMatchingAlgorithm(order);
            broadcast(tradeExecution);

            var postMatchBestAskOrder = orderbook.getAskOrderIfPriceDoesNotMatch(preMatchAskPrice);
            var postMatchBestBidOrder = orderbook.getBidOrderIfPriceDoesNotMatch(preMatchBidPrice);
            broadcastTopOfBook(postMatchBestAskOrder);
            broadcastTopOfBook(postMatchBestBidOrder);
        }

    }

    private void updateState(StateChange stateChange) {
        var orderbook = orderbookCache.getOrCreateOrderbook(stateChange.orderbookId());
        if (orderbook == null) {
            String errorMessage = String.format("Cannot update orderbook %s state to %s does not exist.", stateChange.orderbookId(), stateChange.stateChangeType());
            LOGGER.error(errorMessage);
            return;
        }

        if (orderbook.updateState(stateChange.stateChangeType())) {
            broadcast(AUDIT_TRAIL_KEY, stateChange);

            if (stateChange.stateChangeType() == OPEN_AUCTION_RUN) {
                runAuction(orderbook);
                orderbook.updateState(CONTINUOUS_TRADING);

            } else if (stateChange.stateChangeType() == CLOSING_AUCTION_RUN) {
                runAuction(orderbook);
                orderbook.updateState(POST_TRADE);
            }
        }
    }

    private void runAuction(Orderbook orderbook) {
        var preMatchAskPrice = orderbook.getBestAskPrice();
        var preMatchBidPrice = orderbook.getBestBidPrice();

        var tradeExecution = orderbook.runAuctionAlgorithm();
        broadcast(tradeExecution);

        var postMatchBestAskOrder = orderbook.getAskOrderIfPriceDoesNotMatch(preMatchAskPrice);
        var postMatchBestBidOrder = orderbook.getBidOrderIfPriceDoesNotMatch(preMatchBidPrice);
        broadcastTopOfBook(postMatchBestAskOrder);
        broadcastTopOfBook(postMatchBestBidOrder);
    }

    private void broadcastTopOfBook(Order order) {
        if (order == null) {
            return;
        }
        broadcast(TOP_OF_BOOK_DATA_KEY, order);
    }

    private void broadcast(TradeExecution tradeExecution) {
        if (tradeExecution != null) {
            broadcast(AUDIT_TRAIL_KEY, tradeExecution);
            tradeExecution.messages().stream().filter(Trade.class::isInstance).forEach(t -> broadcast(TRADE_DATA_KEY, t));
        }
    }

    private void broadcast(PartitionKey partitionKey, OrderbookEvent orderbookEvent) {
        if (orderbookEvent == null) {
            return;
        }
        broadcastHandler.broadcastMessage(partitionKey, orderbookEvent);
    }
}
