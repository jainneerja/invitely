package com.invitely.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // -------------------------------------------------------
    // Topic definitions — created automatically on startup
    // -------------------------------------------------------

    @Bean
    public NewTopic inviteCreatedTopic() {
        return TopicBuilder.name(InviteEventProducer.TOPIC_INVITE_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic imageReadyTopic() {
        return TopicBuilder.name(InviteEventProducer.TOPIC_IMAGE_READY)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic videoReadyTopic() {
        return TopicBuilder.name(InviteEventProducer.TOPIC_VIDEO_READY)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rsvpSubmittedTopic() {
        return TopicBuilder.name(InviteEventProducer.TOPIC_RSVP_SUBMITTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // -------------------------------------------------------
    // Producer factory with JSON serialization
    // -------------------------------------------------------

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");              // strongest durability
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // exactly-once semantics
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
