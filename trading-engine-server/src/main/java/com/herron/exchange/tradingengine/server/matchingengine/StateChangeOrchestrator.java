package com.herron.exchange.tradingengine.server.matchingengine;

import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.TradingStatesEnum;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.trading.ImmutableStateChange;
import com.herron.exchange.common.api.common.messages.trading.StateChange;
import com.herron.exchange.tradingengine.server.TradingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.TradingStatesEnum.*;


public class StateChangeOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateChangeOrchestrator.class);
    private final TradingEngine tradingEngine;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long eventsAtSameMilli = 0;

    public StateChangeOrchestrator(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    private static long calculateInitialDelay(LocalTime targetTime) {
        LocalTime currentTime = LocalTime.now();
        if (targetTime.isBefore(currentTime)) {
            return 0;
        }

        Duration duration = Duration.between(currentTime, targetTime);
        return duration.toMillis();
    }

    public void scheduleStateChanges() {
        for (var orderbookData : ReferenceDataCache.getCache().getOrderbookData()) {
            schedulePreTrading(orderbookData);
            scheduleOpenAuctionTrading(orderbookData);
            scheduleOpenAuctionRun(orderbookData);
            scheduleContinuous(orderbookData);
            //  scheduleCloseAuctionTrading(orderbookData);
            //  scheduleCloseAuctionRun(orderbookData);
            // schedulePostTrade(orderbookData);
            // scheduleClosed(orderbookData);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
            //Ignore
        }
        LOGGER.info("Done scheduling state changes.");
    }

    private void schedulePreTrading(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().preTradingHours();
        if (tradingHours == null) {
            return;
        }

        var triggerTime = orderbookData.tradingCalendar().preTradingHours().start();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                PRE_TRADE
        );
    }

    private void scheduleOpenAuctionTrading(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().openAuctionTradingHours();
        if (tradingHours == null) {
            return;
        }
        var triggerTime = orderbookData.tradingCalendar().openAuctionTradingHours().start();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                OPEN_AUCTION_TRADING
        );
    }

    private void scheduleOpenAuctionRun(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().openAuctionTradingHours();
        if (tradingHours == null) {
            return;
        }
        var triggerTime = orderbookData.tradingCalendar().openAuctionTradingHours().end();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                OPEN_AUCTION_RUN
        );
    }

    private void scheduleContinuous(OrderbookData orderbookData) {
        var triggerTime = orderbookData.tradingCalendar().continuousTradingHours().start();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                CONTINUOUS_TRADING
        );
    }

    private void scheduleCloseAuctionTrading(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().closeAuctionTradingHours();
        if (tradingHours == null) {
            return;
        }
        var triggerTime = orderbookData.tradingCalendar().closeAuctionTradingHours().start();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                CLOSING_AUCTION_TRADING
        );
    }

    private void scheduleCloseAuctionRun(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().closeAuctionTradingHours();
        if (tradingHours == null) {
            var triggerTime = orderbookData.tradingCalendar().continuousTradingHours().end();
            scheduleStateChange(
                    calculateInitialDelay(triggerTime),
                    orderbookData.orderbookId(),
                    POST_TRADE
            );
            return;
        }
        var triggerTime = orderbookData.tradingCalendar().closeAuctionTradingHours().end();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                CLOSING_AUCTION_RUN
        );
    }

    private void schedulePostTrade(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().postTradingHours();
        if (tradingHours == null) {
            return;
        }
        var triggerTime = orderbookData.tradingCalendar().postTradingHours().start();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                POST_TRADE
        );
    }

    private void scheduleClosed(OrderbookData orderbookData) {
        var tradingHours = orderbookData.tradingCalendar().closedTradingHours();
        if (tradingHours == null) {
            return;
        }
        var triggerTime = orderbookData.tradingCalendar().closedTradingHours().start();
        scheduleStateChange(
                calculateInitialDelay(triggerTime),
                orderbookData.orderbookId(),
                CLOSED
        );
    }

    private void scheduleStateChange(long initialDelay, String orderbookId, TradingStatesEnum tradingState) {
        if (initialDelay == 0) {
            var stateChange = createStateChange(orderbookId, tradingState);
            tradingEngine.queueStateChange(stateChange);
            eventsAtSameMilli++;
            return;
        }
        scheduler.schedule(() -> {
            var stateChange = createStateChange(orderbookId, tradingState);
            tradingEngine.queueStateChange(stateChange);
        }, initialDelay, TimeUnit.MILLISECONDS);
    }

    private StateChange createStateChange(String orderbookId, TradingStatesEnum tradingState) {
        return ImmutableStateChange.builder()
                .timeOfEvent(Timestamp.from(Instant.now().toEpochMilli() + eventsAtSameMilli))
                .tradeState(tradingState)
                .orderbookId(orderbookId)
                .eventType(SYSTEM)
                .build();
    }
}
