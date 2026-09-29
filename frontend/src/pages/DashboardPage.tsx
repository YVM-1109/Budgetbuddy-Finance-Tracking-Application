import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api'
import type { DashboardData } from '../api'
import { useAuth } from '../auth'
import { formatINR, formatMonth } from '../format'

const PIE_COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7']

export default function DashboardPage() {
  const { user, refreshUser } = useAuth()
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError('')
    try {
      setData(await api.get<DashboardData>('/api/dashboard'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  if (loading) {
    return (
      <div className="page">
        <div className="skeleton-grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton-card" />
          ))}
        </div>
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="page">
        <div className="alert alert-error">
          {error || 'Failed to load dashboard.'}{' '}
          <button className="btn btn-ghost" onClick={load}>
            Retry
          </button>
        </div>
      </div>
    )
  }

  const series = data.monthlySeries.map((p) => ({
    label: formatMonth(p.year, p.month),
    income: p.income,
    expense: p.expense,
    net: p.net,
  }))

  const overBudget = data.budgetInfo.overBudget
  const progress = data.savingsTargetInfo.progressPercent
  const progressPct = progress == null ? 0 : Math.max(0, Math.min(progress, 100))
  const hasNoTransactions = data.last12Months.income === 0 && data.last12Months.expense === 0

  return (
    <div className="page">
      <div className="page-header">
        <h1>Dashboard</h1>
        <Link className="btn btn-primary" to="/transactions/new">
          + Add transaction
        </Link>
      </div>

      {hasNoTransactions && (
        <div className="alert alert-info">
          No transactions yet. Add your first income or expense to see insights.
        </div>
      )}

      {overBudget && (
        <div className="alert alert-warn" role="alert">
          <strong>Budget exceeded.</strong> You are {formatINR(data.budgetInfo.exceededBy)} over
          your monthly budget of {formatINR(data.budgetInfo.budget)}. You can continue recording
          expenses.
        </div>
      )}

      <div className="card-grid">
        <div className="card stat">
          <span className="stat-label">Income (this month)</span>
          <span className="stat-value income">{formatINR(data.currentMonth.income)}</span>
        </div>
        <div className="card stat">
          <span className="stat-label">Expenses (this month)</span>
          <span className="stat-value expense">{formatINR(data.currentMonth.expense)}</span>
        </div>
        <div className="card stat">
          <span className="stat-label">Savings / loss</span>
          <span className={`stat-value ${data.currentMonth.net >= 0 ? 'income' : 'expense'}`}>
            {formatINR(data.currentMonth.net)}
          </span>
        </div>
        <div className="card stat">
          <span className="stat-label">Monthly budget</span>
          <span className="stat-value">{formatINR(data.budget)}</span>
        </div>
        <div className="card stat">
          <span className="stat-label">Savings target</span>
          <span className="stat-value">{formatINR(data.savingsTarget)}</span>
        </div>
        <div className="card stat">
          <span className="stat-label">12-month net</span>
          <span className={`stat-value ${data.last12Months.net >= 0 ? 'income' : 'expense'}`}>
            {formatINR(data.last12Months.net)}
          </span>
        </div>
      </div>

      <div className="chart-row">
        <div className="card chart-card">
          <h2>Income vs expense (12 months)</h2>
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={series}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => formatINR(Number(v))} />
                <Legend />
                <Bar dataKey="income" fill="#22c55e" name="Income" />
                <Bar dataKey="expense" fill="#ef4444" name="Expense" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="card chart-card">
          <h2>Savings / loss trend</h2>
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={series}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => formatINR(Number(v))} />
                <Line type="monotone" dataKey="net" stroke="#6366f1" strokeWidth={2} name="Net" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="chart-row">
        <div className="card chart-card">
          <h2>Expense breakdown (12 months)</h2>
          {data.categoryBreakdown.length === 0 ? (
            <p className="muted">No expenses recorded yet.</p>
          ) : (
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={data.categoryBreakdown}
                    dataKey="total"
                    nameKey="category"
                    outerRadius={90}
                    label={false}
                  >
                    {data.categoryBreakdown.map((entry, i) => (
                      <Cell key={entry.category} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v) => formatINR(Number(v))} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
        <div className="card">
          <h2>Savings target</h2>
          <p className="muted">
            {formatINR(data.savingsTargetInfo.currentMonthNet)} of{' '}
            {formatINR(data.savingsTargetInfo.target)}
          </p>
          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${progressPct}%` }} />
          </div>
          <p className="muted">{progress == null ? 'Set a target' : `${progress}%`}</p>

          <h2>Budget utilization</h2>
          <p className="muted">
            {formatINR(data.budgetInfo.currentMonthExpense)} of {formatINR(data.budgetInfo.budget)}
          </p>
          <div className="progress-track">
            <div
              className={`progress-fill ${overBudget ? 'progress-over' : ''}`}
              style={{
                width: `${Math.min(data.budgetInfo.utilizationPercent ?? 0, 100)}%`,
              }}
            />
          </div>
          <p className="muted">
            {data.budgetInfo.utilizationPercent == null
              ? 'Set a budget'
              : `${data.budgetInfo.utilizationPercent}% used`}
          </p>
        </div>
      </div>

      <div className="card">
        <div className="section-header">
          <h2>Recent transactions</h2>
          <Link to="/transactions">View all</Link>
        </div>
        {data.recentTransactions.length === 0 ? (
          <p className="muted">Nothing here yet — add your first transaction.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Category</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {data.recentTransactions.map((t) => (
                <tr key={t.id}>
                  <td>{t.transactionDate}</td>
                  <td>{t.type}</td>
                  <td>{t.category}</td>
                  <td className={t.type === 'INCOME' ? 'income' : 'expense'}>
                    {formatINR(t.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
