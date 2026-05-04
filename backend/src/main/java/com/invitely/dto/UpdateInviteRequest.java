package com.invitely.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class UpdateInviteRequest {

    @Size(max = 100)
    private String hostName;

    @Size(max = 150)
    private String eventTitle;

    @Future(message = "Event date must be in the future")
    private LocalDate eventDate;

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
}
