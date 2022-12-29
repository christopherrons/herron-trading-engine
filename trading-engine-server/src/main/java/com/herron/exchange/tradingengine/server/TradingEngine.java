package com.herron.exchange.tradingengine.server;

import com.herron.exchange.common.api.common.api.Message;
import org.springframework.context.annotation.Bean;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TradingEngine {

    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    public void queueMessage(Message message) {
        messageQueue.add(message);
    }

    @Bean(initMethod = "init")
    public void init() {
        while (true) {
            Message message = messageQueue.poll();
        }
    }
}
