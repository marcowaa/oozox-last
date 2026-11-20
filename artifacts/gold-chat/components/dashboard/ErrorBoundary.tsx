import React, { Component, ErrorInfo, ReactNode } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';
import { ApiErrorResponse } from '../../types/dashboard';

interface ErrorDisplayProps {
  error: ApiErrorResponse | Error | string | null;
  onRetry?: () => void;
  isRetrying?: boolean;
}

export const ErrorDisplay: React.FC<ErrorDisplayProps> = ({ error, onRetry, isRetrying }) => {
  if (!error) return null;

  let title = 'حدث خطأ في جلب البيانات';
  let message = 'تعذر الاتصال بالخادم. يرجى التأكد من تشغيل السيرفر وعنوان الـ API';
  let statusCode: number | undefined;

  if (typeof error === 'string') {
    message = error;
  } else if ('translatedMessage' in error) {
    message = error.translatedMessage;
    statusCode = error.statusCode;
    if (error.statusCode === 401) title = 'غير مصرح بالدخول';
    else if (error.statusCode === 404) title = 'البيانات غير موجودة';
    else if (error.statusCode >= 500) title = 'خطأ في خادم النظام';
  } else if ('message' in error) {
    message = error.message;
  }

  return (
    <View style={styles.errorContainer}>
      <View style={styles.iconCircle}>
        <Ionicons name="alert-circle-outline" size={32} color="#EF4444" />
      </View>
      <Text style={styles.errorTitle}>{title}</Text>
      <Text style={styles.errorMessage}>{message}</Text>
      {statusCode ? (
        <View style={styles.statusBadge}>
          <Text style={styles.statusText}>رمز الاستجابة: {statusCode}</Text>
        </View>
      ) : null}

      {onRetry && (
        <TouchableOpacity
          style={[styles.retryButton, isRetrying && styles.retryButtonDisabled]}
          onPress={onRetry}
          disabled={isRetrying}
          activeOpacity={0.8}
        >
          <Ionicons name="refresh" size={18} color="#FFFFFF" />
          <Text style={styles.retryText}>{isRetrying ? 'جاري إعادة المحاولة...' : 'إعادة المحاولة الآن'}</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onReset?: () => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class DashboardErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[Dashboard Error Boundary Caught]:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    this.props.onReset?.();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;
      return (
        <ErrorDisplay
          error={this.state.error}
          onRetry={this.handleReset}
        />
      );
    }
    return this.props.children;
  }
}

const styles = StyleSheet.create({
  errorContainer: {
    padding: Spacing.xl,
    backgroundColor: 'rgba(239, 68, 68, 0.08)',
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: 'rgba(239, 68, 68, 0.25)',
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: Spacing.lg,
  },
  iconCircle: {
    width: 64,
    height: 64,
    borderRadius: BorderRadius.full,
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.md,
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#F87171',
    marginBottom: Spacing.xs,
    textAlign: 'center',
  },
  errorMessage: {
    fontSize: 14,
    color: '#CBD5E1',
    textAlign: 'center',
    maxWidth: 450,
    lineHeight: 22,
    marginBottom: Spacing.md,
  },
  statusBadge: {
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xxs,
    borderRadius: BorderRadius.xs,
    marginBottom: Spacing.lg,
  },
  statusText: {
    fontSize: 12,
    color: '#94A3B8',
    fontFamily: 'monospace',
  },
  retryButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    backgroundColor: '#EF4444',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.sm,
    minHeight: 48,
  },
  retryButtonDisabled: {
    opacity: 0.6,
  },
  retryText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 14,
  },
});
