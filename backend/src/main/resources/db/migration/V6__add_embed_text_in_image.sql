-- Tracks whether the AI generated an image with the invitation text baked in.
-- When true, the guest page suppresses its own text overlay to avoid duplication.
ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS embed_text_in_image BOOLEAN NOT NULL DEFAULT FALSE;
