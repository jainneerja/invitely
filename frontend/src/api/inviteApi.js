import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Response interceptor — unwrap data, surface errors cleanly
api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Something went wrong'
    return Promise.reject(new Error(message))
  }
)

// -------------------------------------------------------
// Invites
// -------------------------------------------------------

export const createInvite = (data) =>
  api.post('/invites', data)

export const getInviteBySlug = (slug) =>
  api.get(`/invites/${slug}`)

export const getInviteById = (id) =>
  api.get(`/invites/id/${id}`)

export const updateInvite = (id, data) =>
  api.patch(`/invites/${id}`, data)

export const publishInvite = (id) =>
  api.post(`/invites/${id}/publish`)

export const unpublishInvite = (id) =>
  api.post(`/invites/${id}/unpublish`)

export const deleteInvite = (id) =>
  api.delete(`/invites/${id}`)

// -------------------------------------------------------
// AI generation
// -------------------------------------------------------

export const generateImage = (id, embedText = false) =>
  api.post(`/invites/${id}/generate-image`, null, { params: { embedText } })

export const animateImage = (id) =>
  api.post(`/invites/${id}/animate`)

export const suggestPrompt = (eventType, hostName, eventTitle) =>
  api.get('/invites/suggest-prompt', {
    params: { eventType, hostName, eventTitle },
  })

// -------------------------------------------------------
// RSVP
// -------------------------------------------------------

export const submitRsvp = (slug, data) =>
  api.post(`/invites/${slug}/rsvp`, data)

export const getRsvps = (id) =>
  api.get(`/invites/${id}/rsvp`)

// -------------------------------------------------------
// Templates
// -------------------------------------------------------

export const getTemplates = (eventType) =>
  api.get('/templates', { params: eventType ? { eventType } : {} })

// -------------------------------------------------------
// Polling helper — polls invite status until predicate is met
// -------------------------------------------------------

export const pollInviteStatus = (id, predicate, intervalMs = 3000, maxAttempts = 30) =>
  new Promise((resolve, reject) => {
    let attempts = 0
    const timer = setInterval(async () => {
      try {
        attempts++
        const invite = await getInviteById(id)
        if (predicate(invite)) {
          clearInterval(timer)
          resolve(invite)
        } else if (attempts >= maxAttempts) {
          clearInterval(timer)
          reject(new Error('Generation timed out — please try again'))
        }
      } catch (err) {
        clearInterval(timer)
        reject(err)
      }
    }, intervalMs)
  })
