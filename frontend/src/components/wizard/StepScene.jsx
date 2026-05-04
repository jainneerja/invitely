import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useInviteStore } from '../../store/inviteStore'
import { generateImage, animateImage, updateInvite, pollInviteStatus } from '../../api/inviteApi'
import toast from 'react-hot-toast'
import { Sparkles, ArrowLeft, ArrowRight, RotateCcw, Check, Loader } from 'lucide-react'

const SUGGESTIONS = [
  '🌊 Tropical beach at golden sunset',
  '🏰 Enchanted fairy tale castle',
  '🌸 Cherry blossom garden, spring breeze',
  '🚀 Outer space adventure, stars and nebula',
  '🧚 Magical glowing forest at night',
  '🎠 Vintage carnival with ferris wheel',
]

const ANIMATION_STYLES = [
  { value: 'animals_walk',  emoji: '🐾', label: 'Animals walk' },
  { value: 'leaves_sway',   emoji: '🍃', label: 'Leaves sway' },
  { value: 'confetti',      emoji: '🎊', label: 'Confetti' },
  { value: 'water_flow',    emoji: '🌊', label: 'Water flow' },
  { value: 'sparkle',       emoji: '✨', label: 'Sparkle' },
  { value: 'clouds_drift',  emoji: '☁️', label: 'Clouds drift' },
]

