package com.invitely.service;

import com.invitely.model.Asset;
import com.invitely.model.Asset.AssetType;
import com.invitely.model.Invitation;
import com.invitely.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final S3Client s3Client;
    private final InvitationRepository invitationRepository;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    // -------------------------------------------------------
    // Store base64 image from Gemini → S3
    // -------------------------------------------------------

    @Transactional
    public String storeBase64Image(UUID inviteId, String base64Data,
                                    String mimeType, AssetType assetType) {
        Invitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Invitation not found: " + inviteId));

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        String extension = mimeType.contains("png") ? "png" : "jpg";
        String fileKey = "invites/%s/ai-image-%s.%s"
                .formatted(inviteId, UUID.randomUUID(), extension);

        // Upload to S3
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileKey)
                        .contentType(mimeType)
                        .contentLength((long) imageBytes.length)
                        .build(),
                RequestBody.fromBytes(imageBytes));

        String publicUrl = buildPublicUrl(fileKey);

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

        log.info("Stored AI image asset. invite={} key={}", inviteId, fileKey);
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
                .fileKey(videoUrl)   // external URL as key
                .url(videoUrl)
                .mimeType("video/mp4")
                .build();

        invite.getAssets().add(asset);
        invitationRepository.save(invite);

        log.info("Stored video asset reference. invite={} url={}", inviteId, videoUrl);
    }

    // -------------------------------------------------------
    // Build public S3 URL
    // -------------------------------------------------------

    private String buildPublicUrl(String fileKey) {
        return "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(bucket, region, fileKey);
    }
}
