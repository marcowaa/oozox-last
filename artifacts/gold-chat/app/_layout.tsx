import { Stack, router, useSegments } from 'expo-router';
import { useEffect, useState, useRef } from 'react';
import { View, Text, StyleSheet, Animated, TouchableOpacity, ActivityIndicator } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { MaterialIcons } from '@expo/vector-icons';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { KeyboardProvider } from 'react-native-keyboard-controller';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import {
  Inter_400Regular,
  Inter_500Medium,
  Inter_600SemiBold,
  Inter_700Bold,
  useFonts,
} from '@expo-google-fonts/inter';
import * as SplashScreen from 'expo-splash-screen';
import { AppProvider, useApp } from '@/context/AppContext';
import { Colors } from '@/constants/colors';

// Export ErrorBoundary for Expo Router layout-level error catching
export { ErrorBoundary } from '@/components/ErrorBoundary';

export const unstable_settings = {
  // Ensure that reloading on subroutes keeps a back button present.
  initialRouteName: '(tabs)',
};

// Prevent the splash screen from auto-hiding before asset loading is complete.
SplashScreen.preventAutoHideAsync().catch((err) => {
  /* reloading in development or web can cause harmless rejections; log as warning */
  console.warn('SplashScreen.preventAutoHideAsync rejection:', err);
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      staleTime: 1000 * 60 * 5, // 5 minutes
    },
  },
});

/**
 * Toast banner that renders when custom typography fails to load,
 * providing immediate visual feedback to the user while falling back gracefully.
 */
function FontErrorToast({
  error,
  onDismiss,
}: {
  error: Error | null;
  onDismiss: () => void;
}) {
  const insets = useSafeAreaInsets();
  const translateY = useRef(new Animated.Value(-80)).current;
  const opacity = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(translateY, {
        toValue: 0,
        duration: 280,
        useNativeDriver: true,
      }),
      Animated.timing(opacity, {
        toValue: 1,
        duration: 280,
        useNativeDriver: true,
      }),
    ]).start();

    // Auto-dismiss after 6 seconds
    const timer = setTimeout(() => {
      dismiss();
    }, 6000);

    return () => clearTimeout(timer);
  }, []);

  const dismiss = () => {
    Animated.parallel([
      Animated.timing(translateY, {
        toValue: -80,
        duration: 220,
        useNativeDriver: true,
      }),
      Animated.timing(opacity, {
        toValue: 0,
        duration: 220,
        useNativeDriver: true,
      }),
    ]).start(() => {
      onDismiss();
    });
  };

  return (
    <Animated.View
      style={[
        styles.toastContainer,
        {
          top: Math.max(insets.top, 16) + 8,
          transform: [{ translateY }],
          opacity,
        },
      ]}
    >
      <MaterialIcons name="warning-amber" size={22} color="#F59E0B" style={styles.toastIcon} />
      <View style={styles.toastContent}>
        <Text style={styles.toastTitle}>تعذر تحميل خطوط Inter</Text>
        <Text style={styles.toastSubtitle} numberOfLines={2}>
          {error?.message ? `(${error.message}) ` : ''}تم تفعيل خطوط النظام البديلة لضمان تشغيل التطبيق.
        </Text>
      </View>
      <TouchableOpacity
        onPress={dismiss}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        style={styles.toastDismissButton}
        accessibilityLabel="إغلاق الإشعار"
      >
        <MaterialIcons name="close" size={18} color="#D1D5DB" />
      </TouchableOpacity>
    </Animated.View>
  );
}

/**
 * Full-screen loading overlay and spinner that displays during initial app startup,
 * preventing a blank white screen while fonts or persisted application state are loading.
 */
function LoadingOverlay({ message }: { message?: string }) {
  return (
    <View style={styles.loadingContainer}>
      <StatusBar style="light" backgroundColor={Colors.bg} />
      <View style={styles.loadingContent}>
        <View style={styles.loadingLogoBadge}>
          <MaterialIcons name="diamond" size={36} color={Colors.accent} />
        </View>
        <Text style={styles.loadingTitle}>تطبيق ذهبي</Text>
        <ActivityIndicator size="large" color={Colors.accent} style={styles.loadingSpinner} />
        <Text style={styles.loadingSubtitle}>
          {message || 'جاري تحميل التطبيق...'}
        </Text>
      </View>
    </View>
  );
}

