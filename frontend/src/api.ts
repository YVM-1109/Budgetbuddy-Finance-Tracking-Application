const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('bb_token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    let code: string | undefined
    try {
      const body = await response.json()
      message = body.message ?? message
      code = body.code
    } catch {
      // keep default message
    }
    if (response.status === 401) {
      localStorage.removeItem('bb_token')
    }
    throw new ApiError(response.status, message, code)
  }
  return response.json() as Promise<T>
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}

export interface UserResponse {
  id: string
  name: string
  email: string
  authProvider: string
  monthlyBudget: number
  monthlySavingsTarget: number
}

export interface Transaction {
  id: string
  type: 'INCOME' | 'EXPENSE'
  amount: number
  transactionDate: string
  category: string
  remarks: string | null
  sourceType: 'MANUAL' | 'RECURRING'
  recurringFinanceId: string | null
}

export interface RecurringFinance {
  id: string
  type: 'INCOME' | 'EXPENSE'
  amount: number
  category: string
  remarks: string | null
  startDate: string
  endDate: string | null
  active: boolean
}

export interface DashboardData {
  currentMonth: { income: number; expense: number; net: number }
  last12Months: { income: number; expense: number; net: number }
  budget: number
  savingsTarget: number
  budgetInfo: {
    budget: number
    currentMonthExpense: number
    overBudget: boolean
    exceededBy: number
    utilizationPercent: number | null
  }
  savingsTargetInfo: {
    target: number
    currentMonthNet: number
    progressPercent: number | null
  }
  monthlySeries: { year: number; month: number; income: number; expense: number; net: number }[]
  categoryBreakdown: { category: string; total: number }[]
  recentTransactions: Transaction[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}
