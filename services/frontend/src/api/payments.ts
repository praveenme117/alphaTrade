import { apiClient } from './client'

export interface PaymentRequest {
  amount: number
  currency: string
}

export interface PaymentOrder {
  id: string
  type: 'DEPOSIT' | 'WITHDRAWAL'
  amount: number
  currency: string
  status: string
  createdAt: string
}

export const paymentsApi = {
  deposit: (data: PaymentRequest) =>
    apiClient.post<{ success: boolean; data: PaymentOrder }>('/payments/deposit', data),

  withdraw: (data: PaymentRequest) =>
    apiClient.post<{ success: boolean; data: PaymentOrder }>('/payments/withdraw', data),

  getHistory: (page = 0, size = 20) =>
    apiClient.get<{ success: boolean; data: { content: PaymentOrder[]; totalElements: number } }>('/payments/history', {
      params: { page, size },
    }),
}
