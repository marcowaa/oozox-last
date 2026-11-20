import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE_URL, APP_CONFIG } from '../config';
import { forceRedirectToOnboarding } from '../utils/navigation';

/**
 * OOZOX Centralized Axios Client Instance
 * Dynamically configured via mobile-client/config.js
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: APP_CONFIG.TIMEOUT_MS || 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Storage keys
export const AUTH_STORAGE_KEYS = {
  TOKEN: '@oozox_auth_token',
  USER: '@oozox_user_data',
};

// 1. Request Interceptor: Injects Bearer JWT token from AsyncStorage into outbound requests
apiClient.interceptors.request.use(
  async (config) => {
    try {
      const token = await AsyncStorage.getItem(AUTH_STORAGE_KEYS.TOKEN);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.warn('[API-CLIENT] Could not read auth token from AsyncStorage:', error);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 2. Response Interceptor: Intercepts 401 Unauthorized, clears credentials, and forces redirect to /onboarding
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response && error.response.status === 401) {
      console.warn('[API-CLIENT] 401 Unauthorized received. Purging session & forcing redirect to /onboarding.');
      try {
        await AsyncStorage.multiRemove([AUTH_STORAGE_KEYS.TOKEN, AUTH_STORAGE_KEYS.USER]);
      } catch (storageErr) {
        console.error('[API-CLIENT] Failed to purge storage on 401:', storageErr);
      }

      // Force instant reset and redirect to Onboarding screen via navigationRef
      forceRedirectToOnboarding('Onboarding');
    }
    return Promise.reject(error);
  }
);

export { API_BASE_URL };
export default apiClient;
