package com.invitely.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Slf4j
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.access-key:local}")
    private String accessKey;

    @Value("${aws.s3.secret-key:local}")
    private String secretKey;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    /**
     * Real S3 client — only created when profile=prod
     * (i.e. access-key is NOT the placeholder "local")
     */
    @Bean
    @ConditionalOnProperty(
            name = "aws.s3.access-key",
            matchIfMissing = false)
    public S3Client s3Client() {
        if ("local".equals(accessKey)) {
            log.info("S3 disabled — local file storage active (dev profile)");
            return null;
        }

        log.info("S3 enabled — region={} bucket configured", region);

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
