import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { marketApi } from '@/api/market'
import { ordersApi, type PlaceOrderRequest } from '@/api/orders'
import { walletApi } from '@/api/wallet'
import { useMarketStore } from '@/store/marketStore'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import { PriceChange } from '@/components/shared/PriceChange'
import { toast } from 'sonner'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import { TrendingUp, TrendingDown } from 'lucide-react'
import { format } from 'date-fns'

interface PricePoint {
  time: string
  price: number
}

interface OrderFormValues {
  quantity: string
  price: string
  stopPrice: string
}

export function TradePage() {
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const { prices } = useMarketStore()
  const [selectedSymbol, setSelectedSymbol] = useState(searchParams.get('symbol') ?? 'BTC')
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY')
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT' | 'STOP_LOSS'>('MARKET')
  const [priceHistory, setPriceHistory] = useState<PricePoint[]>([])

  const { register, handleSubmit, watch, reset } = useForm<OrderFormValues>({
    defaultValues: { quantity: '', price: '', stopPrice: '' },
  })

  const { data: instruments } = useQuery({
    queryKey: ['instruments'],
    queryFn: () => marketApi.getInstruments().then((r) => r.data.data),
  })

  const { data: balances } = useQuery({
    queryKey: ['wallet-balances'],
    queryFn: () => walletApi.getAllBalances().then((r) => r.data.data),
    refetchInterval: 10000,
  })

  const selectedInstrument = instruments?.find((i) => i.symbol === selectedSymbol)
  const livePrice = prices[selectedSymbol]
  const isPositive = (livePrice?.changePct ?? 0) >= 0

  // Track price history for chart
  useEffect(() => {
    if (!livePrice) return
    setPriceHistory((prev) => {
      const entry = { time: format(new Date(), 'HH:mm:ss'), price: livePrice.ltp }
      const updated = [...prev, entry]
      return updated.slice(-60) // keep last 60 data points
    })
  }, [livePrice?.ltp])

  const inrBalance = balances?.find((b) => b.currency === 'INR')
  const usdtBalance = balances?.find((b) => b.currency === 'USDT')
  const balance = selectedInstrument?.currency === 'INR' ? inrBalance : usdtBalance

  const qty = parseFloat(watch('quantity') || '0')
  const limitPrice = parseFloat(watch('price') || '0')
  const effectivePrice = orderType === 'MARKET' ? (livePrice?.ltp ?? 0) : limitPrice
  const estimatedCost = qty * effectivePrice

  const placeMutation = useMutation({
    mutationFn: (data: PlaceOrderRequest) => ordersApi.placeOrder(data),
    onSuccess: () => {
      toast.success(`${side} order placed successfully!`)
      reset()
      queryClient.invalidateQueries({ queryKey: ['open-orders'] })
      queryClient.invalidateQueries({ queryKey: ['wallet-balances'] })
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Order failed'
      toast.error(msg)
    },
  })

  const onSubmit = (values: OrderFormValues) => {
    if (!selectedInstrument) return toast.error('Select an instrument first')
    placeMutation.mutate({
      instrumentId: selectedInstrument.id,
      symbol: selectedSymbol,
      orderType,
      side,
      productType: 'CASH',
      quantity: parseFloat(values.quantity),
      price: orderType !== 'MARKET' ? parseFloat(values.price) : undefined,
      stopPrice: orderType === 'STOP_LOSS' ? parseFloat(values.stopPrice) : undefined,
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Trade</h1>
        <p className="text-muted-foreground text-sm mt-1">Place orders with live market prices</p>
      </div>

      {/* Instrument selector */}
      <div className="flex flex-wrap gap-2">
        {instruments?.map((i) => (
          <button
            key={i.symbol}
            onClick={() => setSelectedSymbol(i.symbol)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors border ${
              selectedSymbol === i.symbol
                ? 'bg-primary text-primary-foreground border-primary'
                : 'border-border hover:bg-muted/50'
            }`}
          >
            {i.symbol}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Chart + Quote */}
        <div className="lg:col-span-2 space-y-4">
          {/* Quote header */}
          <Card>
            <CardContent className="pt-4 pb-4">
              <div className="flex items-center justify-between flex-wrap gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-2xl font-bold font-mono">
                      {livePrice?.ltp.toLocaleString('en-IN', { maximumFractionDigits: 2 }) ?? '—'}
                    </h2>
                    <Badge variant="outline">{selectedInstrument?.currency}</Badge>
                  </div>
                  <div className="flex items-center gap-4 mt-1 flex-wrap">
                    {livePrice && (
                      <PriceChange value={livePrice.change} pct={livePrice.changePct} />
                    )}
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-4 text-sm">
                  {[
                    { label: 'Open', value: livePrice?.open },
                    { label: 'High', value: livePrice?.high },
                    { label: 'Low', value: livePrice?.low },
                  ].map(({ label, value }) => (
                    <div key={label}>
                      <p className="text-xs text-muted-foreground">{label}</p>
                      <p className="font-mono font-medium tabular-nums">
                        {value?.toLocaleString('en-IN', { maximumFractionDigits: 2 }) ?? '—'}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Price chart */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm flex items-center gap-2">
                {isPositive ? (
                  <TrendingUp className="h-4 w-4 text-emerald-500" />
                ) : (
                  <TrendingDown className="h-4 w-4 text-red-500" />
                )}
                {selectedSymbol} · Live Price Feed
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={priceHistory} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop
                        offset="5%"
                        stopColor={isPositive ? '#10b981' : '#ef4444'}
                        stopOpacity={0.3}
                      />
                      <stop
                        offset="95%"
                        stopColor={isPositive ? '#10b981' : '#ef4444'}
                        stopOpacity={0}
                      />
                    </linearGradient>
                  </defs>
                  <XAxis
                    dataKey="time"
                    tick={{ fontSize: 10, fill: '#6b7280' }}
                    tickLine={false}
                    axisLine={false}
                    interval="preserveStartEnd"
                  />
                  <YAxis
                    domain={['auto', 'auto']}
                    tick={{ fontSize: 10, fill: '#6b7280' }}
                    tickLine={false}
                    axisLine={false}
                    width={70}
                    tickFormatter={(v: unknown) =>
                      typeof v === 'number' ? v.toLocaleString('en-IN', { maximumFractionDigits: 0 }) : String(v)
                    }
                  />
                  <Tooltip
                    contentStyle={{
                      background: 'hsl(var(--card))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                    formatter={(v: unknown) => [
                      typeof v === 'number' ? v.toLocaleString('en-IN', { maximumFractionDigits: 2 }) : String(v),
                      'Price',
                    ]}
                  />
                  <Area
                    type="monotone"
                    dataKey="price"
                    stroke={isPositive ? '#10b981' : '#ef4444'}
                    strokeWidth={2}
                    fill="url(#priceGradient)"
                    dot={false}
                    isAnimationActive={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>

        {/* Order Form */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Place Order</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* BUY / SELL toggle */}
              <Tabs
                value={side}
                onValueChange={(v) => setSide(v as 'BUY' | 'SELL')}
                className="w-full"
              >
                <TabsList className="w-full">
                  <TabsTrigger
                    value="BUY"
                    className="flex-1 data-[state=active]:bg-emerald-500 data-[state=active]:text-white"
                  >
                    BUY
                  </TabsTrigger>
                  <TabsTrigger
                    value="SELL"
                    className="flex-1 data-[state=active]:bg-red-500 data-[state=active]:text-white"
                  >
                    SELL
                  </TabsTrigger>
                </TabsList>
              </Tabs>

              {/* Order type */}
              <div className="space-y-1.5">
                <Label>Order Type</Label>
                <Select
                  value={orderType}
                  onValueChange={(v) => setOrderType(v as typeof orderType)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MARKET">Market</SelectItem>
                    <SelectItem value="LIMIT">Limit</SelectItem>
                    <SelectItem value="STOP_LOSS">Stop Loss</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* Quantity */}
              <div className="space-y-1.5">
                <Label>Quantity</Label>
                <Input
                  type="number"
                  step="any"
                  placeholder="0.00"
                  {...register('quantity', { required: true, min: 0.0001 })}
                />
              </div>

              {/* Limit price */}
              {(orderType === 'LIMIT' || orderType === 'STOP_LOSS') && (
                <div className="space-y-1.5">
                  <Label>Limit Price</Label>
                  <Input
                    type="number"
                    step="any"
                    placeholder="0.00"
                    {...register('price', { required: true })}
                  />
                </div>
              )}

              {/* Stop price */}
              {orderType === 'STOP_LOSS' && (
                <div className="space-y-1.5">
                  <Label>Stop Price</Label>
                  <Input
                    type="number"
                    step="any"
                    placeholder="0.00"
                    {...register('stopPrice', { required: true })}
                  />
                </div>
              )}

              <Separator />

              {/* Summary */}
              <div className="space-y-1.5 text-sm">
                <div className="flex justify-between text-muted-foreground">
                  <span>Market Price</span>
                  <span className="font-mono">
                    {livePrice?.ltp.toLocaleString('en-IN', { maximumFractionDigits: 2 }) ?? '—'}
                  </span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>Available</span>
                  <span className="font-mono">
                    {balance?.currency === 'INR' ? '₹' : '$'}
                    {(balance?.availableBalance ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                  </span>
                </div>
                <div className="flex justify-between font-medium">
                  <span>Est. Cost</span>
                  <span className="font-mono">
                    {balance?.currency === 'INR' ? '₹' : '$'}
                    {estimatedCost.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                  </span>
                </div>
              </div>

              <Button
                type="submit"
                className={`w-full font-semibold ${
                  side === 'BUY'
                    ? 'bg-emerald-500 hover:bg-emerald-600 text-white'
                    : 'bg-red-500 hover:bg-red-600 text-white'
                }`}
                disabled={placeMutation.isPending}
              >
                {placeMutation.isPending
                  ? 'Placing…'
                  : `${side} ${selectedSymbol}`}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
