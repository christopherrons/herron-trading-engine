package com.herron.exchange.tradingengine.server.audittrail;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.logging.EventLogger;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Queue;

public class AuditTrail {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventLogger eventLogger = new EventLogger("Outgoing");

    public AuditTrail(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleMessages(Queue<Message> messages) {
        for (var message : messages) {
            publish(message);
        }
    }

    public void publish(Message message) {
        // kafkaTemplate.send(TopicEnum.BITSTAMP_AUDIT_TRAIL.getTopicName(), message.messageType().getMessageTypeId(), message);
        System.out.println(message);
        eventLogger.logEvent();
    }
}
