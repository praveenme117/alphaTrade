import { useQuery } from '@tanstack/react-query'
import { portfolioApi, type Holding } from '@/api/portfolio'
import { useMarketStore } from '@/store/marketStore'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { PriceChange } from '@/components/shared/PriceChange'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { TrendingUp, TrendingDown, Briefcase } from 'lucide-react'
import { Link } from 'react-router-dom'

function SummaryCard({
  label,
  value,
  sub,
  positive,
}: {
  label: string
  value: string
  sub?: string
  positive?: boolean
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <p className="text-sm text-muted-foreground mb-2">{label}</p>
        <p
          className={`text-2xl font-bold tabular-nums ${
            positive === true
              ? 'text-emerald-500'
              : positive === false
              ? 'text-red-500'
              : ''
          }`}
        >
          {value}
        </p>
        {sub && <p className="text-xs text-muted-foreground mt-1">{sub}</p>}
      </CardContent>
    </Card>
  )
}

function HoldingRow({ holding }: { holding: Holding }) {
  const { prices } = useMarketStore()
  const live = prices[holding.symbol]
  const ltp = live?.ltp ?? holding.currentPrice ?? holding.averageBuyPrice
  const currentValue = ltp * holding.quantity
  const invested = holding.averageBuyPrice * holding.quantity
  const pnl = currentValue - invested
  const pnlPct = invested > 0 ? (pnl / invested) * 100 : 0
  const isPositive = pnl >= 0

  return (
    <TableRow>
      <TableCell>
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <span className="text-xs font-bold text-primary">{holding.symbol.slice(0, 2)}</span>
          </div>
          <span className="font-semibold">{holding.symbol}</span>
        </div>
      </TableCell>
      <TableCell className="font-mono tabular-nums">{holding.quantity}</TableCell>
      <TableCell className="font-mono tabular-nums text-muted-foreground">
        {holding.averageBuyPrice.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
      </TableCell>
      <TableCell className="font-mono tabular-nums">
        {ltp.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
      </TableCell>
      <TableCell className="font-mono tabular-nums">
        {currentValue.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
      </TableCell>
      <TableCell>
        <div className={`flex items-center gap-1 font-medium tabular-nums ${isPositive ? 'text-emerald-500' : 'text-red-500'}`}>
          {isPositive ? (
            <TrendingUp className="h-3 w-3" />
          ) : (
            <TrendingDown className="h-3 w-3" />
          )}
          {isPositive ? '+' : ''}
          {pnl.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
          <span className="text-xs opacity-80">
            ({isPositive ? '+' : ''}
            {pnlPct.toFixed(2)}%)
          </span>
        </div>
      </TableCell>
      <TableCell>
        <Link
          to={`/trade?symbol=${holding.symbol}`}
          className="text-xs text-primary hover:underline"
        >
          Trade →
        </Link>
      </TableCell>
    </TableRow>
  )
}

export function PortfolioPage() {
  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['portfolio-summary'],
    queryFn: () => portfolioApi.getSummary().then((r) => r.data.data),
    refetchInterval: 10000,
  })

  const { data: holdings, isLoading: holdingsLoading } = useQuery({
    queryKey: ['holdings'],
    queryFn: () => portfolioApi.getHoldings().then((r) => r.data.data),
    refetchInterval: 10000,
  })

  const returnPct =
    summary && summary.totalInvested > 0
      ? ((summary.currentValue - summary.totalInvested) / summary.totalInvested) * 100
      : 0

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Portfolio</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Track your holdings and performance
        </p>
      </div>

      {/* Summary cards */}
      {summaryLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="pt-6 space-y-3">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-8 w-28" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <SummaryCard
            label="Total Invested"
            value={`₹${(summary?.totalInvested ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
          />
          <SummaryCard
            label="Current Value"
            value={`₹${(summary?.currentValue ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
            sub={`${returnPct >= 0 ? '+' : ''}${returnPct.toFixed(2)}% overall`}
          />
          <SummaryCard
            label="Unrealized P&L"
            value={`₹${(summary?.unrealizedPnl ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
            positive={summary ? summary.unrealizedPnl >= 0 : undefined}
          />
          <SummaryCard
            label="Realized P&L"
            value={`₹${(summary?.realizedPnl ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`}
            positive={summary ? summary.realizedPnl >= 0 : undefined}
          />
        </div>
      )}

      {/* Holdings table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Briefcase className="h-4 w-4" />
            Holdings ({holdings?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {holdingsLoading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : holdings?.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <Briefcase className="h-8 w-8 text-muted-foreground mb-3" />
              <p className="text-muted-foreground">No holdings yet</p>
              <Link to="/trade" className="text-sm text-primary mt-1 hover:underline">
                Start trading →
              </Link>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Symbol</TableHead>
                  <TableHead>Qty</TableHead>
                  <TableHead>Avg Buy Price</TableHead>
                  <TableHead>LTP</TableHead>
                  <TableHead>Current Value</TableHead>
                  <TableHead>P&L</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {holdings?.map((h) => (
                  <HoldingRow key={h.id} holding={h} />
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
