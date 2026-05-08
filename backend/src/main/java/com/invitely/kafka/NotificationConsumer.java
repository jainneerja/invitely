package com.invitely.kafka;

import com.invitely.kafka.InviteEventProducer.InviteEvent;
import com.invitely.kafka.InviteEventProducer.RsvpEvent;
import com.invitely.model.Invitation;
import com.invitely.repository.InvitationRepository;
import com.invitely.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class NotificationConsumer {

    private final EmailService emailService;
    private final InvitationRepository invitationRepository;

    @Value("${invitely.base-url}")
    private String baseUrl;

    // -------------------------------------------------------
    // invite-created → send host a confirmation email
    // -------------------------------------------------------

    @KafkaListener(
            topics = InviteEventProducer.TOPIC_INVITE_CREATED,
            groupId = "notification-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInviteCreated(
            @Payload InviteEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[CONSUMER] invite-created event. inviteId={} offset={}",
                event.getInviteId(), offset);

        try {
            Invitation invite = fetchInvite(event.getInviteId());
            if (invite == null) return;

            // Only email if host has an email on file
            // (email auth will be added in a future phase — skipping for now)
            String shareUrl = baseUrl + "/i/" + event.getSlug();
            String dashboardUrl = baseUrl + "/dashboard/" + event.getInviteId();

            // Email host confirmation
            emailService.sendInviteCreatedEmail(
                    "host@placeholder.com",     // replace with real host email post-auth
                    event.getHostName(),
                    invite.getEventTitle(),
                    shareUrl,
                    event.getInviteId().toString()
            );

        } catch (Exception e) {
            // Consumers must never throw — log and move on
            log.error("[CONSUMER] Error processing invite-created. inviteId={}",
                    event.getInviteId(), e);
        }
    }

    // -------------------------------------------------------
    // invite-image-ready → notify host to review + publish
    // -------------------------------------------------------

    @KafkaListener(
            topics = InviteEventProducer.TOPIC_IMAGE_READY,
            groupId = "notification-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onImageReady(
            @Payload InviteEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[CONSUMER] invite-image-ready event. inviteId={} offset={}",
                event.getInviteId(), offset);

        try {
            Invitation invite = fetchInvite(event.getInviteId());
            if (invite == null) return;

            emailService.sendImageReadyEmail(
                    "host@placeholder.com",
                    invite.getHostName(),
                    invite.getEventTitle(),
                    event.getImageUrl(),
                    event.getInviteId().toString()
            );

        } catch (Exception e) {
            log.error("[CONSUMER] Error processing image-ready. inviteId={}",
                    event.getInviteId(), e);
        }
    }

    // -------------------------------------------------------
    // rsvp-submitted → notify host of new RSVP in real time
    // -------------------------------------------------------

    @KafkaListener(
            topics = InviteEventProducer.TOPIC_RSVP_SUBMITTED,
            groupId = "notification-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRsvpSubmitted(
            @Payload RsvpEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[CONSUMER] rsvp-submitted event. inviteId={} guest={} status={} offset={}",
                event.getInviteId(), event.getGuestName(), event.getRsvpStatus(), offset);

        try {
            Invitation invite = fetchInvite(event.getInviteId());
            if (invite == null) return;

            String dashboardUrl = baseUrl + "/dashboard/" + event.getInviteId();

            emailService.sendRsvpNotificationEmail(
                    "host@placeholder.com",
                    invite.getHostName(),
                    event.getGuestName(),
                    event.getRsvpStatus(),
                    invite.getEventTitle(),
                    dashboardUrl
            );

        } catch (Exception e) {
            log.error("[CONSUMER] Error processing rsvp-submitted. inviteId={}",
                    event.getInviteId(), e);
        }
    }

    // -------------------------------------------------------
    // invite-video-ready → notify host animation is done
    // -------------------------------------------------------

    @KafkaListener(
            topics = InviteEventProducer.TOPIC_VIDEO_READY,
            groupId = "notification-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVideoReady(
            @Payload InviteEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[CONSUMER] invite-video-ready event. inviteId={} offset={}",
                event.getInviteId(), offset);

        try {
            Invitation invite = fetchInvite(event.getInviteId());
            if (invite == null) return;

            // Reuse image-ready email — video URL replaces image URL
            emailService.sendImageReadyEmail(
                    "host@placeholder.com",
                    invite.getHostName(),
                    invite.getEventTitle() + " (animated!)",
                    event.getVideoUrl(),
                    event.getInviteId().toString()
            );

        } catch (Exception e) {
            log.error("[CONSUMER] Error processing video-ready. inviteId={}",
                    event.getInviteId(), e);
        }
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    private Invitation fetchInvite(UUID inviteId) {
        return invitationRepository.findById(inviteId).orElseGet(() -> {
            log.warn("[CONSUMER] Invite not found in DB. inviteId={}", inviteId);
            return null;
        });
    }
}
