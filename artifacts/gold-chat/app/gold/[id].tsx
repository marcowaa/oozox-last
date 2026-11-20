import { MaterialIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import {
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
import { formatCurrency, formatDate, KARAT_LABELS } from '../../utils/gold';

export default function GoldDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const { goldPurchases } = useApp();

  const purchase = goldPurchases.find(p => p.id === id);

  if (!purchase) {
    return (
      <View style={[styles.center, { paddingTop: insets.top }]}>
        <MaterialIcons name="error-outline" size={48} color={Colors.textMuted} />
        <Text style={styles.notFound}>لم يتم العثور على العملية</Text>
        <Pressable style={styles.backBtn} onPress={() => router.back()}>
          <Text style={styles.backBtnText}>العودة</Text>
        </Pressable>
      </View>
    );
  }

  const isProfit = purchase.profitLoss >= 0;
  const profitMargin = (purchase.profitLoss / purchase.totalPaid * 100).toFixed(1);

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.headerBack}>
          <MaterialIcons name="arrow-forward-ios" size={20} color={Colors.textInverse} />
        </Pressable>
        <Text style={styles.headerTitle}>تفاصيل الشراء</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Photo */}
        {purchase.photoUri && (
          <View style={styles.photoContainer}>
            <Image source={{ uri: purchase.photoUri }} style={styles.photo} resizeMode="cover" />
            <View style={styles.photoOverlay}>
              <MaterialIcons name="camera-alt" size={16} color="#fff" />
              <Text style={styles.photoLabel}>صورة الميزان والقطعة</Text>
            </View>
          </View>
        )}

        {/* Status card */}
        <View style={[
          styles.statusCard,
          {
            borderLeftColor: isProfit ? Colors.profit : Colors.error,
            backgroundColor: isProfit ? Colors.profitBg : Colors.lossBg,
          },
        ]}>
          <View style={styles.statusLeft}>
            <Text style={[styles.statusMain, { color: isProfit ? Colors.profit : Colors.error }]}>
              {isProfit ? 'ربح' : 'خسارة'}
            </Text>
            <Text style={styles.statusDate}>{formatDate(purchase.createdAt)}</Text>
          </View>
          <View style={[
            styles.statusBadge,
            { backgroundColor: isProfit ? Colors.profit : Colors.error },
          ]}>
            <Text style={styles.statusBadgeText}>
              {KARAT_LABELS[purchase.karat]} • {purchase.extractedWeight}g
            </Text>
          </View>
        </View>

        {/* Receipt breakdown */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>تفصيل الفاتورة</Text>
          <View style={styles.receiptCard}>
            <ReceiptRow label="العيار" value={KARAT_LABELS[purchase.karat]} />
            <ReceiptRow label="الوزن" value={`${purchase.extractedWeight} غرام`} />
            <ReceiptRow label="سعر الغرام وقت الشراء" value={formatCurrency(purchase.pricePerGram)} />
            <ReceiptRow label="السعر الحالي للغرام" value={formatCurrency(purchase.currentPricePerGram)} />
            <ReceiptRow label="الشركة المصنّعة" value={purchase.manufacturer} />
            <ReceiptRow label="أجرة الصنعة" value={formatCurrency(purchase.manufacturingCost)} />
            <ReceiptRow label="الدمغة" value={formatCurrency(purchase.stampDuty)} />
            <View style={styles.receiptDivider} />
            <View style={styles.receiptTotal}>
              <Text style={styles.receiptTotalLabel}>الإجمالي المدفوع</Text>
              <Text style={styles.receiptTotalValue}>{formatCurrency(purchase.totalPaid)}</Text>
            </View>
          </View>
        </View>

        {/* Profit/Loss analysis */}
        <View style={styles.section}>
          <View style={styles.sectionTitleRow}>
            <MaterialIcons name="trending-up" size={16} color={isProfit ? Colors.profit : Colors.error} />
            <Text style={[styles.sectionTitle, { color: isProfit ? Colors.profit : Colors.error }]}>
              تحليل الربح/الخسارة
            </Text>
          </View>
          <View style={[styles.receiptCard, { borderWidth: 1, borderColor: isProfit ? Colors.profit : Colors.error }]}>
            <ReceiptRow
              label="القيمة الحالية"
              value={formatCurrency(purchase.currentValue)}
              valueColor={Colors.textPrimary}
            />
            <ReceiptRow
              label="صافي الربح/الخسارة"
              value={formatCurrency(purchase.profitLoss)}
              valueColor={isProfit ? Colors.profit : Colors.error}
            />
            <ReceiptRow
              label="نسبة الربح/الخسارة"
              value={`${profitMargin}%`}
              valueColor={isProfit ? Colors.profit : Colors.error}
            />
          </View>
        </View>

        {/* Trader */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>التاجر</Text>
          <View style={styles.traderCard}>
            <View style={styles.traderInfo}>
              <MaterialIcons name="tag" size={16} color={Colors.accent} />
              <Text style={styles.traderName}>{purchase.traderHashtag}</Text>
            </View>
            {purchase.traderConfirmed ? (
              <View style={styles.confirmedBadge}>
                <MaterialIcons name="check-circle" size={14} color={Colors.profit} />
                <Text style={styles.confirmedText}>مؤكد</Text>
              </View>
            ) : (
              <View style={styles.pendingBadge}>
                <MaterialIcons name="access-time" size={14} color={Colors.warning} />
                <Text style={styles.pendingText}>بانتظار التأكيد</Text>
              </View>
            )}
          </View>
        </View>

        {/* Immutable notice */}
        <View style={styles.immutableNotice}>
          <MaterialIcons name="verified-user" size={16} color={Colors.info} />
          <Text style={styles.immutableText}>
            هذا السجل محفوظ بشكل دائم ولا يمكن تعديله أو حذفه
          </Text>
        </View>
      </ScrollView>
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
    marginTop: 16, backgroundColor: Colors.accent, borderRadius: 20,
    paddingHorizontal: 24, paddingVertical: 10,
  },
  backBtnText: { color: '#fff', fontWeight: '700' },

  header: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.primary,
    paddingHorizontal: 16, paddingVertical: 14,
  },
  headerBack: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },

  photoContainer: { margin: 16, borderRadius: 14, overflow: 'hidden' },
  photo: { width: '100%', height: 220 },
  photoOverlay: {
    position: 'absolute', bottom: 0, left: 0, right: 0,
    flexDirection: 'row-reverse', alignItems: 'center', gap: 6,
    backgroundColor: 'rgba(0,0,0,0.4)', padding: 10,
  },
  photoLabel: { color: '#fff', fontSize: 13 },

  statusCard: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between', borderRadius: 14,
    borderLeftWidth: 4, padding: 14, marginHorizontal: 16, marginBottom: 12,
  },
  statusLeft: { alignItems: 'flex-end' },
  statusMain: { fontSize: 18, fontWeight: '700' },
  statusDate: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  statusBadge: { borderRadius: 12, paddingHorizontal: 12, paddingVertical: 6 },
  statusBadgeText: { color: '#fff', fontSize: 13, fontWeight: '700' },

  section: { marginBottom: 12, paddingHorizontal: 16 },
  sectionTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, marginBottom: 8, textAlign: 'right' },
  sectionTitleRow: { flexDirection: 'row-reverse', alignItems: 'center', gap: 6, marginBottom: 8 },

  receiptCard: {
    backgroundColor: Colors.surface, borderRadius: 14, padding: 14,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06, shadowRadius: 4, elevation: 2,
  },
  receiptRow: {
    flexDirection: 'row-reverse', justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  receiptLabel: { fontSize: 14, color: Colors.textSecondary },
  receiptValue: { fontSize: 14, fontWeight: '600', color: Colors.textPrimary },
  receiptDivider: { height: 1, backgroundColor: Colors.border, marginVertical: 6 },
  receiptTotal: { flexDirection: 'row-reverse', justifyContent: 'space-between', paddingTop: 4 },
  receiptTotalLabel: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  receiptTotalValue: { fontSize: 22, fontWeight: '700', color: Colors.accent },

  traderCard: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.surface, borderRadius: 14, padding: 14,
  },
  traderInfo: { flexDirection: 'row-reverse', alignItems: 'center', gap: 8 },
  traderName: { fontSize: 15, fontWeight: '700', color: Colors.accent },
  confirmedBadge: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 4,
    backgroundColor: Colors.profitBg, borderRadius: 8,
    paddingHorizontal: 10, paddingVertical: 4,
  },
  confirmedText: { fontSize: 12, color: Colors.profit, fontWeight: '600' },
  pendingBadge: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 4,
    backgroundColor: Colors.warningLight, borderRadius: 8,
    paddingHorizontal: 10, paddingVertical: 4,
  },
  pendingText: { fontSize: 12, color: Colors.warning, fontWeight: '600' },

  immutableNotice: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 8,
    backgroundColor: Colors.infoLight, borderRadius: 12,
    padding: 12, marginHorizontal: 16, marginTop: 4,
  },
  immutableText: { fontSize: 12, color: Colors.info, flex: 1, textAlign: 'right', lineHeight: 18 },
});
