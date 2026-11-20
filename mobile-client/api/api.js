import apiClient from './client';
import { authAPI, invoiceAPI, notificationAPI, systemAPI } from './endpoints';

export const api = {
  ...authAPI,
  ...invoiceAPI,
  ...notificationAPI,
  ...systemAPI,
};

export { authAPI, invoiceAPI, notificationAPI, systemAPI };
export default api;
