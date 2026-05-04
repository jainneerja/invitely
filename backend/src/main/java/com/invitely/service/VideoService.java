package com.invitely.service;

import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
import com.invitely.model.Invitation.InviteStatus;
import com.invitely.repository.InvitationRepository;
import com.invitely.service.video.VideoGenerationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoGenerationProvider videoProvider;
    private final InvitationRepository invitationRepository;
    private final AssetService assetService;

    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 10_000;   // 10 seconds

    // -------------------------------------------------------
    // Submit animation job — async, runs in background
    // -------------------------------------------------------

    @Async
    @Transactional
    public void animateInviteImage(UUID inviteId) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        if (invite.getGeneratedImageUrl() == null) {
            throw new IllegalStateException(
                    "Cannot animate — no generated image found for invite: " + inviteId);
        }

        log.info("[VIDEO] Starting animation for invite={} provider={}",
                inviteId, videoProvider.providerName());

        invite.setStatus(InviteStatus.VIDEO_PENDING);
        invitationRepository.save(invite);

        try {
            String animationPrompt = buildAnimationPrompt(invite);

            // Submit to provider (Kling, Runway, or stub)
            String jobId = videoProvider.submitAnimationJob(
                    invite.getGeneratedImageUrl(),
                    animationPrompt,
                    6   // 6 second video
            );

            log.info("[VIDEO] Job submitted. jobId={} invite={}", jobId, inviteId);

            // Poll until complete
            String videoUrl = pollUntilComplete(jobId, inviteId);

            if (videoUrl != null) {
                // Store asset record
                assetService.storeVideoAsset(inviteId, videoUrl, AssetType.AI_ANIMATED_VIDEO);

                invite.setAnimatedVideoUrl(videoUrl);
                invite.setStatus(InviteStatus.IMAGE_READY);   // ready to publish
                invitationRepository.save(invite);

                log.info("[VIDEO] Animation complete. invite={} url={}", inviteId, videoUrl);
            } else {
                throw new RuntimeException("Video generation timed out after max poll attempts");
            }

        } catch (Exception e) {
            log.error("[VIDEO] Animation failed for invite={}", inviteId, e);
            invite.setStatus(InviteStatus.IMAGE_READY);   // fall back — still has static image
            invitationRepository.save(invite);
            throw new RuntimeException("Video animation failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // Build animation prompt from invite's animation style
    // -------------------------------------------------------

    private String buildAnimationPrompt(Invitation invite) {
        String style = invite.getAnimationStyle() != null
                ? invite.getAnimationStyle() : "gentle_motion";

        return switch (style) {
            case "animals_walk" ->
                "Make all animals walk or move naturally. " +
                "Tree leaves sway gently in the breeze. " +
                "Popup the main animal character. " +
                "Background elements move slowly. Keep text perfectly still.";

            case "leaves_sway" ->
                "Tree leaves and flowers sway gently in a soft breeze. " +
                "Light shimmer effect across the scene. " +
                "Subtle parallax depth movement. Keep text perfectly still.";

            case "confetti" ->
                "Confetti and streamers fall from above. " +
                "Balloons float upward gently. " +
                "Sparkle effects across the scene. Keep text perfectly still.";

            case "water_flow" ->
                "Water flows naturally, ripples on the surface. " +
                "Reflections shimmer. Clouds drift slowly. Keep text perfectly still.";

            case "sparkle" ->
                "Magical sparkle and star effects throughout the scene. " +
                "Fairy lights twinkle. Subtle glow pulses on focal elements. " +
                "Keep text perfectly still.";

            case "clouds_drift" ->
                "Clouds drift slowly across the sky. " +
                "Sun rays move gently. " +
                "Ambient light shifts subtly. Keep text perfectly still.";

            default ->
                "Gentle, subtle motion throughout the scene. " +
                "Natural ambient movement. Keep text perfectly still.";
        };
    }

    // -------------------------------------------------------
    // Poll with backoff until video is ready or timeout
    // -------------------------------------------------------

    private String pollUntilComplete(String jobId, UUID inviteId) {
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Video polling interrupted", e);
            }

            log.debug("[VIDEO] Poll attempt={} jobId={} invite={}",
                    attempt, jobId, inviteId);

            String result = videoProvider.pollJobResult(jobId);
            if (result != null) {
                return result;
            }
        }

        log.warn("[VIDEO] Timed out after {} attempts. jobId={} invite={}",
                MAX_POLL_ATTEMPTS, jobId, inviteId);
        return null;
    }
}
