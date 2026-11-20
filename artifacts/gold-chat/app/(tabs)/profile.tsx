import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  Alert,
  Pressable,
  ScrollView,
  Share,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { GoldCalculatorSheet } from '@/components/GoldCalculatorSheet';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { formatCurrency } from '../../utils/gold';

const KARAT_LABELS: Record<string, string> = { k18: 'عيار 18', k21: 'عيار 21', k24: 'عيار 24' };

function StatCard({ label, value, sub, color, icon }: {
  label: string; value: string; sub?: string; color?: string;
  icon: React.ComponentProps<typeof MaterialIcons>['name'];
}) {
  return (
    <View style={statStyles.card}>
      <View style={[statStyles.iconBox, { backgroundColor: color ? `${color}18` : Colors.accentLight }]}>
        <MaterialIcons name={icon} size={20} color={color ?? Colors.accent} />
      </View>
      <Text style={[statStyles.value, color ? { color } : {}]}>{value}</Text>
      <Text style={statStyles.label}>{label}</Text>
      {sub ? <Text style={statStyles.sub}>{sub}</Text> : null}
    </View>
  );
}

const statStyles = StyleSheet.create({
  card: {
    flex: 1,
    minWidth: '46%',
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 14,
    alignItems: 'center',
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.07,
    shadowRadius: 4,
    elevation: 2,
  },
  iconBox: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center', marginBottom: 8 },
  value: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary, textAlign: 'center' },
  label: { fontSize: 11, color: Colors.textMuted, marginTop: 3, textAlign: 'center' },
  sub: { fontSize: 11, color: Colors.textSecondary, marginTop: 1, textAlign: 'center' },
});

