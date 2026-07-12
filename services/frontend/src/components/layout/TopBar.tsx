import { useNavigate } from 'react-router-dom'
import { Bell, Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useQuery } from '@tanstack/react-query'
import { notificationsApi } from '@/api/notifications'
import { Badge } from '@/components/ui/badge'
import { useAuthStore } from '@/store/authStore'

export function TopBar() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthStore()

  const { data: unreadData } = useQuery({
    queryKey: ['unread-count'],
    queryFn: () => notificationsApi.getUnreadCount().then((r) => r.data.data),
    enabled: isAuthenticated,
    refetchInterval: 30000,
  })

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center gap-4 border-b bg-background/95 backdrop-blur px-6">
      <div className="relative flex-1 max-w-sm">
        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search instruments..."
          className="pl-8 h-8 bg-muted/40 border-0 focus-visible:ring-1"
          onFocus={() => navigate('/market')}
        />
      </div>

      <div className="ml-auto flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon"
          className="relative"
          onClick={() => navigate('/notifications')}
        >
          <Bell className="h-4 w-4" />
          {(unreadData ?? 0) > 0 && (
            <Badge
              className="absolute -top-1 -right-1 h-4 w-4 p-0 flex items-center justify-center text-[10px]"
              variant="destructive"
            >
              {unreadData}
            </Badge>
          )}
        </Button>
      </div>
    </header>
  )
}
