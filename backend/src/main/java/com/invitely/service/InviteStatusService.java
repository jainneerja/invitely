package com.invitely.service;

import com.invitely.model.Invitation;
import com.invitely.model.Invitation.InviteStatus;
import com.invitely.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Owns short, single-purpose status writes for the AI generation pipeline.
 *
 * <p>These methods live on a <em>separate bean</em> from {@link AIService} and
 * {@link VideoService} on purpose. Spring implements {@code @Transactional} with
 * a proxy that wraps the bean; a call only becomes transactional when it crosses
 * that proxy. If these methods lived on AIService and AIService called them via
 * {@code this.markImagePending(...)}, the call would stay inside the raw object,
 * bypass the proxy, and run with no transaction — the classic self-invocation
 * trap. Injecting this service means {@code statusService.markImagePending(...)}
 * is a genuine external call and each write commits in its own short transaction,
 * before/after the slow external API call rather than inside it. See issue #2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InviteStatusService {

    private final InvitationRepository invitationRepository;

    @Transactional
    public void markImagePending(UUID inviteId) {
        setStatus(inviteId, InviteStatus.IMAGE_PENDING);
    }

    @Transactional
    public void markImageReady(UUID inviteId, String imageUrl) {
        markImageReady(inviteId, imageUrl, false);
    }

    @Transactional
    public void markImageReady(UUID inviteId, String imageUrl, boolean embedTextInImage) {
        Invitation invite = require(inviteId);
        invite.setGeneratedImageUrl(imageUrl);
        invite.setEmbedTextInImage(embedTextInImage);
        invite.setStatus(InviteStatus.IMAGE_READY);
        log.info("Invite {} -> IMAGE_READY (embedText={})", inviteId, embedTextInImage);
    }

    @Transactional
    public void markVideoPending(UUID inviteId) {
        setStatus(inviteId, InviteStatus.VIDEO_PENDING);
    }

    @Transactional
    public void markVideoReady(UUID inviteId, String videoUrl) {
        Invitation invite = require(inviteId);
        invite.setAnimatedVideoUrl(videoUrl);
        // Back to IMAGE_READY (publishable). Video is an enhancement, not a gate.
        invite.setStatus(InviteStatus.IMAGE_READY);
        log.info("Invite {} video ready -> IMAGE_READY", inviteId);
    }

    /**
     * Video generation failed or timed out. The invite still has a valid static
     * image, so it stays publishable — return it to IMAGE_READY without touching
     * the (absent) video URL. Distinct from {@link #markFailed} because animation
     * is optional; a failed animation is not a failed invite.
     */
    @Transactional
    public void markVideoUnavailable(UUID inviteId) {
        setStatus(inviteId, InviteStatus.IMAGE_READY);
        log.warn("Invite {} animation unavailable -> IMAGE_READY (static image kept)", inviteId);
    }

    /**
     * Terminal failure state. Committed in its own transaction so it survives
     * regardless of what threw in the caller — unlike the previous design, where
     * the failure write shared the caller's transaction and was rolled back.
     */
    @Transactional
    public void markFailed(UUID inviteId, String reason) {
        setStatus(inviteId, InviteStatus.FAILED);
        log.warn("Invite {} -> FAILED: {}", inviteId, reason);
    }

    private void setStatus(UUID inviteId, InviteStatus status) {
        require(inviteId).setStatus(status);
    }

    /**
     * Loads the managed entity. The returned instance is dirty-tracked within the
     * surrounding transaction, so mutating it is enough — no explicit save needed;
     * the change flushes at commit.
     */
    private Invitation require(UUID inviteId) {
        return invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));
    }
}
