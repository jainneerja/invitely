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

    @Value("${invitely.ai.image.scene-model:imagen-3.0-generate-001}")
    private String sceneModel;

    @Value("${invitely.ai.image.gemini-flash-image-model:gemini-2.5-flash-image}")
    private String geminiFlashImageModel;

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
    public void generateInviteImage(UUID inviteId, boolean embedInvitationText) {
        // Read the fields we need in one short read-only unit, then release.
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));
        String prompt = buildImagePrompt(invite, embedInvitationText);
        String slug = invite.getSlug();

        log.info("Starting image generation for invite={} embedText={}", inviteId, embedInvitationText);
        statusService.markImagePending(inviteId);   // tx 1 — commits, now visible

        try {
            log.debug("Image prompt: {}", prompt);
            // Two models: Imagen for a clean scene (frontend overlays text),
            // Gemini Flash Image when the invitation text must be baked into the image.
            String b64Image = embedInvitationText
                    ? callGeminiFlashImageApi(prompt, geminiFlashImageModel)   // no tx held
                    : callImagenApi(prompt, sceneModel);                        // no tx held

            String imageUrl = assetService.storeBase64Image(
                    inviteId, b64Image, "image/png", AssetType.AI_GENERATED_IMAGE);

            statusService.markImageReady(inviteId, imageUrl, embedInvitationText);   // tx 2
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

    // -------------------------------------------------------
    // Vertex AI Gemini Flash Image (generateContent) — used when the invitation
    // text must be baked directly into the image. Unlike Imagen, this model can
    // render legible text, so we ask it to compose a full invitation card.
    // -------------------------------------------------------

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
            return buildEmbeddedCardPrompt(invite, scene);
        }

        // Clean scene — the frontend overlays the text. Asking Imagen to bake text
        // is unreliable, so we keep this path text-free.
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

    // Builds a full invitation-card prompt with the event text baked in.
    // Only used on the Gemini Flash Image path (embedTextInImage = true).
    private String buildEmbeddedCardPrompt(Invitation invite, String scene) {
        String eventType = invite.getEventType().name();
        StringBuilder detailLines = new StringBuilder();

        if (invite.getEventDate() != null) {
            detailLines.append("📅 ")
                    .append(invite.getEventDate()
                            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
                    .append("\n");
        }
        if (invite.getEventTime() != null) {
            detailLines.append("🕐 ")
                    .append(invite.getEventTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")))
                    .append("\n");
        }
        if (invite.getVenueName() != null && !invite.getVenueName().isBlank()) {
            detailLines.append("📍 ").append(invite.getVenueName());
            if (invite.getVenueAddress() != null && !invite.getVenueAddress().isBlank()) {
                detailLines.append(", ").append(invite.getVenueAddress());
            }
            detailLines.append("\n");
        }

        String hostLine = (invite.getHostName() != null && !invite.getHostName().isBlank())
                ? "Hosted by " + invite.getHostName()
                : "";
        String personalNote = (invite.getPersonalMessage() != null && !invite.getPersonalMessage().isBlank())
                ? "\n“" + invite.getPersonalMessage() + "”"
                : "";
        String thematic = thematicElementsForEventType(eventType);
        String styleGuide = styleGuideForEventType(eventType);

        return """
                Create a stunning, high-resolution printable invitation card in a perfect square (1:1) format.

                CRITICAL — FULL BLEED:
                The artwork MUST fill the entire square canvas edge to edge, corner to corner.
                Do NOT place the design on a white, grey, or colored matte. There must be
                NO border, NO margin, NO frame, and NO empty background around the card —
                the decorative illustration extends all the way to every edge of the image.

                VISUAL THEME:
                %s. %s

                CARD LAYOUT — three clearly defined regions:

                TOP SECTION (top 25%% of card):
                  - Decorative ribbon banner, floral arch, or themed frame along the top edge
                  - Event title "%s" in large, bold, elegant display font — centered, highly legible
                  - "%s" in a smaller graceful subtitle font directly below the title

                CENTER ILLUSTRATION (middle 40%% of card):
                  - Rich, detailed thematic scene: %s
                  - Colorful balloons and celebratory confetti or petals woven naturally into the scene
                  - Soft semi-transparent frosted panel in the lower portion of this zone
                    to ensure the text below reads cleanly against the illustration

                EVENT DETAILS PANEL (bottom 35%% of card):
                  - Clean, well-spaced text rendered in crisp, legible fonts:
                %s%s
                  - Calendar, clock, and location-pin icons before each detail line
                  - Title text in elegant serif font; values in clean modern sans-serif
                  - Generous line spacing so details never feel cramped

                OVERALL STYLE:
                %s
                - Warm celebratory color palette — rich jewel tones with soft pastel accents
                - High-quality painterly or illustrated style, invitation-card quality
                - Clean balanced layout with generous whitespace between sections
                - No watermarks, logos, photographer credits, or extra signatures
                """.formatted(
                scene,
                thematic,
                invite.getEventTitle(),
                hostLine,
                thematic,
                detailLines.toString().trim(),
                personalNote,
                styleGuide
        );
    }

    private String thematicElementsForEventType(String eventType) {
        return switch (eventType) {
            case "BIRTHDAY"    -> "Adorable themed characters (jungle animals, cartoon figures, or fantasy creatures matching the scene) wearing party hats; colorful number centerpiece";
            case "WEDDING"     -> "Romantic floral garlands, dove pair, intertwined rings, rose petals cascading down the sides";
            case "PARTY"       -> "Vibrant streamers, party poppers, disco ball reflection, confetti burst in celebratory colors";
            case "BABY_SHOWER" -> "Gentle baby animals (ducks, bunnies, elephants), soft pastel stars, tiny baby footprints, pram silhouette";
            case "GRADUATION"  -> "Graduation cap and diploma scroll, laurel wreath, golden stars, confetti in school colors";
            case "CORPORATE"   -> "Sleek geometric accents, subtle gold lines, modern abstract shapes, professional emblem";
            default            -> "Festive decorative borders, colorful floral accents, celebratory ribbons";
        };
    }

    private String styleGuideForEventType(String eventType) {
        return switch (eventType) {
            case "BIRTHDAY"    -> "- Bright, playful color palette; kid-friendly illustrations with soft outlines and warm tones";
            case "WEDDING"     -> "- Soft romantic palette (ivory, blush, gold); elegant watercolor or fine-art illustration style";
            case "PARTY"       -> "- Bold vibrant colors; energetic modern illustration with dynamic composition";
            case "BABY_SHOWER" -> "- Soft pastel palette (mint, lavender, peach); gentle watercolor with whimsical linework";
            case "GRADUATION"  -> "- Optimistic palette (navy, gold, white); clean sharp illustration with a sense of achievement";
            case "CORPORATE"   -> "- Sophisticated palette (deep blue, charcoal, gold); polished semi-realistic or flat design style";
            default            -> "- Warm celebratory palette; high-quality painterly style";
        };
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
