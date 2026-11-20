import { useSyncExternalStore } from 'react';
import { dashboardService } from '../services/dashboardService';
import { Spacing, BorderRadius } from '../constants/DesignSystem';
import {
  DashboardStats,
  DashboardChartsResponse,
  TransactionItem,
  PaginationMeta,
  TransactionFilterParams,
  ChartPeriod,
  ApiErrorResponse,
  ToastMessage,
} from '../types/dashboard';

export type KpiFilterType = 'all' | 'revenue' | 'customers' | 'orders' | 'growth';

export interface DashboardState {
  // Core Data models
  stats: DashboardStats | null;
  charts: DashboardChartsResponse | null;
  transactions: TransactionItem[];
  pagination: PaginationMeta;
  selectedTransaction: TransactionItem | null;

  // Granular Status and UI state
  isLoading: boolean;
  isRefreshing: boolean;
  isTableLoading: boolean;
  isChartsLoading: boolean;
  isDetailsLoading: boolean;
  error: ApiErrorResponse | null;
  retryCount: number;
  lastFetchedAt: Date | null;

  // Active Toast Notification System
  toast: ToastMessage | null;

  // Filter & Navigation state
  selectedPeriod: ChartPeriod;
  activeKpiFilter: KpiFilterType;
  filterParams: TransactionFilterParams;

  // 8px Grid Layout Tokens Exposure
  layoutSpacing: typeof Spacing;
  layoutRadius: typeof BorderRadius;

  // Actions
  fetchAllData: (customParams?: Partial<TransactionFilterParams>) => Promise<void>;
  fetchCharts: (period: ChartPeriod) => Promise<void>;
  fetchTransactions: (params?: Partial<TransactionFilterParams>) => Promise<void>;
  fetchTransactionDetails: (id: string) => Promise<TransactionItem | null>;
  setPeriod: (period: ChartPeriod) => void;
  setActiveKpiFilter: (filter: KpiFilterType) => void;
  setSearchQuery: (query: string) => void;
  setPage: (page: number) => void;
  setPageLimit: (limit: number) => void;
  setSorting: (sortBy: TransactionFilterParams['sortBy'], sortOrder: TransactionFilterParams['sortOrder']) => void;
  retryLastFailedRequest: () => Promise<void>;
  clearError: () => void;
  showToast: (message: string, type?: ToastMessage['type'], title?: string, duration?: number) => void;
  dismissToast: () => void;
  setSelectedTransaction: (item: TransactionItem | null) => void;
}

const initialPagination: PaginationMeta = {
  page: 1,
  limit: 10,
  totalItems: 0,
  totalPages: 1,
  hasNextPage: false,
  hasPrevPage: false,
};

const initialFilters: TransactionFilterParams = {
  page: 1,
  limit: 10,
  search: '',
  status: 'all',
  sortBy: 'createdAt',
  sortOrder: 'desc',
};

// ============================================================================
// Production-Ready Reactive Store Engine
// ============================================================================

class DashboardStoreEngine {
  private state: DashboardState;
  private listeners = new Set<() => void>();
  private toastTimer: any = null;

  constructor() {
    this.state = {
      stats: null,
      charts: null,
      transactions: [],
      pagination: initialPagination,
      selectedTransaction: null,
      isLoading: false,
      isRefreshing: false,
      isTableLoading: false,
      isChartsLoading: false,
      isDetailsLoading: false,
      error: null,
      retryCount: 0,
      lastFetchedAt: null,
      toast: null,
      selectedPeriod: 'month',
      activeKpiFilter: 'all',
      filterParams: initialFilters,

      layoutSpacing: Spacing,
      layoutRadius: BorderRadius,

      fetchAllData: this.fetchAllData.bind(this),
      fetchCharts: this.fetchCharts.bind(this),
      fetchTransactions: this.fetchTransactions.bind(this),
      fetchTransactionDetails: this.fetchTransactionDetails.bind(this),
      setPeriod: this.setPeriod.bind(this),
      setActiveKpiFilter: this.setActiveKpiFilter.bind(this),
      setSearchQuery: this.setSearchQuery.bind(this),
      setPage: this.setPage.bind(this),
      setPageLimit: this.setPageLimit.bind(this),
      setSorting: this.setSorting.bind(this),
      retryLastFailedRequest: this.retryLastFailedRequest.bind(this),
      clearError: this.clearError.bind(this),
      showToast: this.showToast.bind(this),
      dismissToast: this.dismissToast.bind(this),
      setSelectedTransaction: this.setSelectedTransaction.bind(this),
    };
  }

  public getState = (): DashboardState => this.state;

  public setState = (partial: Partial<DashboardState> | ((prevState: DashboardState) => Partial<DashboardState>)): void => {
    const nextPartial = typeof partial === 'function' ? partial(this.state) : partial;
    this.state = { ...this.state, ...nextPartial };
    this.listeners.forEach((listener) => listener());
  };

