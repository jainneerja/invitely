package com.invitely.controller;

import com.invitely.service.AIService;
import com.invitely.service.VideoService;
import com.invitely.dto.GenerateImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;
    private final VideoService videoService;

    // -------------------------------------------------------
    // POST /api/invites/{id}/generate-image
    // Triggers async Gemini image generation.
    // Returns 202 Accepted immediately — frontend polls status.
    // -------------------------------------------------------
    @PostMapping("/{id}/generate-image")
    public ResponseEntity<Map<String, String>> generateImage(
            @PathVariable UUID id,
            @RequestBody(required = false) GenerateImageRequest req) {
        boolean embedInvitationText = req != null && Boolean.TRUE.equals(req.getEmbedInvitationText());
        log.info("Image generation requested for invite={}", id);
        aiService.generateInviteImage(id, embedInvitationText);   // @Async — returns immediately
        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "Image generation started",
                        "inviteId", id.toString(),
                        "mode", embedInvitationText ? "EMBED_TEXT" : "SCENE_ONLY",
                        "pollUrl", "/api/invites/id/" + id
                ));
    }

    // -------------------------------------------------------
    // POST /api/invites/{id}/animate
    // Triggers async video animation using the generated image.
    // Returns 202 Accepted — frontend polls status.
    // -------------------------------------------------------
    @PostMapping("/{id}/animate")
    public ResponseEntity<Map<String, String>> animateImage(
            @PathVariable UUID id) {
        log.info("Animation requested for invite={}", id);
        videoService.animateInviteImage(id);   // @Async — returns immediately
        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "Animation started",
                        "inviteId", id.toString(),
                        "pollUrl", "/api/invites/id/" + id
                ));
    }

    // -------------------------------------------------------
    // GET /api/invites/suggest-prompt?eventType=BIRTHDAY&hostName=Sarah&eventTitle=Sarah's+30th
    // Returns an AI-suggested scene prompt for the wizard
    // -------------------------------------------------------
    @GetMapping("/suggest-prompt")
    public ResponseEntity<Map<String, String>> suggestPrompt(
            @RequestParam String eventType,
            @RequestParam(required = false, defaultValue = "") String hostName,
            @RequestParam(required = false, defaultValue = "") String eventTitle) {

        String suggestion = aiService.suggestScenePrompt(eventType, hostName, eventTitle);
        return ResponseEntity.ok(Map.of("prompt", suggestion));
    }
}
