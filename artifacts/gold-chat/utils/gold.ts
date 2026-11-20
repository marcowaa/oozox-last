import { GoldKarat, GoldPrices, TransactionStatus } from '@/types';

export const DEFAULT_GOLD_PRICES: GoldPrices = {
  '14k': 2500,
  '18k': 3200,
  '21k': 3750,
  '24k': 4300,
  goldPound: 30000,
  ounceUSD: 2650,
  updatedAt: Date.now(),
};

export const DEFAULT_MANUFACTURERS = [
  'لازوردي', 'معوض', 'داماس', 'السبيعي', 'الفردان',
  'الرميزان', 'مجوهرات', 'طيف', 'كارتييه', 'بولغري',
];

export const GOLD_TYPES = [
  'مصاغ', 'سبائك', 'عملات', 'جنيه ذهب', 'أساور', 'خواتم', 'قلائد', 'أقراط', 'أخرى',
];

export const KARAT_LABELS: Record<GoldKarat, string> = {
  '14k': 'عيار 14',
  '18k': 'عيار 18',
  '21k': 'عيار 21',
  '24k': 'عيار 24',
};

export const ALL_KARATS: GoldKarat[] = ['14k', '18k', '21k', '24k'];

// ============================================================================
// Basic gold price calculation
// ============================================================================

export function calculateGoldPrice(params: {
  weight: number;
  pricePerGram: number;
  manufacturingCostPerGram: number;
}) {
  const { weight, pricePerGram, manufacturingCostPerGram } = params;
  const totalGoldPrice = weight * pricePerGram;
  const totalManufacturing = weight * manufacturingCostPerGram;
  const totalPrice = totalGoldPrice + totalManufacturing;
  return { totalGoldPrice, totalManufacturing, totalPrice };
}

export function calculateFullGoldPrice(params: {
  weight: number;
  pricePerGram: number;
  manufacturingCost: number;
  stampDuty: number;
}) {
  const { weight, pricePerGram, manufacturingCost, stampDuty } = params;
  const goldValue = weight * pricePerGram;
  const totalPaid = goldValue + manufacturingCost + stampDuty;
  return { goldValue, totalPaid, manufacturingCost, stampDuty };
}

// ============================================================================
// Profit / Loss
// ============================================================================

export function calculateProfitLoss(params: {
  weight: number;
  purchasePricePerGram: number;
  currentPricePerGram: number;
  manufacturingCost: number;
  stampDuty: number;
}) {
  const { weight, purchasePricePerGram, currentPricePerGram, manufacturingCost, stampDuty } = params;
  const purchaseGoldValue = weight * purchasePricePerGram;
  const totalCost = purchaseGoldValue + manufacturingCost + stampDuty;
  const currentValue = weight * currentPricePerGram;
  const profitLoss = currentValue - totalCost;
  const profitLossPercent = totalCost > 0 ? (profitLoss / totalCost) * 100 : 0;
  return { totalCost, currentValue, profitLoss, profitLossPercent };
}

export function calculateCurrentValue(weight: number, currentPricePerGram: number): number {
  return weight * currentPricePerGram;
}

// ============================================================================
// Zakat calculator (2.5% of current gold value if above nisab)
// ============================================================================

export function calculateZakat(totalGoldWeight: number, currentPricePerGram: number): {
  goldValue: number;
  zakatAmount: number;
  isNisabMet: boolean;
  nisabThreshold: number;
} {
  const goldValue = totalGoldWeight * currentPricePerGram;
  // Nisab = 85 grams of gold
  const nisabThreshold = 85 * currentPricePerGram;
  const isNisabMet = goldValue >= nisabThreshold;
  const zakatAmount = isNisabMet ? goldValue * 0.025 : 0;
  return { goldValue, zakatAmount, isNisabMet, nisabThreshold };
}

// ============================================================================
// Portfolio totals
// ============================================================================

export function calculatePortfolioTotals(items: { weight: number; karat: GoldKarat; purchasePricePerGram: number }[], goldPrices: GoldPrices) {
  let totalWeight = 0;
  let totalCost = 0;
  let totalCurrentValue = 0;

  for (const item of items) {
    totalWeight += item.weight;
    totalCost += item.weight * item.purchasePricePerGram;
    totalCurrentValue += item.weight * (goldPrices[item.karat] ?? 0);
  }

  const profitLoss = totalCurrentValue - totalCost;
  const profitLossPercent = totalCost > 0 ? (profitLoss / totalCost) * 100 : 0;

  return { totalWeight, totalCost, totalCurrentValue, profitLoss, profitLossPercent };
}

// ============================================================================
// Hashtag totals
// ============================================================================

export function calculateHashtagTotals(entries: { amount: number; direction: 'debit' | 'credit' }[]) {
  const totalDebit = entries.filter(e => e.direction === 'debit').reduce((s, e) => s + e.amount, 0);
  const totalCredit = entries.filter(e => e.direction === 'credit').reduce((s, e) => s + e.amount, 0);
  const remaining = totalDebit - totalCredit;
  return { totalDebit, totalCredit, remaining };
}

// ============================================================================
// Formatting
// ============================================================================

export function formatCurrency(amount: number): string {
  return `${amount.toLocaleString('ar-EG', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} ج.م`;
}

export function formatWeight(weight: number): string {
  return `${weight} جم`;
}

export function formatDate(timestamp: number): string {
  return new Date(timestamp).toLocaleDateString('ar-EG', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export function formatShortDate(timestamp: number): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return date.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
  if (diffDays === 1) return 'أمس';
  if (diffDays < 7) return date.toLocaleDateString('ar-EG', { weekday: 'long' });
  return date.toLocaleDateString('ar-EG', { month: 'short', day: 'numeric' });
}

export function getStatusLabel(status: TransactionStatus): string {
  switch (status) {
    case 'purchased': return 'مشترى';
    case 'returned': return 'مُرجَع';
    case 'resold': return 'مُعاد بيعه';
  }
}

export function getStatusColor(status: TransactionStatus): string {
  switch (status) {
    case 'purchased': return '#22C55E';
    case 'returned': return '#EF4444';
    case 'resold': return '#3B82F6';
  }
}

export function getInitials(name: string): string {
  return name.split(' ').slice(0, 2).map(n => n[0]).join('');
}

export function formatShortTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
}

export function formatDaySeparator(timestamp: number): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diffDays = Math.floor((now.getTime() - date.getTime()) / 86_400_000);
  if (diffDays === 0) return 'اليوم';
  if (diffDays === 1) return 'أمس';
  if (diffDays < 7) return date.toLocaleDateString('ar-EG', { weekday: 'long' });
  return date.toLocaleDateString('ar-EG', { day: 'numeric', month: 'long', year: 'numeric' });
}

export function dayKey(timestamp: number): string {
  const d = new Date(timestamp);
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}
