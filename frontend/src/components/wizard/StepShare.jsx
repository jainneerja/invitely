import { useState } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useInviteStore } from '../../store/inviteStore'
import { publishInvite } from '../../api/inviteApi'
import toast from 'react-hot-toast'
import { Copy, Check, Share2, LayoutDashboard, Sparkles, Loader } from 'lucide-react'
import Confetti from 'react-confetti'

export default function StepShare() {
  const navigate = useNavigate()
  const { createdInvite, resetForm } = useInviteStore()
  const [published, setPublished]   = useState(false)
  const [loading, setLoading]       = useState(false)
  const [copied, setCopied]         = useState(false)

  const shareUrl = createdInvite?.shareUrl || ''

  const handlePublish = async () => {
    setLoading(true)
    try {
      await publishInvite(createdInvite.id)
      setPublished(true)
      toast.success('Your invite is live! 🎉')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(shareUrl)
    setCopied(true)
    toast.success('Link copied!')
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div style={{ maxWidth: 560, margin: '0 auto', padding: '3rem 2rem', textAlign: 'center' }}>
      {published && <Confetti recycle={false} numberOfPieces={300} />}

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        {!published ? (
          <>
            <div style={{ fontSize: 48, marginBottom: 16 }}>🎉</div>
            <h2 style={{ fontSize: 24, fontWeight: 600, marginBottom: 8 }}>
              Almost there!
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', marginBottom: 32 }}>
              Publish your invite to make it live and get your shareable link.
            </p>
            <button
              className="btn btn-primary"
              style={{ fontSize: 15, padding: '12px 32px' }}
              onClick={handlePublish}
              disabled={loading}
            >
              {loading
                ? <><Loader size={16} /> Publishing...</>
                : <><Sparkles size={16} /> Publish invite</>
              }
            </button>
          </>
        ) : (
          <>
            <motion.div
              initial={{ scale: 0.5, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ type: 'spring', stiffness: 200 }}
              style={{ fontSize: 64, marginBottom: 20 }}
            >
              ✨
            </motion.div>

            <h2 style={{ fontSize: 26, fontWeight: 600, marginBottom: 8 }}>
              Your invite is live!
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', marginBottom: 28 }}>
              Share this link with your guests
            </p>

            {/* Share URL */}
            <div style={{
              background: 'var(--surface)', border: '1px solid var(--border)',
              borderRadius: 'var(--radius-md)', padding: '10px 14px',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              marginBottom: 16, textAlign: 'left',
            }}>
              <span style={{ fontSize: 13, color: 'var(--purple)', fontWeight: 500 }}>
                {shareUrl}
              </span>
              <button
                className="btn"
                style={{ padding: '5px 12px', fontSize: 12, flexShrink: 0, marginLeft: 8 }}
                onClick={handleCopy}
              >
                {copied ? <Check size={12} /> : <Copy size={12} />}
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>

            {/* Action buttons */}
            <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}>
              <button
                className="btn btn-primary"
                onClick={() => window.open(shareUrl, '_blank')}
              >
                <Share2 size={14} /> Preview invite
              </button>
              <button
                className="btn"
                onClick={() => navigate(`/dashboard/${createdInvite.id}`)}
              >
                <LayoutDashboard size={14} /> View RSVPs
              </button>
              <button
                className="btn btn-ghost"
                onClick={resetForm}
              >
                + Create another
              </button>
            </div>
          </>
        )}
      </motion.div>
    </div>
  )
}
