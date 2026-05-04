package com.invitely.dto;

import com.invitely.model.Invitation.EventType;
import com.invitely.model.Invitation.InviteStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class InviteResponse {

    private UUID id;
    private String slug;
    private String shareUrl;            // full shareable link e.g. https://invitely.app/i/abc123

    // event info
    private EventType eventType;
    private String hostName;
    private String eventTitle;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String venueName;
    private String venueAddress;
    private String personalMessage;

    // AI generation
    private String scenePrompt;
    private String generatedImageUrl;
    private String animatedVideoUrl;
    private String animationStyle;

    // config
    private InviteStatus status;
    private LocalDate rsvpDeadline;
    private Integer maxGuests;
    private UUID templateId;

    // meta
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime publishedAt;

    // RSVP summary (populated on host view)
    private RsvpSummaryDto rsvpSummary;

    @Data
    @Builder
    public static class RsvpSummaryDto {
        private long totalResponses;
        private long attending;
        private long notAttending;
        private long maybe;
        private long totalGuests;
    }
}