function RootLayoutNav({ fontsReady }: { fontsReady: boolean }) {
  const { user, isLoading } = useApp();
  const segments = useSegments();

  // Routes that don't require authentication
  const publicRoutes = ['onboarding', 'admin'];

  useEffect(() => {
    // Only resolve routing and hide the splash screen when both fonts & storage state have finished loading
    if (!isLoading && fontsReady) {
      const currentRoute = segments[0] as string;
      const isPublic = publicRoutes.includes(currentRoute);
      if (!user && !isPublic) {
        router.replace('/onboarding');
      }

      // Smoothly dismiss the native splash screen now that destination screen is ready to mount
      SplashScreen.hideAsync().catch((err) => {
        console.warn('Failed to hide splash screen:', err);
      });
    }
  }, [user, isLoading, segments, fontsReady]);

  // Render loading overlay with brand spinner while initial state (isLoading) is true,
  // preventing a blank white screen during app startup.
  if (isLoading || !fontsReady) {
    return (
      <LoadingOverlay
        message={!fontsReady ? 'جاري تهيئة الخطوط...' : 'جاري تحميل البيانات...'}
      />
    );
  }

  return (
    <>
      <StatusBar style="light" backgroundColor={Colors.bg} />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: Colors.bg },
        }}
      >
        <Stack.Screen name="onboarding" options={{ animation: 'fade' }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="chat/[id]" options={{ headerShown: false, animation: 'slide_from_right' }} />
        <Stack.Screen name="transaction/[id]" options={{ headerShown: false, animation: 'slide_from_right' }} />
        <Stack.Screen name="project/[id]" options={{ headerShown: false, animation: 'slide_from_right' }} />
        <Stack.Screen name="gold-purchase" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="gold/[id]" options={{ headerShown: false, animation: 'slide_from_right' }} />
        <Stack.Screen name="new-chat" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="new-project" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="admin" options={{ headerShown: false, animation: 'slide_from_right' }} />
      </Stack>
    </>
  );
}

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
    Inter_700Bold,
  });

  const [fontErrorToastVisible, setFontErrorToastVisible] = useState(false);

  // Specific log check and UI toast trigger when fontError is truthy
  useEffect(() => {
    if (fontError) {
      console.warn(
        `[RootLayout] Inter fonts failed to load (${fontError.message || fontError}). Falling back to system fonts.`
      );
      setFontErrorToastVisible(true);
    }
  }, [fontError]);

  const fontsReady = fontsLoaded || fontError != null;

  // Display full-screen Loading overlay in RootLayout when fonts are not ready yet,
  // ensuring users never see a blank white screen during initial app startup.
  if (!fontsReady) {
    return (
      <SafeAreaProvider>
        <LoadingOverlay message="جاري تهيئة الخطوط..." />
      </SafeAreaProvider>
    );
  }

  return (
    <SafeAreaProvider>
      <ErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <GestureHandlerRootView style={{ flex: 1, backgroundColor: Colors.bg }}>
            <KeyboardProvider>
              <AppProvider>
                <RootLayoutNav fontsReady={fontsReady} />
                {fontErrorToastVisible && (
                  <FontErrorToast
                    error={fontError}
                    onDismiss={() => setFontErrorToastVisible(false)}
                  />
                )}
              </AppProvider>
            </KeyboardProvider>
          </GestureHandlerRootView>
        </QueryClientProvider>
      </ErrorBoundary>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  toastContainer: {
    position: 'absolute',
    left: 16,
    right: 16,
    maxWidth: 500,
    alignSelf: 'center',
    zIndex: 99999,
    backgroundColor: '#1E1810',
    borderWidth: 1,
    borderColor: '#B45309',
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 10,
  },
  toastIcon: {
    marginRight: 10,
  },
  toastContent: {
    flex: 1,
  },
  toastTitle: {
    color: '#FBBF24',
    fontSize: 13,
    fontWeight: '700',
    marginBottom: 2,
    textAlign: 'right',
  },
  toastSubtitle: {
    color: '#E5E7EB',
    fontSize: 11,
    lineHeight: 16,
    textAlign: 'right',
  },
  toastDismissButton: {
    marginLeft: 10,
    padding: 6,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingContainer: {
    flex: 1,
    backgroundColor: Colors.bg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingContent: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  loadingLogoBadge: {
    width: 68,
    height: 68,
    borderRadius: 34,
    backgroundColor: Colors.surface,
    borderWidth: 1.5,
    borderColor: Colors.accent,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
    shadowColor: Colors.accent,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 10,
    elevation: 6,
  },
  loadingTitle: {
    color: Colors.textPrimary,
    fontSize: 19,
    fontWeight: '700',
    marginBottom: 16,
    textAlign: 'center',
    letterSpacing: 0.5,
  },
  loadingSpinner: {
    marginBottom: 12,
  },
  loadingSubtitle: {
    color: Colors.textMuted,
    fontSize: 12,
    textAlign: 'center',
  },
});
