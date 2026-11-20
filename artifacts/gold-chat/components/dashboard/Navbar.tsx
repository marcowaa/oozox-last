import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated, Easing } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';

interface NavbarProps {
  onRefresh: () => void;
  isRefreshing: boolean;
  isDarkMode: boolean;
  onToggleTheme?: () => void;
  userName?: string;
  userRole?: string;
}

export const Navbar: React.FC<NavbarProps> = ({
  onRefresh,
  isRefreshing,
  isDarkMode,
  onToggleTheme,
  userName = 'المدير التنفيذي',
  userRole = 'مدير النظام (Admin)',
}) => {
  const spinValue = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (isRefreshing) {
      const loop = Animated.loop(
        Animated.timing(spinValue, {
          toValue: 1,
          duration: 800,
          easing: Easing.linear,
          useNativeDriver: true,
        })
      );
      loop.start();
      return () => loop.stop();
    } else {
      spinValue.setValue(0);
    }
  }, [isRefreshing]);

  const spin = spinValue.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  const getFormattedDate = (): string => {
    const today = new Date();
    return today.toLocaleDateString('ar-EG', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <View style={[styles.navbarContainer, isDarkMode ? styles.navDark : styles.navLight]}>
      {/* Brand & Date Info */}
      <View style={styles.brandSection}>
        <View style={styles.liveIndicatorRow}>
          <View style={styles.liveDot} />
          <Text style={[styles.liveText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
            بيانات السيرفر الحية
          </Text>
        </View>
        <Text style={[styles.dateText, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
          {getFormattedDate()}
        </Text>
      </View>

      {/* Action Controls */}
      <View style={styles.actionsSection}>
        {/* Manual Refresh Button */}
        <TouchableOpacity
          style={[styles.actionButton, isDarkMode ? styles.actionButtonDark : styles.actionButtonLight]}
          onPress={onRefresh}
          disabled={isRefreshing}
          activeOpacity={0.7}
        >
          <Animated.View style={{ transform: [{ rotate: spin }] }}>
            <Ionicons
              name="refresh-outline"
              size={18}
              color={isDarkMode ? '#E2E8F0' : '#1E293B'}
            />
          </Animated.View>
          <Text style={[styles.actionButtonText, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
            {isRefreshing ? 'جاري التحديث...' : 'تحديث البيانات'}
          </Text>
        </TouchableOpacity>

        {/* Theme Toggle (if provided) */}
        {onToggleTheme && (
          <TouchableOpacity
            style={[styles.iconButton, isDarkMode ? styles.actionButtonDark : styles.actionButtonLight]}
            onPress={onToggleTheme}
            activeOpacity={0.7}
          >
            <Ionicons
              name={isDarkMode ? 'sunny-outline' : 'moon-outline'}
              size={20}
              color={isDarkMode ? '#F59E0B' : '#3B82F6'}
            />
          </TouchableOpacity>
        )}

        {/* User Profile Card */}
        <View style={[styles.userBadge, isDarkMode ? styles.userBadgeDark : styles.userBadgeLight]}>
          <View style={styles.userAvatar}>
            <Ionicons name="person" size={18} color="#D97706" />
          </View>
          <View style={styles.userInfo}>
            <Text style={[styles.userNameText, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
              {userName}
            </Text>
            <Text style={[styles.userRoleText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
              {userRole}
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  navbarContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    marginBottom: Spacing.lg,
    flexWrap: 'wrap',
    gap: Spacing.md,
  },
  navDark: {
    backgroundColor: '#0F172A',
    borderColor: 'rgba(255,255,255,0.08)',
  },
  navLight: {
    backgroundColor: '#FFFFFF',
    borderColor: '#E2E8F0',
  },
  brandSection: {
    gap: Spacing.xxs,
  },
  liveIndicatorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: BorderRadius.full,
    backgroundColor: '#10B981',
  },
  liveText: {
    fontSize: 12,
    fontWeight: '500',
  },
  dateText: {
    fontSize: 18,
    fontWeight: '700',
  },
  actionsSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.sm,
    borderWidth: 1,
    minHeight: 40,
  },
  actionButtonDark: {
    backgroundColor: '#1E293B',
    borderColor: 'rgba(255,255,255,0.1)',
  },
  actionButtonLight: {
    backgroundColor: '#F8FAFC',
    borderColor: '#E2E8F0',
  },
  actionButtonText: {
    fontSize: 13,
    fontWeight: '600',
  },
  iconButton: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.sm,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
  },
  userBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xxs,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
  },
  userBadgeDark: {
    backgroundColor: '#1E293B',
    borderColor: 'rgba(255,255,255,0.06)',
  },
  userBadgeLight: {
    backgroundColor: '#F1F5F9',
    borderColor: '#CBD5E1',
  },
  userAvatar: {
    width: 36,
    height: 36,
    borderRadius: BorderRadius.full,
    backgroundColor: 'rgba(217, 119, 6, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  userInfo: {
    alignItems: 'flex-start',
  },
  userNameText: {
    fontSize: 13,
    fontWeight: '600',
  },
  userRoleText: {
    fontSize: 11,
  },
  textPrimaryDark: {
    color: '#F8FAFC',
  },
  textPrimaryLight: {
    color: '#0F172A',
  },
  textMutedDark: {
    color: '#94A3B8',
  },
  textMutedLight: {
    color: '#64748B',
  },
});
