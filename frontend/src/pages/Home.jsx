import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Sparkles, Zap, Heart, Share2 } from 'lucide-react'

const features = [
  { icon: Sparkles, title: 'AI Scene Generation',    desc: 'Describe any scene — jungle, beach, fairy tale — and Gemini AI brings it to life.' },
  { icon: Zap,      title: 'Animated in seconds',    desc: 'Your invite animates automatically. Animals walk, leaves sway, confetti flies.' },
  { icon: Heart,    title: 'RSVP built-in',          desc: 'Guests respond directly on the invite. You get notified in real time.' },
  { icon: Share2,   title: 'One link, share anywhere', desc: 'Every invite gets a unique URL. Share via WhatsApp, email, or anywhere.' },
]

const examples = [
  { emoji: '🦁', label: 'Jungle Birthday',  bg: 'linear-gradient(135deg,#1a3a2a,#2d5a3d)' },
  { emoji: '💍', label: 'Garden Wedding',   bg: 'linear-gradient(135deg,#3a2a1a,#5a4a2a)' },
  { emoji: '🎉', label: 'Confetti Party',   bg: 'linear-gradient(135deg,#2a1a3a,#4a2a5a)' },
  { emoji: '🚀', label: 'Space Adventure',  bg: 'linear-gradient(135deg,#0a0a2a,#1a1a4a)' },
]

export default function Home() {
  const navigate = useNavigate()

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      {/* Topbar */}
      <nav style={{
        background: 'var(--surface)', borderBottom: '1px solid var(--border)',
        padding: '0 2rem', height: 56,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <span style={{ fontSize: 18, fontWeight: 600 }} className="gradient-text">
          invitely
        </span>
        <button className="btn btn-primary" onClick={() => navigate('/create')}>
          Create invite
        </button>
      </nav>

      {/* Hero */}
      <div style={{ maxWidth: 800, margin: '0 auto', padding: '80px 2rem 60px', textAlign: 'center' }}>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 6,
            background: 'var(--purple-lt)', borderRadius: 99,
            padding: '4px 14px', fontSize: 12, color: 'var(--purple-dk)',
            fontWeight: 500, marginBottom: 24,
          }}>
            <Sparkles size={13} /> Powered by Gemini AI
          </div>

          <h1 style={{ fontSize: 52, fontWeight: 600, lineHeight: 1.15, marginBottom: 20 }}>
            Invitations that{' '}
            <span className="gradient-text">come alive</span>
          </h1>

          <p style={{ fontSize: 18, color: 'var(--text-2)', maxWidth: 500, margin: '0 auto 36px' }}>
            Describe a scene. AI generates a stunning image with your event details baked in —
            then animates it into a shareable invite your guests will remember.
          </p>

          <button
            className="btn btn-primary"
            style={{ fontSize: 15, padding: '12px 32px' }}
            onClick={() => navigate('/create')}
          >
            <Sparkles size={16} /> Create your invite — free
          </button>
        </motion.div>

        {/* Example invite previews */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 56 }}
        >
          {examples.map((ex, i) => (
            <motion.div
              key={ex.label}
              whileHover={{ y: -4, scale: 1.03 }}
              transition={{ type: 'spring', stiffness: 300 }}
              style={{
                width: 140, height: 180, borderRadius: 16,
                background: ex.bg, display: 'flex',
                flexDirection: 'column', alignItems: 'center',
                justifyContent: 'center', gap: 8,
                boxShadow: 'var(--shadow-lg)', cursor: 'pointer',
              }}
              onClick={() => navigate('/create')}
            >
              <span style={{ fontSize: 36 }}>{ex.emoji}</span>
              <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.7)', fontWeight: 500 }}>
                {ex.label}
              </span>
            </motion.div>
          ))}
        </motion.div>
      </div>

      {/* Features */}
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '0 2rem 80px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 16 }}>
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              className="card"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 + i * 0.1 }}
            >
              <div style={{
                width: 36, height: 36, borderRadius: 10,
                background: 'var(--purple-lt)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 12,
              }}>
                <f.icon size={18} color="var(--purple)" />
              </div>
              <div style={{ fontWeight: 600, marginBottom: 6 }}>{f.title}</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)', lineHeight: 1.5 }}>{f.desc}</div>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  )
}
