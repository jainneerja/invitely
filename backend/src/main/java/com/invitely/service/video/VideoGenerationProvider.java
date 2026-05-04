package com.invitely.service.video;

/**
 * Pluggable video generation contract.
 * Swap providers by changing video.provider in application.yml
 * without touching any other code.
 */
public interface VideoGenerationProvider {

    /**
     * Submit an image for animation.
     *
     * @param imageUrl        publicly accessible URL of the source image
     * @param animationPrompt natural language instruction e.g.
     *                        "make the animals walk, leaves sway gently"
     * @param durationSeconds target video length (typically 4–8 seconds)
     * @return job ID to poll for completion
     */
    String submitAnimationJob(String imageUrl,
                              String animationPrompt,
                              int durationSeconds);

    /**
     * Poll job status.
     *
     * @return video URL when complete, null if still processing
     */
    String pollJobResult(String jobId);

    /**
     * Human-readable provider name for logging.
     */
    String providerName();
}
