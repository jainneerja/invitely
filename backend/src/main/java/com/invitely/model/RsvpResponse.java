package com.invitely.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rsvp_responses")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RsvpResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_id", nullable = false)
    private Invitation invitation;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "guest_email", length = 200)
    private String guestEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "rsvp_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RsvpStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "guest_count", nullable = false)
    @Builder.Default
    private Integer guestCount = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum RsvpStatus {
        ATTENDING, NOT_ATTENDING, MAYBE
    }
}
