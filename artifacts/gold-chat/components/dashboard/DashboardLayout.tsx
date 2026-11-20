import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { Spacing } from '../../../constants/DesignSystem';
import { useDashboardStore } from '../../store/useDashboardStore';
import { Navbar } from './Navbar';
import { KpiCardsGrid } from './KpiCardsGrid';
import { ChartsSection } from './ChartsSection';
import { TransactionsTable } from './TransactionsTable';
import { TransactionDetailModal } from './TransactionDetailModal';
import { KpiCardSkeleton, ChartsSectionSkeleton, TableSkeleton } from './SkeletonLoader';
import { ErrorDisplay, DashboardErrorBoundary } from './ErrorBoundary';
import { ToastNotification } from './ToastNotification';
import { TransactionItem } from '../../types/dashboard';

interface DashboardLayoutProps {
  isDarkMode?: boolean;
  onToggleTheme?: () => void;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({
  isDarkMode = true,
  onToggleTheme,
}) => {
  const {
    stats,
    charts,
    transactions,
    pagination,
    selectedTransaction,
    isLoading,
    isRefreshing,
    isTableLoading,
    isChartsLoading,
    error,
    retryCount,
    selectedPeriod,
    activeKpiFilter,
    filterParams,
    fetchAllData,
    fetchCharts,
    fetchTransactions,
    setPeriod,
    setActiveKpiFilter,
    setSearchQuery,
    setPage,
    setSorting,
    retryLastFailedRequest,
    clearError,
    setSelectedTransaction,
  } = useDashboardStore();

  const [toastVisible, setToastVisible] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);

  // Initial API call on mount
  useEffect(() => {
    fetchAllData();
  }, []);

  // Pop toast automatically when error occurs
  useEffect(() => {
    if (error) {
      setToastVisible(true);
    }
  }, [error]);

  const handleSelectTransaction = (item: TransactionItem) => {
    setSelectedTransaction(item);
    setModalVisible(true);
  };

  const handleCloseModal = () => {
    setModalVisible(false);
    setSelectedTransaction(null);
  };

  const handleManualRefresh = () => {
    fetchAllData();
  };

  return (
    <DashboardErrorBoundary onReset={handleManualRefresh}>
      <View style={[styles.rootContainer, isDarkMode ? styles.bgDark : styles.bgLight]}>
        {/* Real-time Toast System for Error & Notification Broadcasts */}
        <ToastNotification
          visible={toastVisible && !!error}
          message={error?.translatedMessage || 'حدث خطأ في جلب البيانات من الخادم'}
          type="error"
          onDismiss={() => {
            setToastVisible(false);
            clearError();
          }}
        />

        <ScrollView
          style={styles.scrollArea}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* 1. Header Navbar */}
          <Navbar
            onRefresh={handleManualRefresh}
            isRefreshing={isRefreshing}
            isDarkMode={isDarkMode}
            onToggleTheme={onToggleTheme}
          />

          {/* 2. Loading State Skeletons */}
          {isLoading && !stats ? (
            <View>
              <View style={styles.kpiSkeletonRow}>
                <KpiCardSkeleton />
                <KpiCardSkeleton />
                <KpiCardSkeleton />
                <KpiCardSkeleton />
              </View>
              <ChartsSectionSkeleton />
              <TableSkeleton />
            </View>
          ) : error && !stats ? (
            /* 3. Initial Failure Error Display */
            <ErrorDisplay
              error={error}
              onRetry={retryLastFailedRequest}
              isRetrying={isRefreshing}
            />
          ) : (
            /* 4. Live Production Dashboard */
            <View>
              {/* KPI Cards Grid */}
              <KpiCardsGrid
                stats={stats}
                activeFilter={activeKpiFilter}
                onSelectFilter={setActiveKpiFilter}
                isDarkMode={isDarkMode}
              />

              {/* Real Charts Section */}
              <ChartsSection
                charts={charts}
                selectedPeriod={selectedPeriod}
                onSelectPeriod={setPeriod}
                isLoading={isChartsLoading}
                isDarkMode={isDarkMode}
              />

              {/* Paginated Real Transactions Table */}
              <TransactionsTable
                transactions={transactions}
                pagination={pagination}
                filterParams={filterParams}
                isLoading={isTableLoading}
                onSearch={setSearchQuery}
                onPageChange={setPage}
                onSortChange={setSorting}
                onSelectTransaction={handleSelectTransaction}
                isDarkMode={isDarkMode}
              />
            </View>
          )}
        </ScrollView>

        {/* Transaction Detail Modal Dialog */}
        <TransactionDetailModal
          transaction={selectedTransaction}
          visible={modalVisible && !!selectedTransaction}
          onClose={handleCloseModal}
          isDarkMode={isDarkMode}
        />
      </View>
    </DashboardErrorBoundary>
  );
};

const styles = StyleSheet.create({
  rootContainer: {
    flex: 1,
  },
  bgDark: {
    backgroundColor: '#020617',
  },
  bgLight: {
    backgroundColor: '#F8FAFC',
  },
  scrollArea: {
    flex: 1,
  },
  scrollContent: {
    padding: Spacing.lg,
    maxWidth: 1400,
    width: '100%',
    alignSelf: 'center',
  },
  kpiSkeletonRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.md,
    marginBottom: Spacing.lg,
  },
});

export default DashboardLayout;
