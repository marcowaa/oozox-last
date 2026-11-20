import React, { createContext, useState, useEffect, useContext, useMemo } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { api } from '../api/api';
import { getErrorMessage } from '../utils/errorHandler';
import { forceRedirectToOnboarding } from '../utils/navigation';

const AuthContext = createContext(null);

const STORAGE_KEYS = {
  AUTH_TOKEN: '@oozox_auth_token',
  USER_DATA: '@oozox_user_data',
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshingProfile, setIsRefreshingProfile] = useState(false);
  const [authError, setAuthError] = useState(null);

  // 1. Initial application bootstrap & token restoration
  useEffect(() => {
    const bootstrapAsync = async () => {
      try {
        const storedToken = await AsyncStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
        const storedUser = await AsyncStorage.getItem(STORAGE_KEYS.USER_DATA);

        if (storedToken) {
          setToken(storedToken);
          if (storedUser) {
            setUser(JSON.parse(storedUser));
          }

          // Fetch fresh server profile silently in the background
          try {
            const profileRes = await api.getMe();
            if (profileRes.data?.data?.user) {
              const freshUser = profileRes.data.data.user;
              setUser(freshUser);
              await AsyncStorage.setItem(STORAGE_KEYS.USER_DATA, JSON.stringify(freshUser));
            }
          } catch (profileErr) {
            console.warn('[AUTH-BOOTSTRAP] Profile verification error:', profileErr.message);
            // If token expired / invalid (401), clear credentials & redirect
            if (profileErr.response?.status === 401) {
              await AsyncStorage.multiRemove([STORAGE_KEYS.AUTH_TOKEN, STORAGE_KEYS.USER_DATA]);
              setToken(null);
              setUser(null);
              forceRedirectToOnboarding('Onboarding');
            }
          }
        }
      } catch (e) {
        console.error('[AUTH-BOOTSTRAP-FATAL]', e);
      } finally {
        setIsLoading(false);
      }
    };

    bootstrapAsync();
  }, []);

  // 2. Login handler
  const login = async (emailOrPhone, password) => {
    setIsLoading(true);
    setAuthError(null);
    try {
      const response = await api.login({ emailOrPhone, password });
      const { user: loggedInUser, token: receivedToken } = response.data.data;

      setUser(loggedInUser);
      setToken(receivedToken);

      await AsyncStorage.multiSet([
        [STORAGE_KEYS.AUTH_TOKEN, receivedToken],
        [STORAGE_KEYS.USER_DATA, JSON.stringify(loggedInUser)],
      ]);

      return { success: true, user: loggedInUser };
    } catch (error) {
      const message = getErrorMessage(error, 'بيانات الدخول غير صحيحة، يرجى المحاولة مرة أخرى');
      setAuthError(message);
      return { success: false, message };
    } finally {
      setIsLoading(false);
    }
  };

  // 3. Register handler
  const register = async (userData) => {
    setIsLoading(true);
    setAuthError(null);
    try {
      const response = await api.register(userData);
      const { user: registeredUser, token: receivedToken } = response.data.data;

      setUser(registeredUser);
      setToken(receivedToken);

      await AsyncStorage.multiSet([
        [STORAGE_KEYS.AUTH_TOKEN, receivedToken],
        [STORAGE_KEYS.USER_DATA, JSON.stringify(registeredUser)],
      ]);

      return { success: true, user: registeredUser };
    } catch (error) {
      const message = getErrorMessage(error, 'تعذر إنشاء الحساب، يرجى مراجعة البيانات المدخلة');
      setAuthError(message);
      return { success: false, message };
    } finally {
      setIsLoading(false);
    }
  };

  // 4. Reload active profile
  const reloadUserProfile = async () => {
    if (!token) return;
    setIsRefreshingProfile(true);
    try {
      const response = await api.getMe();
      if (response.data?.data?.user) {
        const freshUser = response.data.data.user;
        setUser(freshUser);
        await AsyncStorage.setItem(STORAGE_KEYS.USER_DATA, JSON.stringify(freshUser));
      }
    } catch (err) {
      console.warn('[AUTH-RELOAD-PROFILE]', err.message);
      if (err.response?.status === 401) {
        await logout();
      }
    } finally {
      setIsRefreshingProfile(false);
    }
  };

  // 5. Logout & storage cleanup with forced redirection to Onboarding
  const logout = async () => {
    setIsLoading(true);
    try {
      await AsyncStorage.multiRemove([STORAGE_KEYS.AUTH_TOKEN, STORAGE_KEYS.USER_DATA]);
      setUser(null);
      setToken(null);
      setAuthError(null);
      forceRedirectToOnboarding('Onboarding');
    } catch (e) {
      console.error('[LOGOUT-ERROR]', e);
    } finally {
      setIsLoading(false);
    }
  };

  const contextValue = useMemo(
    () => ({
      user,
      token,
      isLoading,
      isRefreshingProfile,
      isAuthenticated: Boolean(token && user),
      authError,
      login,
      register,
      logout,
      reloadUserProfile,
      clearAuthError: () => setAuthError(null),
    }),
    [user, token, isLoading, isRefreshingProfile, authError]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
};

/**
 * Custom hook to consume the global Authentication Context
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider hierarchy');
  }
  return context;
};

export default AuthContext;
