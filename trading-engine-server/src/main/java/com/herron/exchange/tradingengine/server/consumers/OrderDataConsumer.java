package com.herron.exchange.tradingengine.server.consumers;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.api.broadcasts.BroadcastMessage;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.DataConsumer;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.tradingengine.server.TradingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;


public class OrderDataConsumer extends DataConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDataConsumer.class);
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.ORDER_DATA, 0);
    private static final PartitionKey PARTITION_ONE_KEY = new PartitionKey(KafkaTopicEnum.ORDER_DATA, 1);
    private final TradingEngine tradingEngine;

    public OrderDataConsumer(TradingEngine tradingEngine, MessageFactory messageFactory) {
        super(messageFactory);
        this.tradingEngine = tradingEngine;
    }

    @KafkaListener(
            id = "order-data-consumer-one",
            topicPartitions = {
                    @TopicPartition(topic = "order-data", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))
            }
    )
    public void consumeOrderDataPartitionOne(ConsumerRecord<String, String> consumerRecord) {
        var broadcastMessage = deserializeBroadcast(consumerRecord, PARTITION_ZERO_KEY);
        queueMessage(broadcastMessage);
    }

    @KafkaListener(
            id = "order-data-consumer-two",
            topicPartitions = {
                    @TopicPartition(topic = "order-data", partitionOffsets = @PartitionOffset(partition = "1", initialOffset = "0"))
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
            tradingEngine.queueMessage(broadcastMessage);
        } catch (Exception e) {
            LOGGER.warn("Unhandled exception for message {}.", broadcastMessage, e);
        }
    }

}
