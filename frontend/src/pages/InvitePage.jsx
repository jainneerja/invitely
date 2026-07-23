import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { getInviteBySlug } from '../api/inviteApi'
import RsvpForm from '../components/invite/RsvpForm'
import { format } from 'date-fns'
import { MapPin, Calendar, Clock } from 'lucide-react'

export default function InvitePage() {
  const { slug } = useParams()
  const [invite, setInvite] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [rsvpDone, setRsvpDone] = useState(false)

  useEffect(() => {
    getInviteBySlug(slug)
      .then(setInvite)
      .catch(() => setError('This invitation could not be found.'))
      .finally(() => setLoading(false))
  }, [slug])

  if (loading) return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: '#0f0a2e',
      color: 'rgba(255,255,255,0.5)', fontSize: 14,
    }}>
      Loading your invitation...
    </div>
  )

  if (error) return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'var(--bg)',
      flexDirection: 'column', gap: 12,
    }}>
      <div style={{ fontSize: 36 }}>😔</div>
      <div style={{ color: 'var(--text-2)' }}>{error}</div>
    </div>
  )

  const bg = invite.animatedVideoUrl || invite.generatedImageUrl

  return (
    <div style={{ minHeight: '100vh', background: '#0f0a2e' }}>
      {/* Hero — full-screen video or image */}
      <div style={{
        position: 'relative', minHeight: '100vh',
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        // Own dark background so the letterbox around a contained card never
        // falls through to the light body background (--bg #F3F3F0).
        background: '#0f0a2e',
      }}>
        {/* Deep ambient backdrop for contained (baked-in) cards: a heavily
            darkened blur of the card, plus a dark scrim so it stays a rich dark
            field (a lightbox look) regardless of how light the card artwork is */}
        {invite.embedTextInImage && invite.generatedImageUrl && (
          <>
            <div style={{
              position: 'absolute', inset: 0,
              backgroundImage: `url(${invite.generatedImageUrl})`,
              backgroundSize: 'cover', backgroundPosition: 'center',
              filter: 'blur(48px) brightness(0.32) saturate(1.2)',
              transform: 'scale(1.2)',
            }} />
            <div style={{
              position: 'absolute', inset: 0,
              background: 'rgba(10, 6, 30, 0.55)',
            }} />
          </>
        )}

        {/* Background media */}
        {invite.animatedVideoUrl ? (
          <video
            src={invite.animatedVideoUrl}
            autoPlay loop muted playsInline
            style={{
              position: 'absolute', inset: 0,
              width: '100%', height: '100%',
              objectFit: invite.embedTextInImage ? 'contain' : 'cover',
              opacity: invite.embedTextInImage ? 1 : 0.85,
            }}
          />
        ) : invite.generatedImageUrl ? (
          <img
            src={invite.generatedImageUrl}
            alt={invite.eventTitle}
            style={{
              position: 'absolute', inset: 0,
              width: '100%', height: '100%',
              // Baked-in cards: show the WHOLE card (contain) at full opacity.
              // Scene-only images: fill the hero (cover) and dim for overlay text.
              objectFit: invite.embedTextInImage ? 'contain' : 'cover',
              opacity: invite.embedTextInImage ? 1 : 0.85,
            }}
          />
        ) : (
          <div style={{
            position: 'absolute', inset: 0,
            background: 'linear-gradient(160deg,#0f0a2e,#1e1245,#2d1f5e)',
          }} />
        )}

        {/* Gradient overlay — skipped for baked-in cards so their text isn't dimmed */}
        {!invite.embedTextInImage && (
          <div style={{
            position: 'absolute', inset: 0,
            background: 'linear-gradient(to bottom, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.7) 100%)',
          }} />
        )}

        {/* Content — suppressed when the AI baked the text into the image */}
        {!invite.embedTextInImage && (
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          style={{
            position: 'relative', zIndex: 1,
            textAlign: 'center', padding: '2rem',
            maxWidth: 560,
          }}
        >
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            style={{
              fontSize: 38, fontWeight: 600, color: '#EF9F27',
              fontFamily: "'Playfair Display', serif",
              textShadow: '0 2px 16px rgba(0,0,0,0.5)',
              marginBottom: 8,
            }}
          >
            {invite.eventTitle}
          </motion.h1>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
            style={{ color: 'rgba(255,255,255,0.75)', fontSize: 15, marginBottom: 24 }}
          >
            Hosted by <strong style={{ color: '#fff' }}>{invite.hostName}</strong>
          </motion.p>

          {/* Event details chips */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.7 }}
            style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}
          >
            {[
              { icon: Calendar, text: invite.eventDate
                  ? format(new Date(invite.eventDate), 'MMMM d, yyyy') : null },
              { icon: Clock,    text: invite.eventTime },
              { icon: MapPin,   text: invite.venueName },
            ].filter((d) => d.text).map((d) => (
              <div key={d.text} style={{
                display: 'flex', alignItems: 'center', gap: 6,
                background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(8px)',
                borderRadius: 99, padding: '6px 14px',
                fontSize: 13, color: 'rgba(255,255,255,0.9)',
                border: '1px solid rgba(255,255,255,0.15)',
              }}>
                <d.icon size={13} />
                {d.text}
              </div>
            ))}
          </motion.div>

          {/* Personal message */}
          {invite.personalMessage && (
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.9 }}
              style={{
                marginTop: 24, fontSize: 14, color: 'rgba(255,255,255,0.65)',
                fontStyle: 'italic', lineHeight: 1.6,
                maxWidth: 420, margin: '24px auto 0',
              }}
            >
              "{invite.personalMessage}"
            </motion.p>
          )}
        </motion.div>
        )}

        {/* Scroll indicator */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.2 }}
          style={{
            position: 'absolute', bottom: 32, zIndex: 1,
            color: 'rgba(255,255,255,0.4)', fontSize: 12,
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6,
          }}
        >
          <span>RSVP below</span>
          <motion.div
            animate={{ y: [0, 6, 0] }}
            transition={{ repeat: Infinity, duration: 1.5 }}
          >↓</motion.div>
        </motion.div>
      </div>

      {/* RSVP section */}
      <div style={{
        background: 'var(--bg)', padding: '3rem 2rem',
        display: 'flex', justifyContent: 'center',
      }}>
        {rsvpDone ? (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            style={{ textAlign: 'center', maxWidth: 400 }}
          >
            <div style={{ fontSize: 48, marginBottom: 16 }}>🎉</div>
            <h3 style={{ fontSize: 20, fontWeight: 600, marginBottom: 8 }}>
              You're all set!
            </h3>
            <p style={{ fontSize: 14, color: 'var(--text-2)' }}>
              Your RSVP has been recorded. See you there!
            </p>
          </motion.div>
        ) : (
          <RsvpForm
            slug={slug}
            rsvpDeadline={invite.rsvpDeadline}
            onSuccess={() => setRsvpDone(true)}
          />
        )}
      </div>

      {/* Invitely watermark */}
      <div style={{
        textAlign: 'center', padding: '1rem',
        background: 'var(--surface)', borderTop: '1px solid var(--border)',
        fontSize: 11, color: 'var(--text-3)',
      }}>
        Made with <span className="gradient-text" style={{ fontWeight: 600 }}>invitely</span>
      </div>
    </div>
  )
}
