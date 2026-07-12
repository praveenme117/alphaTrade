import { cn } from '@/lib/utils'
import { TrendingUp, TrendingDown } from 'lucide-react'

interface PriceChangeProps {
  value: number
  pct?: number
  showIcon?: boolean
  className?: string
}

export function PriceChange({ value, pct, showIcon = true, className }: PriceChangeProps) {
  const isPositive = value >= 0

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 font-medium tabular-nums',
        isPositive ? 'text-emerald-500' : 'text-red-500',
        className
      )}
    >
      {showIcon &&
        (isPositive ? (
          <TrendingUp className="h-3 w-3 shrink-0" />
        ) : (
          <TrendingDown className="h-3 w-3 shrink-0" />
        ))}
      {isPositive ? '+' : ''}
      {value.toFixed(2)}
      {pct !== undefined && (
        <span className="text-xs opacity-80">
          ({isPositive ? '+' : ''}
          {pct.toFixed(2)}%)
        </span>
      )}
    </span>
  )
}

export function formatPrice(value: number, currency?: string): string {
  if (currency === 'INR') {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(value)
  }
  if (currency === 'USDT') {
    return `$${value.toLocaleString('en-US', { maximumFractionDigits: 2 })}`
  }
  return value.toLocaleString('en-US', { maximumFractionDigits: 8 })
}
