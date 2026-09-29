import { FormEvent, useState } from 'react'
import { api } from '../api'
import type { UserResponse } from '../api'
import { useAuth } from '../auth'

export default function ProfilePage() {
  const { user, refreshUser } = useAuth()
  const [name, setName] = useState(user?.name ?? '')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setMessage('')
    setError('')
    setBusy(true)
    try {
      await api.put<UserResponse>('/api/users/me', { name })
      await refreshUser()
      setMessage('Profile updated.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed.')
    } finally {
      setBusy(false)
    }
  }

  if (!user) return null

  return (
    <div className="page narrow">
      <div className="page-header">
        <h1>Profile</h1>
      </div>
      <form className="card form" onSubmit={handleSubmit}>
        {message && <div className="alert alert-info">{message}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        <label>
          Name
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} required />
        </label>
        <label>
          Email
          <input value={user.email} disabled />
        </label>
        <p className="muted">Sign-in method: {user.authProvider}</p>
        <div className="form-actions">
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  )
}
