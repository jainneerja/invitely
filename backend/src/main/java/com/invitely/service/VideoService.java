package com.invitely.service;

import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
import com.invitely.repository.InvitationRepository;
import com.invitely.service.video.VideoGenerationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoGenerationProvider videoProvider;
    private final InvitationRepository invitationRepository;
    private final InviteStatusService statusService;
    private final AssetService assetService;

    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 10_000;   // 10 seconds

    // -------------------------------------------------------
    // Submit animation job — async, runs in background
    // -------------------------------------------------------

    // NOT @Transactional. This method polls the provider for up to
    // MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS (~5 min); a transaction here would pin
    // a Hikari connection for that entire window and could exhaust the pool,
    // taking down unrelated endpoints. Status writes go through InviteStatusService
    // (short, independent transactions). See issue #2.
    @Async
    public void animateInviteImage(UUID inviteId) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        String imageUrl = invite.getGeneratedImageUrl();
        if (imageUrl == null) {
            throw new IllegalStateException(
                    "Cannot animate — no generated image found for invite: " + inviteId);
        }

        log.info("[VIDEO] Starting animation for invite={} provider={}",
                inviteId, videoProvider.providerName());

        String animationPrompt = buildAnimationPrompt(invite);
        statusService.markVideoPending(inviteId);

        try {
            String jobId = videoProvider.submitAnimationJob(imageUrl, animationPrompt, 6);
            log.info("[VIDEO] Job submitted. jobId={} invite={}", jobId, inviteId);

            String videoUrl = pollUntilComplete(jobId, inviteId);   // no tx held

            if (videoUrl != null) {
                assetService.storeVideoAsset(inviteId, videoUrl, AssetType.AI_ANIMATED_VIDEO);
                statusService.markVideoReady(inviteId, videoUrl);   // back to IMAGE_READY
                log.info("[VIDEO] Animation complete. invite={} url={}", inviteId, videoUrl);
            } else {
                // Timeout: the invite still has a valid static image, so fall back to
                // IMAGE_READY (publishable) rather than FAILED. Video is optional.
                log.warn("[VIDEO] Timed out; keeping static image. invite={}", inviteId);
                statusService.markVideoUnavailable(inviteId);
            }

        } catch (Exception e) {
            // Same fallback: a failed animation must not strand a publishable invite.
            // No rethrow — @Async void, so it would only be logged anyway.
            log.error("[VIDEO] Animation failed for invite={}", inviteId, e);
            statusService.markVideoUnavailable(inviteId);
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
