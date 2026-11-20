import apiClient from './client';

export const authAPI = {
  register: (userData) => apiClient.post('/auth/register', userData),
  login: (credentials) => apiClient.post('/auth/login', credentials),
  getMe: () => apiClient.get('/auth/me'),
  searchUsers: (query) => apiClient.get(`/auth/search?q=${encodeURIComponent(query)}`),
};

export const invoiceAPI = {
  createInvoice: (invoiceData) => apiClient.post('/invoices', invoiceData),
  getInvoices: (params = {}) => apiClient.get('/invoices', { params }),
  getInvoiceById: (id) => apiClient.get(`/invoices/${id}`),
  updateStatus: (id, status) => apiClient.patch(`/invoices/${id}/status`, { status }),
  deleteInvoice: (id) => apiClient.delete(`/invoices/${id}`),
};

export const notificationAPI = {
  getNotifications: () => apiClient.get('/notifications'),
  markAsRead: (id) => apiClient.patch(`/notifications/${id}/read`),
  markAllAsRead: () => apiClient.patch('/notifications/read-all'),
};

export const systemAPI = {
  getHealth: () => apiClient.get('/health'),
};
