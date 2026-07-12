import { apiClient } from './client'

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  phone?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: {
    id: string
    email: string
    fullName: string
    role: string
    kycStatus: string
    active: boolean
  }
}

export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<{ success: boolean; data: AuthResponse }>('/auth/register', data),

  login: (data: LoginRequest) =>
    apiClient.post<{ success: boolean; data: AuthResponse }>('/auth/login', data),

  refresh: (refreshToken: string) =>
    apiClient.post<{ success: boolean; data: AuthResponse }>('/auth/refresh', { refreshToken }),

  me: () =>
    apiClient.get<{ success: boolean; data: AuthResponse['user'] }>('/auth/me'),

  logout: () =>
    apiClient.post('/auth/logout'),
}
