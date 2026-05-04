import { useState } from 'react'
import { useInviteStore } from '../../store/inviteStore'
import { createInvite } from '../../api/inviteApi'
import toast from 'react-hot-toast'
import { ArrowLeft, ArrowRight, Loader } from 'lucide-react'

export default function StepDetails() {
  const { form, setForm, nextStep, prevStep, setCreatedInvite } = useInviteStore()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    // Basic validation
    if (!form.hostName || !form.eventTitle || !form.eventDate || !form.eventTime) {
      toast.error('Please fill in all required fields')
      return
    }

    setLoading(true)
    try {
      const invite = await createInvite({
        eventType:       form.eventType,
        hostName:        form.hostName,
        eventTitle:      form.eventTitle,
        eventDate:       form.eventDate,
        eventTime:       form.eventTime,
        venueName:       form.venueName,
        venueAddress:    form.venueAddress,
        personalMessage: form.personalMessage,
        rsvpDeadline:    form.rsvpDeadline || undefined,
        maxGuests:       form.maxGuests ? parseInt(form.maxGuests) : undefined,
      })
      setCreatedInvite(invite)
      nextStep()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      maxWidth: 1060, margin: '0 auto', padding: '1.5rem 2rem',
      display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1.5rem',
    }}>
      {/* Form */}
      <div className="card">
        <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>
          Tell us about the event
        </h2>
        <p style={{ fontSize: 12, color: 'var(--text-3)', marginBottom: 20 }}>
          These details will appear on your invite
        </p>

        <div className="row">
          <div className="field">
            <label>Host name *</label>
            <input
              placeholder="e.g. Sarah Johnson"
              value={form.hostName}
              onChange={(e) => setForm({ hostName: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Event title *</label>
            <input
              placeholder="e.g. Sarah's 30th Birthday"
              value={form.eventTitle}
              onChange={(e) => setForm({ eventTitle: e.target.value })}
            />
          </div>
        </div>

        <div className="row">
          <div className="field">
            <label>Date *</label>
            <input
              type="date"
              value={form.eventDate}
              onChange={(e) => setForm({ eventDate: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Time *</label>
            <input
              type="time"
              value={form.eventTime}
              onChange={(e) => setForm({ eventTime: e.target.value })}
            />
          </div>
        </div>

        <div className="field">
          <label>Venue name</label>
          <input
            placeholder="e.g. The Garden Terrace"
            value={form.venueName}
            onChange={(e) => setForm({ venueName: e.target.value })}
          />
        </div>

        <div className="field">
          <label>Venue address</label>
          <input
            placeholder="Full address"
            value={form.venueAddress}
            onChange={(e) => setForm({ venueAddress: e.target.value })}
          />
        </div>

        <div className="field">
          <label>Personal message</label>
          <textarea
            placeholder="Write a warm message for your guests..."
            value={form.personalMessage}
            onChange={(e) => setForm({ personalMessage: e.target.value })}
          />
        </div>

        <div className="row">
          <div className="field">
            <label>RSVP deadline</label>
            <input
              type="date"
              value={form.rsvpDeadline}
              onChange={(e) => setForm({ rsvpDeadline: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Max guests</label>
            <input
              type="number"
              placeholder="e.g. 50"
              value={form.maxGuests}
              onChange={(e) => setForm({ maxGuests: e.target.value })}
            />
          </div>
        </div>

        {/* Nav */}
        <div style={{
          display: 'flex', justifyContent: 'space-between',
          paddingTop: '1rem', borderTop: '1px solid var(--border)', marginTop: 8,
        }}>
          <button className="btn btn-ghost" onClick={prevStep}>
            <ArrowLeft size={14} /> Back
          </button>
          <button
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading
              ? <><Loader size={14} className="spin" /> Saving...</>
              : <>Next: Design your scene <ArrowRight size={14} /></>
            }
          </button>
        </div>
      </div>

      {/* Live preview */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{
            padding: '10px 14px', borderBottom: '1px solid var(--border)',
            fontSize: 12, fontWeight: 500, color: 'var(--text-2)',
            display: 'flex', alignItems: 'center', gap: 6,
          }}>
            <div style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--green)' }} />
            Live preview
          </div>
          <div style={{
            background: 'linear-gradient(160deg,#0f0a2e,#1e1245,#2d1f5e,#1a3a2a)',
            padding: '2rem 1.5rem', textAlign: 'center', minHeight: 200,
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center',
          }}>
            <div style={{ fontSize: 32, marginBottom: 10 }}>🌿✨🦁</div>
            <div style={{ fontSize: 16, fontWeight: 500, color: '#EF9F27', marginBottom: 4 }}>
              {form.eventTitle || 'Your event title'}
            </div>
            <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)' }}>
              {form.eventDate || 'Date TBD'}
              {form.eventTime ? ` · ${form.eventTime}` : ''}
            </div>
            {form.venueName && (
              <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', marginTop: 4 }}>
                {form.venueName}
              </div>
            )}
          </div>
          <div style={{
            padding: '8px 12px', background: 'var(--surface-2)',
            borderTop: '1px solid var(--border)',
            fontSize: 11, color: 'var(--text-3)',
            display: 'flex', alignItems: 'center', gap: 6,
          }}>
            <div style={{
              width: 6, height: 6, borderRadius: '50%', background: 'var(--purple)',
            }} />
            AI scene generated in step 3
          </div>
        </div>

        <div className="card" style={{ padding: '1rem 1.25rem' }}>
          <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--text-2)', marginBottom: 8 }}>
            💡 Tips
          </div>
          {[
            'Add a venue so guests know where to go',
            'Set an RSVP deadline — guests respond faster',
            'Your scene prompt in step 3 is the magic moment',
          ].map((tip) => (
            <div key={tip} style={{
              fontSize: 12, color: 'var(--text-3)', paddingBottom: 6,
              borderBottom: '1px solid var(--border)', marginBottom: 6,
              lineHeight: 1.5,
            }}>
              · {tip}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
