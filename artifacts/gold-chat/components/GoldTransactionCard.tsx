import { MaterialIcons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../constants/colors';
import { Transaction, UserType } from '../types';
import { formatCurrency, formatShortDate } from '../utils/gold';

interface Props {
  transaction: Transaction;
  isSent: boolean;
  userType: UserType;
  onPress: () => void;
}

function statusLabel(s: string) {
  return s === 'purchased' ? 'توثيق شراء' : s === 'returned' ? 'تم الإرجاع' : 'تم البيع';
}
function statusColor(s: string) {
  return s === 'purchased' ? Colors.purchased : s === 'returned' ? Colors.returned : Colors.resold;
}
function statusBg(s: string) {
  return s === 'purchased' ? Colors.purchasedBg : s === 'returned' ? Colors.returnedBg : Colors.resoldBg;
}

export function GoldTransactionCard({ transaction: t, isSent, userType, onPress }: Props) {
  const isTrader = userType === 'gold_trader';
  const sc = statusColor(t.status);
  const sb = statusBg(t.status);

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.card, isSent ? styles.cardSent : styles.cardReceived, pressed && styles.pressed]}
    >
      {/* Status bar */}
      <View style={[styles.statusBar, { backgroundColor: sc }]} />

      <View style={styles.body}>
        {/* Header */}
        <View style={styles.headerRow}>
          <View style={[styles.statusBadge, { backgroundColor: sb }]}>
            <Text style={[styles.statusText, { color: sc }]}>{statusLabel(t.status)}</Text>
          </View>
          <View style={styles.karatBadge}>
            <Text style={styles.karatText}>{t.goldKarat}</Text>
          </View>
        </View>

        {/* Photo thumbnail */}
        {t.photoUri ? (
          <Image source={{ uri: t.photoUri }} style={styles.photo} resizeMode="cover" />
        ) : null}

        {/* Main info */}
        <View style={styles.infoRow}>
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>النوع</Text>
            <Text style={styles.infoValue}>{t.goldType}</Text>
          </View>
          <View style={styles.infoDivider} />
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>الوزن</Text>
            <Text style={styles.infoValue}>{t.weight}g</Text>
          </View>
          <View style={styles.infoDivider} />
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>سعر الغرام</Text>
            <Text style={styles.infoValue}>{formatCurrency(t.pricePerGram)}</Text>
          </View>
        </View>

        {/* Total */}
        <View style={styles.totalRow}>
          <MaterialIcons name="chevron-left" size={16} color={Colors.accent} />
          <Text style={styles.totalAmount}>{formatCurrency(t.totalPrice)}</Text>
          <Text style={styles.totalLabel}>الإجمالي</Text>
        </View>

        {/* Trader profit row */}
        {isTrader && t.profit != null && (
          <View style={styles.profitRow}>
            <MaterialIcons name="lock" size={12} color={Colors.profit} />
            <Text style={styles.profitText}>صافي الربح: {formatCurrency(t.profit)}</Text>
          </View>
        )}

        {/* Footer */}
        <View style={styles.footer}>
          <MaterialIcons name="open-in-new" size={12} color={Colors.textMuted} />
          <Text style={styles.footerDetail}>عرض التفاصيل</Text>
          <View style={{ flex: 1 }} />
          <Text style={styles.footerDate}>{formatShortDate(t.timestamp)}</Text>
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row-reverse',
    borderRadius: 16,
    overflow: 'hidden',
    marginVertical: 3,
    maxWidth: 300,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
  },
  cardSent: { backgroundColor: Colors.surface, alignSelf: 'flex-end' },
  cardReceived: { backgroundColor: Colors.surface, alignSelf: 'flex-start' },
  pressed: { opacity: 0.9 },

  statusBar: { width: 4 },
  body: { flex: 1, padding: 12 },

  headerRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  statusBadge: {
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  statusText: { fontSize: 12, fontWeight: '700' },
  karatBadge: {
    backgroundColor: Colors.accentLight,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  karatText: { fontSize: 12, color: Colors.accentDark, fontWeight: '700' },

  photo: {
    width: '100%',
    height: 100,
    borderRadius: 10,
    marginBottom: 10,
  },

  infoRow: {
    flexDirection: 'row-reverse',
    backgroundColor: Colors.background,
    borderRadius: 10,
    padding: 10,
    marginBottom: 10,
  },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 10, color: Colors.textMuted, marginBottom: 3 },
  infoValue: { fontSize: 13, fontWeight: '700', color: Colors.textPrimary },
  infoDivider: { width: StyleSheet.hairlineWidth, backgroundColor: Colors.border, marginHorizontal: 4 },

  totalRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    marginBottom: 8,
  },
  totalLabel: { fontSize: 12, color: Colors.textSecondary },
  totalAmount: { fontSize: 20, fontWeight: '700', color: Colors.textPrimary },

  profitRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    backgroundColor: Colors.profitBg,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginBottom: 8,
    alignSelf: 'flex-end',
  },
  profitText: { fontSize: 12, color: Colors.profit, fontWeight: '600' },

  footer: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
    paddingTop: 8,
  },
  footerDetail: { fontSize: 11, color: Colors.textMuted },
  footerDate: { fontSize: 11, color: Colors.textMuted },
});
