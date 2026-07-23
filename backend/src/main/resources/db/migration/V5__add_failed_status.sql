-- Adds a FAILED terminal status to invite_status.
-- Lets the UI distinguish "generation still running" (IMAGE_PENDING / VIDEO_PENDING)
-- from "generation failed, offer retry" (FAILED) from "not started" (DRAFT).
-- Previously a failed AI generation rolled back to DRAFT, which is indistinguishable
-- from an invite the user never tried to generate. See issue #2.

ALTER TYPE invite_status ADD VALUE IF NOT EXISTS 'FAILED';
