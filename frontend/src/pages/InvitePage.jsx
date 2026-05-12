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
      justifyContent: 'center', background: '#faf7f2',
      color: '#9c8a7a', fontSize: 14,
    }}>
      Loading your invitation...
    </div>
  )

  if (error) return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: '#faf7f2',
      flexDirection: 'column', gap: 12,
    }}>
      <div style={{ fontSize: 36 }}>😔</div>
      <div style={{ color: '#9c8a7a' }}>{error}</div>
    </div>
  )

  const hasEmbeddedText = invite.embedTextInImage === true
  const mediaUrl = invite.animatedVideoUrl || invite.generatedImageUrl

  return (
    <div style={{ minHeight: '100vh', background: '#faf7f2' }}>

      {/* ── Card ── */}
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        padding: '2rem 1rem 0',
      }}>
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          style={{
            width: '100%', maxWidth: 480,
            borderRadius: 20, overflow: 'hidden',
            boxShadow: '0 8px 40px rgba(0,0,0,0.12)',
            background: '#fff',
          }}
        >
          {/* Media — contained, never cropped */}
          {invite.animatedVideoUrl ? (
            <video
              src={invite.animatedVideoUrl}
              autoPlay loop muted playsInline
              style={{ width: '100%', display: 'block', aspectRatio: '1/1', objectFit: 'cover' }}
            />
          ) : invite.generatedImageUrl ? (
            <img
              src={invite.generatedImageUrl}
              alt={invite.eventTitle}
              style={{ width: '100%', display: 'block' }}
            />
          ) : (
            <div style={{
              aspectRatio: '1/1', width: '100%',
              background: 'linear-gradient(160deg,#1e1245,#2d1f5e,#1a3a2a)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: 'rgba(255,255,255,0.3)', fontSize: 13,
            }}>
              No image yet
            </div>
          )}

          {/* Details panel — only shown when text is NOT embedded in image */}
          {!hasEmbeddedText && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3 }}
              style={{ padding: '1.5rem 1.5rem 1.25rem', background: '#fff' }}
            >
              <h1 style={{
                fontSize: 24, fontWeight: 700, color: '#1a1a1a',
                fontFamily: "'Playfair Display', serif",
                margin: '0 0 4px',
              }}>
                {invite.eventTitle}
              </h1>
              <p style={{ fontSize: 14, color: '#666', margin: '0 0 16px' }}>
                Hosted by <strong style={{ color: '#333' }}>{invite.hostName}</strong>
              </p>

              {/* Chips */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: invite.personalMessage ? 16 : 0 }}>
                {[
                  { icon: Calendar, text: invite.eventDate
                      ? format(new Date(invite.eventDate), 'MMMM d, yyyy') : null },
                  { icon: Clock,    text: invite.eventTime },
                  { icon: MapPin,   text: invite.venueName },
                ].filter(d => d.text).map(d => (
                  <div key={d.text} style={{
                    display: 'flex', alignItems: 'center', gap: 6,
                    background: '#f5f0eb', borderRadius: 99,
                    padding: '5px 12px', fontSize: 12, color: '#555',
                    border: '1px solid #e8e0d8',
                  }}>
                    <d.icon size={12} color="#9c7c5a" />
                    {d.text}
                  </div>
                ))}
              </div>

              {invite.personalMessage && (
                <p style={{
                  fontSize: 13, color: '#7a6a5a', fontStyle: 'italic',
                  lineHeight: 1.6, margin: 0,
                  borderTop: '1px solid #f0e8e0', paddingTop: 14,
                }}>
                  "{invite.personalMessage}"
                </p>
              )}
            </motion.div>
          )}
        </motion.div>

        {/* ── RSVP ── */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          style={{ width: '100%', maxWidth: 480, padding: '1.5rem 0 2rem' }}
        >
          {rsvpDone ? (
            <div style={{
              textAlign: 'center', background: '#fff',
              borderRadius: 16, padding: '2rem',
              boxShadow: '0 4px 20px rgba(0,0,0,0.07)',
            }}>
              <div style={{ fontSize: 40, marginBottom: 12 }}>🎉</div>
              <h3 style={{ fontSize: 18, fontWeight: 600, margin: '0 0 6px', color: '#1a1a1a' }}>
                You're all set!
              </h3>
              <p style={{ fontSize: 13, color: '#888', margin: 0 }}>
                Your RSVP has been recorded. See you there!
              </p>
            </div>
          ) : (
            <RsvpForm
              slug={slug}
              rsvpDeadline={invite.rsvpDeadline}
              onSuccess={() => setRsvpDone(true)}
            />
          )}
        </motion.div>
      </div>

      {/* Watermark */}
      <div style={{
        textAlign: 'center', padding: '1rem',
        borderTop: '1px solid #ede6dc',
        fontSize: 11, color: '#bbb', background: '#faf7f2',
      }}>
        Made with <span className="gradient-text" style={{ fontWeight: 600 }}>invitely</span>
      </div>
    </div>
  )
}
