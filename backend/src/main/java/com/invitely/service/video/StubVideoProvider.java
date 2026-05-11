package com.invitely.service.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub provider for local development and portfolio demos.
 * Returns a pre-rendered sample video after a simulated delay.
 * Activated when video.provider=stub (the default).
 */
@Component
@ConditionalOnProperty(name = "video.provider", havingValue = "stub", matchIfMissing = true)
@Slf4j
public class StubVideoProvider implements VideoGenerationProvider {

    // A royalty-free sample animated clip for demo purposes
    private static final String SAMPLE_VIDEO_URL =
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";

    @Override
    public String submitAnimationJob(String imageUrl,
                                     String animationPrompt,
                                     int durationSeconds) {
        log.info("[STUB] Video job submitted. imageUrl={} prompt={}",
                imageUrl, animationPrompt);
        // Return a fake job ID — polling will immediately return the sample video
        return "stub-job-" + System.currentTimeMillis();
    }

    @Override
    public String pollJobResult(String jobId, java.util.UUID inviteId) {
        log.info("[STUB] Polling job={} — returning sample video", jobId);
        // Simulate processing delay in a real scenario;
        // stub always returns immediately for dev speed
        return SAMPLE_VIDEO_URL;
    }

    @Override
    public String providerName() {
        return "stub";
    }
}
