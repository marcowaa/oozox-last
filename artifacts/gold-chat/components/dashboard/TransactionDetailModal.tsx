import React from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';
import { TransactionItem } from '../../types/dashboard';

interface TransactionDetailModalProps {
  transaction: TransactionItem | null;
  visible: boolean;
  onClose: () => void;
  isDarkMode: boolean;
}

export const TransactionDetailModal: React.FC<TransactionDetailModalProps> = ({
  transaction,
  visible,
  onClose,
  isDarkMode,
}) => {
  if (!transaction) return null;

  const formatCurrency = (amount: number, currency: string = 'SAR'): string => {
    return new Intl.NumberFormat('ar-SA', {
      style: 'currency',
      currency: currency || 'SAR',
    }).format(amount);
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'completed':
        return { label: 'مكتملة', bg: 'rgba(16, 185, 129, 0.15)', text: '#10B981', border: '#10B981' };
      case 'pending':
        return { label: 'قيد المعالجة', bg: 'rgba(245, 158, 11, 0.15)', text: '#F59E0B', border: '#F59E0B' };
      case 'refunded':
        return { label: 'مسترجعة', bg: 'rgba(139, 92, 246, 0.15)', text: '#8B5CF6', border: '#8B5CF6' };
      default:
        return { label: 'ملغاة / فاشلة', bg: 'rgba(239, 68, 68, 0.15)', text: '#EF4444', border: '#EF4444' };
    }
  };

  const statusInfo = getStatusBadge(transaction.status);

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={[styles.modalContent, isDarkMode ? styles.contentDark : styles.contentLight]}>
          {/* Header */}
          <View style={styles.modalHeader}>
            <View>
              <Text style={[styles.modalTitle, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                تفاصيل المعاملة المالية
              </Text>
              <Text style={[styles.referenceText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                رقم المرجع: {transaction.referenceNumber || transaction.id}
              </Text>
            </View>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <Ionicons name="close" size={24} color={isDarkMode ? '#94A3B8' : '#64748B'} />
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalBody} showsVerticalScrollIndicator={false}>
            {/* Amount Banner */}
            <View style={[styles.amountBanner, { borderColor: statusInfo.border }]}>
              <Text style={styles.amountLabel}>المبلغ الإجمالي</Text>
              <Text style={styles.amountValue}>
                {formatCurrency(transaction.amount, transaction.currency)}
              </Text>
              <View style={[styles.statusPill, { backgroundColor: statusInfo.bg }]}>
                <Text style={[styles.statusPillText, { color: statusInfo.text }]}>
                  {statusInfo.label}
                </Text>
              </View>
            </View>

            {/* Information Grid */}
            <View style={styles.infoSection}>
              <View style={styles.infoRow}>
                <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                  العميل / المستفيد
                </Text>
                <Text style={[styles.infoValue, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                  {transaction.customerName}
                </Text>
              </View>

              {transaction.customerEmail && (
                <View style={styles.infoRow}>
                  <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                    البريد الإلكتروني
                  </Text>
                  <Text style={[styles.infoValue, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                    {transaction.customerEmail}
                  </Text>
                </View>
              )}

              <View style={styles.infoRow}>
                <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                  طريقة الدفع
                </Text>
                <Text style={[styles.infoValue, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                  {transaction.paymentMethod || 'تحويل بنكي / بطاقة'}
                </Text>
              </View>

              <View style={styles.infoRow}>
                <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                  تصنيف المعاملة
                </Text>
                <Text style={[styles.infoValue, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                  {transaction.category || 'عام'}
                </Text>
              </View>

              <View style={styles.infoRow}>
                <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                  تاريخ الإنشاء
                </Text>
                <Text style={[styles.infoValue, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                  {new Date(transaction.createdAt).toLocaleString('ar-SA')}
                </Text>
              </View>

              {transaction.details?.fees !== undefined && (
                <View style={styles.infoRow}>
                  <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                    الرسوم المقتطعة
                  </Text>
                  <Text style={[styles.infoValue, { color: '#EF4444' }]}>
                    - {formatCurrency(transaction.details.fees, transaction.currency)}
                  </Text>
                </View>
              )}

              {transaction.details?.netAmount !== undefined && (
                <View style={styles.infoRow}>
                  <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                    صافي الاستلام
                  </Text>
                  <Text style={[styles.infoValue, { color: '#10B981', fontWeight: '700' }]}>
                    {formatCurrency(transaction.details.netAmount, transaction.currency)}
                  </Text>
                </View>
              )}

              {transaction.details?.notes && (
                <View style={[styles.infoRow, { flexDirection: 'column', alignItems: 'flex-start', gap: 4 }]}>
                  <Text style={[styles.infoLabel, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                    ملاحظات إضافية
                  </Text>
                  <Text style={[styles.notesBox, isDarkMode ? styles.notesDark : styles.notesLight]}>
                    {transaction.details.notes}
                  </Text>
                </View>
              )}
            </View>
          </ScrollView>

          {/* Footer Actions */}
          <View style={styles.modalFooter}>
            <TouchableOpacity style={styles.dismissButton} onPress={onClose}>
              <Text style={styles.dismissButtonText}>إغلاق النافذة</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.lg,
  },
  modalContent: {
    width: '100%',
    maxWidth: 550,
    maxHeight: '90%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    overflow: 'hidden',
  },
  contentDark: {
    backgroundColor: '#0F172A',
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  contentLight: {
    backgroundColor: '#FFFFFF',
    borderColor: '#CBD5E1',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: Spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.08)',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  referenceText: {
    fontSize: 12,
    marginTop: 2,
    fontFamily: 'monospace',
  },
  closeButton: {
    padding: Spacing.xs,
  },
  modalBody: {
    padding: Spacing.lg,
  },
  amountBanner: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: BorderRadius.lg,
    padding: Spacing.lg,
    alignItems: 'center',
    borderWidth: 1,
    marginBottom: Spacing.lg,
    gap: Spacing.xs,
  },
  amountLabel: {
    fontSize: 12,
    color: '#94A3B8',
  },
  amountValue: {
    fontSize: 28,
    fontWeight: '800',
    color: '#F8FAFC',
    letterSpacing: -0.5,
  },
  statusPill: {
    paddingHorizontal: Spacing.md,
    paddingVertical: 4,
    borderRadius: BorderRadius.full,
    marginTop: Spacing.xs,
  },
  statusPillText: {
    fontSize: 12,
    fontWeight: '700',
  },
  infoSection: {
    gap: Spacing.md,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.xs,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.04)',
  },
  infoLabel: {
    fontSize: 13,
  },
  infoValue: {
    fontSize: 14,
    fontWeight: '600',
  },
  notesBox: {
    fontSize: 13,
    padding: Spacing.sm,
    borderRadius: BorderRadius.sm,
    width: '100%',
  },
  notesDark: {
    backgroundColor: '#1E293B',
    color: '#E2E8F0',
  },
  notesLight: {
    backgroundColor: '#F1F5F9',
    color: '#1E293B',
  },
  modalFooter: {
    padding: Spacing.lg,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
    alignItems: 'flex-end',
  },
  dismissButton: {
    backgroundColor: '#3B82F6',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.sm,
    minHeight: 44,
    justifyContent: 'center',
    alignItems: 'center',
  },
  dismissButtonText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 14,
  },
  textPrimaryDark: {
    color: '#F8FAFC',
  },
  textPrimaryLight: {
    color: '#0F172A',
  },
  textMutedDark: {
    color: '#94A3B8',
  },
  textMutedLight: {
    color: '#64748B',
  },
});
