import { FormEvent, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import { formatINR } from '../format'
import type { UserResponse } from '../api'

export default function BudgetPage() {
  const { user, refreshUser } = useAuth()
  const [budget, setBudget] = useState(user ? String(user.monthlyBudget) : '0')
  const [target, setTarget] = useState(user ? String(user.monthlySavingsTarget) : '0')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const updated = await api.put<UserResponse>('/api/users/me', {
        monthlyBudget: Number(budget),
        monthlySavingsTarget: Number(target),
      })
      await refreshUser()
      setMessage(
        `Saved. Budget ${formatINR(updated.monthlyBudget)}, target ${formatINR(updated.monthlySavingsTarget)}.`,
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page narrow">
      <div className="page-header">
        <h1>Budget & savings target</h1>
      </div>
      <form className="card form" onSubmit={handleSubmit}>
        {message && <div className="alert alert-info">{message}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        <label>
          Monthly budget (INR)
          <input
            type="number"
            min="0"
            step="0.01"
            value={budget}
            onChange={(e) => setBudget(e.target.value)}
            required
          />
        </label>
        <p className="muted">
          If this month&apos;s expenses exceed this amount, the dashboard shows a warning —
          recording expenses is never blocked.
        </p>
        <label>
          Monthly savings target (INR)
          <input
            type="number"
            min="0"
            step="0.01"
            value={target}
            onChange={(e) => setTarget(e.target.value)}
            required
          />
        </label>
        <p className="muted">
          The dashboard compares this month&apos;s net (income − expenses) against this target.
        </p>
        <div className="form-actions">
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  )
}
