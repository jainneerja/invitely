package com.invitely.dto;

import com.invitely.model.Invitation.EventType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateInviteRequest {

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotBlank(message = "Host name is required")
    @Size(max = 100)
    private String hostName;

    @NotBlank(message = "Event title is required")
    @Size(max = 150)
    private String eventTitle;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDate eventDate;

    @NotNull(message = "Event time is required")
    private LocalTime eventTime;

    @Size(max = 200)
    private String venueName;

    @Size(max = 300)
    private String venueAddress;

    private String personalMessage;

    private String scenePrompt;

    private String animationStyle;

    private LocalDate rsvpDeadline;

    private Integer maxGuests;

    private UUID templateId;
}
