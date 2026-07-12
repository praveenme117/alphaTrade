import { apiClient } from './client'

export interface WalletBalance {
  id: string
  currency: string
  balance: number
  lockedBalance: number
  availableBalance: number
}

export interface LedgerEntry {
  id: string
  type: string
  amount: number
  currency: string
  description: string
  createdAt: string
}

export const walletApi = {
  getAllBalances: () =>
    apiClient.get<{ success: boolean; data: WalletBalance[] }>('/wallet'),

  getBalance: (currency: string) =>
    apiClient.get<{ success: boolean; data: WalletBalance }>(`/wallet/${currency}`),

  getLedger: (page = 0, size = 20) =>
    apiClient.get<{ success: boolean; data: { content: LedgerEntry[]; totalElements: number } }>('/wallet/ledger', {
      params: { page, size },
    }),
}
