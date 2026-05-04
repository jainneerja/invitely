package com.invitely.controller;

import com.invitely.dto.TemplateResponse;
import com.invitely.model.Invitation.EventType;
import com.invitely.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    // GET /api/templates
    // GET /api/templates?eventType=BIRTHDAY
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getTemplates(
            @RequestParam(required = false) EventType eventType) {
        if (eventType != null) {
            return ResponseEntity.ok(templateService.getTemplatesForEventType(eventType));
        }
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    // GET /api/templates/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getById(id));
    }
}
