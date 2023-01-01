package com.herron.exchange.tradingengine.server.audittrail;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.datastructures.TimeBoundBlockingPriorityQueue;
import com.herron.exchange.common.api.common.logging.EventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuditTrail {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrail.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventLogger eventLogger;
    private final TimeBoundBlockingPriorityQueue<Message> messageBlockingQueue = new TimeBoundBlockingPriorityQueue<>(10000);
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    private final String partitionId;

    public AuditTrail(KafkaTemplate<String, Object> kafkaTemplate, String partitionId) {
        this.kafkaTemplate = kafkaTemplate;
        this.partitionId = partitionId;
        this.eventLogger = new EventLogger(partitionId);
    }

    public void queueMessage(Message message) {
        eventLogger.logEvent();
        for (var outGoingMessage : messageBlockingQueue.addItemThenPurge(message)) {
            //kafkaTemplate.send(TopicEnum.HERRON_AUDIT_TRAIL.getTopicName(), outGoingMessage.messageType().getMessageTypeId(), message);
        }

    }
}
