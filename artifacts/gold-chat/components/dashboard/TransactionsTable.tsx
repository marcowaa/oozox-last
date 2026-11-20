import React, { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';
import { TransactionItem, PaginationMeta, TransactionFilterParams, TransactionStatus } from '../../types/dashboard';

interface TransactionsTableProps {
  transactions: TransactionItem[];
  pagination: PaginationMeta;
  filterParams: TransactionFilterParams;
  isLoading: boolean;
  onSearch: (query: string) => void;
  onPageChange: (page: number) => void;
  onSortChange: (sortBy: TransactionFilterParams['sortBy'], sortOrder: TransactionFilterParams['sortOrder']) => void;
  onSelectTransaction: (item: TransactionItem) => void;
  isDarkMode: boolean;
}

export const TransactionsTable: React.FC<TransactionsTableProps> = ({
  transactions,
  pagination,
  filterParams,
  isLoading,
  onSearch,
  onPageChange,
  onSortChange,
  onSelectTransaction,
  isDarkMode,
}) => {
  const [localSearch, setLocalSearch] = useState(filterParams.search || '');

  const handleSearchSubmit = () => {
    onSearch(localSearch);
  };

  const handleSortClick = (column: TransactionFilterParams['sortBy']) => {
    if (filterParams.sortBy === column) {
      const nextOrder = filterParams.sortOrder === 'asc' ? 'desc' : 'asc';
      onSortChange(column, nextOrder);
    } else {
      onSortChange(column, 'desc');
    }
  };

  const formatCurrency = (amount: number, currency: string = 'SAR'): string => {
    return new Intl.NumberFormat('ar-SA', {
      style: 'currency',
      currency: currency || 'SAR',
    }).format(amount);
  };

  const getStatusBadge = (status: TransactionStatus) => {
    switch (status) {
      case 'completed':
        return { label: 'مكتملة', bg: 'rgba(16, 185, 129, 0.12)', text: '#10B981' };
      case 'pending':
        return { label: 'قيد الانتظار', bg: 'rgba(245, 158, 11, 0.12)', text: '#F59E0B' };
      case 'refunded':
        return { label: 'مسترجعة', bg: 'rgba(139, 92, 246, 0.12)', text: '#8B5CF6' };
      default:
        return { label: 'فاشلة', bg: 'rgba(239, 68, 68, 0.12)', text: '#EF4444' };
    }
  };

  return (
    <View style={[styles.tableContainer, isDarkMode ? styles.tableDark : styles.tableLight]}>
      {/* Table Top Controls: Title & Search Field */}
      <View style={styles.topControls}>
        <View>
          <Text style={[styles.titleText, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
            سجل المعاملات والعمليات
          </Text>
          <Text style={[styles.subtitleText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
            إجمالي السجلات: {pagination.totalItems} معاملة
          </Text>
        </View>

        {/* Real-time Server Search Bar */}
        <View style={[styles.searchBar, isDarkMode ? styles.searchDark : styles.searchLight]}>
          <Ionicons name="search-outline" size={18} color={isDarkMode ? '#94A3B8' : '#64748B'} />
          <TextInput
            style={[styles.searchInput, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}
            placeholder="بحث بالاسم أو المرجع..."
            placeholderTextColor={isDarkMode ? '#64748B' : '#94A3B8'}
            value={localSearch}
            onChangeText={setLocalSearch}
            onSubmitEditing={handleSearchSubmit}
            returnKeyType="search"
          />
          {localSearch.length > 0 && (
            <TouchableOpacity
              onPress={() => {
                setLocalSearch('');
                onSearch('');
              }}
            >
              <Ionicons name="close-circle" size={16} color={isDarkMode ? '#94A3B8' : '#64748B'} />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* Table Content Area */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.scrollWrapper}>
        <View style={styles.tableGrid}>
          {/* Table Header Row */}
          <View style={[styles.headerRow, isDarkMode ? styles.headerDark : styles.headerLight]}>
            <TouchableOpacity
              style={[styles.headerCell, { width: 140 }]}
              onPress={() => handleSortClick('createdAt')}
            >
              <Text style={[styles.headerCellText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                التاريخ
              </Text>
              {filterParams.sortBy === 'createdAt' && (
                <Ionicons
                  name={filterParams.sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'}
                  size={12}
                  color="#3B82F6"
                />
              )}
            </TouchableOpacity>

            <View style={[styles.headerCell, { width: 130 }]}>
              <Text style={[styles.headerCellText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                رقم المرجع
              </Text>
            </View>

            <TouchableOpacity
              style={[styles.headerCell, { width: 180 }]}
              onPress={() => handleSortClick('customerName')}
            >
              <Text style={[styles.headerCellText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                العميل
              </Text>
              {filterParams.sortBy === 'customerName' && (
                <Ionicons
                  name={filterParams.sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'}
                  size={12}
                  color="#3B82F6"
                />
              )}
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.headerCell, { width: 140 }]}
              onPress={() => handleSortClick('amount')}
            >
              <Text style={[styles.headerCellText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                المبلغ
              </Text>
              {filterParams.sortBy === 'amount' && (
                <Ionicons
                  name={filterParams.sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'}
                  size={12}
                  color="#3B82F6"
                />
              )}
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.headerCell, { width: 120 }]}
              onPress={() => handleSortClick('status')}
            >
              <Text style={[styles.headerCellText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                الحالة
              </Text>
              {filterParams.sortBy === 'status' && (
                <Ionicons
                  name={filterParams.sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'}
                  size={12}
                  color="#3B82F6"
                />
              )}
            </TouchableOpacity>

            <View style={[styles.headerCell, { width: 120, justifyContent: 'center' }]}>
              <Text style={[styles.headerCellText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                الإجراءات
              </Text>
            </View>
          </View>

          {/* Table Data Rows */}
          {transactions.length === 0 ? (
            <View style={styles.emptyRow}>
              <Text style={isDarkMode ? styles.textMutedDark : styles.textMutedLight}>
                {isLoading ? 'جاري تحميل المعاملات من الخادم...' : 'لا توجد معاملات مسجلة مطابقة للبحث'}
              </Text>
            </View>
          ) : (
            transactions.map((item, index) => {
              const statusBadge = getStatusBadge(item.status);
              return (
                <View
                  key={item.id || index}
                  style={[
                    styles.dataRow,
                    isDarkMode ? styles.rowDark : styles.rowLight,
                    index % 2 === 1 && (isDarkMode ? styles.rowStripeDark : styles.rowStripeLight),
                  ]}
                >
                  <Text
                    style={[
                      styles.cellText,
                      { width: 140 },
                      isDarkMode ? styles.textMutedDark : styles.textMutedLight,
                    ]}
                  >
                    {new Date(item.createdAt).toLocaleDateString('ar-SA')}
                  </Text>

                  <Text
                    style={[
                      styles.cellText,
                      { width: 130, fontFamily: 'monospace' },
                      isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight,
                    ]}
                    numberOfLines={1}
                  >
                    {item.referenceNumber || item.id.substring(0, 8)}
                  </Text>

                  <Text
                    style={[
                      styles.cellText,
                      { width: 180, fontWeight: '600' },
                      isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight,
                    ]}
                    numberOfLines={1}
                  >
                    {item.customerName}
                  </Text>

                  <Text
                    style={[
                      styles.cellText,
                      { width: 140, fontWeight: '700' },
                      isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight,
                    ]}
                  >
                    {formatCurrency(item.amount, item.currency)}
                  </Text>

                  <View style={{ width: 120 }}>
                    <View style={[styles.statusBadge, { backgroundColor: statusBadge.bg }]}>
                      <Text style={[styles.statusBadgeText, { color: statusBadge.text }]}>
                        {statusBadge.label}
                      </Text>
                    </View>
                  </View>

                  <View style={{ width: 120, alignItems: 'center' }}>
                    <TouchableOpacity
                      style={styles.actionBtn}
                      onPress={() => onSelectTransaction(item)}
                      activeOpacity={0.7}
                    >
                      <Text style={styles.actionBtnText}>التفاصيل</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              );
            })
          )}
        </View>
      </ScrollView>

      {/* Pagination Bar */}
      <View style={styles.paginationBar}>
        <Text style={[styles.pageInfoText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
          الصفحة {pagination.page} من {pagination.totalPages || 1}
        </Text>

        <View style={styles.paginationControls}>
          <TouchableOpacity
            style={[
              styles.pageBtn,
              !pagination.hasPrevPage && styles.pageBtnDisabled,
              isDarkMode ? styles.pageBtnDark : styles.pageBtnLight,
            ]}
            disabled={!pagination.hasPrevPage || isLoading}
            onPress={() => onPageChange(pagination.page - 1)}
          >
            <Ionicons
              name="chevron-forward"
              size={16}
              color={!pagination.hasPrevPage ? '#64748B' : isDarkMode ? '#F8FAFC' : '#0F172A'}
            />
            <Text
              style={[
                styles.pageBtnText,
                !pagination.hasPrevPage ? { color: '#64748B' } : isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight,
              ]}
            >
              السابق
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.pageBtn,
              !pagination.hasNextPage && styles.pageBtnDisabled,
              isDarkMode ? styles.pageBtnDark : styles.pageBtnLight,
            ]}
            disabled={!pagination.hasNextPage || isLoading}
            onPress={() => onPageChange(pagination.page + 1)}
          >
            <Text
              style={[
                styles.pageBtnText,
                !pagination.hasNextPage ? { color: '#64748B' } : isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight,
              ]}
            >
              التالي
            </Text>
            <Ionicons
              name="chevron-back"
              size={16}
              color={!pagination.hasNextPage ? '#64748B' : isDarkMode ? '#F8FAFC' : '#0F172A'}
            />
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  tableContainer: {
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.lg,
    marginBottom: Spacing.xl,
  },
  tableDark: {
    backgroundColor: '#0F172A',
    borderColor: 'rgba(255,255,255,0.08)',
  },
  tableLight: {
    backgroundColor: '#FFFFFF',
    borderColor: '#E2E8F0',
  },
  topControls: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
    flexWrap: 'wrap',
    gap: Spacing.md,
  },
  titleText: {
    fontSize: 16,
    fontWeight: '700',
  },
  subtitleText: {
    fontSize: 12,
    marginTop: 2,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.sm,
    height: 40,
    borderRadius: BorderRadius.sm,
    borderWidth: 1,
    minWidth: 260,
    gap: Spacing.xs,
  },
  searchDark: {
    backgroundColor: '#1E293B',
    borderColor: 'rgba(255,255,255,0.1)',
  },
  searchLight: {
    backgroundColor: '#F8FAFC',
    borderColor: '#CBD5E1',
  },
  searchInput: {
    flex: 1,
    fontSize: 13,
    paddingVertical: 0,
  },
  scrollWrapper: {
    marginVertical: Spacing.sm,
  },
  tableGrid: {
    minWidth: 840,
  },
  headerRow: {
    flexDirection: 'row',
    paddingVertical: Spacing.sm,
    paddingHorizontal: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.08)',
  },
  headerDark: {
    backgroundColor: 'rgba(255,255,255,0.02)',
  },
  headerLight: {
    backgroundColor: '#F1F5F9',
  },
  headerCell: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  headerCellText: {
    fontSize: 12,
    fontWeight: '700',
  },
  dataRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    paddingHorizontal: Spacing.sm,
    borderBottomWidth: 1,
  },
  rowDark: {
    borderBottomColor: 'rgba(255,255,255,0.04)',
  },
  rowLight: {
    borderBottomColor: '#F1F5F9',
  },
  rowStripeDark: {
    backgroundColor: 'rgba(255,255,255,0.01)',
  },
  rowStripeLight: {
    backgroundColor: '#FAFAFA',
  },
  cellText: {
    fontSize: 13,
  },
  statusBadge: {
    paddingHorizontal: Spacing.xs,
    paddingVertical: 3,
    borderRadius: BorderRadius.xs,
    alignSelf: 'flex-start',
  },
  statusBadgeText: {
    fontSize: 11,
    fontWeight: '700',
  },
  actionBtn: {
    backgroundColor: 'rgba(59, 130, 246, 0.12)',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
    borderRadius: BorderRadius.xs,
  },
  actionBtnText: {
    color: '#3B82F6',
    fontSize: 12,
    fontWeight: '600',
  },
  emptyRow: {
    paddingVertical: Spacing.xl,
    alignItems: 'center',
  },
  paginationBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.md,
    paddingTop: Spacing.sm,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.06)',
  },
  pageInfoText: {
    fontSize: 12,
  },
  paginationControls: {
    flexDirection: 'row',
    gap: Spacing.xs,
  },
  pageBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xxs,
    borderRadius: BorderRadius.xs,
    borderWidth: 1,
    minHeight: 32,
  },
  pageBtnDark: {
    backgroundColor: '#1E293B',
    borderColor: 'rgba(255,255,255,0.1)',
  },
  pageBtnLight: {
    backgroundColor: '#F8FAFC',
    borderColor: '#CBD5E1',
  },
  pageBtnDisabled: {
    opacity: 0.5,
  },
  pageBtnText: {
    fontSize: 12,
    fontWeight: '600',
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
