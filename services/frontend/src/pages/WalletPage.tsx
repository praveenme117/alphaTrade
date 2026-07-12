import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { walletApi } from '@/api/wallet'
import { paymentsApi } from '@/api/payments'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { toast } from 'sonner'
import { Wallet, ArrowDownToLine, ArrowUpFromLine, Lock } from 'lucide-react'
import { format } from 'date-fns'

interface FundsForm {
  amount: string
}

export function WalletPage() {
  const queryClient = useQueryClient()
  const [modal, setModal] = useState<{ type: 'deposit' | 'withdraw'; currency: string } | null>(null)
  const [page, setPage] = useState(0)

  const { data: balances, isLoading: balancesLoading } = useQuery({
    queryKey: ['wallet-balances'],
    queryFn: () => walletApi.getAllBalances().then((r) => r.data.data),
    refetchInterval: 15000,
  })

  const { data: ledger, isLoading: ledgerLoading } = useQuery({
    queryKey: ['ledger', page],
    queryFn: () => walletApi.getLedger(page, 15).then((r) => r.data.data),
  })

  const { register, handleSubmit, reset } = useForm<FundsForm>()

  const depositMutation = useMutation({
    mutationFn: ({ amount, currency }: { amount: number; currency: string }) =>
      paymentsApi.deposit({ amount, currency }),
    onSuccess: () => {
      toast.success('Deposit initiated! Funds will be credited shortly.')
      reset()
      setModal(null)
      queryClient.invalidateQueries({ queryKey: ['wallet-balances'] })
      queryClient.invalidateQueries({ queryKey: ['ledger'] })
    },
    onError: () => toast.error('Deposit failed'),
  })

  const withdrawMutation = useMutation({
    mutationFn: ({ amount, currency }: { amount: number; currency: string }) =>
      paymentsApi.withdraw({ amount, currency }),
    onSuccess: () => {
      toast.success('Withdrawal initiated successfully!')
      reset()
      setModal(null)
      queryClient.invalidateQueries({ queryKey: ['wallet-balances'] })
      queryClient.invalidateQueries({ queryKey: ['ledger'] })
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Withdrawal failed'
      toast.error(msg)
    },
  })

  const onSubmit = (values: FundsForm) => {
    if (!modal) return
    const amount = parseFloat(values.amount)
    if (isNaN(amount) || amount <= 0) return toast.error('Enter a valid amount')
    if (modal.type === 'deposit') {
      depositMutation.mutate({ amount, currency: modal.currency })
    } else {
      withdrawMutation.mutate({ amount, currency: modal.currency })
    }
  }

  const currencySymbol = (currency: string) => (currency === 'INR' ? '₹' : '$')

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Wallet</h1>
        <p className="text-muted-foreground text-sm mt-1">Manage your funds and view transaction history</p>
      </div>

      {/* Balance cards */}
      {balancesLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Array.from({ length: 2 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="pt-6 space-y-3">
                <Skeleton className="h-4 w-16" />
                <Skeleton className="h-10 w-32" />
                <Skeleton className="h-3 w-24" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {balances?.map((b) => (
            <Card key={b.currency} className="relative overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-primary/5 -translate-y-8 translate-x-8" />
              <CardContent className="pt-6 pb-6">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <div className="flex items-center gap-2">
                      <Wallet className="h-4 w-4 text-muted-foreground" />
                      <span className="text-sm font-medium text-muted-foreground">{b.currency}</span>
                    </div>
                    <p className="text-3xl font-bold tabular-nums mt-1">
                      {currencySymbol(b.currency)}
                      {b.balance.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
                  <div className="rounded-lg bg-muted/40 p-3">
                    <p className="text-xs text-muted-foreground mb-1">Available</p>
                    <p className="font-semibold tabular-nums text-emerald-500">
                      {currencySymbol(b.currency)}
                      {b.availableBalance.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                    </p>
                  </div>
                  <div className="rounded-lg bg-muted/40 p-3">
                    <div className="flex items-center gap-1 mb-1">
                      <Lock className="h-3 w-3 text-muted-foreground" />
                      <p className="text-xs text-muted-foreground">Locked</p>
                    </div>
                    <p className="font-semibold tabular-nums text-amber-500">
                      {currencySymbol(b.currency)}
                      {b.lockedBalance.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button
                    size="sm"
                    className="flex-1 gap-1"
                    onClick={() => setModal({ type: 'deposit', currency: b.currency })}
                  >
                    <ArrowDownToLine className="h-3.5 w-3.5" />
                    Deposit
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 gap-1"
                    onClick={() => setModal({ type: 'withdraw', currency: b.currency })}
                  >
                    <ArrowUpFromLine className="h-3.5 w-3.5" />
                    Withdraw
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Transaction ledger */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Transaction History</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {ledgerLoading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Type</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                    <TableHead>Currency</TableHead>
                    <TableHead>Date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {ledger?.content?.map((entry) => {
                    const isCredit = entry.amount > 0
                    return (
                      <TableRow key={entry.id}>
                        <TableCell>
                          <Badge variant={isCredit ? 'default' : 'secondary'} className="text-xs">
                            {entry.type}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground max-w-[240px] truncate">
                          {entry.description}
                        </TableCell>
                        <TableCell className={`text-right font-mono font-semibold tabular-nums ${isCredit ? 'text-emerald-500' : 'text-red-500'}`}>
                          {isCredit ? '+' : ''}
                          {entry.amount.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {entry.currency}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {format(new Date(entry.createdAt), 'MMM d, HH:mm')}
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
              {(ledger?.totalElements ?? 0) > 15 && (
                <div className="flex justify-center gap-2 p-4 border-t">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    Previous
                  </Button>
                  <span className="text-sm text-muted-foreground self-center">Page {page + 1}</span>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={(page + 1) * 15 >= (ledger?.totalElements ?? 0)}
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

      {/* Deposit/Withdraw Modal */}
      <Dialog open={!!modal} onOpenChange={() => { setModal(null); reset() }}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {modal?.type === 'deposit' ? (
                <ArrowDownToLine className="h-4 w-4 text-emerald-500" />
              ) : (
                <ArrowUpFromLine className="h-4 w-4 text-red-500" />
              )}
              {modal?.type === 'deposit' ? 'Deposit' : 'Withdraw'} {modal?.currency}
            </DialogTitle>
            <DialogDescription>
              {modal?.type === 'deposit'
                ? 'Funds will be credited to your wallet instantly (mock gateway).'
                : 'Funds will be deducted from your available balance.'}
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <Label>Amount ({modal?.currency})</Label>
              <Input
                type="number"
                step="any"
                placeholder="0.00"
                autoFocus
                {...register('amount', { required: true })}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => { setModal(null); reset() }}>
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={depositMutation.isPending || withdrawMutation.isPending}
                className={
                  modal?.type === 'deposit'
                    ? 'bg-emerald-500 hover:bg-emerald-600 text-white'
                    : 'bg-red-500 hover:bg-red-600 text-white'
                }
              >
                {depositMutation.isPending || withdrawMutation.isPending
                  ? 'Processing…'
                  : `Confirm ${modal?.type === 'deposit' ? 'Deposit' : 'Withdrawal'}`}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
