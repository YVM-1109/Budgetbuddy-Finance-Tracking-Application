import { FormEvent, useRef, useState } from 'react'
import { api, ApiError } from '../api'

interface ChatMessage {
  role: 'user' | 'assistant'
  text: string
  failed?: boolean
}

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  async function send(text: string) {
    const userMsg: ChatMessage = { role: 'user', text }
    setMessages((prev) => [...prev, userMsg])
    setBusy(true)
    try {
      const res = await api.post<{ message: string }>('/api/chat', { message: text })
      setMessages((prev) => [...prev, { role: 'assistant', text: res.message }])
    } catch (err) {
      const message = err instanceof Error ? err.message : 'The chat service failed.'
      setMessages((prev) => [...prev, { role: 'assistant', text: message, failed: true }])
    } finally {
      setBusy(false)
      setInput('')
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const text = input.trim()
    if (!text || busy) return
    void send(text)
  }

  const lastMessage = messages[messages.length - 1]

  return (
    <div className="page chat-page">
      <div className="page-header">
        <h1>Assistant</h1>
      </div>

      <div className="alert alert-info chat-disclaimer">
        AI responses are general information only — not professional financial advice. The
        assistant cannot see your BudgetBuddy data; share details only if you choose to.
      </div>

      <div className="card chat-window">
        {messages.length === 0 ? (
          <div className="chat-empty">
            <p>Ask a general financial question to get started.</p>
            <p className="muted">Examples: “How can I save more each month?”</p>
          </div>
        ) : (
          messages.map((m, i) => (
            <div key={i} className={`chat-msg ${m.role} ${m.failed ? 'chat-failed' : ''}`}>
              <span>{m.text}</span>
              {m.failed && i === messages.length - 1 && (
                <button
                  className="btn btn-ghost"
                  disabled={busy}
                  onClick={() => {
                    const lastUser = [...messages].reverse().find((x) => x.role === 'user')
                    if (lastUser) void send(lastUser.text)
                  }}
                >
                  Retry
                </button>
              )}
            </div>
          ))
        )}
        {busy && <div className="chat-msg assistant typing">Thinking…</div>}
        <div ref={bottomRef} />
      </div>

      <form className="chat-input" onSubmit={handleSubmit}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={busy ? 'Waiting for a reply…' : 'Type your question…'}
          maxLength={2000}
          disabled={busy}
        />
        <button className="btn btn-primary" type="submit" disabled={busy || !input.trim()}>
          Send
        </button>
      </form>
      {lastMessage?.failed && <p className="muted">Last request failed — you can retry.</p>}
    </div>
  )
}
