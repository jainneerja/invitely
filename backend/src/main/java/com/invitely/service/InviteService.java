package com.invitely.service;

import com.invitely.dto.CreateInviteRequest;
import com.invitely.dto.InviteResponse;
import com.invitely.dto.RsvpDto.RsvpRequest;
import com.invitely.dto.RsvpDto.RsvpResponseDto;
import com.invitely.dto.UpdateInviteRequest;
import com.invitely.model.Invitation;
import com.invitely.model.Invitation.InviteStatus;
import com.invitely.model.RsvpResponse;
import com.invitely.model.Template;
import com.invitely.repository.InvitationRepository;
import com.invitely.repository.RsvpRepository;
import com.invitely.repository.TemplateRepository;
import com.invitely.kafka.InviteEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteService {

    private final InvitationRepository invitationRepository;
    private final RsvpRepository rsvpRepository;
    private final TemplateRepository templateRepository;
    private final InviteEventProducer eventProducer;

    @Value("${invitely.base-url}")
    private String baseUrl;

    @Value("${invitely.slug-length:8}")
    private int slugLength;

    private static final String SLUG_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    // -------------------------------------------------------
    // Create
    // -------------------------------------------------------

    @Transactional
    public InviteResponse createInvite(CreateInviteRequest req) {
        Template template = resolveTemplate(req.getTemplateId(), req.getEventType());

        Invitation invite = Invitation.builder()
                .slug(generateUniqueSlug())
                .eventType(req.getEventType())
                .hostName(req.getHostName())
                .eventTitle(req.getEventTitle())
                .eventDate(req.getEventDate())
                .eventTime(req.getEventTime())
                .venueName(req.getVenueName())
                .venueAddress(req.getVenueAddress())
                .personalMessage(req.getPersonalMessage())
                .scenePrompt(req.getScenePrompt())
                .animationStyle(req.getAnimationStyle() != null
                        ? req.getAnimationStyle() : "animals_walk")
                .template(template)
                .rsvpDeadline(req.getRsvpDeadline())
                .maxGuests(req.getMaxGuests())
                .status(InviteStatus.DRAFT)
                .build();

        invite = invitationRepository.save(invite);
        log.info("Created invitation id={} slug={}", invite.getId(), invite.getSlug());
        eventProducer.inviteCreated(invite.getId(), invite.getSlug(), invite.getHostName());
        return toResponse(invite);
    }

    // -------------------------------------------------------
    // Read
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public InviteResponse getBySlug(String slug) {
        Invitation invite = invitationRepository.findBySlugWithAssets(slug)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + slug));
        return toResponse(invite);
    }

    @Transactional(readOnly = true)
    public InviteResponse getById(UUID id) {
        Invitation invite = invitationRepository.findByIdWithTemplate(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + id));
        return toResponseWithRsvpSummary(invite);
    }

    // -------------------------------------------------------
    // Update
    // -------------------------------------------------------

    @Transactional
    public InviteResponse updateInvite(UUID id, UpdateInviteRequest req) {
        Invitation invite = invitationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + id));

        if (invite.getStatus() == InviteStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Cannot edit a published invitation. Unpublish it first.");
        }

        if (req.getHostName() != null)       invite.setHostName(req.getHostName());
        if (req.getEventTitle() != null)     invite.setEventTitle(req.getEventTitle());
        if (req.getEventDate() != null)      invite.setEventDate(req.getEventDate());
        if (req.getEventTime() != null)      invite.setEventTime(req.getEventTime());
        if (req.getVenueName() != null)      invite.setVenueName(req.getVenueName());
        if (req.getVenueAddress() != null)   invite.setVenueAddress(req.getVenueAddress());
        if (req.getPersonalMessage() != null) invite.setPersonalMessage(req.getPersonalMessage());
        if (req.getScenePrompt() != null)    invite.setScenePrompt(req.getScenePrompt());
        if (req.getAnimationStyle() != null) invite.setAnimationStyle(req.getAnimationStyle());
        if (req.getRsvpDeadline() != null)   invite.setRsvpDeadline(req.getRsvpDeadline());
        if (req.getMaxGuests() != null)      invite.setMaxGuests(req.getMaxGuests());

        invite = invitationRepository.save(invite);
        log.info("Updated invitation id={}", invite.getId());
        return toResponse(invite);
    }

    // -------------------------------------------------------
    // Publish / Unpublish
    // -------------------------------------------------------

    @Transactional
    public InviteResponse publishInvite(UUID id) {
        Invitation invite = invitationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + id));

        if (invite.getStatus() != InviteStatus.IMAGE_READY
                && invite.getStatus() != InviteStatus.VIDEO_PENDING
                && invite.getGeneratedImageUrl() == null) {
            throw new IllegalStateException(
                    "Invite must have a generated image before publishing.");
        }

        invite.setStatus(InviteStatus.PUBLISHED);
        invite.setPublishedAt(OffsetDateTime.now());
        invite = invitationRepository.save(invite);
        log.info("Published invitation id={} slug={}", invite.getId(), invite.getSlug());
        return toResponse(invite);
    }

    @Transactional
    public InviteResponse unpublishInvite(UUID id) {
        Invitation invite = invitationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + id));
        invite.setStatus(InviteStatus.IMAGE_READY);
        invite.setPublishedAt(null);
        invite = invitationRepository.save(invite);
        return toResponse(invite);
    }

    // -------------------------------------------------------
    // Delete
    // -------------------------------------------------------

    @Transactional
    public void deleteInvite(UUID id) {
        if (!invitationRepository.existsById(id)) {
            throw new NoSuchElementException("Invitation not found: " + id);
        }
        invitationRepository.deleteById(id);
        log.info("Deleted invitation id={}", id);
    }

    // -------------------------------------------------------
    // RSVP
    // -------------------------------------------------------

    @Transactional
    public RsvpResponseDto submitRsvp(String slug, RsvpRequest req) {
        Invitation invite = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + slug));

        if (invite.getStatus() != InviteStatus.PUBLISHED) {
            throw new IllegalStateException("This invitation is not accepting RSVPs yet.");
        }

        if (invite.getRsvpDeadline() != null
                && invite.getRsvpDeadline().isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("RSVP deadline has passed.");
        }

        // prevent duplicate email RSVPs
        if (req.getGuestEmail() != null
                && rsvpRepository.existsByInvitationIdAndGuestEmail(
                        invite.getId(), req.getGuestEmail())) {
            throw new IllegalStateException(
                    "An RSVP has already been submitted for this email.");
        }

        RsvpResponse rsvp = RsvpResponse.builder()
                .invitation(invite)
                .guestName(req.getGuestName())
                .guestEmail(req.getGuestEmail())
                .status(req.getStatus())
                .message(req.getMessage())
                .guestCount(req.getGuestCount() != null ? req.getGuestCount() : 1)
                .build();

        rsvp = rsvpRepository.save(rsvp);
        eventProducer.rsvpSubmitted(invite.getId(), slug, req.getGuestName(), req.getStatus().name());
        log.info("RSVP submitted invite={} guest={} status={}",
                slug, req.getGuestName(), req.getStatus());
        return toRsvpDto(rsvp);
    }

    @Transactional(readOnly = true)
    public List<RsvpResponseDto> getRsvps(UUID inviteId) {
        return rsvpRepository.findByInvitationIdOrderByCreatedAtDesc(inviteId)
                .stream()
                .map(this::toRsvpDto)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------

    private String generateUniqueSlug() {
        String slug;
        int attempts = 0;
        do {
            slug = randomSlug();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Could not generate unique slug");
            }
        } while (invitationRepository.existsBySlug(slug));
        return slug;
    }

    private String randomSlug() {
        StringBuilder sb = new StringBuilder(slugLength);
        for (int i = 0; i < slugLength; i++) {
            sb.append(SLUG_ALPHABET.charAt(RANDOM.nextInt(SLUG_ALPHABET.length())));
        }
        return sb.toString();
    }

    private Template resolveTemplate(UUID templateId, Invitation.EventType eventType) {
        if (templateId != null) {
            return templateRepository.findById(templateId).orElse(null);
        }
        // pick first active template matching event type
        return templateRepository
                .findByIsActiveTrueAndEventTypeOrderBySortOrder(eventType)
                .stream().findFirst().orElse(null);
    }

    private InviteResponse toResponse(Invitation invite) {
        return InviteResponse.builder()
                .id(invite.getId())
                .slug(invite.getSlug())
                .shareUrl(baseUrl + "/i/" + invite.getSlug())
                .eventType(invite.getEventType())
                .hostName(invite.getHostName())
                .eventTitle(invite.getEventTitle())
                .eventDate(invite.getEventDate())
                .eventTime(invite.getEventTime())
                .venueName(invite.getVenueName())
                .venueAddress(invite.getVenueAddress())
                .personalMessage(invite.getPersonalMessage())
                .scenePrompt(invite.getScenePrompt())
                .generatedImageUrl(invite.getGeneratedImageUrl())
                .animatedVideoUrl(invite.getAnimatedVideoUrl())
                .animationStyle(invite.getAnimationStyle())
                .status(invite.getStatus())
                .rsvpDeadline(invite.getRsvpDeadline())
                .maxGuests(invite.getMaxGuests())
                .templateId(invite.getTemplate() != null
                        ? invite.getTemplate().getId() : null)
                .createdAt(invite.getCreatedAt())
                .updatedAt(invite.getUpdatedAt())
                .publishedAt(invite.getPublishedAt())
                .build();
    }

    private InviteResponse toResponseWithRsvpSummary(Invitation invite) {
        InviteResponse response = toResponse(invite);
        List<Object[]> raw = rsvpRepository.getRsvpSummaryRaw(invite.getId());

        long attending = 0, notAttending = 0, maybe = 0, totalGuests = 0;
        for (Object[] row : raw) {
            RsvpResponse.RsvpStatus status = (RsvpResponse.RsvpStatus) row[0];
            long count = ((Number) row[1]).longValue();
            long guests = ((Number) row[2]).longValue();
            switch (status) {
                case ATTENDING -> { attending = count; totalGuests = guests; }
                case NOT_ATTENDING -> notAttending = count;
                case MAYBE -> maybe = count;
            }
        }

        response.setRsvpSummary(InviteResponse.RsvpSummaryDto.builder()
                .totalResponses(attending + notAttending + maybe)
                .attending(attending)
                .notAttending(notAttending)
                .maybe(maybe)
                .totalGuests(totalGuests)
                .build());
        return response;
    }

    private RsvpResponseDto toRsvpDto(RsvpResponse rsvp) {
        return RsvpResponseDto.builder()
                .id(rsvp.getId())
                .guestName(rsvp.getGuestName())
                .guestEmail(rsvp.getGuestEmail())
                .status(rsvp.getStatus())
                .message(rsvp.getMessage())
                .guestCount(rsvp.getGuestCount())
                .createdAt(rsvp.getCreatedAt())
                .build();
    }
}
