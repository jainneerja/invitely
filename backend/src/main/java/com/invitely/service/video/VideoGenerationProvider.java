package com.invitely.service.video;

import java.util.UUID;

public interface VideoGenerationProvider {

    /**
     * Submit an image for animation.
     *
     * @param imageUrl        publicly accessible URL of the source image
     * @param animationPrompt natural language instruction
     * @param durationSeconds target video length (typically 4–8 seconds)
     * @return provider-specific job/operation ID to poll
     */
    String submitAnimationJob(String imageUrl,
                              String animationPrompt,
                              int durationSeconds);

    /**
     * Poll job status.
     *
     * @param jobId    provider job/operation ID from submitAnimationJob
     * @param inviteId invitation UUID — providers that return base64 video use
     *                 this to store the asset and return a public URL
     * @return public video URL when complete, null if still processing
     */
    String pollJobResult(String jobId, UUID inviteId);

    String providerName();
}
