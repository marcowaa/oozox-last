import { MaterialIcons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../constants/colors';
import { ChatAccount } from '../types';
import { getHashtagTotals } from '../utils/hashtag';
import { formatCurrency } from '../utils/gold';

interface Props {
  accounts: ChatAccount[];
  onSelectAccount: (account: ChatAccount) => void;
  onCreateAccount: () => void;
}

export function HashtagPicker({ accounts, onSelectAccount, onCreateAccount }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>الحسابات الجارية</Text>
        <Text style={styles.headerSubtitle}>{accounts.length} حساب</Text>
      </View>

      {accounts.length > 0 ? (
        accounts.map(account => {
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
              style={styles.accountCard}
              onPress={() => onSelectAccount(account)}
            >
              <View style={styles.accountLeft}>
                <View style={styles.accountIcon}>
                  <MaterialIcons name="folder" size={20} color={Colors.accent} />
                </View>
                <View style={styles.accountInfo}>
                  <Text style={styles.accountName}>{account.name}</Text>
                  <Text style={styles.accountTags}>
                    {account.hashtags.map(h => `#${h.tag}`).join('  ') || 'لا هاشتاجات'}
                  </Text>
                </View>
              </View>
              <View style={styles.accountRight}>
                <Text style={[
                  styles.balanceAmount,
                  { color: remaining > 0 ? Colors.error : Colors.profit },
                ]}>
                  {formatCurrency(remaining)}
                </Text>
                <Text style={styles.balanceLabel}>
                  {account.status === 'settled' ? 'مُسدّد' : 'المتبقي'}
                </Text>
              </View>
            </Pressable>
          );
        })
      ) : (
        <View style={styles.emptyRow}>
          <Text style={styles.emptyText}>لا توجد حسابات بعد</Text>
        </View>
      )}

      <Pressable style={styles.createBtn} onPress={onCreateAccount}>
        <MaterialIcons name="add-circle-outline" size={18} color={Colors.accent} />
        <Text style={styles.createBtnText}>إنشاء حساب جديد</Text>
      </Pressable>
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
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
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
  headerTitle: { fontSize: 16, fontWeight: '700', color: Colors.accent },
  headerSubtitle: { fontSize: 12, color: Colors.textMuted },

  accountCard: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 10,
    paddingHorizontal: 10,
    backgroundColor: Colors.background,
    borderRadius: 10,
    marginBottom: 6,
  },
  accountLeft: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  accountIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.accentLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  accountInfo: { flex: 1, alignItems: 'flex-end' },
  accountName: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary },
  accountTags: { fontSize: 11, color: Colors.textMuted, marginTop: 3 },

  accountRight: { alignItems: 'flex-end' },
  balanceAmount: { fontSize: 15, fontWeight: '700' },
  balanceLabel: { fontSize: 10, color: Colors.textMuted, marginTop: 2 },

  emptyRow: { paddingVertical: 16, alignItems: 'center' },
  emptyText: { fontSize: 13, color: Colors.textMuted },

  createBtn: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: Colors.accentLight,
    borderRadius: 10,
    paddingVertical: 10,
    marginTop: 4,
  },
  createBtnText: { fontSize: 14, fontWeight: '700', color: Colors.accentDark },
});
