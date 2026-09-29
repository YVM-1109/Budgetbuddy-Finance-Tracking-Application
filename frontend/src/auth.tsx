import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { api } from './api'
import type { UserResponse } from './api'

interface AuthContextValue {
  user: UserResponse | null
  token: string | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  googleSignIn: (credential: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('bb_token'))
  const [loading, setLoading] = useState<boolean>(!!localStorage.getItem('bb_token'))

  const refreshUser = useCallback(async () => {
    if (!localStorage.getItem('bb_token')) {
      setUser(null)
      return
    }
    try {
      const me = await api.get<UserResponse>('/api/users/me')
      setUser(me)
    } catch {
      localStorage.removeItem('bb_token')
      setToken(null)
      setUser(null)
    }
  }, [])

  useEffect(() => {
    setLoading(true)
    refreshUser().finally(() => setLoading(false))
  }, [refreshUser])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.post<{ token: string; user: UserResponse }>('/api/auth/login', {
      email,
      password,
    })
    localStorage.setItem('bb_token', res.token)
    setToken(res.token)
    setUser(res.user)
  }, [])

  const register = useCallback(async (name: string, email: string, password: string) => {
    const res = await api.post<{ token: string; user: UserResponse }>('/api/auth/register', {
      name,
      email,
      password,
    })
    localStorage.setItem('bb_token', res.token)
    setToken(res.token)
    setUser(res.user)
  }, [])

  const googleSignIn = useCallback(async (credential: string) => {
    const res = await api.post<{ token: string; user: UserResponse }>('/api/auth/google', {
      credential,
    })
    localStorage.setItem('bb_token', res.token)
    setToken(res.token)
    setUser(res.user)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('bb_token')
    setToken(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, token, loading, login, register, googleSignIn, logout, refreshUser }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
