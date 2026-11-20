import { MaterialIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { GoldCalculatorSheet } from '@/components/GoldCalculatorSheet';
import { GoldTransactionCard } from '@/components/GoldTransactionCard';
import { HashtagPicker } from '@/components/HashtagPicker';
import { HashtagTable } from '@/components/HashtagTable';
import { InvoiceMessage } from '@/components/InvoiceMessage';
import { MessageBubble } from '@/components/MessageBubble';
import { AccountantAssistant } from '@/components/AccountantAssistant';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { ChatAccount, HashtagAccount, Message } from '../../types';
import { dayKey, formatDaySeparator, formatShortTime } from '../../utils/gold';

type ListItem =
  | { kind: 'message'; message: Message }
  | { kind: 'separator'; key: string; label: string };

function buildListItems(messages: Message[]): ListItem[] {
  const sorted = [...messages].sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
  );
  const items: ListItem[] = [];
  let lastDay: string | null = null;
  for (const msg of sorted) {
    const dk = dayKey(msg.timestamp);
    if (dk !== lastDay) {
      items.push({ kind: 'separator', key: `sep-${dk}`, label: formatDaySeparator(msg.timestamp) });
      lastDay = dk;
    }
    items.push({ kind: 'message', message: msg });
  }
  return items;
}

const QUICK_REPLIES = ['كم السعر؟', 'متاح؟', 'شكراً', 'تمام', 'بكرة إن شاء الله'];

