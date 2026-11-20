import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import {
  DashboardStats,
  DashboardChartsResponse,
  TransactionsResponse,
  TransactionFilterParams,
  TransactionItem,
  ChartPeriod,
  ApiErrorResponse,
} from '../types/dashboard';

// Determine base API URL from environment variables
const getBaseApiUrl = (): string => {
  if (typeof process !== 'undefined' && process.env) {
    if (process.env.REACT_APP_API_URL) return process.env.REACT_APP_API_URL;
    if (process.env.NEXT_PUBLIC_API_URL) return process.env.NEXT_PUBLIC_API_URL;
    if (process.env.EXPO_PUBLIC_API_URL) return process.env.EXPO_PUBLIC_API_URL;
    if (process.env.VITE_API_URL) return process.env.VITE_API_URL;
  }
  if (typeof window !== 'undefined' && (window as unknown as { __ENV__?: { API_URL?: string } }).__ENV__?.API_URL) {
    return (window as unknown as { __ENV__: { API_URL: string } }).__ENV__.API_URL;
  }
  return '/api'; // fallback standard API root
};

// Token storage key
const AUTH_TOKEN_KEY = 'auth_token';

// JWT Token retrieval helper
export const getAuthToken = (): string | null => {
  try {
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem(AUTH_TOKEN_KEY);
    }
  } catch {
    // Graceful fallback for non-browser environments
  }
  return null;
};

export const setAuthToken = (token: string): void => {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(AUTH_TOKEN_KEY, token);
    }
  } catch {
    // Graceful fallback
  }
};

export const removeAuthToken = (): void => {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(AUTH_TOKEN_KEY);
    }
  } catch {
    // Graceful fallback
  }
};

// Create Axios Instance with timeout & base configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: getBaseApiUrl(),
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Request Interceptor: Attach Bearer JWT Token automatically
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAuthToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Translate server & network errors into standardized format
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status || 0;
    let translatedMessage = 'حدث خطأ غير متوقع أثناء الاتصال بالخادم';

    if (!error.response) {
      translatedMessage = 'تعذر الاتصال بالخادم. يرجى التحقق من اتصالك بالإنترنت';
    } else {
      switch (status) {
        case 400:
          translatedMessage = 'طلب غير صالح. يرجى التحقق من المدخلات المرسلة';
          break;
        case 401:
          translatedMessage = 'انتهت صلاحية الجلسة أو التوكن غير صالح. يرجى تسجيل الدخول مجدداً';
          break;
        case 403:
          translatedMessage = 'ليس لديك الصلاحيات الكافية للوصول إلى هذه البيانات';
          break;
        case 404:
          translatedMessage = 'البيانات المطلوبة غير موجودة على الخادم';
          break;
        case 429:
          translatedMessage = 'تم تجاوز الحد المسموح من الطلبات. يرجى الانتظار قليلاً';
          break;
        case 500:
          translatedMessage = 'خطأ داخلي في الخادم (500). يرجى المحاولة لاحقاً';
          break;
        case 502:
        case 503:
        case 504:
          translatedMessage = 'الخدمة غير متوفرة مؤقتاً أو السيرفر تحت الصيانة';
          break;
        default:
          translatedMessage = `خطأ في الخادم (${status}): ${error.message}`;
      }
    }

    const customError: ApiErrorResponse = {
      statusCode: status,
      errorCode: (error.response?.data as { code?: string })?.code || error.code,
      message: (error.response?.data as { message?: string })?.message || error.message,
      translatedMessage,
      timestamp: new Date().toISOString(),
    };

    return Promise.reject(customError);
  }
);

// ============================================================================
// Real Production Dashboard API Service
// ============================================================================

export const dashboardService = {
  /**
   * Fetches overall KPI metrics
   * GET /api/dashboard/stats
   */
  getStats: async (): Promise<DashboardStats> => {
    const response = await apiClient.get<DashboardStats>('/dashboard/stats');
    return response.data;
  },

  /**
   * Fetches trend and distribution chart data with period filter
   * GET /api/dashboard/charts?period=month
   */
  getCharts: async (period: ChartPeriod = 'month'): Promise<DashboardChartsResponse> => {
    const response = await apiClient.get<DashboardChartsResponse>('/dashboard/charts', {
      params: { period },
    });
    return response.data;
  },

  /**
   * Fetches paginated transaction records with real server-side filtering & sorting
   * GET /api/dashboard/transactions?page=1&limit=10&search=...
   */
  getTransactions: async (params: TransactionFilterParams = {}): Promise<TransactionsResponse> => {
    const queryParams = {
      page: params.page || 1,
      limit: params.limit || 10,
      search: params.search?.trim() || undefined,
      status: params.status && params.status !== 'all' ? params.status : undefined,
      category: params.category || undefined,
      sortBy: params.sortBy || 'createdAt',
      sortOrder: params.sortOrder || 'desc',
    };

    const response = await apiClient.get<TransactionsResponse>('/dashboard/transactions', {
      params: queryParams,
    });
    return response.data;
  },

  /**
   * Fetches a single transaction's complete details
   * GET /api/dashboard/transactions/:id
   */
  getTransactionById: async (id: string): Promise<TransactionItem> => {
    const response = await apiClient.get<TransactionItem>(`/dashboard/transactions/${encodeURIComponent(id)}`);
    return response.data;
  },
};

export default dashboardService;
