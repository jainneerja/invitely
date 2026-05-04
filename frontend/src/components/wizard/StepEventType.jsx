import { motion } from 'framer-motion'
import { useInviteStore } from '../../store/inviteStore'

const EVENT_TYPES = [
  { value: 'BIRTHDAY',    emoji: '🎂', label: 'Birthday' },
  { value: 'WEDDING',     emoji: '💍', label: 'Wedding' },
  { value: 'PARTY',       emoji: '🎉', label: 'Party' },
  { value: 'BABY_SHOWER', emoji: '🍼', label: 'Baby Shower' },
  { value: 'GRADUATION',  emoji: '🎓', label: 'Graduation' },
  { value: 'CORPORATE',   emoji: '💼', label: 'Corporate' },
]

export default function StepEventType() {
  const { form, setForm, nextStep } = useInviteStore()

  const select = (value) => {
    setForm({ eventType: value })
    setTimeout(nextStep, 300) // brief pause so selection animates
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto', padding: '2.5rem 2rem' }}>
      <h2 style={{ fontSize: 22, fontWeight: 600, marginBottom: 6 }}>
        What's the occasion?
      </h2>
      <p style={{ fontSize: 13, color: 'var(--text-2)', marginBottom: 28 }}>
        Choose your event type to get started
      </p>

      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12,
      }}>
        {EVENT_TYPES.map((et, i) => (
          <motion.button
            key={et.value}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.06 }}
            whileHover={{ scale: 1.03 }}
            whileTap={{ scale: 0.97 }}
            onClick={() => select(et.value)}
            style={{
              background: form.eventType === et.value
                ? 'var(--purple-lt)' : 'var(--surface)',
              border: `1px solid ${form.eventType === et.value
                ? 'var(--purple)' : 'var(--border)'}`,
              borderRadius: 'var(--radius-lg)',
              padding: '20px 12px',
              textAlign: 'center',
              cursor: 'pointer',
              transition: 'all 0.15s',
            }}
          >
            <div style={{ fontSize: 28, marginBottom: 8 }}>{et.emoji}</div>
            <div style={{
              fontSize: 13, fontWeight: 500,
              color: form.eventType === et.value ? 'var(--purple-dk)' : 'var(--text-1)',
            }}>
              {et.label}
            </div>
          </motion.button>
        ))}
      </div>
    </div>
  )
}