export default function ChatScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const {
    user, chats, messages, transactions, sendMessage, markChatRead,
    chatAccounts, createChatAccount, getChatAccounts,
    addHashtagToAccount, getAccountHashtags, getAllChatHashtags,
    addHashtagEntry, settleHashtag, addHashtagComment, addEntryComment,
  } = useApp();
  const [text, setText] = useState('');
  const [showCalc, setShowCalc] = useState(false);
  const [showAccountant, setShowAccountant] = useState(false);
  const [inputFocused, setInputFocused] = useState(false);
  const [showAccountPicker, setShowAccountPicker] = useState(false);
  const [showCreateAccount, setShowCreateAccount] = useState(false);
  const [newAccountName, setNewAccountName] = useState('');
  const [newHashtagTag, setNewHashtagTag] = useState('');
  const [selectedAccount, setSelectedAccount] = useState<ChatAccount | null>(null);
  const [selectedHashtag, setSelectedHashtag] = useState<HashtagAccount | null>(null);
  const flatListRef = useRef<FlatList>(null);

  const chat = chats.find(c => c.id === id);
  const chatMessages = useMemo(() => (messages[id] ?? []), [messages, id]);
  const listItems = useMemo(() => buildListItems(chatMessages), [chatMessages]);
  const accounts = useMemo(() => getChatAccounts(id), [getChatAccounts, id]);

  useEffect(() => {
    if (id) markChatRead(id);
  }, [id, markChatRead]);

  const showCalculator = user?.type === 'gold_trader' || chat?.contactType === 'gold_trader';

  const handleSend = useCallback(async () => {
    const t = text.trim();
    if (!t || !id) return;

    // Check if input is a hashtag (#something)
    if (t.startsWith('#')) {
      const tag = t.replace(/^#/, '').trim();
      if (tag) {
        // Check if there's already an account with a hashtag matching this tag
        const existingAccount = accounts.find(a =>
          a.hashtags.some(h => h.tag === tag)
        );
        if (existingAccount) {
          // Open the existing hashtag table
          const existingHashtag = existingAccount.hashtags.find(h => h.tag === tag)!;
          setSelectedAccount(existingAccount);
          setSelectedHashtag(existingHashtag);
          setText('');
          return;
        }
        // No existing — create a new account + hashtag
        const accountName = `حساب #${tag}`;
        const account = await createChatAccount(id, accountName);
        await addHashtagToAccount(id, account.id, tag);
        await sendMessage(id, `#${tag} — ${accountName}`, 'hashtag');
        // Re-fetch the updated account with the new hashtag
        const updatedAccounts = getChatAccounts(id);
        const updatedAccount = updatedAccounts.find(a => a.id === account.id);
        if (updatedAccount && updatedAccount.hashtags.length > 0) {
          setSelectedAccount(updatedAccount);
          setSelectedHashtag(updatedAccount.hashtags[updatedAccount.hashtags.length - 1]);
        } else {
          setSelectedAccount(account);
          setSelectedHashtag(null);
        }
        setText('');
        return;
      }
    }

    // Normal text message
    await sendMessage(id, t);
    setText('');
  }, [text, id, sendMessage, accounts, createChatAccount, addHashtagToAccount, getChatAccounts]);

  async function handleCreateAccount() {
    if (!newAccountName.trim() || !id) return;
    const account = await createChatAccount(id, newAccountName.trim());
    let createdHashtag = null;
    if (newHashtagTag.trim()) {
      const tag = newHashtagTag.trim().replace('#', '');
      await addHashtagToAccount(id, account.id, tag);
      await sendMessage(id, `#${tag} — ${newAccountName.trim()}`, 'hashtag');
      // Find the hashtag we just created (it was added by addHashtagToAccount)
      const updatedAccounts = getChatAccounts(id);
      const updatedAccount = updatedAccounts.find(a => a.id === account.id);
      if (updatedAccount) {
        createdHashtag = updatedAccount.hashtags.find(h => h.tag === tag) ?? null;
        setSelectedAccount(updatedAccount);
        if (createdHashtag) setSelectedHashtag(createdHashtag);
      }
    } else {
      await sendMessage(id, `إنشاء حساب: ${newAccountName.trim()}`, 'hashtag');
      setSelectedAccount(account);
    }
    setNewAccountName('');
    setNewHashtagTag('');
    setShowCreateAccount(false);
  }

  async function handleSelectAccount(account: ChatAccount) {
    setSelectedAccount(account);
    setShowAccountPicker(false);
    // If account has hashtags, show first one
    if (account.hashtags.length > 0) {
      setSelectedHashtag(account.hashtags[0]);
    }
  }

  function getAvatarColor(name: string) {
    let hash = 0;
    for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return Colors.avatarPalette[Math.abs(hash) % Colors.avatarPalette.length];
  }

  function renderItem({ item }: { item: ListItem }) {
    if (item.kind === 'separator') {
      return (
        <View style={styles.separatorRow}>
          <View style={styles.separatorChip}>
            <Text style={styles.separatorText}>{item.label}</Text>
          </View>
        </View>
      );
    }
    const msg = item.message;
    const isSent = msg.senderId === user?.id;

    if (msg.type === 'hashtag') {
      // Find the account this message refers to
      const account = accounts.find(a => a.hashtags.some(h => h.id === msg.hashtagId));
      const hashtag = account?.hashtags.find(h => h.id === msg.hashtagId);
      if (hashtag && account) {
        return (
          <View style={[styles.txRow, isSent ? styles.txRowSent : styles.txRowReceived]}>
            <Pressable
              style={styles.hashtagBubble}
              onPress={() => { setSelectedAccount(account); setSelectedHashtag(hashtag); }}
            >
              <View style={styles.hashtagHeader}>
                <MaterialIcons name="tag" size={16} color={Colors.accent} />
                <Text style={styles.hashtagTitle}>#{hashtag.tag}</Text>
                <View style={styles.accountBadge}>
                  <Text style={styles.accountBadgeText}>{account.name}</Text>
                </View>
              </View>
              <Text style={styles.hashtagHint}>اضغط لعرض الحساب الجاري</Text>
              <View style={styles.metaRow}>
                {isSent && <MaterialIcons name="done" size={13} color="rgba(255,255,255,0.6)" />}
                <Text style={styles.timeText}>{formatShortTime(msg.timestamp)}</Text>
              </View>
            </Pressable>
          </View>
        );
      }
    }

    if (msg.type === 'invoice' && msg.hashtagId) {
      // Find entry in any account's hashtag
      for (const acc of accounts) {
        const ha = acc.hashtags.find(h => h.id === msg.hashtagId);
        if (ha) {
          const entry = ha.entries[ha.entries.length - 1];
          if (entry) {
            return (
              <InvoiceMessage
                entry={entry}
                isSent={isSent}
                onPress={() => { setSelectedAccount(acc); setSelectedHashtag(ha); }}
              />
            );
          }
        }
      }
    }

    if (msg.type === 'gold_purchase' && msg.transactionId) {
      const tx = transactions.find(t => t.id === msg.transactionId);
      if (tx && user) {
        return (
          <View style={[styles.txRow, isSent ? styles.txRowSent : styles.txRowReceived]}>
            <GoldTransactionCard
              transaction={tx}
              isSent={isSent}
              userType={user.type}
              onPress={() => router.push(`/transaction/${tx.id}`)}
            />
          </View>
        );
      }
    }

    return <MessageBubble message={msg} isSent={isSent} />;
  }

  const avatarColor = getAvatarColor(chat?.contactName ?? '');
  const initials = (chat?.contactName ?? '?').slice(0, 2).toUpperCase();

  // Get fresh data for selected hashtag
  const currentSelectedAccount = selectedAccount
    ? accounts.find(a => a.id === selectedAccount.id) ?? selectedAccount
    : null;
  const currentSelectedHashtag = selectedHashtag && currentSelectedAccount
    ? currentSelectedAccount.hashtags.find(h => h.id === selectedHashtag.id) ?? selectedHashtag
    : null;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable style={styles.headerBack} onPress={() => router.back()}>
          <MaterialIcons name="arrow-forward-ios" size={20} color={Colors.textInverse} />
        </Pressable>
        <View style={styles.headerInfo}>
          <Text style={styles.headerName}>{chat?.contactName ?? ''}</Text>
          {chat?.contactVerified && (
            <View style={styles.verifiedRow}>
              <MaterialIcons name="verified" size={12} color={Colors.accent} />
              <Text style={styles.verifiedText}>موثق</Text>
            </View>
          )}
        </View>
        <Pressable
          style={[styles.headerAvatar, { backgroundColor: avatarColor }]}
          onPress={() => setShowAccountPicker(true)}
        >
          <Text style={styles.headerAvatarText}>{initials}</Text>
        </Pressable>
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={0}
      >
        <FlatList
          ref={flatListRef}
          data={listItems}
          keyExtractor={item => item.kind === 'separator' ? item.key : item.message.id}
          renderItem={renderItem}
          inverted
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          style={styles.list}
        />

        {/* Accounts summary bar */}
        {accounts.length > 0 && !showAccountPicker && (
          <Pressable style={styles.accountsBar} onPress={() => setShowAccountPicker(true)}>
            <View style={styles.accountsBarLeft}>
              {accounts.slice(0, 3).map(a => (
                <View key={a.id} style={styles.accountChip}>
                  <MaterialIcons name="folder" size={10} color={Colors.accent} />
                  <Text style={styles.accountChipText} numberOfLines={1}>{a.name}</Text>
                </View>
              ))}
              {accounts.length > 3 && (
                <Text style={styles.moreText}>+{accounts.length - 3}</Text>
              )}
            </View>
            <MaterialIcons name="chevron-left" size={16} color={Colors.textMuted} />
          </Pressable>
        )}

        {/* Quick replies */}
        {inputFocused && !showAccountPicker && (
          <View style={styles.quickReplies}>
            {QUICK_REPLIES.map(r => (
              <Pressable key={r} style={styles.quickChip} onPress={() => setText(r)}>
                <Text style={styles.quickChipText}>{r}</Text>
              </Pressable>
            ))}
          </View>
        )}

        {/* Input bar */}
        <View style={[styles.inputBar, { paddingBottom: Math.max(insets.bottom, 8) }]}>
          <Pressable style={styles.sendBtn} onPress={text.trim() ? handleSend : undefined}>
            <MaterialIcons name={text.trim() ? 'send' : 'mic'} size={22} color="#fff" />
          </Pressable>
          <TextInput
            style={styles.input}
            placeholder="اكتب رسالة... أو #هاشتاج للحسابات"
            placeholderTextColor={Colors.placeholder}
            value={text}
            onChangeText={setText}
            multiline
            textAlign="right"
            onFocus={() => setInputFocused(true)}
            onBlur={() => setInputFocused(false)}
            onSubmitEditing={handleSend}
            blurOnSubmit={false}
          />
          {/* Accounts button */}
          <Pressable style={styles.accountBtn} onPress={() => setShowAccountPicker(true)}>
            <MaterialIcons name="folder" size={22} color={Colors.accent} />
            {accounts.length > 0 && (
              <View style={styles.accountBadgeCount}>
                <Text style={styles.accountBadgeCountText}>{accounts.length}</Text>
              </View>
            )}
          </Pressable>
          {/* Accountant button — always visible */}
          <Pressable style={styles.accountantBtn} onPress={() => setShowAccountant(true)}>
            <MaterialIcons name="analytics" size={22} color={Colors.accent} />
          </Pressable>
          {showCalculator && (
            <Pressable style={styles.calcBtn} onPress={() => setShowCalc(true)}>
              <MaterialIcons name="calculate" size={22} color={Colors.accent} />
            </Pressable>
          )}
        </View>
      </KeyboardAvoidingView>

      {/* Account Picker Modal (overlay — doesn't close chat) */}
      <Modal visible={showAccountPicker} animationType="slide" presentationStyle="pageSheet" onRequestClose={() => setShowAccountPicker(false)}>
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <Pressable onPress={() => setShowAccountPicker(false)} style={styles.closeBtn}>
              <MaterialIcons name="close" size={22} color={Colors.textPrimary} />
            </Pressable>
            <Text style={styles.modalTitle}>حسابات {chat?.contactName ?? ''}</Text>
            <Pressable onPress={() => { setShowCreateAccount(true); }} style={styles.addBtn}>
              <MaterialIcons name="add" size={22} color={Colors.accent} />
            </Pressable>
          </View>

          <HashtagPicker
            accounts={accounts}
            onSelectAccount={handleSelectAccount}
            onCreateAccount={() => setShowCreateAccount(true)}
          />

          {/* Create account form */}
          {showCreateAccount && (
            <View style={styles.createForm}>
              <Text style={styles.createFormTitle}>إنشاء حساب جديد</Text>
              <TextInput
                style={styles.createInput}
                value={newAccountName}
                onChangeText={setNewAccountName}
                placeholder="اسم الحساب (مثال: حساب الرمل)"
                placeholderTextColor={Colors.placeholder}
                textAlign="right"
                autoFocus
              />
              <TextInput
                style={styles.createInput}
                value={newHashtagTag}
                onChangeText={setNewHashtagTag}
                placeholder="هاشتاج (مثال: #رمل)"
                placeholderTextColor={Colors.placeholder}
                textAlign="right"
              />
              <View style={styles.createFormActions}>
                <Pressable style={styles.cancelCreateBtn} onPress={() => setShowCreateAccount(false)}>
                  <Text style={styles.cancelCreateText}>إلغاء</Text>
                </Pressable>
                <Pressable style={styles.confirmCreateBtn} onPress={handleCreateAccount}>
                  <Text style={styles.confirmCreateText}>إنشاء</Text>
                </Pressable>
              </View>
            </View>
          )}

          {/* List of hashtags in selected account */}
          {currentSelectedAccount && !showCreateAccount && (
            <View style={styles.hashtagsList}>
              <Text style={styles.hashtagsListTitle}>
                هاشتاجات حساب: {currentSelectedAccount.name}
              </Text>
              {currentSelectedAccount.hashtags.length === 0 ? (
                <Text style={styles.emptyHashtags}>لا توجد هاشتاجات في هذا الحساب</Text>
              ) : (
                currentSelectedAccount.hashtags.map(h => (
                  <Pressable
                    key={h.id}
                    style={[
                      styles.hashtagItem,
                      selectedHashtag?.id === h.id && styles.hashtagItemSelected,
                    ]}
                    onPress={() => { setSelectedHashtag(h); }}
                  >
                    <MaterialIcons name="tag" size={16} color={Colors.accent} />
                    <Text style={styles.hashtagItemText}>#{h.tag}</Text>
                    <Text style={styles.hashtagItemEntries}>{h.entries.length} معاملة</Text>
                  </Pressable>
                ))
              )}
              {/* Add hashtag to account */}
              <Pressable
                style={styles.addHashtagBtn}
                onPress={async () => {
                  if (!currentSelectedAccount || !id) return;
                  const tag = `هاشتاج_${currentSelectedAccount.hashtags.length + 1}`;
                  await addHashtagToAccount(id, currentSelectedAccount.id, tag);
                  await sendMessage(id, `#${tag} في ${currentSelectedAccount.name}`, 'hashtag');
                }}
              >
                <MaterialIcons name="add" size={16} color={Colors.accent} />
                <Text style={styles.addHashtagText}>إضافة هاشتاج</Text>
              </Pressable>
            </View>
          )}
        </View>
      </Modal>

      {/* Hashtag Table Modal (overlay) */}
      {currentSelectedAccount && currentSelectedHashtag && (
        <HashtagTable
          account={currentSelectedAccount}
          hashtag={currentSelectedHashtag}
          contactName={chat?.contactName ?? ''}
          allAccounts={accounts}
          visible={!!currentSelectedHashtag}
          onClose={() => setSelectedHashtag(null)}
          onAddInvoice={async (description, amount, selectedAccountIds) => {
            if (!id || !currentSelectedAccount) return;
            await addHashtagEntry(id, currentSelectedAccount.id, currentSelectedHashtag.id, 'invoice', description, amount, 'debit', selectedAccountIds);
            const accountLabel = selectedAccountIds.length > 1
              ? ` (${selectedAccountIds.length} حسابات)`
              : '';
            await sendMessage(id, `فاتورة: ${description} - ${amount} ج.م${accountLabel}`, 'invoice', currentSelectedHashtag.id);
          }}
          onAddPayment={async (amount) => {
            if (!id || !currentSelectedAccount) return;
            await addHashtagEntry(id, currentSelectedAccount.id, currentSelectedHashtag.id, 'payment', 'دفعة', amount, 'credit');
            await sendMessage(id, `دفعة: ${amount} ج.م`, 'invoice', currentSelectedHashtag.id);
          }}
          onSettleFull={async () => {
            if (!id || !currentSelectedAccount) return;
            await settleHashtag(id, currentSelectedAccount.id, currentSelectedHashtag.id);
            await sendMessage(id, 'تم تسديد الحساب بالكامل ✓', 'system');
          }}
          onAddComment={async (commentText) => {
            if (!id || !currentSelectedAccount) return;
            await addHashtagComment(id, currentSelectedAccount.id, currentSelectedHashtag.id, commentText);
          }}
          onAddEntryComment={async (entryId, commentText) => {
            if (!id || !currentSelectedAccount) return;
            await addEntryComment(id, currentSelectedAccount.id, currentSelectedHashtag.id, entryId, commentText);
          }}
        />
      )}

      <GoldCalculatorSheet visible={showCalc} chatId={id} onClose={() => setShowCalc(false)} />

      {/* Accountant Assistant — available in all chats */}
      <AccountantAssistant
        visible={showAccountant}
        accounts={accounts}
        chatName={chat?.contactName ?? ''}
        onClose={() => setShowAccountant(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F0E8D8' },
  header: {
    flexDirection: 'row-reverse', alignItems: 'center',
    backgroundColor: Colors.primary, paddingHorizontal: 12, paddingVertical: 10, gap: 10,
  },
  headerBack: { padding: 4 },
  headerAvatar: {
    width: 42, height: 42, borderRadius: 21,
    alignItems: 'center', justifyContent: 'center',
  },
  headerAvatarText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  headerInfo: { flex: 1, alignItems: 'flex-end' },
  headerName: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  verifiedRow: { flexDirection: 'row-reverse', alignItems: 'center', gap: 3, marginTop: 1 },
  verifiedText: { fontSize: 11, color: Colors.accent },

  list: { flex: 1 },
  listContent: { paddingVertical: 12 },
  separatorRow: { alignItems: 'center', marginVertical: 10 },
  separatorChip: {
    backgroundColor: 'rgba(61,43,31,0.15)',
    borderRadius: 12, paddingHorizontal: 14, paddingVertical: 4,
  },
  separatorText: { fontSize: 12, color: Colors.primary, fontWeight: '500' },

  txRow: { paddingHorizontal: 12, marginVertical: 3 },
  txRowSent: { alignItems: 'flex-end' },
  txRowReceived: { alignItems: 'flex-start' },

  hashtagBubble: {
    maxWidth: '80%', backgroundColor: Colors.surface, borderRadius: 16, padding: 14,
    borderWidth: 2, borderColor: Colors.accentLight,
    shadowColor: Colors.primary, shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1, shadowRadius: 6, elevation: 3,
  },
  hashtagHeader: { flexDirection: 'row-reverse', alignItems: 'center', gap: 6, marginBottom: 4 },
  hashtagTitle: { fontSize: 16, fontWeight: '700', color: Colors.accent },
  accountBadge: {
    backgroundColor: Colors.accentLight, borderRadius: 8,
    paddingHorizontal: 6, paddingVertical: 1,
  },
  accountBadgeText: { fontSize: 10, color: Colors.accentDark, fontWeight: '600' },
  hashtagHint: { fontSize: 12, color: Colors.textMuted, textAlign: 'right', marginBottom: 6 },
  metaRow: {
    flexDirection: 'row', alignItems: 'center',
    justifyContent: 'flex-end', gap: 3, marginTop: 2,
  },
  timeText: { fontSize: 11, color: Colors.textMuted },

  accountsBar: {
    flexDirection: 'row-reverse',
    alignItems: 'center', justifyContent: 'space-between',
    backgroundColor: Colors.surface, paddingHorizontal: 12, paddingVertical: 6,
    borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: Colors.border,
  },
  accountsBarLeft: { flexDirection: 'row-reverse', gap: 4, flex: 1 },
  accountChip: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 3,
    backgroundColor: Colors.accentLight, borderRadius: 10,
    paddingHorizontal: 6, paddingVertical: 3,
  },
  accountChipText: { fontSize: 10, color: Colors.accentDark, fontWeight: '600' },
  moreText: { fontSize: 11, color: Colors.textMuted, alignSelf: 'center' },

  quickReplies: {
    flexDirection: 'row-reverse', paddingHorizontal: 12, paddingVertical: 8, gap: 8,
    backgroundColor: Colors.surface,
    borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: Colors.border,
  },
  quickChip: {
    backgroundColor: Colors.surfaceAlt, borderRadius: 20,
    paddingHorizontal: 12, paddingVertical: 6, borderWidth: 1, borderColor: Colors.border,
  },
  quickChipText: { fontSize: 13, color: Colors.textPrimary },

  inputBar: {
    flexDirection: 'row-reverse', alignItems: 'flex-end',
    backgroundColor: Colors.surface, paddingHorizontal: 10, paddingTop: 8, gap: 8,
    borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: Colors.border,
  },
  accountBtn: {
    width: 42, height: 42, borderRadius: 21,
    backgroundColor: Colors.accentLight,
    alignItems: 'center', justifyContent: 'center',
  },
  accountBadgeCount: {
    position: 'absolute', top: -2, right: -2,
    minWidth: 18, height: 18, borderRadius: 9,
    backgroundColor: Colors.accent,
    alignItems: 'center', justifyContent: 'center',
  },
  accountBadgeCountText: { fontSize: 10, color: '#fff', fontWeight: '700' },
  calcBtn: {
    width: 42, height: 42, borderRadius: 21,
    backgroundColor: Colors.accentLight,
    alignItems: 'center', justifyContent: 'center',
  },
  accountantBtn: {
    width: 42, height: 42, borderRadius: 21,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center', justifyContent: 'center',
    borderWidth: 1, borderColor: Colors.accentDark + '40',
  },
  input: {
    flex: 1, backgroundColor: Colors.inputBg, borderRadius: 22,
    paddingHorizontal: 16, paddingVertical: 10, fontSize: 15,
    color: Colors.inputText, maxHeight: 120,
    borderWidth: 1, borderColor: Colors.inputBorder,
  },
  sendBtn: {
    width: 42, height: 42, borderRadius: 21, backgroundColor: Colors.accent,
    alignItems: 'center', justifyContent: 'center',
  },

  modalContainer: { flex: 1, backgroundColor: Colors.background },
  modalHeader: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between', backgroundColor: Colors.primary,
    paddingHorizontal: 16, paddingVertical: 14,
  },
  closeBtn: {
    width: 38, height: 38, borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.15)',
    alignItems: 'center', justifyContent: 'center',
  },
  modalTitle: { fontSize: 17, fontWeight: '700', color: Colors.textPrimary },
  addBtn: {
    width: 38, height: 38, borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.15)',
    alignItems: 'center', justifyContent: 'center',
  },

  createForm: {
    backgroundColor: Colors.surface, borderRadius: 14,
    padding: 16, margin: 12, borderWidth: 1, borderColor: Colors.accentLight,
  },
  createFormTitle: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary, marginBottom: 10, textAlign: 'right' },
  createInput: {
    backgroundColor: Colors.inputBg, borderRadius: 12,
    paddingHorizontal: 14, paddingVertical: 10, fontSize: 15,
    color: Colors.inputText, borderWidth: 1, borderColor: Colors.inputBorder, marginBottom: 8,
  },
  createFormActions: { flexDirection: 'row-reverse', gap: 10 },
  cancelCreateBtn: {
    flex: 1, backgroundColor: Colors.background, borderRadius: 10,
    paddingVertical: 10, alignItems: 'center', borderWidth: 1, borderColor: Colors.border,
  },
  cancelCreateText: { fontSize: 14, color: Colors.textSecondary },
  confirmCreateBtn: {
    flex: 1, backgroundColor: Colors.accent, borderRadius: 10,
    paddingVertical: 10, alignItems: 'center',
  },
  confirmCreateText: { fontSize: 14, color: '#fff', fontWeight: '700' },

  hashtagsList: { padding: 12 },
  hashtagsListTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, marginBottom: 10, textAlign: 'right' },
  emptyHashtags: { fontSize: 13, color: Colors.textMuted, textAlign: 'center', paddingVertical: 12 },
  hashtagItem: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 8,
    backgroundColor: Colors.surface, borderRadius: 12,
    paddingHorizontal: 14, paddingVertical: 12, marginBottom: 6,
    borderWidth: 1, borderColor: 'transparent',
  },
  hashtagItemSelected: { borderColor: Colors.accent, backgroundColor: Colors.accentLight + '30' },
  hashtagItemText: { flex: 1, fontSize: 15, fontWeight: '600', color: Colors.textPrimary },
  hashtagItemEntries: { fontSize: 11, color: Colors.textMuted },
  addHashtagBtn: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'center', gap: 6,
    backgroundColor: Colors.accentLight, borderRadius: 10,
    paddingVertical: 10, marginTop: 6,
  },
  addHashtagText: { fontSize: 14, fontWeight: '600', color: Colors.accentDark },
});
