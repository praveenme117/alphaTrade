import { apiClient } from './client'

export interface Holding {
  id: string
  symbol: string
  instrumentId: string
  quantity: number
  averageBuyPrice: number
  currentPrice?: number
  currentValue?: number
  unrealizedPnl?: number
  unrealizedPnlPct?: number
}

export interface PortfolioSummary {
  totalInvested: number
  currentValue: number
  unrealizedPnl: number
  realizedPnl: number
}

export const portfolioApi = {
  getHoldings: () =>
    apiClient.get<{ success: boolean; data: Holding[] }>('/portfolio/holdings'),

  getSummary: () =>
    apiClient.get<{ success: boolean; data: PortfolioSummary }>('/portfolio/summary'),
}
