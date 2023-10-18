package com.herron.exchange.tradingengine.server.consumers;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.api.broadcasts.BroadcastMessage;
import com.herron.exchange.common.api.common.api.broadcasts.DataStreamState;
import com.herron.exchange.common.api.common.api.trading.orders.Order;
import com.herron.exchange.common.api.common.enums.DataStreamEnum;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaDataConsumer;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.tradingengine.server.TradingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.util.Map;


public class UserOrderDataConsumer extends KafkaDataConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserOrderDataConsumer.class);
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.USER_ORDER_DATA, 0);
    private static final PartitionKey PARTITION_ONE_KEY = new PartitionKey(KafkaTopicEnum.USER_ORDER_DATA, 1);
    private final TradingEngine tradingEngine;

    public UserOrderDataConsumer(TradingEngine tradingEngine,
                                 MessageFactory messageFactory,
                                 Map<PartitionKey, Integer> keyToMessageUpdateInterval) {
        super(messageFactory, keyToMessageUpdateInterval);
        this.tradingEngine = tradingEngine;
    }

    @KafkaListener(
            id = "trading-engine-user-order-data-0",
            topicPartitions = {
                    @TopicPartition(topic = "user-order-data", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))
            }
    )
    public void consumeOrderDataPartitionOne(ConsumerRecord<String, String> consumerRecord) {
        var broadcastMessage = deserializeBroadcast(consumerRecord, PARTITION_ZERO_KEY);
        queueMessage(broadcastMessage);
    }

    @KafkaListener(
            id = "order-data-consumer-two",
            topicPartitions = {
                    @TopicPartition(topic = "user-order-data", partitionOffsets = @PartitionOffset(partition = "1", initialOffset = "0"))
            }
    )
    public void consumeOrderDataPartitionTwo(ConsumerRecord<String, String> consumerRecord) {
        var broadcastMessage = deserializeBroadcast(consumerRecord, PARTITION_ONE_KEY);
        queueMessage(broadcastMessage);
    }

    private void queueMessage(BroadcastMessage broadcastMessage) {
        if (broadcastMessage == null) {
            return;
        }
        try {
            if (broadcastMessage.message() instanceof Order order) {
                tradingEngine.queueOrder(order);
            } else if (broadcastMessage.message() instanceof DataStreamState state) {
                if (state.state() == DataStreamEnum.DONE) {
                    LOGGER.info("Done consuming order stream");
                } else {
                    LOGGER.info("Started consuming order stream");
                }

            } else {
                LOGGER.warn("Unexpected message type {}.", broadcastMessage);
            }
        } catch (Exception e) {
            LOGGER.warn("Unhandled exception for message {}.", broadcastMessage, e);
        }
    }

}
