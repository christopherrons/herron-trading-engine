package com.herron.exchange.tradingengine.server.audittrail;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.logging.EventLogger;
import com.herron.exchange.common.api.common.messages.HerronBroadcastMessage;
import com.herron.exchange.common.api.common.model.PartitionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class AuditTrail {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrail.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventLogger eventLogger = new EventLogger(5000);
    private final PartitionKey partitionKey;
    private final AtomicLong sequenceNumberHandler = new AtomicLong(1);

    public AuditTrail(KafkaTemplate<String, Object> kafkaTemplate, PartitionKey partitionKey) {
        this.kafkaTemplate = kafkaTemplate;
        this.partitionKey = partitionKey;
    }

    public synchronized void broadcastMessage(Message message) {
        if (message == null) {
            return;
        }
        eventLogger.logEvent();
        var broadCast = new HerronBroadcastMessage(message, message.messageType().getMessageTypeId(), sequenceNumberHandler.getAndIncrement(), Instant.now().toEpochMilli());
        kafkaTemplate.send(partitionKey.topicEnum().getTopicName(), partitionKey.partitionId(), broadCast.messageType().getMessageTypeId(), broadCast);
    }
}
