CREATE TABLE templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    event_type  event_type,              -- null = works for all event types
    config      JSONB NOT NULL,          -- full JSON template definition
    preview_url VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_templates_event_type ON templates(event_type);
CREATE INDEX idx_templates_active     ON templates(is_active);

-- Add FK from invitations to templates
ALTER TABLE invitations
    ADD CONSTRAINT fk_invitations_template
    FOREIGN KEY (template_id) REFERENCES templates(id)
    ON DELETE SET NULL;

-- Seed 3 prebuilt templates
INSERT INTO templates (name, description, event_type, config, sort_order) VALUES
(
    'Jungle Magic',
    'Warm jungle tones with animated animals — perfect for fun birthdays',
    'BIRTHDAY',
    '{
        "template_id": "birthday-jungle-001",
        "theme": {
            "primary_color": "#EF9F27",
            "secondary_color": "#1a3a2a",
            "font_heading": "Playfair Display",
            "font_body": "Lato",
            "background": "deep-jungle"
        },
        "default_scene_prompt": "Hakuna matata jungle scene, golden sunset, lions elephants giraffes, magical and warm, painterly style",
        "default_animation_style": "animals_walk",
        "sections": [
            {"id": "hero",    "type": "hero",           "order": 1, "animation": {"type": "fade-in",  "duration": 1200, "delay": 0}},
            {"id": "details", "type": "event-details",  "order": 2, "animation": {"type": "slide-up", "duration": 800,  "delay": 400}},
            {"id": "gallery", "type": "photo-gallery",  "order": 3, "animation": {"type": "carousel", "duration": 600,  "delay": 800}},
            {"id": "message", "type": "personal-message","order": 4, "animation": {"type": "fade-in",  "duration": 700,  "delay": 600}},
            {"id": "rsvp",    "type": "rsvp-form",      "order": 5, "animation": {"type": "slide-up", "duration": 600,  "delay": 1000}}
        ]
    }',
    1
),
(
    'Elegant Wedding',
    'Gold and cream, timeless serif fonts, soft fade animations',
    'WEDDING',
    '{
        "template_id": "wedding-elegant-001",
        "theme": {
            "primary_color": "#c9a96e",
            "secondary_color": "#f5f0e8",
            "font_heading": "Cormorant Garamond",
            "font_body": "Lato",
            "background": "parchment-texture"
        },
        "default_scene_prompt": "Romantic garden wedding scene, golden hour light, roses and fairy lights, soft watercolor style",
        "default_animation_style": "leaves_sway",
        "sections": [
            {"id": "hero",    "type": "hero",           "order": 1, "animation": {"type": "fade-in",  "duration": 1500, "delay": 0}},
            {"id": "details", "type": "event-details",  "order": 2, "animation": {"type": "slide-up", "duration": 900,  "delay": 500}},
            {"id": "gallery", "type": "photo-gallery",  "order": 3, "animation": {"type": "carousel", "duration": 700,  "delay": 900}},
            {"id": "message", "type": "personal-message","order": 4, "animation": {"type": "fade-in",  "duration": 800,  "delay": 700}},
            {"id": "rsvp",    "type": "rsvp-form",      "order": 5, "animation": {"type": "slide-up", "duration": 600,  "delay": 1200}}
        ]
    }',
    2
),
(
    'Confetti Party',
    'Bold colors, confetti burst, high energy — for any celebration',
    'PARTY',
    '{
        "template_id": "party-confetti-001",
        "theme": {
            "primary_color": "#7F77DD",
            "secondary_color": "#FBEAF0",
            "font_heading": "Nunito",
            "font_body": "Nunito",
            "background": "colorful-burst"
        },
        "default_scene_prompt": "Colorful confetti party scene, balloons streamers, festive and joyful, vibrant 3D render style",
        "default_animation_style": "confetti",
        "sections": [
            {"id": "hero",    "type": "hero",           "order": 1, "animation": {"type": "bounce-in", "duration": 800,  "delay": 0}},
            {"id": "details", "type": "event-details",  "order": 2, "animation": {"type": "pop-in",    "duration": 600,  "delay": 300}},
            {"id": "gallery", "type": "photo-gallery",  "order": 3, "animation": {"type": "carousel",  "duration": 500,  "delay": 600}},
            {"id": "message", "type": "personal-message","order": 4, "animation": {"type": "fade-in",   "duration": 600,  "delay": 500}},
            {"id": "rsvp",    "type": "rsvp-form",      "order": 5, "animation": {"type": "slide-up",   "duration": 500,  "delay": 800}}
        ]
    }',
    3
);
