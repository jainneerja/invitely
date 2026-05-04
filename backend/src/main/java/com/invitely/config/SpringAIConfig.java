package com.invitely.config;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiImageModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @Bean
    public VertexAiGeminiImageOptions geminiImageOptions() {
        return VertexAiGeminiImageOptions.builder()
                .withModel("imagen-3.0-generate-001")   // Imagen 3 via Vertex AI
                .withN(1)
                .withWidth(1024)
                .withHeight(1024)
                .build();
    }
}
