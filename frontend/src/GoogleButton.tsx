import { useEffect, useRef } from 'react'
import { loadGoogleScript } from './googleAuth'
import { useAuth } from './auth'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string
            callback: (response: { credential: string }) => void
          }) => void
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
        }
      }
    }
  }
}

export default function GoogleButton() {
  const divRef = useRef<HTMLDivElement>(null)
  const { googleSignIn } = useAuth()
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID

  useEffect(() => {
    if (!clientId) return
    let cancelled = false
    loadGoogleScript().then(() => {
      if (cancelled || !divRef.current || !window.google) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          void googleSignIn(response.credential)
        },
      })
      window.google.accounts.id.renderButton(divRef.current, {
        theme: 'outline',
        size: 'large',
        width: 320,
        text: 'continue_with',
      })
    })
    return () => {
      cancelled = true
    }
  }, [clientId, googleSignIn])

  if (!clientId) {
    return (
      <p className="google-unconfigured">
        Google Sign-In is not configured (set VITE_GOOGLE_CLIENT_ID).
      </p>
    )
  }
  return <div ref={divRef} className="google-button" />
}
