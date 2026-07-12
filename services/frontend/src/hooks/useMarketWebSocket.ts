import { useEffect, useRef } from 'react'
import { useAuthStore } from '@/store/authStore'
import { useMarketStore, type LivePrice } from '@/store/marketStore'

export function useMarketWebSocket() {
  const { accessToken, isAuthenticated } = useAuthStore()
  const { setPrice, setConnected } = useMarketStore()
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (!isAuthenticated || !accessToken) return

    const connect = () => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host
      const ws = new WebSocket(`${protocol}//${host}/ws/prices?token=${accessToken}`)

      ws.onopen = () => {
        setConnected(true)
        console.log('[WS] Connected to price feed')
      }

      ws.onmessage = (event) => {
        try {
          const data: LivePrice = JSON.parse(event.data)
          setPrice(data)
        } catch {
          // ignore parse errors
        }
      }

      ws.onclose = () => {
        setConnected(false)
        console.log('[WS] Disconnected — reconnecting in 3s')
        reconnectRef.current = setTimeout(connect, 3000)
      }

      ws.onerror = () => ws.close()

      wsRef.current = ws
    }

    connect()

    return () => {
      if (reconnectRef.current) clearTimeout(reconnectRef.current)
      wsRef.current?.close()
    }
  }, [isAuthenticated, accessToken, setPrice, setConnected])
}
