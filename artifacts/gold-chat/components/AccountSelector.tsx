import { MaterialIcons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../constants/colors';
import { ChatAccount } from '../types';
import { getHashtagTotals } from '../utils/hashtag';
import { formatCurrency } from '../utils/gold';

interface Props {
  accounts: ChatAccount[];
  selectedIds: string[];
  onToggle: (id: string) => void;
  onSelectAll: () => void;
}

export function AccountSelector({ accounts, selectedIds, onToggle, onSelectAll }: Props) {
  const allSelected = selectedIds.length === accounts.length;

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>اختر الحسابات</Text>
        <Pressable
          style={[styles.selectAllBtn, allSelected && styles.selectAllActive]}
          onPress={onSelectAll}
        >
          <MaterialIcons name={allSelected ? 'check-box' : 'check-box-outline-blank'} size={18} color={allSelected ? '#fff' : Colors.accent} />
          <Text style={[styles.selectAllText, allSelected && styles.selectAllTextActive]}>الكل</Text>
        </Pressable>
      </View>

      {accounts.map(account => {
        const isSelected = selectedIds.includes(account.id);
        const totals = account.hashtags.reduce(
          (acc, h) => {
            const t = getHashtagTotals(h);
            return { debit: acc.debit + t.totalDebit, credit: acc.credit + t.totalCredit };
          },
          { debit: 0, credit: 0 },
        );
        const remaining = totals.debit - totals.credit;

        return (
          <Pressable
            key={account.id}
            style={[styles.accountCard, isSelected && styles.accountCardSelected]}
            onPress={() => onToggle(account.id)}
          >
            <View style={styles.checkbox}>
              <MaterialIcons
                name={isSelected ? 'check-circle' : 'radio-button-unchecked'}
                size={22}
                color={isSelected ? Colors.accent : Colors.textMuted}
              />
            </View>
            <View style={styles.accountInfo}>
              <Text style={styles.accountName}>{account.name}</Text>
              <Text style={styles.accountHashtags}>{account.hashtags.length} هاشتاج</Text>
            </View>
            <View style={styles.accountBalance}>
              <Text style={[styles.balanceAmount, { color: remaining > 0 ? Colors.error : Colors.profit }]}>
                {formatCurrency(remaining)}
              </Text>
              <Text style={styles.balanceLabel}>المتبقي</Text>
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 12,
    marginHorizontal: 12,
    marginVertical: 6,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
    paddingBottom: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  title: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  selectAllBtn: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.accent,
  },
  selectAllActive: { backgroundColor: Colors.accent },
  selectAllText: { fontSize: 12, fontWeight: '600', color: Colors.accent },
  selectAllTextActive: { color: '#E6EDF3' },

  accountCard: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 10,
    paddingHorizontal: 10,
    borderRadius: 10,
    backgroundColor: Colors.background,
    marginBottom: 6,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  accountCardSelected: {
    borderColor: Colors.accent,
    backgroundColor: Colors.accentLight + '30',
  },
  checkbox: { width: 26, alignItems: 'center' },
  accountInfo: { flex: 1, alignItems: 'flex-end' },
  accountName: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary },
  accountHashtags: { fontSize: 11, color: Colors.textMuted, marginTop: 2 },
  accountBalance: { alignItems: 'flex-end' },
  balanceAmount: { fontSize: 13, fontWeight: '700' },
  balanceLabel: { fontSize: 9, color: Colors.textMuted, marginTop: 1 },
});
