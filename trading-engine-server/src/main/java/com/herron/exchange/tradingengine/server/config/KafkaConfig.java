package com.herron.exchange.tradingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastProducer;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.kafka.model.KafkaSubscriptionDetails;
import com.herron.exchange.common.api.common.logging.EventLogger;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class KafkaConfig {
    private static final String GROUP_ID = "trading-engine";

    @Bean
    public KafkaBroadcastHandler kafkaBroadcastHandler(KafkaTemplate<String, Object> kafkaTemplate, KafkaConfig.KafkaProducerConfig config) {
        return new KafkaBroadcastHandler(kafkaTemplate, config.createBroadcastProducer(kafkaTemplate));
    }

    @Bean
    public KafkaConsumerClient kafkaConsumerClient(MessageFactory messageFactory, ConsumerFactory<String, String> consumerFactor) {
        return new KafkaConsumerClient(messageFactory, consumerFactor);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                                                           @Value("${kafka.producer.properties.max-request-size}") String maxRequestSize) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic auditTrailTopic(@Value("${kafka.producer.topic.audit-trail.nr-of-partitions:1}") int nrOfPartitions,
                                    @Value("${kafka.producer.topic.audit-trail.max-message-bytes}") String maxMessageBytes) {
        return TopicBuilder
                .name(KafkaTopicEnum.AUDIT_TRAIL.getTopicName())
                .partitions(nrOfPartitions)
                .config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, maxMessageBytes)
                .build();
    }

    @Bean
    public NewTopic tradeDataTopic(@Value("${kafka.producer.topic.trade-data.nr-of-partitions:1}") int nrOfPartitions,
                                   @Value("${kafka.producer.topic.trade-data.max-message-bytes}") String maxMessageBytes) {
        return TopicBuilder
                .name(KafkaTopicEnum.TRADE_DATA.getTopicName())
                .partitions(nrOfPartitions)
                .config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, maxMessageBytes)
                .build();
    }

    @Bean
    public NewTopic topOfOrderBookDataTopic(@Value("${kafka.producer.topic.top-of-book-quote.nr-of-partitions:1}") int nrOfPartitions,
                                            @Value("${kafka.producer.topic.top-of-book-quote.max-message-bytes}") String maxMessageBytes) {
        return TopicBuilder
                .name(KafkaTopicEnum.TOP_OF_BOOK_QUOTE.getTopicName())
                .partitions(nrOfPartitions)
                .config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, maxMessageBytes)
                .build();
    }

    @Component
    @ConfigurationProperties(prefix = "kafka.producer.broadcast")
    public static class KafkaProducerConfig {

        private List<KafkaTopicConfig> config;

        public List<KafkaTopicConfig> getConfig() {
            return config;
        }

        public void setConfig(List<KafkaTopicConfig> config) {
            this.config = config;
        }

        Map<PartitionKey, KafkaBroadcastProducer> createBroadcastProducer(KafkaTemplate<String, Object> kafkaTemplate) {
            return config.stream()
                    .map(c -> {
                        var pk = new PartitionKey(KafkaTopicEnum.fromValue(c.topic()), c.partition());
                        return new KafkaBroadcastProducer(pk, kafkaTemplate, new EventLogger(pk.toString(), c.eventLogging));
                    })
                    .collect(Collectors.toMap(KafkaBroadcastProducer::getPartitionKey, k -> k));
        }

        public record KafkaTopicConfig(int partition,
                                       int eventLogging,
                                       String topic) {
        }
    }

    @Component
    @ConfigurationProperties(prefix = "kafka.consumer")
    public static class KafkaConsumerConfig {

        private List<KafkaTopicConfig> config;

        public List<KafkaTopicConfig> getConfig() {
            return config;
        }

        public void setConfig(List<KafkaTopicConfig> config) {
            this.config = config;
        }

        List<KafkaSubscriptionDetails> getDetails(KafkaTopicEnum topicEnum) {
            return config.stream()
                    .filter(c -> c.topic.equals(topicEnum.getTopicName()))
                    .map(c -> new KafkaSubscriptionDetails(GROUP_ID, new PartitionKey(topicEnum, c.partition), c.offset, c.eventLogging))
                    .toList();
        }

        public record KafkaTopicConfig(int offset,
                                       int partition,
                                       int eventLogging,
                                       String topic) {
        }
    }
}
