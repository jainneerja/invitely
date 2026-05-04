package com.invitely.service;

import com.invitely.dto.TemplateResponse;
import com.invitely.model.Invitation.EventType;
import com.invitely.model.Template;
import com.invitely.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findByIsActiveTrueOrderBySortOrder()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplatesForEventType(EventType eventType) {
        return templateRepository
                .findByIsActiveTrueAndEventTypeOrderBySortOrder(eventType)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TemplateResponse getById(UUID id) {
        return templateRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
    }

    private TemplateResponse toResponse(Template t) {
        return TemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .eventType(t.getEventType())
                .config(t.getConfig())
                .previewUrl(t.getPreviewUrl())
                .sortOrder(t.getSortOrder())
                .build();
    }
}
