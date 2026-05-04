CREATE TYPE rsvp_status AS ENUM (
    'ATTENDING',
    'NOT_ATTENDING',
    'MAYBE'
);

CREATE TABLE rsvp_responses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_id   UUID NOT NULL REFERENCES invitations(id) ON DELETE CASCADE,
    guest_name  VARCHAR(100) NOT NULL,
    guest_email VARCHAR(200),
    status      rsvp_status NOT NULL,
    message     TEXT,
    guest_count INTEGER NOT NULL DEFAULT 1,   -- how many people they're bringing
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rsvp_invite_id ON rsvp_responses(invite_id);
CREATE INDEX idx_rsvp_status    ON rsvp_responses(invite_id, status);

-- Handy view for host dashboard — RSVP summary per invite
CREATE VIEW rsvp_summary AS
SELECT
    invite_id,
    COUNT(*)                                                    AS total_responses,
    COUNT(*) FILTER (WHERE status = 'ATTENDING')                AS attending,
    COUNT(*) FILTER (WHERE status = 'NOT_ATTENDING')            AS not_attending,
    COUNT(*) FILTER (WHERE status = 'MAYBE')                    AS maybe,
    COALESCE(SUM(guest_count) FILTER (WHERE status = 'ATTENDING'), 0) AS total_guests
FROM rsvp_responses
GROUP BY invite_id;
