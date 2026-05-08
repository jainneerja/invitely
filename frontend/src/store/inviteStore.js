import { create } from 'zustand'

export const useInviteStore = create((set, get) => ({
  // Wizard step (1–4)
  step: 1,
  setStep: (step) => set({ step }),
  nextStep: () => set((s) => ({ step: Math.min(s.step + 1, 4) })),
  prevStep: () => set((s) => ({ step: Math.max(s.step - 1, 1) })),

  // Form data — mirrors CreateInviteRequest
  form: {
    eventType: '',
    hostName: '',
    eventTitle: '',
    eventDate: '',
    eventTime: '',
    venueName: '',
    venueAddress: '',
    personalMessage: '',
    scenePrompt: '',
    embedInvitationText: false,
    animationStyle: 'animals_walk',
    rsvpDeadline: '',
    maxGuests: '',
    templateId: '',
  },
  setForm: (fields) =>
    set((s) => ({ form: { ...s.form, ...fields } })),
  resetForm: () =>
    set({
      step: 1,
      form: {
        eventType: '',
        hostName: '',
        eventTitle: '',
        eventDate: '',
        eventTime: '',
        venueName: '',
        venueAddress: '',
        personalMessage: '',
        scenePrompt: '',
        embedInvitationText: false,
        animationStyle: 'animals_walk',
        rsvpDeadline: '',
        maxGuests: '',
        templateId: '',
      },
      createdInvite: null,
    }),

  // Created invite (returned from backend after step 1 submission)
  createdInvite: null,
  setCreatedInvite: (invite) => set({ createdInvite: invite }),
}))
