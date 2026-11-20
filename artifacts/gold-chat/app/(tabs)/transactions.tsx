import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  FlatList,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { Transaction } from '../../types';
import { formatCurrency, formatShortDate } from '../../utils/gold';

type Period = 'today' | 'week' | 'month' | 'all';
type FilterStatus = 'all' | 'purchased' | 'returned' | 'resold';
type ViewMode = 'cards' | 'table';

const PERIODS: { key: Period; label: string }[] = [
  { key: 'today', label: 'اليوم' },
  { key: 'week', label: 'الأسبوع' },
  { key: 'month', label: 'الشهر' },
  { key: 'all', label: 'الكل' },
];

const STATUS_FILTERS: { key: FilterStatus; label: string }[] = [
  { key: 'all', label: 'الكل' },
  { key: 'purchased', label: 'شراء' },
  { key: 'returned', label: 'إرجاع' },
  { key: 'resold', label: 'بيع' },
];

function filterByPeriod(items: Transaction[], period: Period): Transaction[] {
  if (period === 'all') return items;
  const now = new Date();
  return items.filter(t => {
    const d = new Date(t.timestamp);
    if (period === 'today') {
      return d.toDateString() === now.toDateString();
    } else if (period === 'week') {
      const diff = (now.getTime() - d.getTime()) / 86400000;
      return diff <= 7;
    } else {
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }
  });
}

function computeAnalytics(items: Transaction[], isTrader: boolean) {
  const active = items.filter(t => t.status !== 'returned');
  const totalSales = active.reduce((s, t) => s + t.totalPrice, 0);
  const totalWeight = active.reduce((s, t) => s + t.weight, 0);
  const totalProfit = isTrader ? items.reduce((s, t) => s + (t.profit ?? 0), 0) : 0;
  const avgPricePerGram = totalWeight > 0 ? totalSales / totalWeight : 0;
  const profitMargin = totalSales > 0 ? (totalProfit / totalSales) * 100 : 0;

  const karatCounts: Record<string, number> = {};
  items.forEach(t => { karatCounts[t.goldKarat] = (karatCounts[t.goldKarat] ?? 0) + 1; });
  const bestKarat = Object.entries(karatCounts).sort((a, b) => b[1] - a[1])[0]?.[0] ?? '-';

  return { totalSales, totalWeight, totalProfit, avgPricePerGram, profitMargin, bestKarat, count: items.length };
}

function statusLabel(s: string) {
  return s === 'purchased' ? 'شراء' : s === 'returned' ? 'إرجاع' : 'بيع';
}
function statusColor(s: string) {
  return s === 'purchased' ? Colors.purchased : s === 'returned' ? Colors.returned : Colors.resold;
}
function statusBg(s: string) {
  return s === 'purchased' ? Colors.purchasedBg : s === 'returned' ? Colors.returnedBg : Colors.resoldBg;
}

function AnalyticsCard({ label, value, sub, color }: { label: string; value: string; sub?: string; color?: string }) {
  return (
    <View style={analyticsStyles.card}>
      <Text style={analyticsStyles.label}>{label}</Text>
      <Text style={[analyticsStyles.value, color ? { color } : {}]}>{value}</Text>
      {sub ? <Text style={analyticsStyles.sub}>{sub}</Text> : null}
    </View>
  );
}

const analyticsStyles = StyleSheet.create({
  card: {
    width: 140,
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 14,
    marginRight: 10,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  label: { fontSize: 11, color: Colors.textMuted, marginBottom: 6, textAlign: 'right' },
  value: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary, textAlign: 'right' },
  sub: { fontSize: 11, color: Colors.textSecondary, marginTop: 2, textAlign: 'right' },
});

