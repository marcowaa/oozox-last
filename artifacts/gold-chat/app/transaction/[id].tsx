import { MaterialIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import {
  Alert,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { formatCurrency, formatShortDate } from '../../utils/gold';

function statusLabel(s: string) {
  return s === 'purchased' ? 'تم الشراء' : s === 'returned' ? 'تم الإرجاع' : 'تم البيع';
}
function statusColor(s: string) {
  return s === 'purchased' ? Colors.purchased : s === 'returned' ? Colors.returned : Colors.resold;
}
function statusBg(s: string) {
  return s === 'purchased' ? Colors.purchasedBg : s === 'returned' ? Colors.returnedBg : Colors.resoldBg;
}

export default function TransactionDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const { user, transactions, addTransaction } = useApp();
  const [loading, setLoading] = useState(false);

  const tx = transactions.find(t => t.id === id);
  if (!tx) {
    return (
      <View style={[styles.center, { paddingTop: insets.top }]}>
        <MaterialIcons name="error-outline" size={48} color={Colors.textMuted} />
        <Text style={styles.notFound}>لم يتم العثور على المعاملة</Text>
        <Pressable style={styles.backBtn} onPress={() => router.back()}>
          <Text style={styles.backBtnText}>العودة</Text>
        </Pressable>
      </View>
    );
  }

  const isTrader = user?.type === 'gold_trader';
  const sc = statusColor(tx.status);
  const sb = statusBg(tx.status);

  async function handleReturn() {
    Alert.alert(
      'تأكيد الإرجاع',
      'هل تريد تسجيل إرجاع هذه المعاملة؟ لا يمكن التراجع عن هذا الإجراء.',
      [
        { text: 'إلغاء', style: 'cancel' },
        {
          text: 'تسجيل الإرجاع',
          style: 'destructive',
          onPress: async () => {
            setLoading(true);
            const { id: _oldId, ...txData } = tx!;
            await addTransaction({ ...txData, status: 'returned' });
            setLoading(false);
            router.back();
          },
        },
      ]
    );
  }

  async function handleResell() {
    Alert.alert(
      'تأكيد إعادة البيع',
      'هل تريد تسجيل إعادة بيع هذه المعاملة؟',
      [
        { text: 'إلغاء', style: 'cancel' },
        {
          text: 'تسجيل البيع',
          onPress: async () => {
            setLoading(true);
            const { id: _oldId, ...txData } = tx!;
            await addTransaction({ ...txData, status: 'resold' });
            setLoading(false);
            router.back();
          },
        },
      ]
    );
  }

  const profitMargin = tx.profit != null && tx.totalPrice > 0
    ? ((tx.profit / tx.totalPrice) * 100).toFixed(1)
    : null;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.headerBack}>
          <MaterialIcons name="arrow-forward-ios" size={20} color={Colors.textInverse} />
        </Pressable>
        <Text style={styles.headerTitle}>تفاصيل المعاملة</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        {/* Status card */}
        <View style={[styles.statusCard, { borderLeftColor: sc, backgroundColor: sb }]}>
          <View style={styles.statusLeft}>
            <Text style={[styles.statusMain, { color: sc }]}>{statusLabel(tx.status)}</Text>
            <Text style={styles.statusDate}>{formatShortDate(tx.timestamp)}</Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: sc }]}>
            <Text style={styles.statusBadgeText}>{tx.goldType} • {tx.goldKarat}</Text>
          </View>
        </View>

        {/* Photo */}
        {tx.photoUri ? (
          <View style={styles.photoContainer}>
            <Image source={{ uri: tx.photoUri }} style={styles.photo} resizeMode="cover" />
            <View style={styles.photoOverlay}>
              <MaterialIcons name="camera-alt" size={16} color="#fff" />
              <Text style={styles.photoLabel}>صورة البضاعة</Text>
            </View>
          </View>
        ) : null}

        {/* Receipt breakdown */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>تفصيل الفاتورة</Text>
          <View style={styles.receiptCard}>
            <ReceiptRow label="نوع الذهب" value={tx.goldType} />
            <ReceiptRow label="العيار" value={tx.goldKarat} />
            <ReceiptRow label="الوزن" value={`${tx.weight} غرام`} />
            <ReceiptRow label="سعر الغرام" value={formatCurrency(tx.pricePerGram)} />
            {tx.totalManufacturing && tx.totalManufacturing > 0 ? (
              <ReceiptRow label="أجرة الصنعة" value={formatCurrency(tx.totalManufacturing)} />
            ) : null}
            <View style={styles.receiptDivider} />
            <View style={styles.receiptTotal}>
              <Text style={styles.receiptTotalLabel}>الإجمالي</Text>
              <Text style={styles.receiptTotalValue}>{formatCurrency(tx.totalPrice)}</Text>
            </View>
          </View>
        </View>

        {/* Profit analysis (trader only) */}
        {isTrader && tx.profit != null && (
          <View style={styles.section}>
            <View style={styles.sectionTitleRow}>
              <MaterialIcons name="lock" size={14} color={Colors.profit} />
              <Text style={[styles.sectionTitle, { color: Colors.profit }]}>تحليل الربح (خاص)</Text>
            </View>
            <View style={[styles.receiptCard, styles.profitCard]}>
              {tx.traderCostPerGram != null && (
                <ReceiptRow
                  label="تكلفة الغرام"
                  value={formatCurrency(tx.traderCostPerGram)}
                  valueColor={Colors.textSecondary}
                />
              )}
              <ReceiptRow
                label="صافي الربح"
                value={formatCurrency(tx.profit)}
                valueColor={tx.profit >= 0 ? Colors.profit : Colors.loss}
              />
              {profitMargin && (
                <ReceiptRow
                  label="هامش الربح"
                  value={`${profitMargin}%`}
                  valueColor={tx.profit >= 0 ? Colors.profit : Colors.loss}
                />
              )}
            </View>
          </View>
        )}

        {/* Immutability notice */}
        <View style={styles.immutableNotice}>
          <MaterialIcons name="verified-user" size={16} color={Colors.info} />
          <Text style={styles.immutableText}>
            هذا السجل محفوظ بشكل دائم ولا يمكن تعديله أو حذفه
          </Text>
        </View>
      </ScrollView>

      {/* Action buttons */}
      {tx.status === 'purchased' && (
        <View style={[styles.actions, { paddingBottom: Math.max(insets.bottom, 16) }]}>
          <Pressable
            style={[styles.actionBtn, styles.returnBtn]}
            onPress={handleReturn}
            disabled={loading}
          >
            <MaterialIcons name="undo" size={18} color={Colors.returned} />
            <Text style={styles.returnBtnText}>تسجيل إرجاع</Text>
          </Pressable>
          {isTrader && (
            <Pressable
              style={[styles.actionBtn, styles.resellBtn]}
              onPress={handleResell}
              disabled={loading}
            >
              <MaterialIcons name="sell" size={18} color="#fff" />
              <Text style={styles.resellBtnText}>إعادة بيع</Text>
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

function ReceiptRow({ label, value, valueColor }: { label: string; value: string; valueColor?: string }) {
  return (
    <View style={styles.receiptRow}>
      <Text style={[styles.receiptValue, valueColor ? { color: valueColor } : {}]}>{value}</Text>
      <Text style={styles.receiptLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  notFound: { fontSize: 18, color: Colors.textSecondary, marginTop: 12 },
  backBtn: {
    marginTop: 16,
    backgroundColor: Colors.accent,
    borderRadius: 20,
    paddingHorizontal: 24,
    paddingVertical: 10,
  },
  backBtnText: { color: '#fff', fontWeight: '700' },

  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.primary,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  headerBack: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },

  scrollContent: { padding: 16, paddingBottom: 120 },

  statusCard: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderRadius: 14,
    borderLeftWidth: 4,
    padding: 14,
    marginBottom: 12,
  },
  statusLeft: { alignItems: 'flex-end' },
  statusMain: { fontSize: 18, fontWeight: '700' },
  statusDate: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  statusBadge: {
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  statusBadgeText: { color: '#fff', fontSize: 13, fontWeight: '700' },

  photoContainer: {
    borderRadius: 14,
    overflow: 'hidden',
    marginBottom: 12,
  },
  photo: { width: '100%', height: 220 },
  photoOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(0,0,0,0.4)',
    padding: 10,
  },
  photoLabel: { color: '#fff', fontSize: 13 },

  section: { marginBottom: 12 },
  sectionTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, marginBottom: 8, textAlign: 'right' },
  sectionTitleRow: { flexDirection: 'row-reverse', alignItems: 'center', gap: 6, marginBottom: 8 },

  receiptCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 14,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  profitCard: { borderWidth: 1, borderColor: Colors.successLight },
  receiptRow: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  receiptLabel: { fontSize: 14, color: Colors.textSecondary },
  receiptValue: { fontSize: 14, fontWeight: '600', color: Colors.textPrimary },
  receiptDivider: { height: 1, backgroundColor: Colors.border, marginVertical: 6 },
  receiptTotal: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    paddingTop: 4,
  },
  receiptTotalLabel: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  receiptTotalValue: { fontSize: 22, fontWeight: '700', color: Colors.accent },

  immutableNotice: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 8,
    backgroundColor: Colors.infoLight,
    borderRadius: 12,
    padding: 12,
    marginTop: 4,
  },
  immutableText: { fontSize: 12, color: Colors.info, flex: 1, textAlign: 'right', lineHeight: 18 },

  actions: {
    flexDirection: 'row-reverse',
    gap: 10,
    padding: 16,
    backgroundColor: Colors.surface,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
  },
  actionBtn: {
    flex: 1,
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    borderRadius: 14,
    paddingVertical: 14,
  },
  returnBtn: { backgroundColor: Colors.returnedBg, borderWidth: 1, borderColor: Colors.returned },
  returnBtnText: { fontSize: 15, fontWeight: '700', color: Colors.returned },
  resellBtn: { backgroundColor: Colors.resold },
  resellBtnText: { fontSize: 15, fontWeight: '700', color: '#fff' },
});