  public subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  /**
   * Fetches Stats, Charts, and Transactions concurrently with Promise.all
   */
  public async fetchAllData(customParams?: Partial<TransactionFilterParams>): Promise<void> {
    const isInitial = !this.state.stats;
    this.setState({
      isLoading: isInitial,
      isRefreshing: !isInitial,
      error: null,
    });

    const activeParams = { ...this.state.filterParams, ...customParams };

    try {
      // Parallel execution via Promise.all for real high-performance loading
      const [statsResult, chartsResult, transactionsResult] = await Promise.all([
        dashboardService.getStats(),
        dashboardService.getCharts(this.state.selectedPeriod),
        dashboardService.getTransactions(activeParams),
      ]);

      this.setState({
        stats: statsResult,
        charts: chartsResult,
        transactions: transactionsResult.data,
        pagination: transactionsResult.meta,
        filterParams: activeParams,
        isLoading: false,
        isRefreshing: false,
        error: null,
        retryCount: 0,
        lastFetchedAt: new Date(),
      });
    } catch (err) {
      const apiError = err as ApiErrorResponse;
      this.setState({
        isLoading: false,
        isRefreshing: false,
        error: apiError,
      });

      this.showToast(
        apiError.translatedMessage || 'تعذر الاتصال بالخادم لجلب بيانات لوحة التحكم',
        'error',
        'خطأ في الاتصال'
      );
    }
  }

  /**
   * Fetches charts with period filter
   */
  public async fetchCharts(period: ChartPeriod): Promise<void> {
    this.setState({ isChartsLoading: true, selectedPeriod: period, error: null });
    try {
      const chartsResult = await dashboardService.getCharts(period);
      this.setState({
        charts: chartsResult,
        isChartsLoading: false,
        error: null,
      });
    } catch (err) {
      const apiError = err as ApiErrorResponse;
      this.setState({
        isChartsLoading: false,
        error: apiError,
      });
      this.showToast(apiError.translatedMessage, 'error');
    }
  }

  /**
   * Fetches paginated transaction records
   */
  public async fetchTransactions(params?: Partial<TransactionFilterParams>): Promise<void> {
    const mergedParams = { ...this.state.filterParams, ...params };
    this.setState({ isTableLoading: true, filterParams: mergedParams, error: null });

    try {
      const transactionsResult = await dashboardService.getTransactions(mergedParams);
      this.setState({
        transactions: transactionsResult.data,
        pagination: transactionsResult.meta,
        isTableLoading: false,
        error: null,
      });
    } catch (err) {
      const apiError = err as ApiErrorResponse;
      this.setState({
        isTableLoading: false,
        error: apiError,
      });
      this.showToast(apiError.translatedMessage, 'error');
    }
  }

  /**
   * Fetches detail for a specific transaction item
   */
  public async fetchTransactionDetails(id: string): Promise<TransactionItem | null> {
    this.setState({ isDetailsLoading: true, error: null });
    try {
      const details = await dashboardService.getTransactionById(id);
      this.setState({
        selectedTransaction: details,
        isDetailsLoading: false,
      });
      return details;
    } catch (err) {
      const apiError = err as ApiErrorResponse;
      this.setState({
        isDetailsLoading: false,
        error: apiError,
      });
      this.showToast(apiError.translatedMessage, 'error');
      return null;
    }
  }

  public setPeriod(period: ChartPeriod): void {
    if (this.state.selectedPeriod === period) return;
    this.fetchCharts(period);
  }

  public setActiveKpiFilter(filter: KpiFilterType): void {
    this.setState({ activeKpiFilter: filter });
    let statusFilter: TransactionFilterParams['status'] = 'all';
    if (filter === 'orders') statusFilter = 'pending';
    if (filter === 'revenue') statusFilter = 'completed';

    this.fetchTransactions({ page: 1, status: statusFilter });
  }

  public setSearchQuery(query: string): void {
    this.fetchTransactions({ page: 1, search: query });
  }

  public setPage(page: number): void {
    this.fetchTransactions({ page });
  }

  public setPageLimit(limit: number): void {
    this.fetchTransactions({ page: 1, limit });
  }

  public setSorting(sortBy: TransactionFilterParams['sortBy'], sortOrder: TransactionFilterParams['sortOrder']): void {
    this.fetchTransactions({ page: 1, sortBy, sortOrder });
  }

  public async retryLastFailedRequest(): Promise<void> {
    const currentRetries = this.state.retryCount + 1;
    this.setState({ retryCount: currentRetries });
    this.showToast(`جاري إعادة المحاولة (المحاولة ${currentRetries})...`, 'info');
    await this.fetchAllData();
  }

  public clearError(): void {
    this.setState({ error: null });
  }

  public showToast(
    message: string,
    type: ToastMessage['type'] = 'info',
    title?: string,
    duration: number = 4000
  ): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    const newToast: ToastMessage = {
      id: `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      type,
      title,
      message,
      duration,
    };
    this.setState({ toast: newToast });

    this.toastTimer = setTimeout(() => {
      this.dismissToast();
    }, duration);
  }

  public dismissToast(): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
    this.setState({ toast: null });
  }

  public setSelectedTransaction(item: TransactionItem | null): void {
    this.setState({ selectedTransaction: item });
  }
}

export const dashboardStoreEngine = new DashboardStoreEngine();

/**
 * Custom React Hook providing state subscription to Dashboard Store
 */
export function useDashboardStore<T>(selector?: (state: DashboardState) => T): T {
  const getSnapshot = () => (selector ? selector(dashboardStoreEngine.getState()) : (dashboardStoreEngine.getState() as unknown as T));
  return useSyncExternalStore(dashboardStoreEngine.subscribe, getSnapshot, getSnapshot);
}

export default useDashboardStore;
