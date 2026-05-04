CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE event_type AS ENUM (
    'BIRTHDAY',
    'WEDDING',
    'PARTY',
    'BABY_SHOWER',
    'GRADUATION',
    'CORPORATE'
);

CREATE TYPE invite_status AS ENUM (
    'DRAFT',
    'IMAGE_PENDING',
    'IMAGE_READY',
    'VIDEO_PENDING',
    'PUBLISHED'
);

CREATE TABLE invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(12) UNIQUE NOT NULL,

    -- event info
    event_type      event_type NOT NULL,
    host_name       VARCHAR(100) NOT NULL,
    event_title     VARCHAR(150) NOT NULL,
    event_date      DATE NOT NULL,
    event_time      TIME NOT NULL,
    venue_name      VARCHAR(200),
    venue_address   VARCHAR(300),
    personal_message TEXT,

    -- AI generation
    scene_prompt    TEXT,
    generated_image_url TEXT,
    animated_video_url  TEXT,
    animation_style VARCHAR(50) DEFAULT 'animals_walk',

    -- config
    template_id     UUID,
    status          invite_status NOT NULL DEFAULT 'DRAFT',
    rsvp_deadline   DATE,
    max_guests      INTEGER,

    -- meta
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_invitations_slug   ON invitations(slug);
CREATE INDEX idx_invitations_status ON invitations(status);
CREATE INDEX idx_invitations_created ON invitations(created_at DESC);

-- auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invitations_updated_at
    BEFORE UPDATE ON invitations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
