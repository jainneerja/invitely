package com.invitely.service;

import com.invitely.model.Asset;
import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
import com.invitely.repository.InvitationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class AssetService {

    private final InvitationRepository invitationRepository;

    @Nullable
    private final S3Client s3Client;   // null when dev profile active

    @Value("${invitely.base-url}")
    private String baseUrl;

    @Value("${invitely.asset-storage-path:uploads}")
    private String storagePath;

    @Value("${aws.s3.bucket:invitely-dev-local}")
    private String s3Bucket;

    @Value("${aws.s3.region:us-east-1}")
    private String s3Region;

    // S3Client is optional — null when dev profile (local file storage)
    @Autowired
    public AssetService(InvitationRepository invitationRepository,
                        @Nullable S3Client s3Client) {
        this.invitationRepository = invitationRepository;
        this.s3Client = s3Client;
    }

    // -------------------------------------------------------
    // Store AI-generated image
    // Uses S3 in prod, local filesystem in dev
    // -------------------------------------------------------

    @Transactional
    public String storeBase64Image(UUID inviteId, String base64Data,
                                    String mimeType, AssetType assetType) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        String extension  = mimeType.contains("png") ? "png" : "jpg";
        String fileName   = "ai-image-" + UUID.randomUUID() + "." + extension;
        String fileKey    = "invites/" + inviteId + "/" + fileName;

        String publicUrl;

        if (s3Client != null) {
            // --- PROD: upload to S3 ---
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(fileKey)
                            .contentType(mimeType)
                            .contentLength((long) imageBytes.length)
                            .build(),
                    RequestBody.fromBytes(imageBytes));

            publicUrl = "https://%s.s3.%s.amazonaws.com/%s"
                    .formatted(s3Bucket, s3Region, fileKey);
            log.info("Image uploaded to S3. invite={} key={}", inviteId, fileKey);

        } else {
            // --- DEV: save to local filesystem ---
            try {
                Path uploadDir = Paths.get(storagePath, "invites", inviteId.toString());
                Files.createDirectories(uploadDir);
                Files.write(uploadDir.resolve(fileName), imageBytes);
                publicUrl = baseUrl + "/uploads/" + fileKey;
                log.info("Image saved locally. invite={} path={}", inviteId,
                        uploadDir.resolve(fileName));
            } catch (IOException e) {
                throw new RuntimeException("Failed to save image locally: " + e.getMessage(), e);
            }
        }

        // Persist asset record
        Asset asset = Asset.builder()
                .invitation(invite)
                .assetType(assetType)
                .fileKey(fileKey)
                .url(publicUrl)
                .mimeType(mimeType)
                .sizeBytes((long) imageBytes.length)
                .build();

        invite.getAssets().add(asset);
        invitationRepository.save(invite);

        return publicUrl;
    }

    // -------------------------------------------------------
    // Store video URL reference (video lives on provider CDN)
    // -------------------------------------------------------

    @Transactional
    public void storeVideoAsset(UUID inviteId, String videoUrl, AssetType assetType) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        Asset asset = Asset.builder()
                .invitation(invite)
                .assetType(assetType)
                .fileKey(videoUrl)
                .url(videoUrl)
                .mimeType("video/mp4")
                .build();

        invite.getAssets().add(asset);
        invitationRepository.save(invite);

        log.info("Stored video asset. invite={} url={}", inviteId, videoUrl);
    }

    // -------------------------------------------------------
    // Store AI-generated video (base64 → local file or S3)
    // -------------------------------------------------------

    @Transactional
    public String storeBase64Video(UUID inviteId, String base64Data, String mimeType) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        byte[] videoBytes = Base64.getDecoder().decode(base64Data);
        String fileName   = "ai-video-" + UUID.randomUUID() + ".mp4";
        String fileKey    = "invites/" + inviteId + "/" + fileName;

        String publicUrl;

        if (s3Client != null) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(fileKey)
                            .contentType(mimeType)
                            .contentLength((long) videoBytes.length)
                            .build(),
                    RequestBody.fromBytes(videoBytes));

            publicUrl = "https://%s.s3.%s.amazonaws.com/%s"
                    .formatted(s3Bucket, s3Region, fileKey);
            log.info("Video uploaded to S3. invite={} key={}", inviteId, fileKey);
        } else {
            try {
                Path uploadDir = Paths.get(storagePath, "invites", inviteId.toString());
                Files.createDirectories(uploadDir);
                Files.write(uploadDir.resolve(fileName), videoBytes);
                publicUrl = baseUrl + "/uploads/" + fileKey;
                log.info("Video saved locally. invite={} path={}", inviteId,
                        uploadDir.resolve(fileName));
            } catch (IOException e) {
                throw new RuntimeException("Failed to save video locally: " + e.getMessage(), e);
            }
        }

        Asset asset = Asset.builder()
                .invitation(invite)
                .assetType(AssetType.AI_ANIMATED_VIDEO)
                .fileKey(fileKey)
                .url(publicUrl)
                .mimeType(mimeType)
                .sizeBytes((long) videoBytes.length)
                .build();

        invite.getAssets().add(asset);
        invitationRepository.save(invite);

        return publicUrl;
    }
}
