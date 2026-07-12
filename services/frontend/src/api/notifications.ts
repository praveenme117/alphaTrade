import { apiClient } from './client'

export interface Notification {
  id: string
  title: string
  message: string
  type: string
  status: 'UNREAD' | 'READ'
  createdAt: string
}

export const notificationsApi = {
  getNotifications: (page = 0, size = 20) =>
    apiClient.get<{ success: boolean; data: { content: Notification[]; totalElements: number } }>('/notifications', {
      params: { page, size },
    }),

  getUnreadCount: () =>
    apiClient.get<{ success: boolean; data: number }>('/notifications/unread-count'),

  markRead: (id: string) =>
    apiClient.patch(`/notifications/${id}/read`),

  markAllRead: () =>
    apiClient.patch('/notifications/read-all'),
}
