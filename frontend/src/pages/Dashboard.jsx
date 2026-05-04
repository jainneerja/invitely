import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { getInviteById, getRsvps } from '../api/inviteApi'
import { format } from 'date-fns'
import { Users, CheckCircle, XCircle, HelpCircle, Copy, ExternalLink } from 'lucide-react'
import toast from 'react-hot-toast'

export default function Dashboard() {
  const { id } = useParams()
  const [invite, setInvite] = useState(null)
  const [rsvps, setRsvps]   = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getInviteById(id), getRsvps(id)])
      .then(([inv, rs]) => { setInvite(inv); setRsvps(rs) })
      .catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <div style={{
      minHeight: '100vh', display: 'flex',
      alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg)', color: 'var(--text-3)',
    }}>
      Loading dashboard...
    </div>
  )

  const summary = invite?.rsvpSummary || {}
  const shareUrl = invite?.shareUrl || ''

  const stats = [
    { icon: Users,       label: 'Total responses', value: summary.totalResponses || 0, color: 'var(--purple)' },
    { icon: CheckCircle, label: 'Attending',        value: summary.attending || 0,      color: 'var(--green)'  },
    { icon: XCircle,     label: "Can't make it",   value: summary.notAttending || 0,   color: 'var(--red)'    },
    { icon: HelpCircle,  label: 'Maybe',            value: summary.maybe || 0,          color: 'var(--amber)'  },
  ]

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      {/* Topbar */}
      <nav style={{
        background: 'var(--surface)', borderBottom: '1px solid var(--border)',
        padding: '0 2rem', height: 56,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <a href="/" className="gradient-text" style={{ fontSize: 18, fontWeight: 600 }}>
          invitely
        </a>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="btn"
            style={{ fontSize: 12 }}
            onClick={() => { navigator.clipboard.writeText(shareUrl); toast.success('Link copied!') }}
          >
            <Copy size={13} /> Copy invite link
          </button>
          <button
            className="btn btn-primary"
            style={{ fontSize: 12 }}
            onClick={() => window.open(shareUrl, '_blank')}
          >
            <ExternalLink size={13} /> View invite
          </button>
        </div>
      </nav>

      <div style={{ maxWidth: 860, margin: '0 auto', padding: '1.5rem 2rem' }}>
        {/* Header */}
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ fontSize: 22, fontWeight: 600, marginBottom: 4 }}>
            {invite?.eventTitle}
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-3)' }}>
            {invite?.eventDate
              ? format(new Date(invite.eventDate), 'MMMM d, yyyy')
              : ''
            }
            {invite?.venueName ? ` · ${invite.venueName}` : ''}
          </p>
        </div>

        {/* Stats */}
        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(4,1fr)',
          gap: 12, marginBottom: 24,
        }}>
          {stats.map((s, i) => (
            <motion.div
              key={s.label}
              className="card"
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.08 }}
              style={{ textAlign: 'center', padding: '1.25rem 1rem' }}
            >
              <s.icon size={22} color={s.color} style={{ marginBottom: 8 }} />
              <div style={{ fontSize: 28, fontWeight: 600, color: s.color }}>
                {s.value}
              </div>
              <div style={{ fontSize: 11, color: 'var(--text-3)', marginTop: 2 }}>
                {s.label}
              </div>
            </motion.div>
          ))}
        </div>

        {/* Total guests badge */}
        {summary.totalGuests > 0 && (
          <div style={{
            background: 'var(--purple-lt)', border: '1px solid #AFA9EC',
            borderRadius: 'var(--radius-md)', padding: '10px 16px',
            fontSize: 13, color: 'var(--purple-dk)', marginBottom: 20,
            display: 'flex', alignItems: 'center', gap: 8,
          }}>
            <Users size={15} />
            <strong>{summary.totalGuests}</strong> total guests attending
          </div>
        )}

        {/* RSVP list */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{
            padding: '14px 20px', borderBottom: '1px solid var(--border)',
            fontWeight: 600, fontSize: 14,
          }}>
            Guest responses ({rsvps.length})
          </div>

          {rsvps.length === 0 ? (
            <div style={{
              padding: '40px', textAlign: 'center',
              color: 'var(--text-3)', fontSize: 13,
            }}>
              No RSVPs yet — share your invite link to get responses!
            </div>
          ) : (
            rsvps.map((r, i) => (
              <motion.div
                key={r.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.04 }}
                style={{
                  display: 'flex', alignItems: 'center',
                  padding: '12px 20px',
                  borderBottom: i < rsvps.length - 1 ? '1px solid var(--border)' : 'none',
                  gap: 12,
                }}
              >
                {/* Status indicator */}
                <div style={{
                  width: 32, height: 32, borderRadius: '50%',
                  background: r.status === 'ATTENDING'
                    ? '#E6F9F3'
                    : r.status === 'NOT_ATTENDING'
                    ? '#FEE2E2' : '#FEF9E7',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 14, flexShrink: 0,
                }}>
                  {r.status === 'ATTENDING'
                    ? '🎉'
                    : r.status === 'NOT_ATTENDING'
                    ? '😔' : '🤔'}
                </div>

                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500, fontSize: 14 }}>
                    {r.guestName}
                    {r.guestCount > 1 && (
                      <span style={{
                        fontSize: 11, color: 'var(--text-3)',
                        marginLeft: 6,
                      }}>
                        +{r.guestCount - 1} guest{r.guestCount > 2 ? 's' : ''}
                      </span>
                    )}
                  </div>
                  {r.message && (
                    <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>
                      "{r.message}"
                    </div>
                  )}
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{
                    fontSize: 11, fontWeight: 500,
                    color: r.status === 'ATTENDING'
                      ? 'var(--green)'
                      : r.status === 'NOT_ATTENDING'
                      ? 'var(--red)' : 'var(--amber)',
                  }}>
                    {r.status === 'ATTENDING'
                      ? 'Attending'
                      : r.status === 'NOT_ATTENDING'
                      ? "Can't attend" : 'Maybe'}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-3)', marginTop: 2 }}>
                    {r.createdAt
                      ? format(new Date(r.createdAt), 'MMM d, h:mm a')
                      : ''}
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
