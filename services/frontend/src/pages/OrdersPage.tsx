import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ordersApi, type Order } from '@/api/orders'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { X } from 'lucide-react'

const statusVariant: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  OPEN: 'default',
  FILLED: 'secondary',
  PARTIALLY_FILLED: 'outline',
  CANCELLED: 'outline',
  REJECTED: 'destructive',
}

function OrderRow({
  order,
  showCancel,
  onCancel,
}: {
  order: Order
  showCancel?: boolean
  onCancel?: (id: string) => void
}) {
  return (
    <TableRow>
      <TableCell>
        <div className="flex items-center gap-2">
          <Badge
            variant={order.side === 'BUY' ? 'default' : 'destructive'}
            className="text-xs w-10 justify-center"
          >
            {order.side}
          </Badge>
          <span className="font-semibold">{order.symbol}</span>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant="outline" className="text-xs">
          {order.orderType}
        </Badge>
      </TableCell>
      <TableCell className="font-mono tabular-nums text-sm">{order.quantity}</TableCell>
      <TableCell className="font-mono tabular-nums text-sm text-muted-foreground">
        {order.price?.toLocaleString('en-IN', { maximumFractionDigits: 2 }) ?? 'MARKET'}
      </TableCell>
      <TableCell className="font-mono tabular-nums text-sm text-muted-foreground">
        {order.averagePrice?.toLocaleString('en-IN', { maximumFractionDigits: 2 }) ?? '—'}
      </TableCell>
      <TableCell>
        <Badge variant={statusVariant[order.status] ?? 'outline'} className="text-xs">
          {order.status}
        </Badge>
      </TableCell>
      <TableCell className="text-xs text-muted-foreground">
        {format(new Date(order.createdAt), 'MMM d, HH:mm:ss')}
      </TableCell>
      {showCancel && (
        <TableCell>
          {order.status === 'OPEN' && (
            <Button
              size="sm"
              variant="ghost"
              className="h-7 w-7 p-0 text-muted-foreground hover:text-destructive"
              onClick={() => onCancel?.(order.id)}
            >
              <X className="h-3.5 w-3.5" />
            </Button>
          )}
        </TableCell>
      )}
    </TableRow>
  )
}

export function OrdersPage() {
  const queryClient = useQueryClient()
  const [cancelId, setCancelId] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const { data: openOrders, isLoading: openLoading } = useQuery({
    queryKey: ['open-orders'],
    queryFn: () => ordersApi.getOpenOrders().then((r) => r.data.data),
    refetchInterval: 5000,
  })

  const { data: historyData, isLoading: historyLoading } = useQuery({
    queryKey: ['order-history', page],
    queryFn: () => ordersApi.getOrders(page, 20).then((r) => r.data.data),
  })

  const cancelMutation = useMutation({
    mutationFn: (id: string) => ordersApi.cancelOrder(id),
    onSuccess: () => {
      toast.success('Order cancelled')
      setCancelId(null)
      queryClient.invalidateQueries({ queryKey: ['open-orders'] })
      queryClient.invalidateQueries({ queryKey: ['order-history'] })
      queryClient.invalidateQueries({ queryKey: ['wallet-balances'] })
    },
    onError: () => toast.error('Failed to cancel order'),
  })

  const columns = (
    <TableHeader>
      <TableRow>
        <TableHead>Symbol</TableHead>
        <TableHead>Type</TableHead>
        <TableHead>Qty</TableHead>
        <TableHead>Price</TableHead>
        <TableHead>Avg Price</TableHead>
        <TableHead>Status</TableHead>
        <TableHead>Time</TableHead>
      </TableRow>
    </TableHeader>
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Orders</h1>
        <p className="text-muted-foreground text-sm mt-1">Manage your open and past orders</p>
      </div>

      <Tabs defaultValue="open">
        <TabsList>
          <TabsTrigger value="open">
            Open Orders ({openOrders?.length ?? 0})
          </TabsTrigger>
          <TabsTrigger value="history">Order History</TabsTrigger>
        </TabsList>

        <TabsContent value="open">
          <Card>
            <CardContent className="p-0">
              {openLoading ? (
                <div className="p-6 space-y-3">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Skeleton key={i} className="h-12 w-full" />
                  ))}
                </div>
              ) : openOrders?.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-16 text-center">
                  <p className="text-muted-foreground">No open orders</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Symbol</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Qty</TableHead>
                      <TableHead>Price</TableHead>
                      <TableHead>Avg Price</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Time</TableHead>
                      <TableHead></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {openOrders?.map((order) => (
                      <OrderRow
                        key={order.id}
                        order={order}
                        showCancel
                        onCancel={setCancelId}
                      />
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="history">
          <Card>
            <CardContent className="p-0">
              {historyLoading ? (
                <div className="p-6 space-y-3">
                  {Array.from({ length: 8 }).map((_, i) => (
                    <Skeleton key={i} className="h-12 w-full" />
                  ))}
                </div>
              ) : (
                <>
                  <Table>
                    {columns}
                    <TableBody>
                      {historyData?.content?.map((order) => (
                        <OrderRow key={order.id} order={order} />
                      ))}
                    </TableBody>
                  </Table>
                  {(historyData?.totalElements ?? 0) > 20 && (
                    <div className="flex justify-center gap-2 p-4 border-t">
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                      >
                        Previous
                      </Button>
                      <span className="text-sm text-muted-foreground self-center">
                        Page {page + 1}
                      </span>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={(page + 1) * 20 >= (historyData?.totalElements ?? 0)}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Cancel confirmation dialog */}
      <Dialog open={!!cancelId} onOpenChange={() => setCancelId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel Order</DialogTitle>
            <DialogDescription>
              Are you sure you want to cancel this order? Locked funds will be released back to your
              wallet.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelId(null)}>
              Keep Order
            </Button>
            <Button
              variant="destructive"
              onClick={() => cancelId && cancelMutation.mutate(cancelId)}
              disabled={cancelMutation.isPending}
            >
              {cancelMutation.isPending ? 'Cancelling…' : 'Cancel Order'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
