import { apiClient } from './client'

export interface AssistantMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AssistantSource {
  title: string
  source: string
  score: number
}

export interface AssistantReply {
  reply: string
  model: string
  sources: AssistantSource[]
}

export const assistantApi = {
  chat: (message: string, history: AssistantMessage[]) =>
    apiClient.post<{ success: boolean; data: AssistantReply }>('/assistant/chat', {
      message,
      history,
    }),
}
