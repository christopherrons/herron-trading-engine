package com.herron.exchange.tradingengine.server.audittrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogger.class);
    private static final int MESSAGE_UPDATE_INTERVAL = 1000;
    private final AtomicLong totalNrOfEvents = new AtomicLong();
    private final Instant startTime = Instant.now();
    private Instant lastLogUpdateTime = Instant.now();
    private AtomicLong lastUpdateTimeNrOfEvents = new AtomicLong();

    private final String eventLoggDescription;

    public AuditLogger() {
        this("");
    }

    public AuditLogger(String eventLoggDescription) {
        this.eventLoggDescription = eventLoggDescription;
    }

    public void logEvent() {
        try {
            if (totalNrOfEvents.incrementAndGet() % MESSAGE_UPDATE_INTERVAL == 0) {
                Instant currentTime = Instant.now();
                LOGGER.info("{}: Messages received: {}. Current event rate {}/s, average event rate {}/s",
                        eventLoggDescription, totalNrOfEvents.get(), getCurrentEventsPerSecond(currentTime), getAverageEventsPerSecond(currentTime));
                lastLogUpdateTime = currentTime;
                lastUpdateTimeNrOfEvents = new AtomicLong(totalNrOfEvents.get());
            }
        } catch (ArithmeticException ignore) {
        }

    }

    private long getCurrentEventsPerSecond(final Instant currentTime) {
        return (totalNrOfEvents.get() - lastUpdateTimeNrOfEvents.get()) / currentTime.minusMillis((lastLogUpdateTime.toEpochMilli())).getEpochSecond();
    }

    private long getAverageEventsPerSecond(final Instant currentTime) {
        return totalNrOfEvents.get() / currentTime.minusMillis((startTime.toEpochMilli())).getEpochSecond();
    }
}
