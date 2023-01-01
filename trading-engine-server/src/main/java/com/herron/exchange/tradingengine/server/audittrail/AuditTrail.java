package com.herron.exchange.tradingengine.server.audittrail;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.logging.EventLogger;
import com.herron.exchange.common.api.common.messages.herron.HerronBroadcastMessage;
import com.herron.exchange.common.api.common.model.PartitionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class AuditTrail {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrail.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventLogger eventLogger;
    private final PartitionKey partitionKey;
    private final AtomicLong sequenceNumberHandler = new AtomicLong();

    public AuditTrail(KafkaTemplate<String, Object> kafkaTemplate, PartitionKey partitionKey) {
        this.kafkaTemplate = kafkaTemplate;
        this.partitionKey = partitionKey;
        this.eventLogger = new EventLogger(String.valueOf(partitionKey.partitionId()));
    }

    public void queueMessage(Message message) {
        eventLogger.logEvent();
        var broadCast = new HerronBroadcastMessage(message.serialize(), message.messageType(), sequenceNumberHandler.getAndIncrement(), Instant.now().toEpochMilli());
        //kafkaTemplate.send(TopicEnum.HERRON_AUDIT_TRAIL.getTopicName(), outGoingMessage.messageType().getMessageTypeId(), message);
    }
}