export default function ProfileScreen() {
  const insets = useSafeAreaInsets();
  const { user, transactions, goldPrices, updateGoldPrices, logout, resetApp } = useApp();
  const isTrader = user?.type === 'gold_trader';
  const [editingPrices, setEditingPrices] = useState(false);
  const [localPrices, setLocalPrices] = useState(goldPrices);
  const [showCalc, setShowCalc] = useState(false);
  const [showTips, setShowTips] = useState(false);

  // Computed stats
  const stats = useMemo(() => {
    const active = transactions.filter(t => t.status !== 'returned');
    const totalSales = active.reduce((s, t) => s + t.totalPrice, 0);
    const totalWeight = transactions.reduce((s, t) => s + t.weight, 0);
    const totalProfit = transactions.reduce((s, t) => s + (t.profit ?? 0), 0);
    const profitMargin = totalSales > 0 ? (totalProfit / totalSales) * 100 : 0;

    const karatCounts: Record<string, number> = {};
    transactions.forEach(t => { karatCounts[t.goldKarat] = (karatCounts[t.goldKarat] ?? 0) + 1; });
    const bestKarat = Object.entries(karatCounts).sort((a, b) => b[1] - a[1])[0]?.[0] ?? '-';

    return {
      totalSales, totalWeight: totalWeight.toFixed(1), totalProfit,
      profitMargin: profitMargin.toFixed(1), bestKarat, count: transactions.length,
    };
  }, [transactions]);

  async function savePrices() {
    await updateGoldPrices(localPrices);
    setEditingPrices(false);
  }

  function handleLogout() {
    Alert.alert('تسجيل الخروج', 'هل تريد تسجيل الخروج؟', [
      { text: 'إلغاء', style: 'cancel' },
      { text: 'خروج', style: 'destructive', onPress: logout },
    ]);
  }

  function handleReset() {
    Alert.alert(
      '⚠️ إعادة تعيين التطبيق',
      'سيتم حذف جميع البيانات نهائياً بما فيها المحادثات والسجلات. هذا الإجراء لا يمكن التراجع عنه.',
      [
        { text: 'إلغاء', style: 'cancel' },
        {
          text: 'متأكد، احذف كل شيء',
          style: 'destructive',
          onPress: () =>
            Alert.alert('تأكيد أخير', 'هل أنت متأكد تماماً؟ لن تتمكن من استرجاع البيانات.', [
              { text: 'إلغاء', style: 'cancel' },
              { text: 'نعم، امسح الكل', style: 'destructive', onPress: resetApp },
            ]),
        },
      ]
    );
  }

  // Avatar
  const name = user?.name ?? '؟';
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  const avatarColor = Colors.avatarPalette[Math.abs(hash) % Colors.avatarPalette.length];
  const initials = name.split(' ').map((w: string) => w[0]).join('').slice(0, 2).toUpperCase();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <View style={[styles.avatar, { backgroundColor: avatarColor }]}>
          <Text style={styles.avatarText}>{initials}</Text>
          {isTrader && (
            <View style={styles.avatarBadge}>
              <MaterialIcons name="verified" size={14} color={Colors.accent} />
            </View>
          )}
        </View>
        <Text style={styles.userName}>{user?.name ?? ''}</Text>
        <View style={styles.roleBadge}>
          <MaterialIcons
            name={isTrader ? 'store' : 'person'}
            size={13}
            color={isTrader ? Colors.accent : Colors.textMuted}
          />
          <Text style={[styles.roleText, isTrader && styles.roleTextTrader]}>
            {isTrader ? 'تاجر موثق' : 'عميل'}
          </Text>
        </View>
        {user?.phone ? <Text style={styles.phone}>{user.phone}</Text> : null}
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Stats */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>الإحصائيات</Text>
          <View style={styles.statsGrid}>
            <StatCard label="المعاملات" value={String(stats.count)} icon="receipt-long" />
            <StatCard label="الوزن الكلي" value={`${stats.totalWeight}g`} icon="scale" />
            <StatCard label="إجمالي المبيعات" value={formatCurrency(stats.totalSales)} icon="payments" color={Colors.accent} />
            <StatCard label="أفضل عيار" value={stats.bestKarat} icon="star" />
            {isTrader && (
              <StatCard
                label="صافي الربح"
                value={formatCurrency(stats.totalProfit)}
                sub={`${stats.profitMargin}%`}
                icon="trending-up"
                color={parseFloat(stats.profitMargin) >= 0 ? Colors.profit : Colors.loss}
              />
            )}
          </View>
        </View>

        {/* Quick actions */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>إجراءات سريعة</Text>
          <View style={styles.actionsRow}>
            <Pressable style={styles.quickAction} onPress={() => router.push('/new-chat')}>
              <View style={[styles.quickActionIcon, { backgroundColor: Colors.accentLight }]}>
                <MaterialIcons name="add-comment" size={22} color={Colors.accent} />
              </View>
              <Text style={styles.quickActionText}>محادثة جديدة</Text>
            </Pressable>
            <Pressable style={styles.quickAction} onPress={() => setShowCalc(true)}>
              <View style={[styles.quickActionIcon, { backgroundColor: Colors.infoLight }]}>
                <MaterialIcons name="calculate" size={22} color={Colors.info} />
              </View>
              <Text style={styles.quickActionText}>حاسبة الذهب</Text>
            </Pressable>
            <Pressable style={styles.quickAction} onPress={() => Share.share({ message: 'تطبيق ذهبي - تداول الذهب بثقة وتوثيق' })}>
              <View style={[styles.quickActionIcon, { backgroundColor: Colors.successLight }]}>
                <MaterialIcons name="share" size={22} color={Colors.success} />
              </View>
              <Text style={styles.quickActionText}>مشاركة</Text>
            </Pressable>
          </View>
        </View>

        {/* Gold prices */}
        <View style={styles.section}>
          <View style={styles.sectionTitleRow}>
            {isTrader && !editingPrices && (
              <Pressable onPress={() => setEditingPrices(true)}>
                <Text style={styles.editLink}>تعديل</Text>
              </Pressable>
            )}
            {editingPrices && (
              <View style={styles.editActions}>
                <Pressable onPress={() => setEditingPrices(false)}>
                  <Text style={styles.cancelLink}>إلغاء</Text>
                </Pressable>
                <Pressable onPress={savePrices} style={styles.saveBtn}>
                  <Text style={styles.saveBtnText}>حفظ</Text>
                </Pressable>
              </View>
            )}
            <Text style={styles.sectionTitle}>أسعار الذهب</Text>
          </View>
          <View style={styles.pricesCard}>
            {Object.entries(localPrices).map(([k, v], i, arr) => (
              <View key={k} style={[styles.priceRow, i < arr.length - 1 && styles.priceRowBorder]}>
                <View style={styles.priceValueArea}>
                  {isTrader && editingPrices ? (
                    <TextInput
                      style={styles.priceInput}
                      value={String(v)}
                      onChangeText={val => setLocalPrices(prev => ({ ...prev, [k]: Number(val) || 0 }))}
                      keyboardType="numeric"
                      textAlign="right"
                    />
                  ) : (
                    <Text style={styles.priceValue}>{formatCurrency(v as number)}</Text>
                  )}
                  <Text style={styles.priceUnit}>/ غرام</Text>
                </View>
                <View style={styles.karatBadge}>
                  <Text style={styles.karatBadgeText}>{KARAT_LABELS[k] ?? k}</Text>
                </View>
              </View>
            ))}
            {!isTrader && (
              <View style={styles.readOnlyNote}>
                <MaterialIcons name="info-outline" size={13} color={Colors.textMuted} />
                <Text style={styles.readOnlyText}>أسعار مرجعية — التاجر يحدد السعر الفعلي</Text>
              </View>
            )}
          </View>
        </View>

        {/* Tips (collapsible) */}
        <View style={styles.section}>
          <Pressable style={styles.tipsHeader} onPress={() => setShowTips(v => !v)}>
            <MaterialIcons name={showTips ? 'expand-less' : 'expand-more'} size={20} color={Colors.textMuted} />
            <Text style={styles.sectionTitle}>نصائح واختصارات</Text>
          </Pressable>
          {showTips && (
            <View style={styles.tipsCard}>
              {[
                { icon: 'calculate' as const, text: 'اضغط على أيقونة الحاسبة في المحادثة لتوثيق صفقة' },
                { icon: 'touch-app' as const, text: 'اضغط طويلاً على أي رسالة لنسخها' },
                { icon: 'receipt-long' as const, text: 'راجع تبويب السجلات لتحليل أعمالك' },
                { icon: 'table-rows' as const, text: 'بدّل بين عرض البطاقات والجدول في السجلات' },
              ].map((tip, i) => (
                <View key={i} style={styles.tipRow}>
                  <MaterialIcons name={tip.icon} size={18} color={Colors.accent} />
                  <Text style={styles.tipText}>{tip.text}</Text>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* App settings */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>الإعدادات</Text>
          <View style={styles.settingsCard}>
            <Pressable style={styles.settingsRow} onPress={handleLogout}>
              <MaterialIcons name="logout" size={18} color={Colors.textSecondary} />
              <Text style={styles.settingsText}>تسجيل الخروج</Text>
            </Pressable>
            <View style={styles.settingsDivider} />
            <Pressable style={styles.settingsRow} onPress={handleReset}>
              <MaterialIcons name="delete-forever" size={18} color={Colors.error} />
              <Text style={[styles.settingsText, { color: Colors.error }]}>إعادة تعيين التطبيق</Text>
            </Pressable>
          </View>
          <Text style={styles.versionText}>ذهبي • الإصدار 1.0</Text>
        </View>
      </ScrollView>

      {/* Standalone calculator */}
      <GoldCalculatorSheet visible={showCalc} chatId="" onClose={() => setShowCalc(false)} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },

  header: {
    backgroundColor: Colors.primary,
    alignItems: 'center',
    paddingVertical: 28,
    paddingHorizontal: 24,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
    borderWidth: 3,
    borderColor: Colors.accent,
  },
  avatarText: { color: '#0B0F14', fontSize: 30, fontWeight: '700' },
  avatarBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  userName: { fontSize: 22, fontWeight: '700', color: Colors.textPrimary, marginBottom: 6 },
  roleBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: 'rgba(255,255,255,0.12)',
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 5,
    marginBottom: 6,
  },
  roleText: { fontSize: 13, color: 'rgba(11,15,20,0.7)', fontWeight: '500' },
  roleTextTrader: { color: Colors.accent, fontWeight: '700' },
  phone: { fontSize: 13, color: 'rgba(11,15,20,0.5)' },

  section: { paddingHorizontal: 16, paddingTop: 20 },
  sectionTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, textAlign: 'right', marginBottom: 10 },
  sectionTitleRow: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 },

  statsGrid: { flexDirection: 'row-reverse', flexWrap: 'wrap', gap: 10 },

  actionsRow: { flexDirection: 'row-reverse', gap: 10 },
  quickAction: { flex: 1, alignItems: 'center', gap: 8 },
  quickActionIcon: {
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quickActionText: { fontSize: 12, color: Colors.textSecondary, textAlign: 'center' },

  pricesCard: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.07,
    shadowRadius: 4,
    elevation: 2,
  },
  priceRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  priceRowBorder: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: Colors.border },
  priceValueArea: { flexDirection: 'row-reverse', alignItems: 'baseline', gap: 4 },
  priceValue: { fontSize: 17, fontWeight: '700', color: Colors.textPrimary },
  priceInput: {
    fontSize: 17, fontWeight: '700', color: Colors.textPrimary,
    borderBottomWidth: 1, borderBottomColor: Colors.accent, minWidth: 80,
    paddingVertical: 2,
  },
  priceUnit: { fontSize: 12, color: Colors.textMuted },
  karatBadge: {
    backgroundColor: Colors.accentLight,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 5,
  },
  karatBadgeText: { fontSize: 13, color: Colors.accentDark, fontWeight: '700' },
  readOnlyNote: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 6,
    padding: 12,
    backgroundColor: Colors.background,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
  },
  readOnlyText: { fontSize: 12, color: Colors.textMuted },

  editLink: { fontSize: 14, color: Colors.accent, fontWeight: '600' },
  editActions: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  cancelLink: { fontSize: 14, color: Colors.textMuted },
  saveBtn: { backgroundColor: Colors.accent, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 6 },
  saveBtnText: { color: '#0B0F14', fontSize: 13, fontWeight: '700' },

  tipsHeader: { flexDirection: 'row-reverse', alignItems: 'center', gap: 8, marginBottom: 8 },
  tipsCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 14,
    gap: 12,
  },
  tipRow: { flexDirection: 'row-reverse', alignItems: 'flex-start', gap: 10 },
  tipText: { flex: 1, fontSize: 13, color: Colors.textSecondary, lineHeight: 20, textAlign: 'right' },

  settingsCard: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.07,
    shadowRadius: 4,
    elevation: 2,
  },
  settingsRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  settingsText: { fontSize: 15, color: Colors.textPrimary },
  settingsDivider: { height: StyleSheet.hairlineWidth, backgroundColor: Colors.border, marginHorizontal: 16 },
  versionText: { fontSize: 12, color: Colors.textMuted, textAlign: 'center', marginTop: 16 },
});
