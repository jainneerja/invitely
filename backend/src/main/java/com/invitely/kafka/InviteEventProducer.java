package com.invitely.kafka;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kafka event producer — fires events on invite lifecycle.
 * When kafka.enabled=false a no-op stub is used so the app
 * starts without Kafka running.
 */
public interface InviteEventProducer {

    String TOPIC_INVITE_CREATED = "invite-created";
    String TOPIC_IMAGE_READY    = "invite-image-ready";
    String TOPIC_VIDEO_READY    = "invite-video-ready";
    String TOPIC_RSVP_SUBMITTED = "rsvp-submitted";

    void inviteCreated(UUID inviteId, String slug, String hostName);
    void imageReady(UUID inviteId, String slug, String imageUrl);
    void videoReady(UUID inviteId, String slug, String videoUrl);
    void rsvpSubmitted(UUID inviteId, String slug, String guestName, String rsvpStatus);

    // -------------------------------------------------------
    // Real Kafka implementation — active when kafka.enabled=true
    // -------------------------------------------------------

    @Component
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
    @Slf4j
    class KafkaInviteEventProducer implements InviteEventProducer {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        public KafkaInviteEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void inviteCreated(UUID inviteId, String slug, String hostName) {
            send(TOPIC_INVITE_CREATED, inviteId.toString(), InviteEvent.builder()
                    .inviteId(inviteId).slug(slug).hostName(hostName)
                    .eventType("INVITE_CREATED").occurredAt(OffsetDateTime.now()).build());
        }

        public void imageReady(UUID inviteId, String slug, String imageUrl) {
            send(TOPIC_IMAGE_READY, inviteId.toString(), InviteEvent.builder()
                    .inviteId(inviteId).slug(slug).imageUrl(imageUrl)
                    .eventType("IMAGE_READY").occurredAt(OffsetDateTime.now()).build());
        }

        public void videoReady(UUID inviteId, String slug, String videoUrl) {
            send(TOPIC_VIDEO_READY, inviteId.toString(), InviteEvent.builder()
                    .inviteId(inviteId).slug(slug).videoUrl(videoUrl)
                    .eventType("VIDEO_READY").occurredAt(OffsetDateTime.now()).build());
        }

        public void rsvpSubmitted(UUID inviteId, String slug,
                                   String guestName, String rsvpStatus) {
            send(TOPIC_RSVP_SUBMITTED, inviteId.toString(), RsvpEvent.builder()
                    .inviteId(inviteId).slug(slug).guestName(guestName)
                    .rsvpStatus(rsvpStatus).occurredAt(OffsetDateTime.now()).build());
        }

        private void send(String topic, String key, Object payload) {
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka send failed. topic={} key={}", topic, key, ex);
                        } else {
                            log.debug("Kafka event sent. topic={} offset={}",
                                    topic, result.getRecordMetadata().offset());
                        }
                    });
        }
    }

    // -------------------------------------------------------
    // No-op stub — active when kafka.enabled=false (default)
    // Logs events instead of sending to Kafka
    // -------------------------------------------------------

    @Component
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "false", matchIfMissing = true)
    @Slf4j
    class StubInviteEventProducer implements InviteEventProducer {

        public void inviteCreated(UUID inviteId, String slug, String hostName) {
            log.info("[KAFKA-STUB] invite-created inviteId={} slug={}", inviteId, slug);
        }

        public void imageReady(UUID inviteId, String slug, String imageUrl) {
            log.info("[KAFKA-STUB] image-ready inviteId={} slug={}", inviteId, slug);
        }

        public void videoReady(UUID inviteId, String slug, String videoUrl) {
            log.info("[KAFKA-STUB] video-ready inviteId={} slug={}", inviteId, slug);
        }

        public void rsvpSubmitted(UUID inviteId, String slug,
                                   String guestName, String rsvpStatus) {
            log.info("[KAFKA-STUB] rsvp-submitted inviteId={} guest={} status={}",
                    inviteId, guestName, rsvpStatus);
        }
    }

    // -------------------------------------------------------
    // Event payloads
    // -------------------------------------------------------

    @Data @Builder
    class InviteEvent {
        private UUID inviteId;
        private String slug;
        private String hostName;
        private String imageUrl;
        private String videoUrl;
        private String eventType;
        private OffsetDateTime occurredAt;
    }

    @Data @Builder
    class RsvpEvent {
        private UUID inviteId;
        private String slug;
        private String guestName;
        private String rsvpStatus;
        private OffsetDateTime occurredAt;
    }
}
