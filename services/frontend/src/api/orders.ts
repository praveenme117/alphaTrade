import { apiClient } from './client'

export type OrderSide = 'BUY' | 'SELL'
export type OrderType = 'MARKET' | 'LIMIT' | 'STOP_LOSS'
export type OrderStatus = 'OPEN' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'REJECTED'
export type ProductType = 'CASH' | 'INTRADAY' | 'DELIVERY'

export interface PlaceOrderRequest {
  instrumentId: string
  symbol: string
  orderType: OrderType
  side: OrderSide
  productType: ProductType
  quantity: number
  price?: number
  stopPrice?: number
}

export interface Order {
  id: string
  symbol: string
  orderType: OrderType
  side: OrderSide
  status: OrderStatus
  productType: ProductType
  quantity: number
  price?: number
  filledQuantity: number
  averagePrice?: number
  fee: number
  rejectReason?: string
  createdAt: string
  updatedAt: string
}

export const ordersApi = {
  placeOrder: (data: PlaceOrderRequest) =>
    apiClient.post<{ success: boolean; data: Order }>('/orders', data),

  getOrders: (page = 0, size = 20) =>
    apiClient.get<{ success: boolean; data: { content: Order[]; totalElements: number } }>('/orders', {
      params: { page, size },
    }),

  getOpenOrders: () =>
    apiClient.get<{ success: boolean; data: Order[] }>('/orders/open'),

  cancelOrder: (orderId: string) =>
    apiClient.delete<{ success: boolean }>(`/orders/${orderId}`),
}
