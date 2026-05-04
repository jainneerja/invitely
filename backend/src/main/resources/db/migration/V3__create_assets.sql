CREATE TYPE asset_type AS ENUM (
    'USER_UPLOAD',
    'AI_GENERATED_IMAGE',
    'AI_ANIMATED_VIDEO'
);

CREATE TABLE assets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_id   UUID NOT NULL REFERENCES invitations(id) ON DELETE CASCADE,
    asset_type  asset_type NOT NULL,
    file_key    VARCHAR(500) NOT NULL,   -- S3 key or local path
    url         VARCHAR(1000) NOT NULL,
    mime_type   VARCHAR(100),
    size_bytes  BIGINT,
    width_px    INTEGER,
    height_px   INTEGER,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assets_invite_id  ON assets(invite_id);
CREATE INDEX idx_assets_type       ON assets(asset_type);
