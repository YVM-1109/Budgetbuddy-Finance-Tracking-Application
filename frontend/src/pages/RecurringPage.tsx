import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import type { RecurringFinance } from '../api'
import { formatINR } from '../format'

const CATEGORIES = [
  'Salary',
  'Rent',
  'Loan',
  'Utilities',
  'Subscriptions',
  'Other',
]

function RecurringForm({
  initial,
  onSaved,
  onCancel,
}: {
  initial?: RecurringFinance
  onSaved: () => void
  onCancel: () => void
}) {
  const [type, setType] = useState<'INCOME' | 'EXPENSE'>(initial?.type ?? 'EXPENSE')
  const [amount, setAmount] = useState(initial ? String(initial.amount) : '')
  const [category, setCategory] = useState(initial?.category ?? 'Rent')
  const [customCategory, setCustomCategory] = useState('')
  const [startDate, setStartDate] = useState(
    initial?.startDate ?? new Date().toISOString().slice(0, 10),
  )
  const [endDate, setEndDate] = useState(initial?.endDate ?? '')
  const [remarks, setRemarks] = useState(initial?.remarks ?? '')
  const [active, setActive] = useState(initial?.active ?? true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    const finalCategory = category === 'Other' ? customCategory.trim() : category
    if (!finalCategory) {
      setError('Please describe the custom category.')
      return
    }
    const body = {
      type,
      amount: Number(amount),
      category: finalCategory,
      startDate,
      endDate: endDate || null,
      remarks: remarks || null,
      active,
    }
    setBusy(true)
    try {
      if (initial) {
        await api.put(`/api/recurring-finances/${initial.id}`, body)
      } else {
        await api.post('/api/recurring-finances', body)
      }
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form" onSubmit={handleSubmit}>
      {error && <div className="alert alert-error">{error}</div>}
      {initial && (
        <div className="alert alert-info">
          Changing this amount affects future monthly transactions only — historical generated
          transactions stay unchanged.
        </div>
      )}
      <div className="form-row">
        <label>
          Type
          <select value={type} onChange={(e) => setType(e.target.value as 'INCOME' | 'EXPENSE')}>
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </select>
        </label>
        <label>
          Amount (INR)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </label>
      </div>
      <div className="form-row">
        <label>
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        {category === 'Other' && (
          <label>
            Custom category
            <input
              value={customCategory}
              onChange={(e) => setCustomCategory(e.target.value)}
              maxLength={80}
            />
          </label>
        )}
      </div>
      <div className="form-row">
        <label>
          Start date
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
        </label>
        <label>
          End date (optional)
          <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </label>
      </div>
      <label>
        Remarks
        <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} maxLength={500} rows={2} />
      </label>
      <label className="checkbox-label">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Active (generate monthly transactions)
      </label>
      <div className="form-actions">
        <button className="btn btn-primary" type="submit" disabled={busy}>
          {busy ? 'Saving…' : 'Save'}
        </button>
        <button className="btn btn-ghost" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}

export default function RecurringPage() {
  const [items, setItems] = useState<RecurringFinance[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<RecurringFinance | null>(null)
  const [creating, setCreating] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await api.get<RecurringFinance[]>('/api/recurring-finances'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load recurring finances.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function handleDelete(id: string) {
    if (!confirm('Deactivate/remove this recurring finance? Generated history is kept.')) return
    try {
      await api.delete(`/api/recurring-finances/${id}`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed.')
    }
  }

  if (loading) return <div className="page-loading">Loading…</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Recurring finances</h1>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>
          + Add recurring
        </button>
      </div>

      {error && (
        <div className="alert alert-error">
          {error}{' '}
          <button className="btn btn-ghost" onClick={load}>
            Retry
          </button>
        </div>
      )}

      {(creating || editing) && (
        <RecurringForm
          initial={editing ?? undefined}
          onSaved={() => {
            setCreating(false)
            setEditing(null)
            void load()
          }}
          onCancel={() => {
            setCreating(false)
            setEditing(null)
          }}
        />
      )}

      {items.length === 0 && !creating && !editing ? (
        <div className="card empty-state">
          <p>No recurring finances yet. Add salary, rent, loans and similar monthly items.</p>
        </div>
      ) : (
        <div className="card-grid">
          {items.map((r) => (
            <div key={r.id} className="card">
              <div className="recurring-head">
                <strong>{r.category}</strong>
                <span className={`badge ${r.active ? 'badge-active' : 'badge-inactive'}`}>
                  {r.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              <p className={r.type === 'INCOME' ? 'income' : 'expense'}>
                {r.type === 'INCOME' ? '+' : '−'} {formatINR(r.amount)} / month
              </p>
              <p className="muted">
                From {r.startDate}
                {r.endDate ? ` to ${r.endDate}` : ''} · Monthly
              </p>
              {r.remarks && <p className="muted">{r.remarks}</p>}
              <div className="row-actions">
                <button className="btn btn-ghost" onClick={() => setEditing(r)}>
                  Edit
                </button>
                <button className="link-danger" onClick={() => handleDelete(r.id)}>
                  Remove
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
