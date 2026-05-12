package com.invitely.service.video;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kling AI video generation provider (image-to-video).
 * Activated when video.provider=kling in application.yml.
 *
 * Auth: Kling uses JWT signed with AccessKey/SecretKey — NOT a plain bearer token.
 * JWT payload: iss=accessKey, exp=now+30min, nbf=now-5s
 */
@Component
@ConditionalOnProperty(name = "video.provider", havingValue = "kling")
@Slf4j
public class KlingVideoProvider implements VideoGenerationProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${video.kling.access-key}")
    private String accessKey;

    @Value("${video.kling.secret-key}")
    private String secretKey;

    @Value("${video.kling.base-url:https://api.kling.ai}")
    private String baseUrl;

    @Override
    @SuppressWarnings("unchecked")
    public String submitAnimationJob(String imageUrl, String animationPrompt, int durationSeconds) {
        log.info("[KLING] Submitting I2V job. imageUrl={}", imageUrl);

        Map<String, Object> body = Map.of(
                "model", "kling-v2-master",
                "image_url", imageUrl,
                "prompt", animationPrompt,
                "duration", durationSeconds,
                "cfg_scale", 0.5,
                "mode", "std",
                "aspect_ratio", "1:1"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/v1/videos/image2video",
                new HttpEntity<>(body, authHeaders()),
                Map.class);

        Map<String, Object> data = extractData(response, "submit");
        String taskId = (String) data.get("task_id");
        log.info("[KLING] Job submitted. taskId={}", taskId);
        return taskId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String pollJobResult(String jobId, UUID inviteId) {
        log.debug("[KLING] Polling taskId={}", jobId);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/v1/videos/image2video/" + jobId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class);

        Map<String, Object> data = extractData(response, "poll");
        String status = (String) data.get("task_status");

        return switch (status) {
            case "succeed" -> {
                var result = (Map<String, Object>) data.get("task_result");
                if (result == null) yield null;
                var videos = (List<Map<String, Object>>) result.get("videos");
                if (videos == null || videos.isEmpty()) yield null;
                yield (String) videos.get(0).get("url");
            }
            case "failed" -> throw new RuntimeException(
                    "[KLING] Job failed: " + data.get("task_status_msg"));
            default -> null;  // processing
        };
    }

    @Override
    public String providerName() { return "kling"; }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateJwt());
        return headers;
    }

    private String generateJwt() {
        long nowMs = System.currentTimeMillis();
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .issuer(accessKey)
                .issuedAt(new Date(nowMs))
                .notBefore(new Date(nowMs - 5_000))
                .expiration(new Date(nowMs + 1_800_000))  // 30 min
                .signWith(key)
                .compact();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(ResponseEntity<Map> response, String ctx) {
        if (response.getBody() == null) throw new RuntimeException("[KLING] Empty response (" + ctx + ")");
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        if (data == null) throw new RuntimeException("[KLING] No data in response (" + ctx + ")");
        return data;
    }
}
