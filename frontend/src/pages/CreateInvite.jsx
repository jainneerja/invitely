import { useInviteStore } from '../store/inviteStore'
import { motion, AnimatePresence } from 'framer-motion'
import { Check } from 'lucide-react'
import StepEventType from '../components/wizard/StepEventType'
import StepDetails from '../components/wizard/StepDetails'
import StepScene from '../components/wizard/StepScene'
import StepShare from '../components/wizard/StepShare'

const STEPS = [
  { label: 'Event type' },
  { label: 'Event details' },
  { label: 'Scene & image' },
  { label: 'Share' },
]

export default function CreateInvite() {
  const { step } = useInviteStore()

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      {/* Topbar */}
      <nav style={{
        background: 'var(--surface)', borderBottom: '1px solid var(--border)',
        padding: '0 2rem', height: 56,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <a href="/" style={{ fontSize: 18, fontWeight: 600 }} className="gradient-text">
          invitely
        </a>
        <span style={{ fontSize: 12, color: 'var(--text-3)' }}>
          Step {step} of {STEPS.length}
        </span>
      </nav>

      {/* Stepper */}
      <div style={{
        background: 'var(--surface)', borderBottom: '1px solid var(--border)',
        padding: '0 2rem', height: 52,
        display: 'flex', alignItems: 'center', gap: 0,
      }}>
        {STEPS.map((s, i) => {
          const num = i + 1
          const done = step > num
          const active = step === num
          return (
            <div key={s.label} style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 7,
                fontSize: 12,
                color: active ? 'var(--purple)' : done ? 'var(--text-3)' : 'var(--text-3)',
                fontWeight: active ? 500 : 400,
              }}>
                <div style={{
                  width: 22, height: 22, borderRadius: '50%',
                  background: done ? 'var(--green)' : active ? 'var(--purple)' : 'var(--surface-2)',
                  border: `1px solid ${done ? 'var(--green)' : active ? 'var(--purple)' : 'var(--border-2)'}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 10, fontWeight: 600,
                  color: done || active ? '#fff' : 'var(--text-3)',
                  flexShrink: 0,
                }}>
                  {done ? <Check size={11} /> : num}
                </div>
                <span style={{ display: i === 0 || active || done ? 'block' : 'none' }}>
                  {s.label}
                </span>
              </div>
              {i < STEPS.length - 1 && (
                <div style={{
                  width: 32, height: 1, background: 'var(--border)',
                  margin: '0 10px', flexShrink: 0,
                }} />
              )}
            </div>
          )
        })}
      </div>

      {/* Step content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={step}
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -20 }}
          transition={{ duration: 0.2 }}
        >
          {step === 1 && <StepEventType />}
          {step === 2 && <StepDetails />}
          {step === 3 && <StepScene />}
          {step === 4 && <StepShare />}
        </motion.div>
      </AnimatePresence>
    </div>
  )
}
