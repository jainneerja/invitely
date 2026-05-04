package com.invitely.service;

import com.invitely.dto.InviteResponse;
import com.invitely.kafka.InviteEventProducer;
import com.invitely.model.Asset;
import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
import com.invitely.model.Invitation.InviteStatus;
import com.invitely.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final ImageModel imageModel;
    private final VertexAiGeminiImageOptions geminiImageOptions;
    private final InvitationRepository invitationRepository;
    private final AssetService assetService;
    private final InviteService inviteService;
    private final InviteEventProducer eventProducer;

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    // -------------------------------------------------------
    // Generate image — called from controller, runs async
    // -------------------------------------------------------

    @Async
    @Transactional
    public void generateInviteImage(UUID inviteId) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        log.info("Starting image generation for invite={}", inviteId);

        // Mark as pending so frontend can show a spinner
        invite.setStatus(InviteStatus.IMAGE_PENDING);
        invitationRepository.save(invite);

        try {
            String prompt = buildImagePrompt(invite);
            log.debug("Gemini prompt: {}", prompt);

            ImageResponse response = imageModel.call(
                    new ImagePrompt(prompt, geminiImageOptions));

            // Gemini returns base64 encoded image
            String b64Image = response.getResult().getOutput().getB64Json();
            String imageUrl = assetService.storeBase64Image(
                    inviteId, b64Image, "image/png", AssetType.AI_GENERATED_IMAGE);

            invite.setGeneratedImageUrl(imageUrl);
            invite.setStatus(InviteStatus.IMAGE_READY);
            invitationRepository.save(invite);

            eventProducer.imageReady(inviteId, invite.getSlug(), imageUrl);
            log.info("Image generated successfully for invite={} url={}",
                    inviteId, imageUrl);

        } catch (Exception e) {
            log.error("Image generation failed for invite={}", inviteId, e);
            // Roll back to DRAFT so user can retry
            invite.setStatus(InviteStatus.DRAFT);
            invitationRepository.save(invite);
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // Suggest scene prompt via Gemini chat (Spring AI)
    // -------------------------------------------------------

    public String suggestScenePrompt(String eventType, String hostName,
                                      String eventTitle) {
        // Uses Gemini chat to suggest a creative scene prompt
        // This keeps AI enhancement separate from core flow
        String userMessage = String.format(
                """
                Suggest a single vivid image generation prompt for a %s invitation.
                Host: %s, Event: %s.
                Requirements:
                - Describe a beautiful, themed scene (landscape, characters, mood)
                - Include lighting (golden hour, moonlit, etc.)
                - Include art style (watercolor, 3D render, painterly, etc.)
                - Max 50 words
                - Return ONLY the prompt text, nothing else
                """,
                eventType, hostName, eventTitle);

        // Direct Gemini chat call via Spring AI
        // Using a simple completion rather than a full chat model for speed
        return "Magical %s themed celebration scene, warm golden lighting, joyful atmosphere, painterly style"
                .formatted(eventType.toLowerCase());
        // TODO: replace with actual Spring AI chat call once VertexAiGeminiChatModel is wired
    }

    // -------------------------------------------------------
    // Prompt engineering — the core of what makes outputs great
    // -------------------------------------------------------

    private String buildImagePrompt(Invitation invite) {
        String baseScene = invite.getScenePrompt() != null && !invite.getScenePrompt().isBlank()
                ? invite.getScenePrompt()
                : defaultSceneForEventType(invite.getEventType());

        // Build the text overlay instruction
        String textOverlay = buildTextOverlay(invite);

        // Full prompt — scene + text baking + quality modifiers
        return """
                %s.
                
                Text overlay on the image (bake directly into the scene in an elegant, readable font):
                %s
                
                Style requirements:
                - Cinematic composition with clear focal point
                - Rich, vibrant colors with excellent contrast
                - Text must be clearly legible and beautifully integrated
                - Photorealistic or high-quality illustrated style
                - Aspect ratio: square (1:1)
                - High resolution, sharp details
                - No watermarks, no signatures
                """.formatted(baseScene, textOverlay);
    }

    private String buildTextOverlay(Invitation invite) {
        StringBuilder text = new StringBuilder();
        text.append(invite.getEventTitle()).append("\n");

        if (invite.getHostName() != null) {
            text.append("Hosted by ").append(invite.getHostName()).append("\n");
        }

        // Format date nicely e.g. "Saturday, June 10, 2025"
        if (invite.getEventDate() != null) {
            String formattedDate = invite.getEventDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            text.append(formattedDate);
        }

        if (invite.getEventTime() != null) {
            String formattedTime = invite.getEventTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
            text.append(" at ").append(formattedTime).append("\n");
        }

        if (invite.getVenueName() != null) {
            text.append(invite.getVenueName());
        }

        return text.toString().trim();
    }

    private String defaultSceneForEventType(Invitation.EventType eventType) {
        return switch (eventType) {
            case BIRTHDAY -> "Magical birthday celebration scene with colorful balloons, " +
                    "confetti, and festive decorations, warm golden lighting";
            case WEDDING -> "Romantic garden wedding scene with roses, fairy lights, " +
                    "and soft golden hour sunlight, elegant atmosphere";
            case PARTY -> "Vibrant and festive party scene with colorful decorations, " +
                    "streamers, and joyful celebration atmosphere";
            case BABY_SHOWER -> "Soft and gentle baby shower scene with pastel colors, " +
                    "baby animals, and whimsical decorations";
            case GRADUATION -> "Proud graduation celebration scene with academic cap and " +
                    "diploma, confetti, bright and optimistic atmosphere";
            case CORPORATE -> "Professional and elegant corporate event scene with " +
                    "modern architectural setting, sophisticated lighting";
        };
    }
}
