package com.invitely.kafka;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InviteEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_INVITE_CREATED  = "invite-created";
    public static final String TOPIC_IMAGE_READY     = "invite-image-ready";
    public static final String TOPIC_VIDEO_READY     = "invite-video-ready";
    public static final String TOPIC_RSVP_SUBMITTED  = "rsvp-submitted";

    public void inviteCreated(UUID inviteId, String slug, String hostName) {
        InviteEvent event = InviteEvent.builder()
                .inviteId(inviteId)
                .slug(slug)
                .hostName(hostName)
                .eventType("INVITE_CREATED")
                .occurredAt(OffsetDateTime.now())
                .build();
        send(TOPIC_INVITE_CREATED, inviteId.toString(), event);
    }

    public void imageReady(UUID inviteId, String slug, String imageUrl) {
        InviteEvent event = InviteEvent.builder()
                .inviteId(inviteId)
                .slug(slug)
                .imageUrl(imageUrl)
                .eventType("IMAGE_READY")
                .occurredAt(OffsetDateTime.now())
                .build();
        send(TOPIC_IMAGE_READY, inviteId.toString(), event);
    }

    public void videoReady(UUID inviteId, String slug, String videoUrl) {
        InviteEvent event = InviteEvent.builder()
                .inviteId(inviteId)
                .slug(slug)
                .videoUrl(videoUrl)
                .eventType("VIDEO_READY")
                .occurredAt(OffsetDateTime.now())
                .build();
        send(TOPIC_VIDEO_READY, inviteId.toString(), event);
    }

    public void rsvpSubmitted(UUID inviteId, String slug,
                               String guestName, String rsvpStatus) {
        RsvpEvent event = RsvpEvent.builder()
                .inviteId(inviteId)
                .slug(slug)
                .guestName(guestName)
                .rsvpStatus(rsvpStatus)
                .occurredAt(OffsetDateTime.now())
                .build();
        send(TOPIC_RSVP_SUBMITTED, inviteId.toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send Kafka event. topic={} key={}", topic, key, ex);
                    } else {
                        log.debug("Kafka event sent. topic={} key={} offset={}",
                                topic, key,
                                result.getRecordMetadata().offset());
                    }
                });
    }

    // -------------------------------------------------------
    // Event payloads
    // -------------------------------------------------------

    @Data @Builder
    public static class InviteEvent {
        private UUID inviteId;
        private String slug;
        private String hostName;
        private String imageUrl;
        private String videoUrl;
        private String eventType;
        private OffsetDateTime occurredAt;
    }

    @Data @Builder
    public static class RsvpEvent {
        private UUID inviteId;
        private String slug;
        private String guestName;
        private String rsvpStatus;
        private OffsetDateTime occurredAt;
    }
}
