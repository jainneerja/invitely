package com.invitely.repository;

import com.invitely.model.Invitation;
import com.invitely.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    // all active templates for a specific event type + universal templates (null event_type)
    List<Template> findByIsActiveTrueAndEventTypeOrderBySortOrder(Invitation.EventType eventType);

    List<Template> findByIsActiveTrueOrderBySortOrder();
}
