package com.invitely.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.invitely.kafka.InviteEventProducer;
import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
import com.invitely.model.Invitation.InviteStatus;
import com.invitely.repository.InvitationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class AIService {

    private final InvitationRepository invitationRepository;
    private final AssetService assetService;
    private final InviteEventProducer eventProducer;
    private final GoogleCredentials googleCredentials;
    private final ChatModel chatModel;
    private final RestTemplate vertexRestTemplate;

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @Value("${invitely.ai.image.scene-model:imagen-3.0-generate-001}")
    private String sceneModel;

    @Value("${invitely.ai.image.gemini-flash-image-model:gemini-2.5-flash-image}")
    private String geminiFlashImageModel;

    // Explicit constructor — avoids @RequiredArgsConstructor conflict with @Qualifier
    public AIService(
            InvitationRepository invitationRepository,
            AssetService assetService,
            InviteEventProducer eventProducer,
            GoogleCredentials googleCredentials,
            ChatModel chatModel,
            @Qualifier("vertexRestTemplate") RestTemplate vertexRestTemplate) {
        this.invitationRepository = invitationRepository;
        this.assetService         = assetService;
        this.eventProducer        = eventProducer;
        this.googleCredentials    = googleCredentials;
        this.chatModel            = chatModel;
        this.vertexRestTemplate   = vertexRestTemplate;
    }

    // -------------------------------------------------------
    // Generate image — async, status polling driven
    // -------------------------------------------------------

    @Async
    @Transactional
    public void generateInviteImage(UUID inviteId, boolean embedInvitationText) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        log.info("Starting image generation for invite={}", inviteId);

        invite.setStatus(InviteStatus.IMAGE_PENDING);
        invitationRepository.save(invite);

        try {
            String prompt = buildImagePrompt(invite, embedInvitationText);
            log.debug("Imagen prompt: {}", prompt);

            String b64Image = embedInvitationText
                    ? callGeminiFlashImageApi(prompt, geminiFlashImageModel)
                    : callImagenApi(prompt, sceneModel);

            String imageUrl = assetService.storeBase64Image(
                    inviteId, b64Image, "image/png", AssetType.AI_GENERATED_IMAGE);

            invite.setGeneratedImageUrl(imageUrl);
            invite.setStatus(InviteStatus.IMAGE_READY);
            invitationRepository.save(invite);

            eventProducer.imageReady(inviteId, invite.getSlug(), imageUrl);
            log.info("Image generated successfully. invite={} url={}", inviteId, imageUrl);

        } catch (Exception e) {
            log.error("Image generation failed for invite={}", inviteId, e);
            invite.setStatus(InviteStatus.DRAFT);
            invitationRepository.save(invite);
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // Vertex AI Imagen REST API
    // Spring AI has no ImageModel for Vertex — calling REST directly
    // Docs: cloud.google.com/vertex-ai/docs/generative-ai/image/generate-images
    // -------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String callImagenApi(String prompt, String model) throws IOException {
        googleCredentials.refreshIfExpired();
        String accessToken = googleCredentials.getAccessToken().getTokenValue();

        String url = "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict"
                .formatted(location, projectId, location, model);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "instances",  List.of(Map.of("prompt", prompt)),
                "parameters", Map.of(
                        "sampleCount",       1,
                        "aspectRatio",       "1:1",
                        "safetyFilterLevel", "block_some",
                        "personGeneration",  "allow_adult"
                )
        );

        ResponseEntity<Map> response = vertexRestTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Imagen API");
        }

        List<Map<String, Object>> predictions =
                (List<Map<String, Object>>) response.getBody().get("predictions");

        if (predictions == null || predictions.isEmpty()) {
            throw new RuntimeException("No predictions in Imagen API response");
        }

        String b64 = (String) predictions.get(0).get("bytesBase64Encoded");
        if (b64 == null || b64.isBlank()) {
            throw new RuntimeException("Empty image bytes from Imagen API");
        }

        return b64;
    }

    @SuppressWarnings("unchecked")
    private String callGeminiFlashImageApi(String prompt, String model) throws IOException {
        googleCredentials.refreshIfExpired();
        String accessToken = googleCredentials.getAccessToken().getTokenValue();

        String url = "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent"
                .formatted(location, projectId, location, model);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseModalities", List.of("TEXT", "IMAGE")
                )
        );

        ResponseEntity<Map> response = vertexRestTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Gemini Flash Image API");
        }

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini Flash Image API response");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) throw new RuntimeException("Missing content in Gemini response");

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException("No parts in Gemini response content");
        }

        for (Map<String, Object> part : parts) {
            Map<String, Object> inlineData = (Map<String, Object>) part.get("inlineData");
            if (inlineData != null) {
                String b64 = (String) inlineData.get("data");
                if (b64 != null && !b64.isBlank()) {
                    return b64;
                }
            }
        }

        throw new RuntimeException("No inline image bytes returned by Gemini Flash Image API");
    }

    // -------------------------------------------------------
    // Gemini chat for prompt suggestions — via Spring AI ChatModel
    //
    // Spring AI version compatibility note:
    //   1.0.0-M1  → withModel(), withTemperature(), withMaxOutputTokens()
    //   1.0.0-GA+ → model(), temperature(), maxOutputTokens()
    //
    // AssistantMessage:
    //   All versions → getText()   (getContent() does not exist)
    // -------------------------------------------------------

    public String suggestScenePrompt(String eventType, String hostName, String eventTitle) {
        try {
            String message = """
                    Suggest a single vivid image generation prompt for a %s invitation.
                    Host: %s, Event: %s.
                    - Describe a beautiful themed scene with characters, mood, and lighting
                    - Include an art style (watercolor, 3D render, painterly)
                    - Max 50 words
                    - Return ONLY the prompt text, nothing else
                    """.formatted(eventType, hostName, eventTitle);

            var chatResponse = chatModel.call(
                    new Prompt(message,
                            VertexAiGeminiChatOptions.builder()
                                    .model("gemini-2.0-flash")       
                                    .maxOutputTokens(100)            
                                    .build()));

            // getText() — correct method on AssistantMessage
            // getContent() does NOT exist
            return chatResponse.getResult().getOutput().getText().trim();

        } catch (Exception e) {
            log.warn("Prompt suggestion failed, using default. error={}", e.getMessage());
            return defaultSceneForEventType(eventType);
        }
    }

    // -------------------------------------------------------
    // Prompt engineering
    // -------------------------------------------------------

    private String buildImagePrompt(Invitation invite, boolean embedInvitationText) {
        String scene = (invite.getScenePrompt() != null && !invite.getScenePrompt().isBlank())
                ? invite.getScenePrompt()
                : defaultSceneForEventType(invite.getEventType().name());

        if (embedInvitationText) {
            String overlayText = buildTextOverlay(invite);
            return """
                    %s.

                    Create a printable invitation card layout with embedded text.
                    Include the following invitation text exactly as written and preserve spelling:
                    ---
                    %s
                    ---

                    Style requirements:
                    - Balanced composition with readable typography
                    - Soft visual areas behind text for contrast
                    - Beautiful invitation-quality illustration
                    - Cinematic square composition (1:1)
                    - Rich vibrant colors, warm and celebratory mood
                    - High-quality painterly or photorealistic style
                    - No watermarks, logos, or signatures
                    """.formatted(scene, overlayText);
        }

        // Scene-only mode uses frontend text overlay for predictable readability
        return """
                %s.
                
                Style requirements:
                - Beautiful invitation-quality illustration
                - Cinematic square composition (1:1)
                - Rich vibrant colors, warm and celebratory mood
                - Leave some visual breathing room (sky, ground, or soft area)
                  that can serve as a backdrop for text overlay
                - High-quality painterly or photorealistic style
                - No text, watermarks, logos, or signatures
                """.formatted(scene);
    }

    private String buildTextOverlay(Invitation invite) {
        StringBuilder sb = new StringBuilder();
        sb.append(invite.getEventTitle()).append("\n");
        if (invite.getHostName() != null)
            sb.append("Hosted by ").append(invite.getHostName()).append("\n");
        if (invite.getEventDate() != null)
            sb.append(invite.getEventDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        if (invite.getEventTime() != null)
            sb.append(" at ").append(invite.getEventTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))).append("\n");
        if (invite.getVenueName() != null)
            sb.append(invite.getVenueName());
        return sb.toString().trim();
    }

    private String defaultSceneForEventType(String eventType) {
        return switch (eventType) {
            case "BIRTHDAY"    -> "Magical birthday jungle scene, lions and elephants, golden sunset, painterly style";
            case "WEDDING"     -> "Romantic garden wedding, roses and fairy lights, golden hour, soft watercolor";
            case "PARTY"       -> "Vibrant confetti party, balloons and streamers, joyful atmosphere, 3D render";
            case "BABY_SHOWER" -> "Soft pastel baby shower scene, gentle animals, whimsical watercolor";
            case "GRADUATION"  -> "Proud graduation celebration, confetti and diploma, bright optimistic atmosphere";
            case "CORPORATE"   -> "Professional corporate event, modern architecture, sophisticated lighting";
            default            -> "Beautiful celebration scene, warm golden lighting, painterly style";
        };
    }
}
