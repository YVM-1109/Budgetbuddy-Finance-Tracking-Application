import { FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import type { Transaction } from '../api'

const CATEGORIES = [
  'Food',
  'Rent',
  'Salary',
  'Travel',
  'Utilities',
  'Entertainment',
  'Loan',
  'Shopping',
  'Health',
  'Other',
]

export default function TransactionFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = Boolean(id)

  const [type, setType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [category, setCategory] = useState('Food')
  const [customCategory, setCustomCategory] = useState('')
  const [remarks, setRemarks] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(isEdit)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!id) return
    api
      .get<Transaction>(`/api/transactions/${id}`)
      .then((t) => {
        setType(t.type)
        setAmount(String(t.amount))
        setDate(t.transactionDate)
        if (CATEGORIES.includes(t.category)) {
          setCategory(t.category)
        } else {
          setCategory('Other')
          setCustomCategory(t.category)
        }
        setRemarks(t.remarks ?? '')
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load transaction.')
        setLoading(false)
      })
  }, [id])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    const finalCategory =
      category === 'Other' ? customCategory.trim() : category
    if (!finalCategory) {
      setError('Please describe the custom category.')
      return
    }
    const body = {
      type,
      amount: Number(amount),
      transactionDate: date,
      category: finalCategory,
      remarks: remarks || null,
    }
    setBusy(true)
    try {
      if (isEdit) {
        await api.put(`/api/transactions/${id}`, body)
      } else {
        await api.post('/api/transactions', body)
      }
      navigate('/transactions')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed.')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <div className="page-loading">Loading…</div>

  return (
    <div className="page narrow">
      <div className="page-header">
        <h1>{isEdit ? 'Edit transaction' : 'New transaction'}</h1>
      </div>
      <form className="card form" onSubmit={handleSubmit}>
        {error && <div className="alert alert-error">{error}</div>}
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
        <label>
          Date
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </label>
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
              placeholder="Describe the category"
            />
          </label>
        )}
        <label>
          Remarks
          <textarea
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            maxLength={500}
            rows={3}
          />
        </label>
        <div className="form-actions">
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? 'Saving…' : 'Save'}
          </button>
          <button className="btn btn-ghost" type="button" onClick={() => navigate(-1)}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
