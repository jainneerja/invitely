package com.invitely.dto;

import com.invitely.model.RsvpResponse.RsvpStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RsvpDto {

    @Data
    public static class RsvpRequest {

        @NotBlank(message = "Guest name is required")
        @Size(max = 100)
        private String guestName;

        @Email(message = "Invalid email address")
        @Size(max = 200)
        private String guestEmail;

        @NotNull(message = "RSVP status is required")
        private RsvpStatus status;

        private String message;

        @Min(value = 1, message = "Guest count must be at least 1")
        @Max(value = 20, message = "Guest count cannot exceed 20")
        private Integer guestCount = 1;
    }

    @Data
    @Builder
    public static class RsvpResponseDto {
        private UUID id;
        private String guestName;
        private String guestEmail;
        private RsvpStatus status;
        private String message;
        private Integer guestCount;
        private OffsetDateTime createdAt;
    }
}
