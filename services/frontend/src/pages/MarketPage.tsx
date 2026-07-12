import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { marketApi, type Instrument } from '@/api/market'
import { useMarketStore } from '@/store/marketStore'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Skeleton } from '@/components/ui/skeleton'
import { PriceChange } from '@/components/shared/PriceChange'
import { Search, TrendingUp, TrendingDown } from 'lucide-react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

function InstrumentRow({
  instrument,
  onTrade,
}: {
  instrument: Instrument
  onTrade: (symbol: string) => void
}) {
  const { prices } = useMarketStore()
  const live = prices[instrument.symbol]
  const isUp = (live?.changePct ?? 0) >= 0

  return (
    <TableRow
      className="cursor-pointer hover:bg-muted/50 transition-colors"
      onClick={() => onTrade(instrument.symbol)}
    >
      <TableCell>
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
            <span className="text-xs font-bold text-primary">{instrument.symbol.slice(0, 2)}</span>
          </div>
          <div>
            <p className="font-semibold text-sm">{instrument.symbol}</p>
            <p className="text-xs text-muted-foreground">{instrument.name}</p>
          </div>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant="secondary" className="text-xs">
          {instrument.type}
        </Badge>
      </TableCell>
      <TableCell className="text-right font-mono font-semibold tabular-nums">
        {live ? live.ltp.toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—'}
      </TableCell>
      <TableCell className="text-right">
        {live ? (
          <PriceChange value={live.change} pct={live.changePct} showIcon={false} />
        ) : (
          <span className="text-muted-foreground">—</span>
        )}
      </TableCell>
      <TableCell className="text-right font-mono text-sm text-muted-foreground tabular-nums">
        {live ? live.high.toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—'}
      </TableCell>
      <TableCell className="text-right font-mono text-sm text-muted-foreground tabular-nums">
        {live ? live.low.toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—'}
      </TableCell>
      <TableCell className="text-right">
        <div
          className={`inline-flex items-center gap-1 text-xs font-medium ${
            isUp ? 'text-emerald-500' : 'text-red-500'
          }`}
        >
          {isUp ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
          Trade
        </div>
      </TableCell>
    </TableRow>
  )
}

export function MarketPage() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')

  const { data: instruments, isLoading } = useQuery({
    queryKey: ['instruments'],
    queryFn: () => marketApi.getInstruments().then((r) => r.data.data),
  })

  const filtered = instruments?.filter(
    (i) =>
      i.symbol.toLowerCase().includes(search.toLowerCase()) ||
      i.name.toLowerCase().includes(search.toLowerCase())
  )

  const stocks = filtered?.filter((i) => i.type === 'STOCK')
  const cryptos = filtered?.filter((i) => i.type === 'CRYPTO')

  const handleTrade = (symbol: string) => navigate(`/trade?symbol=${symbol}`)

  const TableHead_ = () => (
    <TableHeader>
      <TableRow>
        <TableHead>Instrument</TableHead>
        <TableHead>Type</TableHead>
        <TableHead className="text-right">LTP</TableHead>
        <TableHead className="text-right">Change</TableHead>
        <TableHead className="text-right">High</TableHead>
        <TableHead className="text-right">Low</TableHead>
        <TableHead className="text-right">Action</TableHead>
      </TableRow>
    </TableHeader>
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Market</h1>
        <p className="text-muted-foreground text-sm mt-1">Live prices updated every second</p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search symbol or name…"
          className="pl-9"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <Tabs defaultValue="all">
        <TabsList>
          <TabsTrigger value="all">All ({filtered?.length ?? 0})</TabsTrigger>
          <TabsTrigger value="stocks">Stocks ({stocks?.length ?? 0})</TabsTrigger>
          <TabsTrigger value="crypto">Crypto ({cryptos?.length ?? 0})</TabsTrigger>
        </TabsList>

        {['all', 'stocks', 'crypto'].map((tab) => {
          const list =
            tab === 'all' ? filtered : tab === 'stocks' ? stocks : cryptos

          return (
            <TabsContent key={tab} value={tab}>
              <Card>
                <CardContent className="p-0">
                  {isLoading ? (
                    <div className="p-6 space-y-3">
                      {Array.from({ length: 6 }).map((_, i) => (
                        <Skeleton key={i} className="h-12 w-full" />
                      ))}
                    </div>
                  ) : (
                    <Table>
                      <TableHead_ />
                      <TableBody>
                        {list?.map((instrument) => (
                          <InstrumentRow
                            key={instrument.id}
                            instrument={instrument}
                            onTrade={handleTrade}
                          />
                        ))}
                      </TableBody>
                    </Table>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
          )
        })}
      </Tabs>
    </div>
  )
}