export default function TransactionsScreen() {
  const insets = useSafeAreaInsets();
  const { user, transactions } = useApp();
  const isTrader = user?.type === 'gold_trader';
  const [period, setPeriod] = useState<Period>('all');
  const [statusFilter, setStatusFilter] = useState<FilterStatus>('all');
  const [viewMode, setViewMode] = useState<ViewMode>('cards');

  const periodFiltered = useMemo(() => filterByPeriod(transactions, period), [transactions, period]);
  const analytics = useMemo(() => computeAnalytics(periodFiltered, isTrader), [periodFiltered, isTrader]);
  const displayList = useMemo(() => {
    const f = statusFilter === 'all' ? periodFiltered : periodFiltered.filter(t => t.status === statusFilter);
    return [...f].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  }, [periodFiltered, statusFilter]);

  function renderTableRow({ item, index }: { item: Transaction; index: number }) {
    return (
      <Pressable
        style={[tableStyles.row, index % 2 === 0 ? tableStyles.rowEven : tableStyles.rowOdd]}
        onPress={() => router.push(`/transaction/${item.id}`)}
      >
        <View style={[tableStyles.statusDot, { backgroundColor: statusColor(item.status) }]} />
        <Text style={tableStyles.cellStatus}>{statusLabel(item.status)}</Text>
        <Text style={tableStyles.cellAmount}>{formatCurrency(item.totalPrice)}</Text>
        <Text style={tableStyles.cellWeight}>{item.weight}g</Text>
        <Text style={tableStyles.cellKarat}>{item.goldKarat}</Text>
        <Text style={tableStyles.cellDate}>{formatShortDate(item.timestamp)}</Text>
      </Pressable>
    );
  }

  function renderCard({ item }: { item: Transaction }) {
    return (
      <Pressable style={cardStyles.card} onPress={() => router.push(`/transaction/${item.id}`)}>
        {/* Left accent */}
        <View style={[cardStyles.accentBar, { backgroundColor: statusColor(item.status) }]} />
        <View style={cardStyles.body}>
          {/* Top row */}
          <View style={cardStyles.topRow}>
            <Text style={cardStyles.date}>{formatShortDate(item.timestamp)}</Text>
            <View style={[cardStyles.statusBadge, { backgroundColor: statusBg(item.status) }]}>
              <Text style={[cardStyles.statusText, { color: statusColor(item.status) }]}>
                {statusLabel(item.status)}
              </Text>
            </View>
          </View>
          {/* Main row */}
          <View style={cardStyles.mainRow}>
            <MaterialIcons name="chevron-left" size={18} color={Colors.textMuted} />
            <Text style={cardStyles.total}>{formatCurrency(item.totalPrice)}</Text>
            <View style={cardStyles.flex1} />
            <View style={cardStyles.metaChips}>
              <View style={cardStyles.chip}>
                <Text style={cardStyles.chipText}>{item.goldKarat}</Text>
              </View>
              <View style={cardStyles.chip}>
                <Text style={cardStyles.chipText}>{item.weight}g</Text>
              </View>
              <View style={cardStyles.chip}>
                <Text style={cardStyles.chipText}>{item.goldType}</Text>
              </View>
            </View>
          </View>
          {/* Profit row (trader only) */}
          {isTrader && item.profit != null && (
            <View style={cardStyles.profitRow}>
              <MaterialIcons name="lock" size={12} color={Colors.profit} />
              <Text style={cardStyles.profitText}>
                ربح: {formatCurrency(item.profit)}
              </Text>
            </View>
          )}
        </View>
      </Pressable>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable
          style={[styles.viewToggle, viewMode === 'table' && styles.viewToggleActive]}
          onPress={() => setViewMode(v => v === 'cards' ? 'table' : 'cards')}
        >
          <MaterialIcons
            name={viewMode === 'cards' ? 'table-rows' : 'view-stream'}
            size={20}
            color={Colors.accent}
          />
        </Pressable>
        <Text style={styles.headerTitle}>سجل المعاملات</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Period selector */}
      <View style={styles.periodRow}>
        {PERIODS.map(p => (
          <Pressable
            key={p.key}
            style={[styles.periodBtn, period === p.key && styles.periodBtnActive]}
            onPress={() => setPeriod(p.key)}
          >
            <Text style={[styles.periodText, period === p.key && styles.periodTextActive]}>
              {p.label}
            </Text>
          </Pressable>
        ))}
      </View>

      <FlatList
        data={displayList}
        keyExtractor={i => i.id}
        renderItem={viewMode === 'cards' ? renderCard : renderTableRow}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: 24 }}
        ListHeaderComponent={
          <>
            {/* Analytics cards */}
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.analyticsScroll}
              style={styles.analyticsRow}
            >
              <AnalyticsCard
                label="إجمالي المبيعات"
                value={formatCurrency(analytics.totalSales)}
                color={Colors.accent}
              />
              <AnalyticsCard
                label="الوزن الكلي"
                value={`${analytics.totalWeight.toFixed(1)}g`}
              />
              {isTrader && (
                <AnalyticsCard
                  label="صافي الربح"
                  value={formatCurrency(analytics.totalProfit)}
                  sub={`${analytics.profitMargin.toFixed(1)}%`}
                  color={analytics.totalProfit >= 0 ? Colors.profit : Colors.loss}
                />
              )}
              <AnalyticsCard
                label="عدد المعاملات"
                value={String(analytics.count)}
              />
              <AnalyticsCard
                label="متوسط الغرام"
                value={formatCurrency(analytics.avgPricePerGram)}
              />
              <AnalyticsCard
                label="أفضل عيار"
                value={analytics.bestKarat}
              />
            </ScrollView>

            {/* Status filter tabs */}
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.filterScroll}
              style={styles.filterRow}
            >
              {STATUS_FILTERS.map(f => {
                const count = f.key === 'all'
                  ? periodFiltered.length
                  : periodFiltered.filter(t => t.status === f.key).length;
                return (
                  <Pressable
                    key={f.key}
                    style={[styles.filterBtn, statusFilter === f.key && styles.filterBtnActive]}
                    onPress={() => setStatusFilter(f.key)}
                  >
                    <Text style={[styles.filterText, statusFilter === f.key && styles.filterTextActive]}>
                      {f.label}
                    </Text>
                    {count > 0 && (
                      <View style={[styles.filterBadge, statusFilter === f.key && styles.filterBadgeActive]}>
                        <Text style={[styles.filterBadgeText, statusFilter === f.key && styles.filterBadgeTextActive]}>
                          {count}
                        </Text>
                      </View>
                    )}
                  </Pressable>
                );
              })}
            </ScrollView>

            {/* Table header (table mode only) */}
            {viewMode === 'table' && (
              <View style={tableStyles.header}>
                <Text style={tableStyles.headerCell}>الحالة</Text>
                <Text style={tableStyles.headerCell}>المبلغ</Text>
                <Text style={tableStyles.headerCell}>الوزن</Text>
                <Text style={tableStyles.headerCell}>العيار</Text>
                <Text style={tableStyles.headerCell}>التاريخ</Text>
              </View>
            )}
          </>
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <MaterialIcons name="receipt-long" size={56} color={Colors.accent} />
            <Text style={styles.emptyTitle}>لا توجد معاملات</Text>
            <Text style={styles.emptySubtitle}>وثّق صفقاتك عبر الحاسبة في المحادثة</Text>
          </View>
        }
      />
    </View>
  );
}

