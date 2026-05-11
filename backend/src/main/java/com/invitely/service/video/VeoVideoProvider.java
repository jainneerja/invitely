package com.invitely.service.video;

import com.google.auth.oauth2.GoogleCredentials;
import com.invitely.service.AssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Veo video generation provider via Vertex AI.
 * Uses image-to-video (I2V): takes the generated invite image + a motion
 * prompt and returns an animated MP4.
 *
 * Activated when video.provider=veo in application.yml.
 *
 * LRO flow:
 *   1. POST /{model}:predictLongRunning  → returns operation name
 *   2. GET  /operations/{id}            → poll until done=true
 *   3. Extract base64 MP4 from predictions → store via AssetService
 */
@Component
@ConditionalOnProperty(name = "video.provider", havingValue = "veo")
@Slf4j
public class VeoVideoProvider implements VideoGenerationProvider {

    private final GoogleCredentials googleCredentials;
    private final AssetService assetService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @Value("${invitely.ai.video.veo-model:veo-3.0-generate-preview}")
    private String veoModel;

    @Value("${invitely.ai.video.duration-seconds:6}")
    private int defaultDurationSeconds;
n    @Value("${invitely.base-url}")
    private String appBaseUrl;

    @Value("${invitely.asset-storage-path:uploads}")
    private String storagePath;

    public VeoVideoProvider(GoogleCredentials googleCredentials, AssetService assetService) {
        this.googleCredentials = googleCredentials;
        this.assetService = assetService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String submitAnimationJob(String imageUrl, String animationPrompt, int durationSeconds) {
        log.info("[VEO] Submitting I2V job. model={} imageUrl={}", veoModel, imageUrl);

        byte[] imageBytes = downloadImage(imageUrl);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String accessToken = refreshedToken();
        String url = vertexUrl(veoModel + ":predictLongRunning");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "instances", List.of(Map.of(
                        "prompt", animationPrompt,
                        "image", Map.of(
                                "bytesBase64Encoded", base64Image,
                                "mimeType", "image/png"
                        )
                )),
                "parameters", Map.of(
                        "aspectRatio", "1:1",
                        "sampleCount", 1,
                        "durationSeconds", durationSeconds > 0 ? durationSeconds : defaultDurationSeconds,
                        "enhancePrompt", true
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("name")) {
            throw new RuntimeException("[VEO] No operation name in response");
        }

        String operationName = (String) response.getBody().get("name");
        log.info("[VEO] Job submitted. operation={}", operationName);
        return operationName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String pollJobResult(String operationName, UUID inviteId) {
        log.debug("[VEO] Polling operation={}", operationName);

        String accessToken = refreshedToken();
        // operationName is the full path e.g. projects/.../locations/.../operations/123
        String url = "https://%s-aiplatform.googleapis.com/v1/%s"
                .formatted(location, operationName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null || !Boolean.TRUE.equals(body.get("done"))) {
            return null;  // still processing
        }

        // Extract base64 MP4 from predictions
        Map<String, Object> resp = (Map<String, Object>) body.get("response");
        if (resp == null) throw new RuntimeException("[VEO] Operation done but no response body");

        List<Map<String, Object>> predictions = (List<Map<String, Object>>) resp.get("predictions");
        if (predictions == null || predictions.isEmpty()) {
            throw new RuntimeException("[VEO] No predictions in completed operation");
        }

        String b64Video = (String) predictions.get(0).get("bytesBase64Encoded");
        if (b64Video == null || b64Video.isBlank()) {
            throw new RuntimeException("[VEO] Empty video bytes in prediction");
        }

        log.info("[VEO] Video ready. Storing asset for invite={}", inviteId);
        return assetService.storeBase64Video(inviteId, b64Video, "video/mp4");
    }

    @Override
    public String providerName() {
        return "veo";
    }

    private String refreshedToken() {
        try {
            googleCredentials.refreshIfExpired();
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new RuntimeException("[VEO] Failed to refresh Google credentials", e);
        }
    }

    private String vertexUrl(String modelAndMethod) {
        return "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s"
                .formatted(location, projectId, location, modelAndMethod);
    }

    private byte[] downloadImage(String imageUrl) {
        // Dev: images are stored locally — read from disk directly to avoid
        // port mismatch (base-url may point to the frontend; uploads are on the backend).
        // Prod: images are on S3 — fall through to HTTP download.
        if (imageUrl.contains("/uploads/")) {
            String afterUploads = imageUrl.substring(imageUrl.indexOf("/uploads/") + "/uploads/".length());
            Path localFile = Paths.get(storagePath, afterUploads);
            if (java.nio.file.Files.exists(localFile)) {
                try {
                    log.debug("[VEO] Reading image from disk: {}", localFile);
                    return Files.readAllBytes(localFile);
                } catch (IOException e) {
                    throw new RuntimeException("[VEO] Failed to read local image: " + localFile, e);
                }
            }
        }
        // S3 or other external URL
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
            if (response.getBody() == null || response.getBody().length == 0) {
                throw new RuntimeException("[VEO] Empty image downloaded from: " + imageUrl);
            }
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("[VEO] Failed to download source image: " + imageUrl, e);
        }
    }
}
