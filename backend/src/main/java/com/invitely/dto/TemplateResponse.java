package com.invitely.dto;

import com.invitely.model.Invitation.EventType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class TemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private EventType eventType;
    private Map<String, Object> config;
    private String previewUrl;
    private Integer sortOrder;
}
