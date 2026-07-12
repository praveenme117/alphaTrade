import { useQuery } from '@tanstack/react-query'
import { portfolioApi } from '@/api/portfolio'
import { ordersApi } from '@/api/orders'
import { walletApi } from '@/api/wallet'
import { useMarketStore } from '@/store/marketStore'
import { useAuthStore } from '@/store/authStore'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { PriceChange } from '@/components/shared/PriceChange'
import { TrendingUp, Wallet, ShoppingCart, ArrowUpRight, ArrowDownRight, Activity } from 'lucide-react'
import { Link } from 'react-router-dom'
import { format } from 'date-fns'

function StatCard({
  title,
  value,
  sub,
  icon: Icon,
  trend,
}: {
  title: string
  value: string
  sub?: string
  icon: React.ElementType
  trend?: 'up' | 'down' | 'neutral'
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10">
            <Icon className="h-4 w-4 text-primary" />
          </div>
        </div>
        <p className="text-2xl font-bold tabular-nums">{value}</p>
        {sub && (
          <p
            className={`text-xs mt-1 flex items-center gap-1 ${
              trend === 'up'
                ? 'text-emerald-500'
                : trend === 'down'
                ? 'text-red-500'
                : 'text-muted-foreground'
            }`}
          >
            {trend === 'up' && <ArrowUpRight className="h-3 w-3" />}
            {trend === 'down' && <ArrowDownRight className="h-3 w-3" />}
            {sub}
          </p>
        )}
      </CardContent>
    </Card>
  )
}

export function DashboardPage() {
  const { user } = useAuthStore()
  const { prices } = useMarketStore()

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['portfolio-summary'],
    queryFn: () => portfolioApi.getSummary().then((r) => r.data.data),
    refetchInterval: 10000,
  })

  const { data: openOrders, isLoading: ordersLoading } = useQuery({
    queryKey: ['open-orders'],
    queryFn: () => ordersApi.getOpenOrders().then((r) => r.data.data),
    refetchInterval: 5000,
  })

  const { data: balances, isLoading: walletLoading } = useQuery({
    queryKey: ['wallet-balances'],
    queryFn: () => walletApi.getAllBalances().then((r) => r.data.data),
    refetchInterval: 15000,
  })

  const topMovers = Object.values(prices)
    .filter((p) => p.changePct !== 0)
    .sort((a, b) => Math.abs(b.changePct) - Math.abs(a.changePct))
    .slice(0, 5)

  const pnlTrend = summary
    ? summary.unrealizedPnl >= 0
      ? 'up'
      : 'down'
    : 'neutral'

  const inrBalance = balances?.find((b) => b.currency === 'INR')
  const usdtBalance = balances?.find((b) => b.currency === 'USDT')

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">
          Good {new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 17 ? 'afternoon' : 'evening'},{' '}
          {user?.fullName?.split(' ')[0]} 👋
        </h1>
        <p className="text-muted-foreground text-sm mt-1">
          {format(new Date(), 'EEEE, MMMM d, yyyy')} · Here's your portfolio overview
        </p>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {summaryLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="pt-6 space-y-3">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-8 w-32" />
              </CardContent>
            </Card>
          ))
        ) : (
          <>
            <StatCard
              title="Portfolio Value"
              value={`₹${(summary?.currentValue ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
              sub={`Invested ₹${(summary?.totalInvested ?? 0).toLocaleString('en-IN')}`}
              icon={TrendingUp}
              trend="neutral"
            />
            <StatCard
              title="Unrealized P&L"
              value={`₹${(summary?.unrealizedPnl ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
              sub={
                summary
                  ? `${summary.unrealizedPnl >= 0 ? '+' : ''}${(
                      ((summary.unrealizedPnl) / (summary.totalInvested || 1)) *
                      100
                    ).toFixed(2)}% overall`
                  : undefined
              }
              icon={Activity}
              trend={pnlTrend}
            />
            <StatCard
              title="INR Balance"
              value={`₹${(inrBalance?.availableBalance ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
              sub={walletLoading ? undefined : `Locked: ₹${(inrBalance?.lockedBalance ?? 0).toLocaleString()}`}
              icon={Wallet}
              trend="neutral"
            />
            <StatCard
              title="Open Orders"
              value={String(openOrders?.length ?? 0)}
              sub={ordersLoading ? undefined : 'Active orders'}
              icon={ShoppingCart}
              trend="neutral"
            />
          </>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Movers */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Top Movers</CardTitle>
            <CardDescription>Live price changes</CardDescription>
          </CardHeader>
          <CardContent>
            {topMovers.length === 0 ? (
              <div className="space-y-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="flex items-center justify-between">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-4 w-16" />
                  </div>
                ))}
              </div>
            ) : (
              <div className="space-y-3">
                {topMovers.map((p) => (
                  <Link
                    key={p.symbol}
                    to={`/trade?symbol=${p.symbol}`}
                    className="flex items-center justify-between py-2 px-3 rounded-lg hover:bg-muted/50 transition-colors group"
                  >
                    <div className="flex items-center gap-3">
                      <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
                        <span className="text-xs font-bold text-primary">
                          {p.symbol.slice(0, 2)}
                        </span>
                      </div>
                      <div>
                        <p className="text-sm font-semibold">{p.symbol}</p>
                        <p className="text-xs text-muted-foreground font-mono">
                          {p.ltp.toLocaleString('en-US', { maximumFractionDigits: 2 })}
                        </p>
                      </div>
                    </div>
                    <PriceChange value={p.change} pct={p.changePct} showIcon={false} />
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Open Orders */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Open Orders</CardTitle>
            <CardDescription>Currently active orders</CardDescription>
          </CardHeader>
          <CardContent>
            {ordersLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : openOrders?.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-center">
                <ShoppingCart className="h-8 w-8 text-muted-foreground mb-2" />
                <p className="text-sm text-muted-foreground">No open orders</p>
                <Link to="/trade" className="text-xs text-primary mt-1 hover:underline">
                  Place your first order →
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {openOrders?.slice(0, 5).map((order) => (
                  <div
                    key={order.id}
                    className="flex items-center justify-between py-2 px-3 rounded-lg bg-muted/30"
                  >
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={order.side === 'BUY' ? 'default' : 'destructive'}
                        className="text-xs"
                      >
                        {order.side}
                      </Badge>
                      <span className="text-sm font-medium">{order.symbol}</span>
                      <span className="text-xs text-muted-foreground">×{order.quantity}</span>
                    </div>
                    <span className="text-xs text-muted-foreground">{order.orderType}</span>
                  </div>
                ))}
                {(openOrders?.length ?? 0) > 5 && (
                  <Link
                    to="/orders"
                    className="block text-center text-xs text-primary hover:underline pt-1"
                  >
                    View all {openOrders?.length} orders →
                  </Link>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Wallet summary */}
      {(inrBalance || usdtBalance) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Wallet Balances</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              {[inrBalance, usdtBalance].filter(Boolean).map((b) => (
                <div key={b!.currency} className="rounded-xl border p-4">
                  <p className="text-xs text-muted-foreground mb-1">{b!.currency}</p>
                  <p className="text-xl font-bold tabular-nums">
                    {b!.currency === 'INR' ? '₹' : '$'}
                    {b!.balance.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    Available: {b!.currency === 'INR' ? '₹' : '$'}
                    {b!.availableBalance.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                  </p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
