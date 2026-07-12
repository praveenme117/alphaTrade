import { apiClient } from './client'

export interface Instrument {
  id: string
  symbol: string
  name: string
  type: 'STOCK' | 'CRYPTO'
  currency: string
  seedPrice: number
  active: boolean
}

export interface Quote {
  symbol: string
  ltp: number
  open: number
  high: number
  low: number
  change: number
  changePct: number
  volume: number
}

export const marketApi = {
  getInstruments: (type?: 'STOCK' | 'CRYPTO') =>
    apiClient.get<{ success: boolean; data: Instrument[] }>('/market/instruments', {
      params: type ? { type } : undefined,
    }),

  getInstrumentBySymbol: (symbol: string) =>
    apiClient.get<{ success: boolean; data: Instrument }>(`/market/instruments/symbol/${symbol}`),

  searchInstruments: (q: string) =>
    apiClient.get<{ success: boolean; data: Instrument[] }>('/market/instruments/search', {
      params: { q },
    }),

  getQuote: (symbol: string) =>
    apiClient.get<{ success: boolean; data: Quote }>(`/market/quotes/${symbol}`),
}
