import { MaterialIcons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../constants/colors';
import { HashtagEntry } from '../types';
import { formatCurrency, formatShortDate } from '../utils/gold';
import { getEntryLabel } from '../utils/hashtag';

interface Props {
  entry: HashtagEntry;
  isSent: boolean;
  onPress: () => void;
}

export function InvoiceMessage({ entry, isSent, onPress }: Props) {
  const isDebit = entry.direction === 'debit';
  const accentColor = isDebit ? Colors.error : Colors.profit;

  return (
    <View style={[styles.row, isSent ? styles.rowSent : styles.rowReceived]}>
      <Pressable
        onPress={onPress}
        style={[styles.card, isSent ? styles.cardSent : styles.cardReceived]}
      >
        <View style={[styles.accentBar, { backgroundColor: accentColor }]} />
        <View style={styles.body}>
          <View style={styles.headerRow}>
            <View style={[styles.typeBadge, { backgroundColor: isDebit ? Colors.errorLight : Colors.profitBg }]}>
              <MaterialIcons name={isDebit ? 'receipt' : 'payments'} size={14} color={accentColor} />
              <Text style={[styles.typeText, { color: accentColor }]}>{getEntryLabel(entry)}</Text>
            </View>
            <Text style={[styles.date, { color: isSent ? 'rgba(11,15,20,0.6)' : Colors.textMuted }]}>
              {formatShortDate(entry.createdAt)}
            </Text>
          </View>
          <Text style={[styles.description, { color: isSent ? '#fff' : Colors.textPrimary }]}>
            {entry.description}
          </Text>
          <View style={styles.amountRow}>
            <Text style={[styles.amount, { color: accentColor }]}>
              {formatCurrency(entry.amount)}
            </Text>
            <Text style={[styles.directionLabel, {
              color: isSent ? 'rgba(11,15,20,0.5)' : Colors.textMuted,
              backgroundColor: isSent ? 'rgba(255,255,255,0.1)' : Colors.surfaceAlt,
            }]}>
              {isDebit ? 'مدين' : 'دائن'}
            </Text>
          </View>
          <View style={[styles.footer, { borderTopColor: isSent ? 'rgba(255,255,255,0.15)' : Colors.border }]}>
            <MaterialIcons name="open-in-new" size={11} color={isSent ? 'rgba(11,15,20,0.5)' : Colors.textMuted} />
            <Text style={[styles.footerText, { color: isSent ? 'rgba(11,15,20,0.5)' : Colors.textMuted }]}>
              عرض التفاصيل
            </Text>
          </View>
        </View>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { paddingHorizontal: 12, marginVertical: 3 },
  rowSent: { alignItems: 'flex-end' },
  rowReceived: { alignItems: 'flex-start' },
  card: {
    flexDirection: 'row-reverse',
    maxWidth: '80%',
    borderRadius: 14,
    overflow: 'hidden',
  },
  cardSent: { backgroundColor: Colors.sentBubble, borderBottomRightRadius: 4 },
  cardReceived: {
    backgroundColor: Colors.receivedBubble,
    borderBottomLeftRadius: 4,
    borderWidth: 1, borderColor: Colors.border,
  },
  accentBar: { width: 4 },
  body: { flex: 1, padding: 12 },
  headerRow: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between', marginBottom: 8,
  },
  typeBadge: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 4,
    borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3,
  },
  typeText: { fontSize: 12, fontWeight: '700' },
  date: { fontSize: 11 },
  description: { fontSize: 14, textAlign: 'right', marginBottom: 6 },
  amountRow: {
    flexDirection: 'row-reverse', alignItems: 'center',
    gap: 8, marginBottom: 6,
  },
  amount: { fontSize: 18, fontWeight: '700' },
  directionLabel: { fontSize: 11, borderRadius: 6, paddingHorizontal: 6, paddingVertical: 1 },
  footer: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 4,
    borderTopWidth: StyleSheet.hairlineWidth, paddingTop: 6,
  },
  footerText: { fontSize: 10 },
});
