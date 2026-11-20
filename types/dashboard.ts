/**
 * Production-Ready Dashboard Types and Interfaces
 * Strict data contracts for API responses, query filters, and state management.
 */

export interface DashboardStats {
  totalRevenue: number;
  revenueCurrency: string;
  revenueGrowthPercentage: number;
  totalCustomers: number;
  customersGrowthPercentage: number;
  activeOrders: number;
  ordersGrowthPercentage: number;
  overallGrowthRate: number;
  lastUpdated: string;
}

export type ChartPeriod = '7d' | 'month' | 'year';

export interface ChartDataPoint {
  date: string;
  revenue: number;
  orders: number;
  customers: number;
}

export interface DistributionDataPoint {
  category: string;
  value: number;
  percentage: number;
  color?: string;
}

export interface DashboardChartsResponse {
  period: ChartPeriod;
  trend: ChartDataPoint[];
  distribution: DistributionDataPoint[];
}

export type TransactionStatus = 'completed' | 'pending' | 'failed' | 'refunded';

export interface TransactionItem {
  id: string;
  referenceNumber: string;
  customerName: string;
  customerEmail?: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  category: string;
  paymentMethod: string;
  createdAt: string;
  updatedAt?: string;
  details?: {
    itemCount?: number;
    billingAddress?: string;
    notes?: string;
    fees?: number;
    netAmount?: number;
  };
}

export interface PaginationMeta {
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
  hasNextPage: boolean;
  hasPrevPage: boolean;
}

export interface TransactionsResponse {
  data: TransactionItem[];
  meta: PaginationMeta;
}

export interface TransactionFilterParams {
  page?: number;
  limit?: number;
  search?: string;
  status?: TransactionStatus | 'all';
  category?: string;
  sortBy?: 'createdAt' | 'amount' | 'customerName' | 'status';
  sortOrder?: 'asc' | 'desc';
}

export interface ApiErrorResponse {
  statusCode: number;
  errorCode?: string;
  message: string;
  translatedMessage: string;
  timestamp: string;
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title?: string;
  message: string;
  duration?: number;
}
