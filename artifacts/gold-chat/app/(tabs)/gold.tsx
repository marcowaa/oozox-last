import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  Alert, FlatList, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { GoldSale } from '../../types';
import {
  formatCurrency, formatShortDate, KARAT_LABELS, ALL_KARATS,
  calculateZakat, calculatePortfolioTotals,
} from '../../utils/gold';

type GoldTab = 'overview' | 'prices' | 'portfolio' | 'trader' | 'sales';

export default function GoldScreen() {
  const insets = useSafeAreaInsets();
  const {
    user, goldPrices, goldSales, goldPurchases,
    submitTraderRequest, updateGoldPrices, confirmGoldSale,
  } = useApp();
  const [tab, setTab] = useState<GoldTab>('overview');
  const [showTraderForm, setShowTraderForm] = useState(false);
  const [traderForm, setTraderForm] = useState({ shopName: '', shopAddress: '', license: '' });
  const [editingPrices, setEditingPrices] = useState(false);
  const [localPrices, setLocalPrices] = useState(goldPrices);

  const isTrader = user?.type === 'gold_trader' && user?.traderStatus === 'approved';
  const traderPending = user?.traderStatus === 'pending';

  const portfolioItems = goldPurchases.filter(p => p.status === 'purchased');
  const portfolioTotals = useMemo(
    () => calculatePortfolioTotals(
      portfolioItems.map(p => ({ weight: p.extractedWeight, karat: p.karat, purchasePricePerGram: p.pricePerGram })),
      goldPrices,
    ),
    [portfolioItems, goldPrices],
  );

  const zakatCalc = calculateZakat(portfolioTotals.totalWeight, goldPrices['21k'] ?? 0);

  const completedSales = goldSales.filter(s => s.status === 'completed');
  const pendingSales = goldSales.filter(s => s.status === 'confirmed_by_buyer');
  const totalSalesAmount = completedSales.reduce((s, sale) => s + sale.totalAmount, 0);
  const totalSalesWeight = completedSales.reduce((s, sale) => s + sale.weight, 0);

  async function handleSubmitTraderRequest() {
    if (!traderForm.shopName.trim() || !traderForm.license.trim()) {
      Alert.alert('تنبيه', 'يرجى إدخال اسم المحل ورقم الترخيص');
      return;
    }
    await submitTraderRequest({
      shopName: traderForm.shopName.trim(),
      shopAddress: traderForm.shopAddress.trim(),
      license: traderForm.license.trim(),
    });
    setShowTraderForm(false);
    setTraderForm({ shopName: '', shopAddress: '', license: '' });
    Alert.alert('تم', 'تم إرسال طلبك للمراجعة — سيتم إشعارك عند التوثيق');
  }

  const tabs: { key: GoldTab; label: string; icon: string }[] = [
    { key: 'overview', label: 'نظرة عامة', icon: 'dashboard' },
    { key: 'prices', label: 'الأسعار', icon: 'trending-up' },
    { key: 'portfolio', label: 'محفظتي', icon: 'savings' },
    ...(isTrader ? [
      { key: 'trader' as GoldTab, label: 'لوحة التاجر', icon: 'store' },
      { key: 'sales' as GoldTab, label: 'المبيعات', icon: 'point-of-sale' },
    ] : []),
  ];

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <View style={{ width: 40 }} />
        <Text style={styles.headerTitle}>الذهب</Text>
        <Pressable style={styles.adminBtn} onPress={() => router.push('/admin')}>
          <MaterialIcons name="admin-panel-settings" size={22} color={Colors.accent} />
        </Pressable>
      </View>

      {/* Tabs */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabsScroll} contentContainerStyle={styles.tabsRow}>
        {tabs.map(t => (
          <Pressable key={t.key} style={[styles.tab, tab === t.key && styles.tabActive]} onPress={() => setTab(t.key)}>
            <MaterialIcons name={t.icon as keyof typeof MaterialIcons.glyphMap} size={16} color={tab === t.key ? '#fff' : Colors.textMuted} />
            <Text style={[styles.tabText, tab === t.key && styles.tabTextActive]}>{t.label}</Text>
          </Pressable>
        ))}
      </ScrollView>

      <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 40 }}>
        {/* ===== OVERVIEW ===== */}
        {tab === 'overview' && (
          <View style={styles.section}>
            {/* Gold prices card */}
            <View style={styles.pricesCard}>
              <Text style={styles.cardTitle}>أسعار الذهب الآن</Text>
              {ALL_KARATS.map(k => (
                <View key={k} style={styles.priceRow}>
                  <Text style={styles.priceKarat}>{KARAT_LABELS[k]}</Text>
                  <Text style={styles.priceValue}>{formatCurrency(goldPrices[k] ?? 0)}</Text>
                  <Text style={styles.priceUnit}>/ غرام</Text>
                </View>
              ))}
              <View style={styles.priceRow}>
                <Text style={styles.priceKarat}>الجنيه الذهب</Text>
                <Text style={styles.priceValue}>{formatCurrency(goldPrices.goldPound ?? 0)}</Text>
              </View>
              <View style={styles.priceRow}>
                <Text style={styles.priceKarat}>الأوقية (عالمي)</Text>
                <Text style={styles.priceValue}>${goldPrices.ounceUSD?.toLocaleString() ?? '0'}</Text>
              </View>
            </View>

            {/* Portfolio summary */}
            <Text style={styles.sectionTitle}>محفظتي الذهبية</Text>
            <View style={styles.statsGrid}>
              <StatCard label="الوزن الكلي" value={`${portfolioTotals.totalWeight.toFixed(1)}g`} icon="scale" color={Colors.accent} />
              <StatCard label="التكلفة الإجمالية" value={formatCurrency(portfolioTotals.totalCost)} icon="payments" />
              <StatCard label="القيمة الحالية" value={formatCurrency(portfolioTotals.totalCurrentValue)} icon="trending-up" color={Colors.profit} />
              <StatCard label="الربح/الخسارة" value={formatCurrency(portfolioTotals.profitLoss)} sub={`${portfolioTotals.profitLossPercent.toFixed(1)}%`} icon="analytics" color={portfolioTotals.profitLoss >= 0 ? Colors.profit : Colors.error} />
            </View>

            {/* Zakat */}
            <View style={styles.zakatCard}>
              <View style={styles.zakatHeader}>
                <MaterialIcons name="volunteer-activism" size={20} color={Colors.accent} />
                <Text style={styles.zakatTitle}>حاسبة الزكاة</Text>
              </View>
              <Text style={styles.zakatText}>قيمة ذهبك: {formatCurrency(zakatCalc.goldValue)}</Text>
              <Text style={styles.zakatText}>نصاب الزكاة (85 جم): {formatCurrency(zakatCalc.nisabThreshold)}</Text>
              {zakatCalc.isNisabMet ? (
                <View style={styles.zakatAmountRow}>
                  <Text style={styles.zakatLabel}>مقدار الزكاة (2.5%):</Text>
                  <Text style={styles.zakatAmount}>{formatCurrency(zakatCalc.zakatAmount)}</Text>
                </View>
              ) : (
                <Text style={styles.zakatNotMet}>لم يبلغ الذهب نصاب الزكاة</Text>
              )}
            </View>

            {/* Trader registration */}
            {!isTrader && !traderPending && (
              <Pressable style={styles.traderRegBtn} onPress={() => setShowTraderForm(true)}>
                <MaterialIcons name="store" size={24} color={Colors.accent} />
                <View style={styles.traderRegInfo}>
                  <Text style={styles.traderRegTitle}>سجّل كتاجر ذهب</Text>
                  <Text style={styles.traderRegSub}>توثيق المبيعات وإدارة المحل والجرد</Text>
                </View>
                <MaterialIcons name="chevron-left" size={20} color={Colors.textMuted} />
              </Pressable>
            )}

            {traderPending && (
              <View style={styles.pendingCard}>
                <MaterialIcons name="hourglass-top" size={20} color={Colors.warning} />
                <Text style={styles.pendingText}>طلب توثيق تاجر ذهب قيد المراجعة</Text>
              </View>
            )}

            {isTrader && (
              <Pressable style={styles.traderDashboardBtn} onPress={() => setTab('trader')}>
                <MaterialIcons name="store" size={24} color="#fff" />
                <Text style={styles.traderDashboardText}>لوحة تحكم التاجر</Text>
              </Pressable>
            )}
          </View>
        )}

        {/* ===== PRICES ===== */}
        {tab === 'prices' && (
          <View style={styles.section}>
            <View style={styles.pricesCard}>
              <View style={styles.cardHeaderRow}>
                <Text style={styles.cardTitle}>أسعار الذهب</Text>
                {user?.type === 'gold_trader' && (
                  <Pressable onPress={() => { setEditingPrices(!editingPrices); setLocalPrices(goldPrices); }}>
                    <Text style={styles.editLink}>{editingPrices ? 'إلغاء' : 'تعديل'}</Text>
                  </Pressable>
                )}
              </View>
              {ALL_KARATS.map(k => (
                <View key={k} style={styles.priceRow}>
                  <Text style={styles.priceKarat}>{KARAT_LABELS[k]}</Text>
                  {editingPrices ? (
                    <TextInput
                      style={styles.priceEditInput}
                      value={String(localPrices[k] ?? 0)}
                      onChangeText={v => setLocalPrices({ ...localPrices, [k]: Number(v) || 0 })}
                      keyboardType="decimal-pad"
                      textAlign="right"
                    />
                  ) : (
                    <Text style={styles.priceValue}>{formatCurrency(goldPrices[k] ?? 0)}</Text>
                  )}
                  <Text style={styles.priceUnit}>/ غرام</Text>
                </View>
              ))}
              {editingPrices && (
                <Pressable style={styles.savePricesBtn} onPress={async () => { await updateGoldPrices(localPrices); setEditingPrices(false); }}>
                  <Text style={styles.savePricesText}>حفظ الأسعار</Text>
                </Pressable>
              )}
            </View>
          </View>
        )}

        {/* ===== PORTFOLIO ===== */}
        {tab === 'portfolio' && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>مشترياتي ({portfolioItems.length})</Text>
            {portfolioItems.length === 0 ? (
              <View style={styles.empty}>
                <MaterialIcons name="savings" size={48} color={Colors.textMuted} />
                <Text style={styles.emptyText}>لا توجد مشتريات بعد</Text>
              </View>
            ) : (
              portfolioItems.map(p => (
                <View key={p.id} style={styles.portfolioCard}>
                  <View style={styles.portfolioHeader}>
                    <Text style={styles.portfolioKarat}>{KARAT_LABELS[p.karat]}</Text>
                    <Text style={styles.portfolioDate}>{formatShortDate(p.createdAt)}</Text>
                  </View>
                  <View style={styles.portfolioRow}>
                    <Text style={styles.portfolioItem}>الوزن: {p.extractedWeight}g</Text>
                    <Text style={styles.portfolioItem}>سعر الشراء: {formatCurrency(p.pricePerGram)}</Text>
                  </View>
                  <View style={styles.portfolioRow}>
                    <Text style={styles.portfolioItem}>الإجمالي: {formatCurrency(p.totalPaid)}</Text>
                    <Text style={[styles.portfolioItem, { color: p.profitLoss >= 0 ? Colors.profit : Colors.error }]}>
                      {p.profitLoss >= 0 ? '+' : ''}{formatCurrency(p.profitLoss)} ({p.profitLossPercent.toFixed(1)}%)
                    </Text>
                  </View>
                </View>
              ))
            )}
          </View>
        )}

        {/* ===== TRADER DASHBOARD ===== */}
        {tab === 'trader' && isTrader && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>لوحة تحكم التاجر</Text>
            <View style={styles.statsGrid}>
              <StatCard label="إجمالي الذهب" value={`${user?.traderTotalGoldWeight ?? 0}g`} icon="inventory" color={Colors.accent} />
              <StatCard label="مبيعات اليوم" value={String(completedSales.filter(s => new Date(s.createdAt).toDateString() === new Date().toDateString()).length)} icon="today" />
              <StatCard label="مبيعات معلّقة" value={String(pendingSales.length)} icon="pending" color={Colors.warning} />
              <StatCard label="إجمالي المبيعات" value={formatCurrency(totalSalesAmount)} icon="payments" color={Colors.profit} />
            </View>

            <Text style={styles.sectionTitle}>تنبيهات المبيعات</Text>
            {pendingSales.length === 0 ? (
              <Text style={styles.emptyText}>لا توجد مبيعات بانتظار التأكيد</Text>
            ) : (
              pendingSales.map(sale => (
                <View key={sale.id} style={styles.saleAlertCard}>
                  <View style={[styles.saleAlertBar, { backgroundColor: Colors.warning }]} />
                  <View style={styles.saleAlertBody}>
                    <Text style={styles.saleAlertTitle}>طلب شراء — {sale.buyerName}</Text>
                    <Text style={styles.saleAlertDetail}>{sale.weight}g • {KARAT_LABELS[sale.goldKarat]} • {formatCurrency(sale.totalAmount)}</Text>
                    <Text style={styles.saleAlertDate}>{formatShortDate(sale.createdAt)}</Text>
                    <View style={styles.saleAlertActions}>
                      <Pressable style={styles.confirmSaleBtn} onPress={() => confirmGoldSale(sale.id)}>
                        <Text style={styles.confirmSaleText}>تأكيد البيع ✓</Text>
                      </Pressable>
                    </View>
                  </View>
                </View>
              ))
            )}

            <Text style={styles.sectionTitle}>تقارير</Text>
            <Pressable style={styles.reportBtn} onPress={() => Alert.alert('تقرير يومي', `مبيعات اليوم: ${completedSales.filter(s => new Date(s.createdAt).toDateString() === new Date().toDateString()).length} عملية\nالوزن المباع: ${totalSalesWeight.toFixed(1)}g\nالإجمالي: ${formatCurrency(totalSalesAmount)}`)}>
              <MaterialIcons name="today" size={20} color={Colors.accent} />
              <Text style={styles.reportBtnText}>تقرير مبيعات اليوم</Text>
            </Pressable>
            <Pressable style={styles.reportBtn} onPress={() => Alert.alert('تقرير تفصيلي', `${completedSales.length} عملية بيع\nإجمالي الوزن: ${totalSalesWeight.toFixed(1)}g\nإجمالي المبلغ: ${formatCurrency(totalSalesAmount)}`)}>
              <MaterialIcons name="description" size={20} color={Colors.accent} />
              <Text style={styles.reportBtnText}>تقرير مفصّل بالمبيعات</Text>
            </Pressable>
            <Pressable style={styles.reportBtn} onPress={() => Alert.alert('الجرد', 'سيتم فتح الجرد — قريباً')}>
              <MaterialIcons name="inventory-2" size={20} color={Colors.accent} />
              <Text style={styles.reportBtnText}>جرد ربع سنوي / نصف سنوي / سنوي</Text>
            </Pressable>
          </View>
        )}

        {/* ===== SALES ===== */}
        {tab === 'sales' && isTrader && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>سجل المبيعات ({completedSales.length})</Text>
            {completedSales.length === 0 ? (
              <Text style={styles.emptyText}>لا توجد مبيعات مكتملة</Text>
            ) : (
              completedSales.map(sale => (
                <View key={sale.id} style={styles.saleCard}>
                  <View style={[styles.saleBar, { backgroundColor: Colors.profit }]} />
                  <View style={styles.saleBody}>
                    <View style={styles.saleTopRow}>
                      <Text style={styles.saleBuyer}>{sale.buyerName}</Text>
                      <Text style={styles.saleDate}>{formatShortDate(sale.createdAt)}</Text>
                    </View>
                    <View style={styles.saleChips}>
                      <View style={styles.saleChip}><Text style={styles.saleChipText}>{KARAT_LABELS[sale.goldKarat]}</Text></View>
                      <View style={styles.saleChip}><Text style={styles.saleChipText}>{sale.weight}g</Text></View>
                      <View style={styles.saleChip}><Text style={styles.saleChipText}>{sale.goldType}</Text></View>
                    </View>
                    <Text style={styles.saleAmount}>{formatCurrency(sale.totalAmount)}</Text>
                  </View>
                </View>
              ))
            )}
          </View>
        )}
      </ScrollView>

      {/* Trader registration modal */}
      <Modal visible={showTraderForm} animationType="slide" presentationStyle="pageSheet" onRequestClose={() => setShowTraderForm(false)}>
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <Pressable onPress={() => setShowTraderForm(false)}><MaterialIcons name="close" size={22} color={Colors.textPrimary} /></Pressable>
            <Text style={styles.modalTitle}>تسجيل تاجر ذهب</Text>
            <View style={{ width: 22 }} />
          </View>
          <ScrollView contentContainerStyle={{ padding: 20, gap: 14 }}>
            <Text style={styles.modalHint}>سيتم إرسال طلبك للإدارة للمراجعة والتوثيق</Text>
            <View>
              <Text style={styles.formLabel}>اسم المحل *</Text>
              <TextInput style={styles.formInput} value={traderForm.shopName} onChangeText={v => setTraderForm({ ...traderForm, shopName: v })} placeholder="اسم محل الذهب" placeholderTextColor={Colors.placeholder} textAlign="right" />
            </View>
            <View>
              <Text style={styles.formLabel}>عنوان المحل</Text>
              <TextInput style={styles.formInput} value={traderForm.shopAddress} onChangeText={v => setTraderForm({ ...traderForm, shopAddress: v })} placeholder="عنوان المحل" placeholderTextColor={Colors.placeholder} textAlign="right" />
            </View>
            <View>
              <Text style={styles.formLabel}>رقم الترخيص *</Text>
              <TextInput style={styles.formInput} value={traderForm.license} onChangeText={v => setTraderForm({ ...traderForm, license: v })} placeholder="رقم ترخيص تجارة الذهب" placeholderTextColor={Colors.placeholder} textAlign="right" />
            </View>
            <Pressable style={styles.submitBtn} onPress={handleSubmitTraderRequest}>
              <Text style={styles.submitBtnText}>إرسال طلب التوثيق</Text>
            </Pressable>
          </ScrollView>
        </View>
      </Modal>
    </View>
  );
}

