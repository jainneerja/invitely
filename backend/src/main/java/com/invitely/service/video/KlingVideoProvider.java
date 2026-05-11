package com.invitely.service.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Kling AI video generation provider.
 * Activated when video.provider=kling in application.yml.
 *
 * Kling API docs: https://api.kling.ai/docs
 * Specialises in image-to-video with natural language motion prompts —
 * ideal for "make animals walk, leaves sway" style instructions.
 */
@Component
@ConditionalOnProperty(name = "video.provider", havingValue = "kling")
@Slf4j
public class KlingVideoProvider implements VideoGenerationProvider {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public KlingVideoProvider(
            @Value("${video.kling.api-key}") String apiKey,
            @Value("${video.kling.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String submitAnimationJob(String imageUrl,
                                     String animationPrompt,
                                     int durationSeconds) {
        log.info("[KLING] Submitting animation job. imageUrl={}", imageUrl);

        HttpHeaders headers = buildHeaders();

        Map<String, Object> body = Map.of(
                "model", "kling-v1",
                "image_url", imageUrl,
                "prompt", animationPrompt,
                "duration", durationSeconds,
                "cfg_scale", 0.5,       // creativity vs prompt adherence
                "mode", "standard"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/videos/image2video",
                    request,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from Kling API");
            }

            // Kling returns { data: { task_id: "..." } }
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            String taskId = (String) data.get("task_id");
            log.info("[KLING] Job submitted. taskId={}", taskId);
            return taskId;

        } catch (Exception e) {
            log.error("[KLING] Failed to submit job", e);
            throw new RuntimeException("Kling API error: " + e.getMessage(), e);
        }
    }

    @Override
    public String pollJobResult(String jobId, java.util.UUID inviteId) {
        log.debug("[KLING] Polling jobId={}", jobId);

        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/videos/image2video/" + jobId,
                    HttpMethod.GET,
                    request,
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return null;

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String status = (String) data.get("task_status");

            return switch (status) {
                case "succeed" -> {
                    // Extract video URL from works array
                    var works = (java.util.List<Map<String, Object>>) data.get("task_result");
                    if (works != null && !works.isEmpty()) {
                        var videos = (java.util.List<Map<String, Object>>)
                                works.get(0).get("videos");
                        if (videos != null && !videos.isEmpty()) {
                            yield (String) videos.get(0).get("url");
                        }
                    }
                    yield null;
                }
                case "failed" -> throw new RuntimeException(
                        "Kling job failed: " + data.get("task_status_msg"));
                default -> null;   // still processing
            };

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KLING] Polling error for jobId={}", jobId, e);
            return null;
        }
    }

    @Override
    public String providerName() {
        return "kling";
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }
}
