package com.invitely.controller;

import com.invitely.dto.CreateInviteRequest;
import com.invitely.dto.InviteResponse;
import com.invitely.dto.RsvpDto.RsvpRequest;
import com.invitely.dto.RsvpDto.RsvpResponseDto;
import com.invitely.dto.UpdateInviteRequest;
import com.invitely.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    // -------------------------------------------------------
    // POST /api/invites
    // Create a new invitation
    // -------------------------------------------------------
    @PostMapping
    public ResponseEntity<InviteResponse> createInvite(
            @Valid @RequestBody CreateInviteRequest req) {
        InviteResponse response = inviteService.createInvite(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------
    // GET /api/invites/{slug}
    // Guest-facing: fetch invite by slug (public)
    // -------------------------------------------------------
    @GetMapping("/{slug}")
    public ResponseEntity<InviteResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(inviteService.getBySlug(slug));
    }

    // -------------------------------------------------------
    // GET /api/invites/id/{id}
    // Host-facing: fetch invite by UUID with RSVP summary
    // -------------------------------------------------------
    @GetMapping("/id/{id}")
    public ResponseEntity<InviteResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(inviteService.getById(id));
    }

    // -------------------------------------------------------
    // PATCH /api/invites/{id}
    // Update invite details (partial update)
    // -------------------------------------------------------
    @PatchMapping("/{id}")
    public ResponseEntity<InviteResponse> updateInvite(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInviteRequest req) {
        return ResponseEntity.ok(inviteService.updateInvite(id, req));
    }

    // -------------------------------------------------------
    // POST /api/invites/{id}/publish
    // Publish invite — makes it live and accessible via slug
    // -------------------------------------------------------
    @PostMapping("/{id}/publish")
    public ResponseEntity<InviteResponse> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(inviteService.publishInvite(id));
    }

    // -------------------------------------------------------
    // POST /api/invites/{id}/unpublish
    // Take invite offline without deleting
    // -------------------------------------------------------
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<InviteResponse> unpublish(@PathVariable UUID id) {
        return ResponseEntity.ok(inviteService.unpublishInvite(id));
    }

    // -------------------------------------------------------
    // DELETE /api/invites/{id}
    // Delete invite and all associated assets
    // -------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvite(@PathVariable UUID id) {
        inviteService.deleteInvite(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------
    // POST /api/invites/{slug}/rsvp
    // Guest submits RSVP (public endpoint)
    // -------------------------------------------------------
    @PostMapping("/{slug}/rsvp")
    public ResponseEntity<RsvpResponseDto> submitRsvp(
            @PathVariable String slug,
            @Valid @RequestBody RsvpRequest req) {
        RsvpResponseDto response = inviteService.submitRsvp(slug, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------
    // GET /api/invites/{id}/rsvp
    // Host fetches all RSVP responses for their invite
    // -------------------------------------------------------
    @GetMapping("/{id}/rsvp")
    public ResponseEntity<List<RsvpResponseDto>> getRsvps(@PathVariable UUID id) {
        return ResponseEntity.ok(inviteService.getRsvps(id));
    }
}
