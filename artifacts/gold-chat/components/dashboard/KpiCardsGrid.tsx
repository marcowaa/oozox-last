import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';
import { DashboardStats } from '../../types/dashboard';
import { KpiFilterType } from '../../store/useDashboardStore';

interface KpiCardsGridProps {
  stats: DashboardStats | null;
  activeFilter: KpiFilterType;
  onSelectFilter: (filter: KpiFilterType) => void;
  isDarkMode: boolean;
}

interface CardConfig {
  key: KpiFilterType;
  title: string;
  value: string;
  growth: number;
  growthLabel: string;
  icon: keyof typeof Ionicons.glyphMap;
  accentColor: string;
}

export const KpiCardsGrid: React.FC<KpiCardsGridProps> = ({
  stats,
  activeFilter,
  onSelectFilter,
  isDarkMode,
}) => {
  if (!stats) return null;

  const formatCurrency = (amount: number, currency: string = 'SAR'): string => {
    return new Intl.NumberFormat('ar-SA', {
      style: 'currency',
      currency: currency || 'SAR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatNumber = (num: number): string => {
    return new Intl.NumberFormat('ar-SA').format(num);
  };

  const cards: CardConfig[] = [
    {
      key: 'revenue',
      title: 'إجمالي الإيرادات',
      value: formatCurrency(stats.totalRevenue, stats.revenueCurrency),
      growth: stats.revenueGrowthPercentage,
      growthLabel: 'مقارنة بالشهر السابق',
      icon: 'cash-outline',
      accentColor: '#10B981',
    },
    {
      key: 'customers',
      title: 'إجمالي العملاء',
      value: formatNumber(stats.totalCustomers),
      growth: stats.customersGrowthPercentage,
      growthLabel: 'عملاء جدد نشطين',
      icon: 'people-outline',
      accentColor: '#3B82F6',
    },
    {
      key: 'orders',
      title: 'الطلبات النشطة',
      value: formatNumber(stats.activeOrders),
      growth: stats.ordersGrowthPercentage,
      growthLabel: 'طلبات قيد المعالجة',
      icon: 'cube-outline',
      accentColor: '#F59E0B',
    },
    {
      key: 'growth',
      title: 'نسبة النمو الإجمالية',
      value: `${stats.overallGrowthRate > 0 ? '+' : ''}${stats.overallGrowthRate}%`,
      growth: stats.overallGrowthRate,
      growthLabel: 'المعدل التراكمي العام',
      icon: 'trending-up-outline',
      accentColor: '#8B5CF6',
    },
  ];

  return (
    <View style={styles.gridContainer}>
      {cards.map((card) => {
        const isSelected = activeFilter === card.key;
        const isPositive = card.growth >= 0;

        return (
          <TouchableOpacity
            key={card.key}
            style={[
              styles.kpiCard,
              isDarkMode ? styles.cardDark : styles.cardLight,
              isSelected && {
                borderColor: card.accentColor,
                borderWidth: 2,
                backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.95)' : '#F8FAFC',
              },
            ]}
            onPress={() => onSelectFilter(isSelected ? 'all' : card.key)}
            activeOpacity={0.8}
          >
            {/* Header: Title & Icon */}
            <View style={styles.cardHeader}>
              <Text style={[styles.cardTitle, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                {card.title}
              </Text>
              <View
                style={[
                  styles.iconBox,
                  { backgroundColor: `${card.accentColor}18` },
                ]}
              >
                <Ionicons name={card.icon} size={20} color={card.accentColor} />
              </View>
            </View>

            {/* Value Display */}
            <Text
              style={[styles.cardValue, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}
              numberOfLines={1}
            >
              {card.value}
            </Text>

            {/* Growth Footer & Filter Pill */}
            <View style={styles.cardFooter}>
              <View
                style={[
                  styles.growthBadge,
                  {
                    backgroundColor: isPositive ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                  },
                ]}
              >
                <Ionicons
                  name={isPositive ? 'arrow-up' : 'arrow-down'}
                  size={12}
                  color={isPositive ? '#10B981' : '#EF4444'}
                />
                <Text
                  style={[
                    styles.growthText,
                    { color: isPositive ? '#10B981' : '#EF4444' },
                  ]}
                >
                  {Math.abs(card.growth)}%
                </Text>
              </View>
              <Text
                style={[styles.growthLabelText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}
                numberOfLines={1}
              >
                {card.growthLabel}
              </Text>
            </View>

            {isSelected && (
              <View style={[styles.activeFilterIndicator, { backgroundColor: card.accentColor }]}>
                <Text style={styles.activeFilterText}>تصفية الجدول مفعلة</Text>
              </View>
            )}
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.md,
    marginBottom: Spacing.lg,
  },
  kpiCard: {
    flex: 1,
    minWidth: 240,
    padding: Spacing.md,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    justifyContent: 'space-between',
    minHeight: 140,
  },
  cardDark: {
    backgroundColor: '#0F172A',
    borderColor: 'rgba(255,255,255,0.08)',
  },
  cardLight: {
    backgroundColor: '#FFFFFF',
    borderColor: '#E2E8F0',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  cardTitle: {
    fontSize: 13,
    fontWeight: '600',
  },
  iconBox: {
    width: 36,
    height: 36,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardValue: {
    fontSize: 24,
    fontWeight: '700',
    marginVertical: Spacing.xs,
    letterSpacing: -0.5,
  },
  cardFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    marginTop: Spacing.xs,
  },
  growthBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.xs,
    paddingVertical: 2,
    borderRadius: BorderRadius.xs,
    gap: 2,
  },
  growthText: {
    fontSize: 12,
    fontWeight: '700',
  },
  growthLabelText: {
    fontSize: 11,
    flex: 1,
  },
  activeFilterIndicator: {
    marginTop: Spacing.xs,
    paddingVertical: 2,
    paddingHorizontal: Spacing.xs,
    borderRadius: BorderRadius.xs,
    alignSelf: 'flex-start',
  },
  activeFilterText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
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
