package com.herron.exchange.tradingengine.server.audittrail;

import com.herron.exchange.common.api.common.api.Message;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Queue;

public class AuditTrail {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuditLogger auditLogger = new AuditLogger();

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
        auditLogger.logEvent();
    }
}
