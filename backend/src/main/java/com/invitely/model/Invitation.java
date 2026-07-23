package com.invitely.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 12)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false,
            columnDefinition = "event_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EventType eventType;

    @Column(name = "host_name", nullable = false, length = 100)
    private String hostName;

    @Column(name = "event_title", nullable = false, length = 150)
    private String eventTitle;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "venue_address", length = 300)
    private String venueAddress;

    @Column(name = "personal_message", columnDefinition = "TEXT")
    private String personalMessage;

    // AI generation fields
    @Column(name = "scene_prompt", columnDefinition = "TEXT")
    private String scenePrompt;

    @Column(name = "generated_image_url", columnDefinition = "TEXT")
    private String generatedImageUrl;

    @Column(name = "animated_video_url", columnDefinition = "TEXT")
    private String animatedVideoUrl;

    @Column(name = "animation_style", length = 50)
    @Builder.Default
    private String animationStyle = "animals_walk";

    // When true, the AI baked the invitation text (title, date, venue) directly
    // into the generated image, so the frontend must NOT render its text overlay.
    @Column(name = "embed_text_in_image", nullable = false)
    @Builder.Default
    private boolean embedTextInImage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "invite_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private InviteStatus status = InviteStatus.DRAFT;

    @Column(name = "rsvp_deadline")
    private LocalDate rsvpDeadline;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Asset> assets = new ArrayList<>();

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RsvpResponse> rsvpResponses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Enums
    public enum EventType {
        BIRTHDAY, WEDDING, PARTY, BABY_SHOWER, GRADUATION, CORPORATE
    }

    public enum InviteStatus {
        DRAFT, IMAGE_PENDING, IMAGE_READY, VIDEO_PENDING, PUBLISHED, FAILED
    }
}
