import { create } from 'zustand'

export interface LivePrice {
  symbol: string
  ltp: number
  open: number
  high: number
  low: number
  close: number
  change: number
  changePct: number
  volume: number
  timestamp: string
}

interface MarketState {
  prices: Record<string, LivePrice>
  connected: boolean
  setPrice: (price: LivePrice) => void
  setConnected: (v: boolean) => void
}

export const useMarketStore = create<MarketState>((set) => ({
  prices: {},
  connected: false,
  setPrice: (price) =>
    set((s) => ({ prices: { ...s.prices, [price.symbol]: price } })),
  setConnected: (connected) => set({ connected }),
}))