const cardStyles = StyleSheet.create({
  card: {
    flexDirection: 'row-reverse',
    backgroundColor: Colors.surface,
    marginHorizontal: 16,
    marginVertical: 5,
    borderRadius: 14,
    overflow: 'hidden',
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.07,
    shadowRadius: 4,
    elevation: 2,
  },
  accentBar: { width: 4 },
  body: { flex: 1, padding: 12 },
  topRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  date: { fontSize: 12, color: Colors.textMuted },
  statusBadge: {
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  statusText: { fontSize: 12, fontWeight: '600' },
  mainRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    marginBottom: 6,
  },
  total: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },
  flex1: { flex: 1 },
  metaChips: { flexDirection: 'row-reverse', gap: 6 },
  chip: {
    backgroundColor: Colors.surfaceAlt,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  chipText: { fontSize: 11, color: Colors.textSecondary, fontWeight: '600' },
  profitRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    backgroundColor: Colors.profitBg,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    alignSelf: 'flex-end',
  },
  profitText: { fontSize: 12, color: Colors.profit, fontWeight: '600' },
});

const tableStyles = StyleSheet.create({
  header: {
    flexDirection: 'row-reverse',
    backgroundColor: Colors.primary,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  headerCell: {
    flex: 1,
    color: Colors.accent,
    fontSize: 11,
    fontWeight: '600',
    textAlign: 'center',
  },
  row: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.divider,
  },
  rowEven: { backgroundColor: Colors.surface },
  rowOdd: { backgroundColor: Colors.background },
  statusDot: { width: 8, height: 8, borderRadius: 4, marginLeft: 4 },
  cellStatus: { flex: 1, fontSize: 12, color: Colors.textSecondary, textAlign: 'center' },
  cellAmount: { flex: 1.5, fontSize: 13, fontWeight: '700', color: Colors.textPrimary, textAlign: 'center' },
  cellWeight: { flex: 0.8, fontSize: 12, color: Colors.textSecondary, textAlign: 'center' },
  cellKarat: { flex: 0.8, fontSize: 12, color: Colors.textSecondary, textAlign: 'center' },
  cellDate: { flex: 1, fontSize: 11, color: Colors.textMuted, textAlign: 'center' },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.primary,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  headerTitle: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },
  viewToggle: {
    padding: 8,
    borderRadius: 8,
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  viewToggleActive: { backgroundColor: 'rgba(201,168,76,0.25)' },

  periodRow: {
    flexDirection: 'row-reverse',
    backgroundColor: Colors.primary,
    paddingHorizontal: 16,
    paddingBottom: 12,
    gap: 8,
  },
  periodBtn: {
    paddingHorizontal: 16,
    paddingVertical: 6,
    borderRadius: 20,
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  periodBtnActive: { backgroundColor: Colors.accent },
  periodText: { fontSize: 13, color: 'rgba(11,15,20,0.7)', fontWeight: '500' },
  periodTextActive: { color: '#0B0F14', fontWeight: '700' },

  analyticsRow: { backgroundColor: Colors.background },
  analyticsScroll: { paddingHorizontal: 16, paddingVertical: 14, gap: 10 },

  filterRow: { backgroundColor: Colors.surface },
  filterScroll: { paddingHorizontal: 16, paddingVertical: 10, gap: 8 },
  filterBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 16,
    paddingVertical: 7,
    borderRadius: 20,
    backgroundColor: Colors.surfaceAlt,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  filterBtnActive: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  filterText: { fontSize: 13, color: Colors.textSecondary, fontWeight: '500' },
  filterTextActive: { color: '#0B0F14', fontWeight: '700' },
  filterBadge: {
    backgroundColor: Colors.border,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  filterBadgeActive: { backgroundColor: Colors.accent },
  filterBadgeText: { fontSize: 10, color: Colors.textSecondary, fontWeight: '700' },
  filterBadgeTextActive: { color: '#E6EDF3' },

  empty: { alignItems: 'center', paddingVertical: 60, gap: 12 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },
  emptySubtitle: { fontSize: 14, color: Colors.textSecondary, textAlign: 'center' },
});
