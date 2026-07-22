package com.invitely.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.invitely.kafka.InviteEventProducer;
import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
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
    private final InviteStatusService statusService;
    private final AssetService assetService;
    private final InviteEventProducer eventProducer;
    private final GoogleCredentials googleCredentials;
    private final ChatModel chatModel;
    private final RestTemplate vertexRestTemplate;

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    private static final String IMAGEN_MODEL = "imagen-3.0-generate-001";

    // Explicit constructor — avoids @RequiredArgsConstructor conflict with @Qualifier
    public AIService(
            InvitationRepository invitationRepository,
            InviteStatusService statusService,
            AssetService assetService,
            InviteEventProducer eventProducer,
            GoogleCredentials googleCredentials,
            ChatModel chatModel,
            @Qualifier("vertexRestTemplate") RestTemplate vertexRestTemplate) {
        this.invitationRepository = invitationRepository;
        this.statusService        = statusService;
        this.assetService         = assetService;
        this.eventProducer        = eventProducer;
        this.googleCredentials    = googleCredentials;
        this.chatModel            = chatModel;
        this.vertexRestTemplate   = vertexRestTemplate;
    }

    // -------------------------------------------------------
    // Generate image — async, status polling driven
    // -------------------------------------------------------

    // NOTE: deliberately NOT @Transactional. This method runs on an async thread
    // and makes a slow external call (Imagen). Wrapping it in a transaction would
    // hold a DB connection for the whole call. Instead, each status write is a
    // short transaction on InviteStatusService (a separate bean, so the proxy is
    // actually crossed — see that class and issue #2). Because the PENDING write
    // now commits before the external call, clients can observe it while polling.
    @Async
    public void generateInviteImage(UUID inviteId) {
        // Read the fields we need in one short read-only unit, then release.
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));
        String prompt = buildImagePrompt(invite);
        String slug = invite.getSlug();

        log.info("Starting image generation for invite={}", inviteId);
        statusService.markImagePending(inviteId);   // tx 1 — commits, now visible

        try {
            log.debug("Imagen prompt: {}", prompt);
            String b64Image = callImagenApi(prompt);              // no tx held here

            String imageUrl = assetService.storeBase64Image(
                    inviteId, b64Image, "image/png", AssetType.AI_GENERATED_IMAGE);

            statusService.markImageReady(inviteId, imageUrl);     // tx 2
            eventProducer.imageReady(inviteId, slug, imageUrl);
            log.info("Image generated successfully. invite={} url={}", inviteId, imageUrl);

        } catch (Exception e) {
            // Land a durable FAILED state (tx 3) instead of rolling the write back.
            // Do NOT rethrow: this is an @Async void method, so an exception would
            // only reach SimpleAsyncUncaughtExceptionHandler and be logged with no
            // effect on state. The FAILED write is the signal the frontend polls for.
            log.error("Image generation failed for invite={}", inviteId, e);
            statusService.markFailed(inviteId, e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Vertex AI Imagen REST API
    // Spring AI has no ImageModel for Vertex — calling REST directly
    // Docs: cloud.google.com/vertex-ai/docs/generative-ai/image/generate-images
    // -------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String callImagenApi(String prompt) throws IOException {
        googleCredentials.refreshIfExpired();
        String accessToken = googleCredentials.getAccessToken().getTokenValue();

        String url = "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict"
                .formatted(location, projectId, location, IMAGEN_MODEL);

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

    private String buildImagePrompt(Invitation invite) {
        String scene = (invite.getScenePrompt() != null && !invite.getScenePrompt().isBlank())
                ? invite.getScenePrompt()
                : defaultSceneForEventType(invite.getEventType().name());

        // Text overlay is handled by the frontend (CSS overlay)
        // Asking Imagen to bake text is unreliable — clean scene gives better results
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
