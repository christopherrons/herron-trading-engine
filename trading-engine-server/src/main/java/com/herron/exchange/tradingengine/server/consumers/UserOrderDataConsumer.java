package com.herron.exchange.tradingengine.server.consumers;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.kafka.KafkaMessageHandler;
import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.consumer.DataConsumer;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.kafka.model.KafkaSubscriptionDetails;
import com.herron.exchange.common.api.common.kafka.model.KafkaSubscriptionRequest;
import com.herron.exchange.common.api.common.messages.BroadcastMessage;
import com.herron.exchange.common.api.common.messages.common.DataStreamState;
import com.herron.exchange.tradingengine.server.TradingEngine;

import java.util.List;
import java.util.concurrent.CountDownLatch;


public class UserOrderDataConsumer extends DataConsumer implements KafkaMessageHandler {
    private final TradingEngine tradingEngine;
    private final KafkaConsumerClient consumerClient;
    private final List<KafkaSubscriptionRequest> requests;

    public UserOrderDataConsumer(TradingEngine tradingEngine, KafkaConsumerClient consumerClient, List<KafkaSubscriptionDetails> subscriptionDetails) {
        super("User-Order-Data", new CountDownLatch(subscriptionDetails.size()));
        this.tradingEngine = tradingEngine;
        this.consumerClient = consumerClient;
        this.requests = subscriptionDetails.stream().map(d -> new KafkaSubscriptionRequest(d, this)).toList();
    }

    @Override
    protected void consumerInit() {
        requests.forEach(consumerClient::subscribeToBroadcastTopic);
    }

    @Override
    public void onMessage(BroadcastMessage broadcastMessage) {
        Message message = broadcastMessage.message();

        if (broadcastMessage.message() instanceof Order order) {
            tradingEngine.queueOrder(order);

        } else if (message instanceof DataStreamState state) {
            switch (state.state()) {
                case START -> logger.info("Started user order data.");
                case DONE -> {
                    consumerClient.stop(broadcastMessage.partitionKey());
                    countDownLatch.countDown();
                    if (countDownLatch.getCount() == 0) {
                        consumerComplete();
                    }
                }
            }
        }
    }
}
