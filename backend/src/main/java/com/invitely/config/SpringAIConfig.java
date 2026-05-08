package com.invitely.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    @Bean(name = "vertexRestTemplate")
    public RestTemplate vertexRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public VertexAiGeminiChatOptions geminiChatOptions() {
        return VertexAiGeminiChatOptions.builder()
                .model("gemini-2.0-flash")
                .temperature(0.8)
                .maxOutputTokens(200)
                .build();
    }
}