// (placeholder removed — confirmGoldSale is now used directly from useApp)

function StatCard({ label, value, sub, icon, color }: { label: string; value: string; sub?: string; icon: string; color?: string }) {
  return (
    <View style={statStyles.card}>
      <View style={[statStyles.iconBox, { backgroundColor: (color ?? Colors.accent) + '18' }]}>
        <MaterialIcons name={icon as keyof typeof MaterialIcons.glyphMap} size={20} color={color ?? Colors.accent} />
      </View>
      <Text style={statStyles.value}>{value}</Text>
      <Text style={statStyles.label}>{label}</Text>
      {sub ? <Text style={[statStyles.sub, { color: color ?? Colors.textSecondary }]}>{sub}</Text> : null}
    </View>
  );
}

const statStyles = StyleSheet.create({
  card: { flex: 1, minWidth: '46%', backgroundColor: Colors.surface, borderRadius: 16, padding: 14, alignItems: 'center', shadowColor: Colors.primary, shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.07, shadowRadius: 4, elevation: 2 },
  iconBox: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center', marginBottom: 8 },
  value: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary, textAlign: 'center' },
  label: { fontSize: 11, color: Colors.textMuted, marginTop: 3, textAlign: 'center' },
  sub: { fontSize: 11, fontWeight: '600', marginTop: 2 },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  header: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', backgroundColor: Colors.primary, paddingHorizontal: 16, paddingVertical: 14 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },
  adminBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.1)', alignItems: 'center', justifyContent: 'center' },

  tabsScroll: { backgroundColor: Colors.surface, maxHeight: 50 },
  tabsRow: { flexDirection: 'row-reverse', paddingHorizontal: 8, paddingVertical: 6, gap: 4 },
  tab: { flexDirection: 'row-reverse', alignItems: 'center', gap: 4, paddingHorizontal: 12, paddingVertical: 8, borderRadius: 10, backgroundColor: Colors.background },
  tabActive: { backgroundColor: Colors.primary },
  tabText: { fontSize: 12, fontWeight: '600', color: Colors.textMuted },
  tabTextActive: { color: '#E6EDF3' },

  scroll: { flex: 1 },
  section: { padding: 16 },
  sectionTitle: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary, marginBottom: 10, textAlign: 'right' },

  pricesCard: { backgroundColor: Colors.surface, borderRadius: 16, padding: 14, marginBottom: 12 },
  cardTitle: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary, marginBottom: 10, textAlign: 'right' },
  cardHeaderRow: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 },
  editLink: { fontSize: 14, color: Colors.accent, fontWeight: '600' },
  priceRow: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 8, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: Colors.border },
  priceKarat: { fontSize: 14, color: Colors.textSecondary, fontWeight: '600' },
  priceValue: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  priceUnit: { fontSize: 11, color: Colors.textMuted },
  priceEditInput: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary, borderBottomWidth: 1, borderBottomColor: Colors.accent, minWidth: 100, paddingVertical: 2 },
  savePricesBtn: { backgroundColor: Colors.accent, borderRadius: 10, paddingVertical: 10, alignItems: 'center', marginTop: 8 },
  savePricesText: { color: '#0B0F14', fontSize: 14, fontWeight: '700' },

  statsGrid: { flexDirection: 'row-reverse', flexWrap: 'wrap', gap: 10, marginBottom: 12 },

  zakatCard: { backgroundColor: Colors.accentLight, borderRadius: 16, padding: 14, marginBottom: 12 },
  zakatHeader: { flexDirection: 'row-reverse', alignItems: 'center', gap: 8, marginBottom: 8 },
  zakatTitle: { fontSize: 15, fontWeight: '700', color: Colors.accentDark },
  zakatText: { fontSize: 13, color: Colors.textSecondary, marginTop: 4, textAlign: 'right' },
  zakatAmountRow: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', marginTop: 8, backgroundColor: Colors.surface, borderRadius: 10, padding: 10 },
  zakatLabel: { fontSize: 14, color: Colors.textPrimary, fontWeight: '600' },
  zakatAmount: { fontSize: 18, fontWeight: '700', color: Colors.accent },
  zakatNotMet: { fontSize: 12, color: Colors.textMuted, marginTop: 6, textAlign: 'right' },

  traderRegBtn: { flexDirection: 'row-reverse', alignItems: 'center', gap: 12, backgroundColor: Colors.surface, borderRadius: 14, padding: 14, marginBottom: 8, borderWidth: 1, borderColor: Colors.accentLight },
  traderRegInfo: { flex: 1 },
  traderRegTitle: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  traderRegSub: { fontSize: 12, color: Colors.textMuted, marginTop: 2 },

  pendingCard: { flexDirection: 'row-reverse', alignItems: 'center', gap: 8, backgroundColor: Colors.warningLight, borderRadius: 12, padding: 12 },
  pendingText: { fontSize: 13, color: Colors.warning, fontWeight: '600' },

  traderDashboardBtn: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'center', gap: 10, backgroundColor: Colors.accent, borderRadius: 14, paddingVertical: 16 },
  traderDashboardText: { fontSize: 16, fontWeight: '700', color: '#E6EDF3' },

  portfolioCard: { backgroundColor: Colors.surface, borderRadius: 14, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: Colors.border },
  portfolioHeader: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 },
  portfolioKarat: { fontSize: 15, fontWeight: '700', color: Colors.accent },
  portfolioDate: { fontSize: 11, color: Colors.textMuted },
  portfolioRow: { flexDirection: 'row-reverse', justifyContent: 'space-between', paddingVertical: 3 },
  portfolioItem: { fontSize: 13, color: Colors.textSecondary },

  saleAlertCard: { flexDirection: 'row-reverse', backgroundColor: Colors.surface, borderRadius: 12, overflow: 'hidden', marginBottom: 8 },
  saleAlertBar: { width: 4 },
  saleAlertBody: { flex: 1, padding: 12 },
  saleAlertTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, marginBottom: 4 },
  saleAlertDetail: { fontSize: 12, color: Colors.textSecondary },
  saleAlertDate: { fontSize: 11, color: Colors.textMuted, marginTop: 2 },
  saleAlertActions: { flexDirection: 'row-reverse', gap: 8, marginTop: 8 },
  confirmSaleBtn: { backgroundColor: Colors.profit, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6 },
  confirmSaleText: { fontSize: 12, color: '#0B0F14', fontWeight: '700' },

  reportBtn: { flexDirection: 'row-reverse', alignItems: 'center', gap: 10, backgroundColor: Colors.surface, borderRadius: 12, padding: 12, marginBottom: 6, borderWidth: 1, borderColor: Colors.border },
  reportBtnText: { fontSize: 14, fontWeight: '600', color: Colors.textPrimary },

  saleCard: { flexDirection: 'row-reverse', backgroundColor: Colors.surface, borderRadius: 12, overflow: 'hidden', marginBottom: 8 },
  saleBar: { width: 4 },
  saleBody: { flex: 1, padding: 12 },
  saleTopRow: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 },
  saleBuyer: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary },
  saleDate: { fontSize: 11, color: Colors.textMuted },
  saleChips: { flexDirection: 'row-reverse', gap: 6, marginBottom: 6 },
  saleChip: { backgroundColor: Colors.surfaceAlt, borderRadius: 8, paddingHorizontal: 8, paddingVertical: 3 },
  saleChipText: { fontSize: 11, color: Colors.textSecondary, fontWeight: '600' },
  saleAmount: { fontSize: 16, fontWeight: '700', color: Colors.accent },

  empty: { alignItems: 'center', paddingVertical: 40, gap: 12 },
  emptyText: { fontSize: 14, color: Colors.textMuted, textAlign: 'center', paddingVertical: 12 },

  modalContainer: { flex: 1, backgroundColor: Colors.surface },
  modalHeader: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: Colors.border },
  modalTitle: { fontSize: 17, fontWeight: '700', color: Colors.textPrimary },
  modalHint: { fontSize: 13, color: Colors.textMuted, marginBottom: 8, textAlign: 'right' },
  formLabel: { fontSize: 13, fontWeight: '600', color: Colors.textSecondary, marginBottom: 6, textAlign: 'right' },
  formInput: { backgroundColor: Colors.inputBg, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 15, color: Colors.inputText, borderWidth: 1, borderColor: Colors.inputBorder },
  submitBtn: { backgroundColor: Colors.accent, borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginTop: 8 },
  submitBtnText: { color: '#0B0F14', fontSize: 15, fontWeight: '700' },
});
