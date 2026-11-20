import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState, useMemo } from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ChatListItem } from '@/components/ChatListItem';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { formatCurrency } from '../../utils/gold';
import { calculateHashtagTotals } from '../../utils/hashtag';

export default function ChatsScreen() {
  const insets = useSafeAreaInsets();
  const { user, chats, chatAccounts } = useApp();
  const [search, setSearch] = useState('');

  // Search across chats AND accounts/hashtags
  const searchResults = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return { chats: [...chats].sort((a, b) => (b.lastMessageTime ?? 0) - (a.lastMessageTime ?? 0)), accounts: [] as typeof chatAccounts };

    const matchedChats = chats.filter(c =>
      c.contactName.toLowerCase().includes(q) ||
      c.lastMessage?.toLowerCase().includes(q)
    ).sort((a, b) => (b.lastMessageTime ?? 0) - (a.lastMessageTime ?? 0));

    // Search in accounts and hashtags
    const tag = q.startsWith('#') ? q.replace('#', '') : q;
    const matchedAccounts = chatAccounts.filter(a => {
      // Match account name
      if (a.name.toLowerCase().includes(q)) return true;
      // Match any hashtag tag
      if (a.hashtags.some(h => h.tag.toLowerCase().includes(tag))) return true;
      // Match any entry description
      if (a.hashtags.some(h => h.entries.some(e => e.description.toLowerCase().includes(q)))) return true;
      return false;
    });

    return { chats: matchedChats, accounts: matchedAccounts };
  }, [chats, chatAccounts, search]);

  const sorted = searchResults.chats;
  const matchedAccounts = searchResults.accounts;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>ذهبي</Text>
        <View style={styles.headerActions}>
          {user?.type === 'gold_trader' && (
            <View style={styles.verifiedBadge}>
              <MaterialIcons name="verified" size={14} color={Colors.accent} />
              <Text style={styles.verifiedText}>تاجر</Text>
            </View>
          )}
        </View>
      </View>

      {/* Search */}
      <View style={styles.searchContainer}>
        <View style={styles.searchBar}>
          <MaterialIcons name="search" size={18} color={Colors.placeholder} />
          <TextInput
            style={styles.searchInput}
            placeholder="ابحث في المحادثات أو #هاشتاج..."
            placeholderTextColor={Colors.placeholder}
            value={search}
            onChangeText={setSearch}
            textAlign="right"
          />
          {search.length > 0 && (
            <Pressable onPress={() => setSearch('')}>
              <MaterialIcons name="close" size={16} color={Colors.textMuted} />
            </Pressable>
          )}
        </View>
      </View>

      {/* Hashtag/Account search results */}
      {search.length > 0 && matchedAccounts.length > 0 && (
        <View style={styles.resultsSection}>
          <Text style={styles.resultsTitle}>حسابات وهاشتاجات ({matchedAccounts.length})</Text>
          <FlatList
            data={matchedAccounts}
            keyExtractor={item => item.id}
            renderItem={({ item: account }) => {
              const totals = account.hashtags.reduce(
                (acc, h) => {
                  const t = calculateHashtagTotals(h.entries);
                  return { debit: acc.debit + t.totalDebit, credit: acc.credit + t.totalCredit };
                },
                { debit: 0, credit: 0 },
              );
              const remaining = totals.debit - totals.credit;
              // Find the chat name for this account
              const chat = chats.find(c => c.id === account.chatId);
              return (
                <Pressable
                  style={styles.accountResult}
                  onPress={() => {
                    if (chat) router.push(`/chat/${chat.id}`);
                    setSearch('');
                  }}
                >
                  <View style={styles.accountResultLeft}>
                    <View style={styles.accountResultIcon}>
                      <MaterialIcons name="folder" size={18} color={Colors.accent} />
                    </View>
                    <View style={styles.accountResultInfo}>
                      <Text style={styles.accountResultName}>{account.name}</Text>
                      <Text style={styles.accountResultChat}>{chat?.contactName ?? ''}</Text>
                      <View style={styles.accountResultTags}>
                        {account.hashtags.slice(0, 3).map(h => (
                          <View key={h.id} style={styles.tagChip}>
                            <Text style={styles.tagChipText}>#{h.tag}</Text>
                          </View>
                        ))}
                        {account.hashtags.length > 3 && (
                          <Text style={styles.moreTags}>+{account.hashtags.length - 3}</Text>
                        )}
                      </View>
                    </View>
                  </View>
                  <View style={styles.accountResultRight}>
                    <Text style={[styles.accountResultBalance, { color: remaining > 0 ? Colors.error : Colors.profit }]}>
                      {formatCurrency(remaining)}
                    </Text>
                    <Text style={styles.accountResultLabel}>المتبقي</Text>
                  </View>
                </Pressable>
              );
            }}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={{ paddingBottom: 8 }}
          />
        </View>
      )}

      {/* Chat List */}
      {sorted.length === 0 && matchedAccounts.length === 0 ? (
        <View style={styles.empty}>
          <View style={styles.emptyIcon}>
            <MaterialIcons name="chat-bubble-outline" size={48} color={Colors.accent} />
          </View>
          <Text style={styles.emptyTitle}>لا توجد محادثات</Text>
          <Text style={styles.emptySubtitle}>ابدأ محادثة جديدة مع تاجر أو عميل</Text>
          <Pressable style={styles.emptyBtn} onPress={() => router.push('/new-chat')}>
            <MaterialIcons name="add" size={18} color="#0B0F14" />
            <Text style={styles.emptyBtnText}>محادثة جديدة</Text>
          </Pressable>
        </View>
      ) : sorted.length > 0 ? (
        <>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionLabel}>المحادثات الأخيرة</Text>
            <View style={styles.sectionBadge}>
              <Text style={styles.sectionBadgeText}>{sorted.length}</Text>
            </View>
          </View>
          <FlatList
            data={sorted}
            keyExtractor={item => item.id}
            renderItem={({ item }) => (
              <ChatListItem
                chat={item}
                onPress={() => router.push(`/chat/${item.id}`)}
              />
            )}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.listContent}
          />
        </>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg },

  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 16,
    backgroundColor: Colors.primary,
  },
  headerTitle: { fontSize: 24, fontWeight: '800', color: Colors.accent, letterSpacing: 1 },
  headerActions: { flexDirection: 'row-reverse', gap: 6 },
  verifiedBadge: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 4,
    backgroundColor: Colors.accentGlow, borderRadius: 16,
    paddingHorizontal: 10, paddingVertical: 4,
    borderWidth: 1, borderColor: Colors.accentDark + '40',
  },
  verifiedText: { fontSize: 11, color: Colors.accent, fontWeight: '700' },

  searchContainer: { backgroundColor: Colors.primary, paddingHorizontal: 16, paddingBottom: 12 },
  searchBar: {
    flexDirection: 'row-reverse', alignItems: 'center',
    backgroundColor: Colors.surface, borderRadius: 14,
    paddingHorizontal: 14, paddingVertical: 10, gap: 8,
    borderWidth: 1, borderColor: Colors.border,
  },
  searchInput: { flex: 1, fontSize: 14, color: Colors.textPrimary, padding: 0 },

  resultsSection: { padding: 12, flex: 1 },
  resultsTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, marginBottom: 10, textAlign: 'right' },

  accountResult: {
    flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between',
    backgroundColor: Colors.surface, borderRadius: 14, padding: 12, marginBottom: 6,
    borderWidth: 1, borderColor: Colors.border,
  },
  accountResultLeft: { flexDirection: 'row-reverse', alignItems: 'center', gap: 10, flex: 1 },
  accountResultIcon: {
    width: 36, height: 36, borderRadius: 10,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center', justifyContent: 'center',
  },
  accountResultInfo: { flex: 1, alignItems: 'flex-end' },
  accountResultName: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary },
  accountResultChat: { fontSize: 11, color: Colors.textMuted, marginTop: 2 },
  accountResultTags: { flexDirection: 'row-reverse', gap: 4, marginTop: 4 },
  tagChip: { backgroundColor: Colors.accentGlow, borderRadius: 6, paddingHorizontal: 6, paddingVertical: 1 },
  tagChipText: { fontSize: 10, color: Colors.accentLight, fontWeight: '600' },
  moreTags: { fontSize: 10, color: Colors.textMuted, alignSelf: 'center' },
  accountResultRight: { alignItems: 'flex-end' },
  accountResultBalance: { fontSize: 14, fontWeight: '700' },
  accountResultLabel: { fontSize: 9, color: Colors.textMuted, marginTop: 1 },

  sectionHeader: {
    flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 10, backgroundColor: Colors.bg,
  },
  sectionLabel: { fontSize: 12, color: Colors.textMuted, fontWeight: '600' },
  sectionBadge: {
    backgroundColor: Colors.accentGlow, borderRadius: 10,
    minWidth: 20, height: 20, alignItems: 'center', justifyContent: 'center',
    paddingHorizontal: 6,
  },
  sectionBadgeText: { fontSize: 11, color: Colors.accent, fontWeight: '700' },

  listContent: { paddingBottom: 80 },

  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  emptyIcon: {
    width: 96, height: 96, borderRadius: 28,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center', justifyContent: 'center', marginBottom: 20,
    borderWidth: 1, borderColor: Colors.accentDark + '30',
  },
  emptyTitle: { fontSize: 20, fontWeight: '700', color: Colors.textPrimary, marginBottom: 8 },
  emptySubtitle: { fontSize: 14, color: Colors.textSecondary, textAlign: 'center', marginBottom: 24 },
  emptyBtn: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 8,
    backgroundColor: Colors.accent, borderRadius: 14,
    paddingHorizontal: 24, paddingVertical: 12,
  },
  emptyBtnText: { color: '#0B0F14', fontSize: 15, fontWeight: '700' },
});
