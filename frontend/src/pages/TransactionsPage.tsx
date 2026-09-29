import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import type { Page, Transaction } from '../api'
import { formatINR } from '../format'

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

export default function TransactionsPage() {
  const [page, setPage] = useState<Page<Transaction> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [type, setType] = useState('')
  const [category, setCategory] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('transactionDate')
  const [direction, setDirection] = useState('desc')
  const [pageNumber, setPageNumber] = useState(0)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = new URLSearchParams()
      params.set('page', String(pageNumber))
      params.set('size', '20')
      if (type) params.set('type', type)
      if (category) params.set('category', category)
      if (startDate) params.set('startDate', startDate)
      if (endDate) params.set('endDate', endDate)
      if (search) params.set('search', search)
      params.set('sort', sort)
      params.set('direction', direction)
      setPage(await api.get<Page<Transaction>>(`/api/transactions?${params.toString()}`))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions.')
    } finally {
      setLoading(false)
    }
  }, [pageNumber, type, category, startDate, endDate, search, sort, direction])

  useEffect(() => {
    void load()
  }, [load])

  async function handleDelete(id: string) {
    if (!confirm('Delete this transaction?')) return
    try {
      await api.delete(`/api/transactions/${id}`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Transactions</h1>
        <Link className="btn btn-primary" to="/transactions/new">
          + Add transaction
        </Link>
      </div>

      <div className="card filters">
        <input
          placeholder="Search remarks or category…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPageNumber(0)
          }}
        />
        <select
          value={type}
          onChange={(e) => {
            setType(e.target.value)
            setPageNumber(0)
          }}
        >
          <option value="">All types</option>
          <option value="INCOME">Income</option>
          <option value="EXPENSE">Expense</option>
        </select>
        <select
          value={category}
          onChange={(e) => {
            setCategory(e.target.value)
            setPageNumber(0)
          }}
        >
          <option value="">All categories</option>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <input
          type="date"
          value={startDate}
          onChange={(e) => {
            setStartDate(e.target.value)
            setPageNumber(0)
          }}
        />
        <input
          type="date"
          value={endDate}
          onChange={(e) => {
            setEndDate(e.target.value)
            setPageNumber(0)
          }}
        />
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="transactionDate">Sort by date</option>
          <option value="amount">Sort by amount</option>
          <option value="category">Sort by category</option>
        </select>
        <select value={direction} onChange={(e) => setDirection(e.target.value)}>
          <option value="desc">Descending</option>
          <option value="asc">Ascending</option>
        </select>
      </div>

      {error && (
        <div className="alert alert-error">
          {error}{' '}
          <button className="btn btn-ghost" onClick={load}>
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="skeleton-card tall" />
      ) : page && page.content.length > 0 ? (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Category</th>
                <th>Remarks</th>
                <th>Amount</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((t) => (
                <tr key={t.id}>
                  <td>{t.transactionDate}</td>
                  <td>{t.type}</td>
                  <td>{t.category}</td>
                  <td className="muted">{t.remarks ?? '—'}</td>
                  <td className={t.type === 'INCOME' ? 'income' : 'expense'}>
                    {formatINR(t.amount)}
                  </td>
                  <td className="row-actions">
                    <Link to={`/transactions/${t.id}`}>Edit</Link>
                    <button className="link-danger" onClick={() => handleDelete(t.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination">
            <button
              className="btn btn-ghost"
              disabled={page.number === 0}
              onClick={() => setPageNumber(page.number - 1)}
            >
              Previous
            </button>
            <span className="muted">
              Page {page.number + 1} of {Math.max(page.totalPages, 1)} · {page.totalElements} items
            </span>
            <button
              className="btn btn-ghost"
              disabled={page.number + 1 >= page.totalPages}
              onClick={() => setPageNumber(page.number + 1)}
            >
              Next
            </button>
          </div>
        </div>
      ) : (
        <div className="card empty-state">
          <p>No transactions match your filters.</p>
          <Link className="btn btn-primary" to="/transactions/new">
            Add your first transaction
          </Link>
        </div>
      )}
    </div>
  )
}
