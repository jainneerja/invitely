import { useState } from 'react'
import { motion } from 'framer-motion'
import { submitRsvp } from '../../api/inviteApi'
import toast from 'react-hot-toast'
import { Loader } from 'lucide-react'

const STATUS_OPTIONS = [
  { value: 'ATTENDING',     emoji: '🎉', label: "Yes, I'll be there!" },
  { value: 'NOT_ATTENDING', emoji: '😔', label: "Sorry, can't make it" },
  { value: 'MAYBE',         emoji: '🤔', label: 'Maybe' },
]

export default function RsvpForm({ slug, rsvpDeadline, onSuccess }) {
  const [form, setForm] = useState({
    guestName: '', guestEmail: '', status: '', message: '', guestCount: 1,
  })
  const [loading, setLoading] = useState(false)

  const update = (fields) => setForm((f) => ({ ...f, ...fields }))

  const handleSubmit = async () => {
    if (!form.guestName || !form.status) {
      toast.error('Please enter your name and response')
      return
    }
    setLoading(true)
    try {
      await submitRsvp(slug, form)
      onSuccess?.()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="card"
      style={{ width: '100%', maxWidth: 480 }}
    >
      <h3 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>
        Will you be attending?
      </h3>
      {rsvpDeadline && (
        <p style={{ fontSize: 12, color: 'var(--text-3)', marginBottom: 16 }}>
          Please RSVP by {rsvpDeadline}
        </p>
      )}

      {/* Status buttons */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        {STATUS_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => update({ status: opt.value })}
            style={{
              flex: 1, padding: '10px 8px',
              border: `1px solid ${form.status === opt.value ? 'var(--purple)' : 'var(--border)'}`,
              borderRadius: 'var(--radius-md)',
              background: form.status === opt.value ? 'var(--purple-lt)' : 'var(--surface)',
              cursor: 'pointer', transition: 'all 0.15s',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: 20, marginBottom: 3 }}>{opt.emoji}</div>
            <div style={{
              fontSize: 10, fontWeight: 500,
              color: form.status === opt.value ? 'var(--purple-dk)' : 'var(--text-2)',
            }}>
              {opt.label}
            </div>
          </button>
        ))}
      </div>

      <div className="field">
        <label>Your name *</label>
        <input
          placeholder="Full name"
          value={form.guestName}
          onChange={(e) => update({ guestName: e.target.value })}
        />
      </div>

      <div className="row">
        <div className="field">
          <label>Email (optional)</label>
          <input
            type="email"
            placeholder="your@email.com"
            value={form.guestEmail}
            onChange={(e) => update({ guestEmail: e.target.value })}
          />
        </div>
        <div className="field">
          <label>Number of guests</label>
          <input
            type="number" min={1} max={10}
            value={form.guestCount}
            onChange={(e) => update({ guestCount: parseInt(e.target.value) || 1 })}
          />
        </div>
      </div>

      <div className="field">
        <label>Message to host (optional)</label>
        <textarea
          placeholder="Leave a note..."
          value={form.message}
          onChange={(e) => update({ message: e.target.value })}
          style={{ minHeight: 60 }}
        />
      </div>

      <button
        className="btn btn-primary"
        style={{ width: '100%', justifyContent: 'center', padding: '11px' }}
        onClick={handleSubmit}
        disabled={loading}
      >
        {loading ? <><Loader size={14} /> Sending...</> : 'Send RSVP'}
      </button>
    </motion.div>
  )
}