export default function StepScene() {
  const { form, setForm, createdInvite, setCreatedInvite, nextStep, prevStep } = useInviteStore()

  const [genLoading, setGenLoading]   = useState(false)
  const [animLoading, setAnimLoading] = useState(false)
  const [imageUrl, setImageUrl]       = useState(createdInvite?.generatedImageUrl || null)
  const [videoUrl, setVideoUrl]       = useState(createdInvite?.animatedVideoUrl || null)

  const handleGenerate = async () => {
    if (!form.scenePrompt.trim()) {
      toast.error('Please describe your scene first')
      return
    }
    setGenLoading(true)
    try {
      // Save prompt to backend first
      await updateInvite(createdInvite.id, {
        scenePrompt:    form.scenePrompt,
        animationStyle: form.animationStyle,
      })

      // Trigger async generation
      await generateImage(createdInvite.id)
      toast('Generating your scene...', { icon: '✨' })

      // Poll until IMAGE_READY
      const updated = await pollInviteStatus(
        createdInvite.id,
        (inv) => inv.status === 'IMAGE_READY' || inv.status === 'VIDEO_PENDING',
        3000, 30
      )
      setImageUrl(updated.generatedImageUrl)
      setCreatedInvite(updated)
      toast.success('Image ready!')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setGenLoading(false)
    }
  }

  const handleAnimate = async () => {
    setAnimLoading(true)
    try {
      await animateImage(createdInvite.id)
      toast('Animating your invite...', { icon: '🎬' })

      const updated = await pollInviteStatus(
        createdInvite.id,
        (inv) => inv.animatedVideoUrl != null,
        5000, 30
      )
      setVideoUrl(updated.animatedVideoUrl)
      setCreatedInvite(updated)
      toast.success('Animation complete!')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setAnimLoading(false)
    }
  }

  return (
    <div style={{
      maxWidth: 1060, margin: '0 auto', padding: '1.5rem 2rem',
      display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1.5rem',
    }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

        {/* Prompt input */}
        <div className="card">
          <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>
            Describe your scene
          </h2>
          <p style={{ fontSize: 12, color: 'var(--text-3)', marginBottom: 14 }}>
            Be vivid — include characters, mood, lighting, and art style
          </p>

          <div style={{
            border: '1px solid var(--border-2)', borderRadius: 'var(--radius-md)',
            overflow: 'hidden', transition: 'border-color 0.15s',
          }}>
            <textarea
              style={{
                width: '100%', border: 'none', padding: '10px 12px',
                fontSize: 13, background: 'var(--surface)',
                color: 'var(--text-1)', resize: 'none', height: 80,
                fontFamily: 'inherit', lineHeight: 1.5, outline: 'none',
              }}
              placeholder='e.g. "Hakuna matata jungle scene, lions and elephants, golden sunset, painterly style"'
              value={form.scenePrompt}
              onChange={(e) => setForm({ scenePrompt: e.target.value })}
            />
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              padding: '6px 10px', borderTop: '1px solid var(--border)',
              background: 'var(--surface-2)',
            }}>
              <span style={{ fontSize: 11, color: 'var(--text-3)' }}>
                {form.scenePrompt.length} / 300
              </span>
              <button
                className="btn btn-primary"
                style={{ padding: '6px 14px', fontSize: 12 }}
                onClick={handleGenerate}
                disabled={genLoading}
              >
                {genLoading
                  ? <><Loader size={12} /> Generating...</>
                  : <><Sparkles size={12} /> Generate image</>
                }
              </button>
            </div>
          </div>

          {/* Prompt suggestions */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 10 }}>
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                onClick={() => setForm({ scenePrompt: s.slice(2) })}
                style={{
                  background: 'var(--surface-2)', border: '1px solid var(--border)',
                  borderRadius: 99, padding: '3px 10px',
                  fontSize: 11, color: 'var(--text-2)', cursor: 'pointer',
                  transition: 'all 0.15s',
                }}
              >
                {s}
              </button>
            ))}
          </div>
        </div>

        {/* Generated image */}
        <div className="card">
          <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>
            Generated image
          </h2>
          <p style={{ fontSize: 12, color: 'var(--text-3)', marginBottom: 14 }}>
            Your invite text is baked into the scene by AI
          </p>

          <div style={{
            borderRadius: 'var(--radius-md)', overflow: 'hidden',
            border: '1px solid var(--border)', minHeight: 220,
            background: 'linear-gradient(160deg,#0f0a2e,#1e1245,#2d1f5e,#1a3a2a)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            position: 'relative',
          }}>
            <AnimatePresence mode="wait">
              {genLoading ? (
                <motion.div
                  key="loading"
                  initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                  style={{ textAlign: 'center', color: 'rgba(255,255,255,0.6)' }}
                >
                  <Loader size={32} style={{ marginBottom: 12, opacity: 0.5 }} />
                  <div style={{ fontSize: 13 }}>Gemini is painting your scene...</div>
                </motion.div>
              ) : imageUrl ? (
                <motion.img
                  key="image"
                  initial={{ opacity: 0, scale: 0.97 }}
                  animate={{ opacity: 1, scale: 1 }}
                  src={imageUrl}
                  alt="Generated invite"
                  style={{ width: '100%', display: 'block' }}
                />
              ) : (
                <motion.div
                  key="placeholder"
                  style={{ textAlign: 'center', color: 'rgba(255,255,255,0.4)' }}
                >
                  <div style={{ fontSize: 36, marginBottom: 8 }}>✦</div>
                  <div style={{ fontSize: 13 }}>Your AI scene will appear here</div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Image toolbar */}
          {imageUrl && (
            <div style={{
              display: 'flex', justifyContent: 'space-between',
              alignItems: 'center', marginTop: 10,
            }}>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn" style={{ fontSize: 12, padding: '5px 12px' }}
                  onClick={handleGenerate} disabled={genLoading}>
                  <RotateCcw size={12} /> Regenerate
                </button>
              </div>
              <button
                className="btn"
                style={{
                  fontSize: 12, padding: '5px 12px',
                  background: 'var(--green)', color: '#fff', border: 'none',
                }}
                onClick={handleAnimate}
                disabled={animLoading || !!videoUrl}
              >
                {animLoading
                  ? <><Loader size={12} /> Animating...</>
                  : videoUrl
                  ? <><Check size={12} /> Animated!</>
                  : '🎬 Animate'
                }
              </button>
            </div>
          )}
        </div>

        {/* Animation style picker */}
        <div className="card">
          <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>
            Animation style
          </h2>
          <p style={{ fontSize: 12, color: 'var(--text-3)', marginBottom: 14 }}>
            How should elements in your scene move?
          </p>
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8,
          }}>
            {ANIMATION_STYLES.map((a) => (
              <button
                key={a.value}
                onClick={() => setForm({ animationStyle: a.value })}
                style={{
                  background: form.animationStyle === a.value
                    ? '#FBEAF0' : 'var(--surface)',
                  border: `1px solid ${form.animationStyle === a.value
                    ? 'var(--pink)' : 'var(--border)'}`,
                  borderRadius: 'var(--radius-md)', padding: '10px 8px',
                  textAlign: 'center', cursor: 'pointer', transition: 'all 0.15s',
                }}
              >
                <div style={{ fontSize: 18, marginBottom: 3 }}>{a.emoji}</div>
                <div style={{
                  fontSize: 11,
                  color: form.animationStyle === a.value ? '#993556' : 'var(--text-2)',
                }}>
                  {a.label}
                </div>
              </button>
            ))}
          </div>

          <div style={{
            marginTop: 16, background: 'var(--purple-lt)',
            border: '1px solid #AFA9EC', borderRadius: 'var(--radius-md)',
            padding: '10px 12px', fontSize: 12, color: 'var(--purple-dk)',
            display: 'flex', gap: 8,
          }}>
            <Sparkles size={14} style={{ flexShrink: 0, marginTop: 1 }} />
            Animation is applied by AI video generation. Takes 30–60 seconds.
          </div>

          {/* Nav */}
          <div style={{
            display: 'flex', justifyContent: 'space-between',
            paddingTop: '1rem', borderTop: '1px solid var(--border)', marginTop: 16,
          }}>
            <button className="btn btn-ghost" onClick={prevStep}>
              <ArrowLeft size={14} /> Back
            </button>
            <button
              className="btn btn-primary"
              onClick={nextStep}
              disabled={!imageUrl}
            >
              Next: Get your link <ArrowRight size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* Right — phone preview */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{
            padding: '10px 14px', borderBottom: '1px solid var(--border)',
            fontSize: 12, fontWeight: 500, color: 'var(--text-2)',
            display: 'flex', justifyContent: 'space-between',
          }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <div style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--green)' }} />
              Guest view
            </span>
            <span style={{ fontSize: 11, color: 'var(--text-3)' }}>Mobile</span>
          </div>

          <div style={{ padding: '1rem', display: 'flex', justifyContent: 'center' }}>
            <div style={{
              width: 160, background: '#111', borderRadius: 20,
              padding: 8, border: '2px solid var(--border-2)',
            }}>
              <div style={{
                borderRadius: 14, overflow: 'hidden',
                background: 'linear-gradient(160deg,#0f0a2e,#1e1245,#2d1f5e)',
                height: 260, display: 'flex', flexDirection: 'column',
                alignItems: 'center', justifyContent: 'center',
                position: 'relative',
              }}>
                {videoUrl
                  ? <video src={videoUrl} autoPlay loop muted
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  : imageUrl
                  ? <img src={imageUrl} alt="preview"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  : (
                    <div style={{ textAlign: 'center', color: 'rgba(255,255,255,0.4)' }}>
                      <div style={{ fontSize: 24 }}>🌿🦁</div>
                      <div style={{ fontSize: 9, marginTop: 8 }}>
                        {form.eventTitle || 'Your invite'}
                      </div>
                    </div>
                  )
                }
                <div style={{
                  position: 'absolute', bottom: 0, left: 0, right: 0,
                  background: 'rgba(127,119,221,0.9)',
                  padding: 6, textAlign: 'center',
                  fontSize: 9, color: '#fff', fontWeight: 500,
                }}>
                  RSVP now
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="card" style={{ padding: '1rem 1.25rem' }}>
          <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--text-2)', marginBottom: 8 }}>
            Prompt tips
          </div>
          {[
            'Mention specific animals or characters',
            'Add lighting mood — "golden sunset", "moonlit"',
            'Try a style — "watercolor", "3D render"',
            'More vivid = better result',
          ].map((tip) => (
            <div key={tip} style={{
              fontSize: 12, color: 'var(--text-3)', marginBottom: 5, lineHeight: 1.4,
            }}>
              · {tip}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
