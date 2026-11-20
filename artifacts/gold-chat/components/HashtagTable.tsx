import { MaterialIcons } from '@expo/vector-icons';
import { useState } from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Colors } from '../constants/colors';
import { ChatAccount, HashtagAccount } from '../types';
import { AccountSelector } from './AccountSelector';
import { getHashtagTotals, getEntryLabel, getEntryDirectionLabel } from '../utils/hashtag';
import { formatCurrency, formatShortDate } from '../utils/gold';

interface Props {
  account: ChatAccount;
  hashtag: HashtagAccount;
  contactName: string;
  allAccounts: ChatAccount[];
  visible: boolean;
  onClose: () => void;
  onAddInvoice: (description: string, amount: number, selectedAccountIds: string[]) => void;
  onAddPayment: (amount: number) => void;
  onSettleFull: () => void;
  onAddComment: (text: string) => void;
  onAddEntryComment: (entryId: string, text: string) => void;
}

type ActionMode = 'none' | 'invoice' | 'payment';

export function HashtagTable({
  account,
  hashtag,
  contactName,
  allAccounts,
  visible,
  onClose,
  onAddInvoice,
  onAddPayment,
  onSettleFull,
  onAddComment,
  onAddEntryComment,
}: Props) {
  const [actionMode, setActionMode] = useState<ActionMode>('none');
  const [invoiceDesc, setInvoiceDesc] = useState('');
  const [invoiceAmount, setInvoiceAmount] = useState('');
  const [paymentAmount, setPaymentAmount] = useState('');
  const [commentText, setCommentText] = useState('');
  const [selectedEntryId, setSelectedEntryId] = useState<string | null>(null);
  const [entryCommentText, setEntryCommentText] = useState('');
  const [selectedAccountIds, setSelectedAccountIds] = useState<string[]>([account.id]);

  const totals = getHashtagTotals(hashtag);
  const isSettled = hashtag.status === 'settled';
  const hasMultipleAccounts = allAccounts.length > 1;

  function handleInvoice() {
    const amt = parseFloat(invoiceAmount);
    if (!invoiceDesc.trim() || !amt || amt <= 0) return;
    onAddInvoice(invoiceDesc.trim(), amt, selectedAccountIds);
    setInvoiceDesc('');
    setInvoiceAmount('');
    setActionMode('none');
  }

  function handlePayment() {
    const amt = parseFloat(paymentAmount);
    if (!amt || amt <= 0) return;
    onAddPayment(amt);
    setPaymentAmount('');
    setActionMode('none');
  }

  function handleSettle() {
    onSettleFull();
    setActionMode('none');
  }

  function handleComment() {
    if (!commentText.trim()) return;
    onAddComment(commentText.trim());
    setCommentText('');
  }

  function handleEntryComment() {
    if (!entryCommentText.trim() || !selectedEntryId) return;
    onAddEntryComment(selectedEntryId, entryCommentText.trim());
    setEntryCommentText('');
  }

  function toggleAccountId(id: string) {
    setSelectedAccountIds(prev =>
      prev.includes(id) ? prev.filter(a => a !== id) : [...prev, id],
    );
  }

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable onPress={onClose} style={styles.closeBtn}>
            <MaterialIcons name="close" size={22} color={Colors.textPrimary} />
          </Pressable>
          <View style={styles.headerInfo}>
            <Text style={styles.title}>#{hashtag.tag}</Text>
            <Text style={styles.subtitle}>{account.name} — {contactName}</Text>
          </View>
          <View style={{ width: 38 }} />
        </View>

        <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 100 }}>
          {/* Summary card */}
          <View style={[styles.summaryCard, { borderLeftColor: totals.remaining > 0 ? Colors.error : Colors.profit }]}>
            <View style={styles.summaryLeft}>
              <Text style={styles.summaryLabel}>المبلغ المستحق</Text>
              <Text style={[styles.summaryAmount, { color: totals.remaining > 0 ? Colors.error : Colors.profit }]}>
                {formatCurrency(totals.remaining)}
              </Text>
              {isSettled && <Text style={styles.settledBadge}>✓ مُسدّد</Text>}
            </View>
            <View style={styles.summaryRight}>
              <View style={styles.summaryRow}>
                <Text style={styles.summarySmallLabel}>إجمالي المدين</Text>
                <Text style={styles.summarySmallValue}>{formatCurrency(totals.totalDebit)}</Text>
              </View>
              <View style={styles.summaryRow}>
                <Text style={styles.summarySmallLabel}>إجمالي الدائن</Text>
                <Text style={styles.summarySmallValue}>{formatCurrency(totals.totalCredit)}</Text>
              </View>
              <View style={styles.summaryRow}>
                <Text style={styles.summarySmallLabel}>عدد المعاملات</Text>
                <Text style={styles.summarySmallValue}>{hashtag.entries.length}</Text>
              </View>
            </View>
          </View>

          {/* Table */}
          <View style={styles.tableCard}>
            <View style={styles.tableHeader}>
              <Text style={[styles.tableHeaderCell, styles.colNum]}>#</Text>
              <Text style={[styles.tableHeaderCell, styles.colDate]}>التاريخ</Text>
              <Text style={[styles.tableHeaderCell, styles.colDesc]}>البيان</Text>
              <Text style={[styles.tableHeaderCell, styles.colAmount]}>مدين</Text>
              <Text style={[styles.tableHeaderCell, styles.colAmount]}>دائن</Text>
            </View>

            {hashtag.entries.length === 0 ? (
              <View style={styles.tableEmpty}>
                <MaterialIcons name="receipt-long" size={40} color={Colors.textMuted} />
                <Text style={styles.tableEmptyText}>لا توجد معاملات بعد</Text>
              </View>
            ) : (
              hashtag.entries.map((entry, i) => (
                <Pressable
                  key={entry.id}
                  style={[styles.tableRow, i % 2 === 0 ? styles.rowEven : styles.rowOdd]}
                  onPress={() => setSelectedEntryId(selectedEntryId === entry.id ? null : entry.id)}
                >
                  <Text style={[styles.tableCell, styles.colNum]}>{i + 1}</Text>
                  <Text style={[styles.tableCell, styles.colDate]}>{formatShortDate(entry.createdAt)}</Text>
                  <Text style={[styles.tableCell, styles.colDesc]}>{entry.description}</Text>
                  <Text style={[styles.tableCell, styles.colAmount, { color: Colors.error }]}>
                    {entry.direction === 'debit' ? formatCurrency(entry.amount) : '—'}
                  </Text>
                  <Text style={[styles.tableCell, styles.colAmount, { color: Colors.profit }]}>
                    {entry.direction === 'credit' ? formatCurrency(entry.amount) : '—'}
                  </Text>
                </Pressable>
              ))
            )}

            {hashtag.entries.length > 0 && (
              <View style={styles.tableTotalRow}>
                <Text style={[styles.tableCell, styles.colNum]} />
                <Text style={[styles.tableCell, styles.colDate]} />
                <Text style={[styles.tableTotalCell, styles.colDesc]}>الإجمالي</Text>
                <Text style={[styles.tableTotalCell, styles.colAmount, { color: Colors.error }]}>
                  {formatCurrency(totals.totalDebit)}
                </Text>
                <Text style={[styles.tableTotalCell, styles.colAmount, { color: Colors.profit }]}>
                  {formatCurrency(totals.totalCredit)}
                </Text>
              </View>
            )}
          </View>

          {/* Selected entry details */}
          {selectedEntryId && (() => {
            const entry = hashtag.entries.find(e => e.id === selectedEntryId);
            if (!entry) return null;
            return (
              <View style={styles.entryDetailCard}>
                <View style={styles.entryDetailHeader}>
                  <Text style={styles.entryDetailTitle}>{getEntryLabel(entry)} — {entry.description}</Text>
                  <Pressable onPress={() => setSelectedEntryId(null)}>
                    <MaterialIcons name="close" size={18} color={Colors.textMuted} />
                  </Pressable>
                </View>
                <View style={styles.entryDetailInfo}>
                  <Text style={styles.entryDetailAmount}>{formatCurrency(entry.amount)}</Text>
                  <Text style={styles.entryDetailDir}>{getEntryDirectionLabel(entry.direction)}</Text>
                  <Text style={styles.entryDetailDate}>{formatShortDate(entry.createdAt)}</Text>
                </View>
                {/* Linked accounts */}
                {entry.accountIds && entry.accountIds.length > 1 && (
                  <View style={styles.linkedAccountsRow}>
                    <MaterialIcons name="link" size={12} color={Colors.info} />
                    <Text style={styles.linkedAccountsText}>
                      مرتبط بـ {entry.accountIds.length} حسابات
                    </Text>
                  </View>
                )}
                {/* Comments */}
                {entry.comments.length > 0 && (
                  <View style={styles.commentsSection}>
                    <Text style={styles.commentsTitle}>💬 تعليقات ({entry.comments.length})</Text>
                    {entry.comments.map(c => (
                      <View key={c.id} style={styles.commentRow}>
                        <Text style={styles.commentText}>{c.text}</Text>
                        <Text style={styles.commentDate}>{formatShortDate(c.createdAt)}</Text>
                      </View>
                    ))}
                  </View>
                )}
                <View style={styles.commentInputRow}>
                  <TextInput
                    style={styles.commentInput}
                    value={entryCommentText}
                    onChangeText={setEntryCommentText}
                    placeholder="أضف تعليقاً..."
                    placeholderTextColor={Colors.placeholder}
                    textAlign="right"
                  />
                  <Pressable style={styles.commentSendBtn} onPress={handleEntryComment}>
                    <MaterialIcons name="send" size={18} color="#fff" />
                  </Pressable>
                </View>
              </View>
            );
          })()}

          {/* Account comments */}
          {hashtag.comments.length > 0 && (
            <View style={styles.commentsCard}>
              <Text style={styles.commentsTitle}>💬 تعليقات ({hashtag.comments.length})</Text>
              {hashtag.comments.map(c => (
                <View key={c.id} style={styles.commentRow}>
                  <Text style={styles.commentText}>{c.text}</Text>
                  <Text style={styles.commentDate}>{formatShortDate(c.createdAt)}</Text>
                </View>
              ))}
            </View>
          )}

          <View style={styles.commentInputRow}>
            <TextInput
              style={styles.commentInput}
              value={commentText}
              onChangeText={setCommentText}
              placeholder="أضف تعليقاً على الحساب..."
              placeholderTextColor={Colors.placeholder}
              textAlign="right"
            />
            <Pressable style={styles.commentSendBtn} onPress={handleComment}>
              <MaterialIcons name="send" size={18} color="#fff" />
            </Pressable>
          </View>
        </ScrollView>

        {/* Action buttons */}
        {!isSettled && actionMode === 'none' && (
          <View style={styles.actionsBar}>
            <Pressable style={[styles.actionBtn, styles.invoiceBtn]} onPress={() => setActionMode('invoice')}>
              <MaterialIcons name="receipt" size={18} color="#fff" />
              <Text style={styles.actionBtnText}>فاتورة</Text>
            </Pressable>
            <Pressable style={[styles.actionBtn, styles.payBtn]} onPress={() => setActionMode('payment')}>
              <MaterialIcons name="payments" size={18} color={Colors.profit} />
              <Text style={[styles.actionBtnText, { color: Colors.profit }]}>دفع جزء</Text>
            </Pressable>
            <Pressable
              style={[styles.actionBtn, styles.settleBtn]}
              onPress={handleSettle}
              disabled={totals.remaining <= 0}
            >
              <MaterialIcons name="check-circle" size={18} color="#fff" />
              <Text style={styles.actionBtnText}>سداد كامل</Text>
            </Pressable>
          </View>
        )}

        {/* Invoice form with multi-account selector */}
        {actionMode === 'invoice' && (
          <ScrollView style={styles.formSheet} contentContainerStyle={{ paddingBottom: 40 }}>
            <Text style={styles.formTitle}>إنشاء فاتورة (مدين)</Text>
            <TextInput
              style={styles.formInput}
              value={invoiceDesc}
              onChangeText={setInvoiceDesc}
              placeholder="البيان (مثال: شراء 10m³ رمل)"
              placeholderTextColor={Colors.placeholder}
              textAlign="right"
            />
            <TextInput
              style={styles.formInput}
              value={invoiceAmount}
              onChangeText={setInvoiceAmount}
              placeholder="المبلغ"
              placeholderTextColor={Colors.placeholder}
              keyboardType="decimal-pad"
              textAlign="right"
            />
            {hasMultipleAccounts && (
              <View style={styles.multiAccountSection}>
                <Text style={styles.multiAccountLabel}>الفاتورة تشمل حسابات:</Text>
                <AccountSelector
                  accounts={allAccounts}
                  selectedIds={selectedAccountIds}
                  onToggle={toggleAccountId}
                  onSelectAll={() =>
                    setSelectedAccountIds(
                      selectedAccountIds.length === allAccounts.length ? [account.id] : allAccounts.map(a => a.id),
                    )
                  }
                />
              </View>
            )}
            <View style={styles.formActions}>
              <Pressable style={styles.formCancelBtn} onPress={() => setActionMode('none')}>
                <Text style={styles.formCancelText}>إلغاء</Text>
              </Pressable>
              <Pressable style={styles.formSubmitBtn} onPress={handleInvoice}>
                <Text style={styles.formSubmitText}>تأكيد</Text>
              </Pressable>
            </View>
          </ScrollView>
        )}

        {/* Payment form */}
        {actionMode === 'payment' && (
          <View style={styles.formSheet}>
            <Text style={styles.formTitle}>تسجيل دفعة (دائن)</Text>
            <Text style={styles.formHint}>المبلغ المتبقي: {formatCurrency(totals.remaining)}</Text>
            <TextInput
              style={styles.formInput}
              value={paymentAmount}
              onChangeText={setPaymentAmount}
              placeholder="المبلغ المدفوع"
              placeholderTextColor={Colors.placeholder}
              keyboardType="decimal-pad"
              textAlign="right"
            />
            <View style={styles.formActions}>
              <Pressable style={styles.formCancelBtn} onPress={() => setActionMode('none')}>
                <Text style={styles.formCancelText}>إلغاء</Text>
              </Pressable>
              <Pressable style={styles.formSubmitBtn} onPress={handlePayment}>
                <Text style={styles.formSubmitText}>تأكيد</Text>
              </Pressable>
            </View>
          </View>
        )}

        <View style={styles.immutableNotice}>
          <MaterialIcons name="lock" size={12} color={Colors.textMuted} />
          <Text style={styles.immutableText}>سجلات دائمة — لا يمكن الحذف أو التعديل</Text>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  header: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between', backgroundColor: Colors.primary,
    paddingHorizontal: 16, paddingVertical: 14,
  },
  closeBtn: {
    width: 38, height: 38, borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.15)',
    alignItems: 'center', justifyContent: 'center',
  },
  headerInfo: { flex: 1, alignItems: 'center' },
  title: { fontSize: 18, fontWeight: '700', color: Colors.accent },
  subtitle: { fontSize: 12, color: 'rgba(11,15,20,0.6)', marginTop: 2 },

  scroll: { flex: 1, padding: 12 },

  summaryCard: {
    flexDirection: 'row-reverse', backgroundColor: Colors.surface,
    borderRadius: 14, borderLeftWidth: 4, padding: 14, marginBottom: 12,
    shadowColor: Colors.primary, shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06, shadowRadius: 4, elevation: 2,
  },
  summaryLeft: { flex: 1, alignItems: 'flex-end' },
  summaryLabel: { fontSize: 12, color: Colors.textMuted, marginBottom: 4 },
  summaryAmount: { fontSize: 24, fontWeight: '700' },
  settledBadge: {
    fontSize: 12, color: Colors.profit, fontWeight: '700',
    backgroundColor: Colors.profitBg, borderRadius: 8,
    paddingHorizontal: 8, paddingVertical: 2, marginTop: 6,
  },
  summaryRight: { gap: 4, minWidth: 140 },
  summaryRow: { flexDirection: 'row-reverse', justifyContent: 'space-between' },
  summarySmallLabel: { fontSize: 11, color: Colors.textMuted },
  summarySmallValue: { fontSize: 12, fontWeight: '600', color: Colors.textPrimary },

  tableCard: {
    backgroundColor: Colors.surface, borderRadius: 12,
    overflow: 'hidden', marginBottom: 12,
  },
  tableHeader: {
    flexDirection: 'row-reverse', backgroundColor: Colors.primary,
    paddingVertical: 10, paddingHorizontal: 8,
  },
  tableHeaderCell: { fontSize: 11, fontWeight: '700', color: Colors.accent, textAlign: 'center' },
  tableRow: {
    flexDirection: 'row-reverse', paddingVertical: 10, paddingHorizontal: 8,
    borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: Colors.border,
  },
  rowEven: { backgroundColor: Colors.surface },
  rowOdd: { backgroundColor: Colors.background },
  tableCell: { fontSize: 12, color: Colors.textSecondary, textAlign: 'center' },
  colNum: { width: 30 },
  colDate: { width: 70 },
  colDesc: { flex: 1, textAlign: 'right' },
  colAmount: { width: 80 },

  tableTotalRow: {
    flexDirection: 'row-reverse', backgroundColor: Colors.surfaceAlt,
    paddingVertical: 10, paddingHorizontal: 8,
    borderTopWidth: 2, borderTopColor: Colors.border,
  },
  tableTotalCell: { fontSize: 13, fontWeight: '700', color: Colors.textPrimary, textAlign: 'center' },
  tableEmpty: { paddingVertical: 40, alignItems: 'center', gap: 8 },
  tableEmptyText: { fontSize: 14, color: Colors.textMuted },

  entryDetailCard: {
    backgroundColor: Colors.surface, borderRadius: 12, padding: 14, marginBottom: 12,
    borderWidth: 1, borderColor: Colors.accentLight,
  },
  entryDetailHeader: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between', marginBottom: 8,
  },
  entryDetailTitle: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary, flex: 1, textAlign: 'right' },
  entryDetailInfo: { flexDirection: 'row-reverse', alignItems: 'center', gap: 10, marginBottom: 8 },
  entryDetailAmount: { fontSize: 16, fontWeight: '700', color: Colors.accent },
  entryDetailDir: { fontSize: 12, color: Colors.textSecondary, backgroundColor: Colors.surfaceAlt, paddingHorizontal: 8, paddingVertical: 2, borderRadius: 8 },
  entryDetailDate: { fontSize: 11, color: Colors.textMuted },
  linkedAccountsRow: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 4, marginBottom: 8,
  },
  linkedAccountsText: { fontSize: 11, color: Colors.info },

  commentsSection: { backgroundColor: Colors.background, borderRadius: 8, padding: 10, marginBottom: 8 },
  commentsCard: { backgroundColor: Colors.surface, borderRadius: 12, padding: 14, marginBottom: 8 },
  commentsTitle: { fontSize: 13, fontWeight: '700', color: Colors.textPrimary, marginBottom: 8, textAlign: 'right' },
  commentRow: {
    flexDirection: 'row-reverse', justifyContent: 'space-between',
    paddingVertical: 6, borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  commentText: { fontSize: 13, color: Colors.textSecondary, flex: 1, textAlign: 'right' },
  commentDate: { fontSize: 10, color: Colors.textMuted, marginLeft: 8 },
  commentInputRow: { flexDirection: 'row-reverse', alignItems: 'center', gap: 8, marginBottom: 8 },
  commentInput: {
    flex: 1, backgroundColor: Colors.inputBg, borderRadius: 20,
    paddingHorizontal: 14, paddingVertical: 8, fontSize: 13,
    color: Colors.inputText, borderWidth: 1, borderColor: Colors.inputBorder,
  },
  commentSendBtn: {
    width: 36, height: 36, borderRadius: 18,
    backgroundColor: Colors.accent, alignItems: 'center', justifyContent: 'center',
  },

  actionsBar: {
    flexDirection: 'row-reverse', gap: 8,
    paddingHorizontal: 12, paddingVertical: 10,
    backgroundColor: Colors.surface,
    borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: Colors.border,
  },
  actionBtn: {
    flex: 1, flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'center', gap: 6, borderRadius: 12, paddingVertical: 12,
  },
  invoiceBtn: { backgroundColor: Colors.accent },
  payBtn: { backgroundColor: Colors.profitBg, borderWidth: 1, borderColor: Colors.profit },
  settleBtn: { backgroundColor: Colors.profit },
  actionBtnText: { fontSize: 13, fontWeight: '700', color: '#E6EDF3' },

  formSheet: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, paddingBottom: 40,
    borderTopWidth: 1, borderTopColor: Colors.border,
  },
  formTitle: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary, marginBottom: 12, textAlign: 'right' },
  formHint: { fontSize: 13, color: Colors.textSecondary, marginBottom: 10, textAlign: 'right' },
  formInput: {
    backgroundColor: Colors.inputBg, borderRadius: 12,
    paddingHorizontal: 14, paddingVertical: 12, fontSize: 15,
    color: Colors.inputText, borderWidth: 1, borderColor: Colors.inputBorder,
    marginBottom: 10,
  },
  multiAccountSection: { marginBottom: 10 },
  multiAccountLabel: { fontSize: 13, fontWeight: '600', color: Colors.textSecondary, marginBottom: 6, textAlign: 'right' },
  formActions: { flexDirection: 'row-reverse', gap: 10 },
  formCancelBtn: {
    flex: 1, backgroundColor: Colors.background, borderRadius: 12,
    paddingVertical: 12, alignItems: 'center', borderWidth: 1, borderColor: Colors.border,
  },
  formCancelText: { fontSize: 14, fontWeight: '600', color: Colors.textSecondary },
  formSubmitBtn: {
    flex: 1, backgroundColor: Colors.accent, borderRadius: 12,
    paddingVertical: 12, alignItems: 'center',
  },
  formSubmitText: { fontSize: 14, fontWeight: '700', color: '#E6EDF3' },

  immutableNotice: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'center', gap: 6, paddingVertical: 8,
    backgroundColor: Colors.background,
  },
  immutableText: { fontSize: 11, color: Colors.textMuted },
});
