import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, AccessibilityInfo } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Spacing, BorderRadius, Gap, TouchTarget, TypographyScale } from '../../../constants/DesignSystem';
import { DashboardStats } from '../../types/dashboard';
import { KpiFilterType } from '../../store/useDashboardStore';

export interface DashboardGridProps {
  /** Core metrics data returned by the backend stats API */
  stats: DashboardStats | null;
  /** Active KPI filter selection (defaults to 'all') */
  activeFilter?: KpiFilterType;
  /** Callback triggered when a KPI card is tapped */
  onSelectFilter?: (filter: KpiFilterType) => void;
  /** Theme toggle flag */
  isDarkMode?: boolean;
  /** Optional custom container style */
  style?: object;
}

interface KpiCardItem {
  key: KpiFilterType;
  title: string;
  value: string;
  growth: number;
  growthLabel: string;
  icon: keyof typeof Ionicons.glyphMap;
  accentColor: string;
  testId: string;
}

/**
 * DashboardGrid Component
 * 
 * Renders the 4 core KPI cards (Revenue, Customers, Active Orders, Overall Growth)
 * adhering strictly to the 8px spatial grid token system:
 * - Margins & Paddings: Spacing.xs (8px), Spacing.sm (12px), Spacing.md (16px), Spacing.lg (24px)
 * - Border Radii: BorderRadius.xs (4px), BorderRadius.md (12px), BorderRadius.lg (16px)
 * - Accessible Touch Targets: TouchTarget.min (48px)
 */
export const DashboardGrid: React.FC<DashboardGridProps> = ({
  stats,
  activeFilter = 'all',
  onSelectFilter,
  isDarkMode = true,
  style,
}) => {
  if (!stats) return null;

  // Format currency with Arabic locale
  const formatCurrency = (amount: number, currency: string = 'SAR'): string => {
    try {
      return new Intl.NumberFormat('ar-SA', {
        style: 'currency',
        currency: currency || 'SAR',
        maximumFractionDigits: 0,
      }).format(amount);
    } catch {
      return `${amount.toLocaleString('ar-SA')} ${currency || 'ر.س'}`;
    }
  };

  // Format numbers with localized separators
  const formatNumber = (num: number): string => {
    try {
      return new Intl.NumberFormat('ar-SA').format(num);
    } catch {
      return num.toLocaleString('ar-SA');
    }
  };

  // 4 Core KPI definitions
  const kpiCards: KpiCardItem[] = [
    {
      key: 'revenue',
      title: 'إجمالي الإيرادات',
      value: formatCurrency(stats.totalRevenue, stats.revenueCurrency),
      growth: stats.revenueGrowthPercentage,
      growthLabel: 'مقارنة بالشهر السابق',
      icon: 'cash-outline',
      accentColor: '#10B981', // Emerald Green
      testId: 'kpi_card_revenue',
    },
    {
      key: 'customers',
      title: 'إجمالي العملاء',
      value: formatNumber(stats.totalCustomers),
      growth: stats.customersGrowthPercentage,
      growthLabel: 'عملاء نشطين',
      icon: 'people-outline',
      accentColor: '#3B82F6', // Royal Blue
      testId: 'kpi_card_customers',
    },
    {
      key: 'orders',
      title: 'الطلبات النشطة',
      value: formatNumber(stats.activeOrders),
      growth: stats.ordersGrowthPercentage,
      growthLabel: 'طلبات قيد المعالجة',
      icon: 'cube-outline',
      accentColor: '#F59E0B', // Amber
      testId: 'kpi_card_orders',
    },
    {
      key: 'growth',
      title: 'نسبة النمو الإجمالية',
      value: `${stats.overallGrowthRate > 0 ? '+' : ''}${stats.overallGrowthRate}%`,
      growth: stats.overallGrowthRate,
      growthLabel: 'المعدل التراكمي السنوي',
      icon: 'trending-up-outline',
      accentColor: '#8B5CF6', // Purple
      testId: 'kpi_card_growth',
    },
  ];

  return (
    <View style={[styles.gridContainer, style]} testID="dashboard_kpi_grid">
      {kpiCards.map((card) => {
        const isSelected = activeFilter === card.key;
        const isPositive = card.growth >= 0;

        return (
          <TouchableOpacity
            key={card.key}
            testID={card.testId}
            accessible={true}
            accessibilityRole="button"
            accessibilityLabel={`${card.title}: ${card.value}, نسبة النمو ${card.growth}% ${card.growthLabel}`}
            accessibilityState={{ selected: isSelected }}
            style={[
              styles.card,
              isDarkMode ? styles.cardDark : styles.cardLight,
              isSelected && {
                borderColor: card.accentColor,
                borderWidth: 2,
                backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.95)' : '#F8FAFC',
              },
            ]}
            onPress={() => {
              if (onSelectFilter) {
                onSelectFilter(isSelected ? 'all' : card.key);
              }
            }}
            activeOpacity={0.8}
          >
            {/* Header: Title & Accent Icon */}
            <View style={styles.cardHeader}>
              <Text
                style={[
                  styles.cardTitle,
                  isDarkMode ? styles.textMutedDark : styles.textMutedLight,
                ]}
                numberOfLines={1}
              >
                {card.title}
              </Text>
              <View
                style={[
                  styles.iconContainer,
                  { backgroundColor: `${card.accentColor}18` },
                ]}
              >
                <Ionicons name={card.icon} size={20} color={card.accentColor} />
              </View>
            </View>

            {/* Core KPI Metric Value Display */}
            <Text
              style={[
                styles.cardValue,
                isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight,
              ]}
              numberOfLines={1}
              adjustsFontSizeToFit={true}
            >
              {card.value}
            </Text>

            {/* Footer: Trend Growth Badge & Comparison Period */}
            <View style={styles.cardFooter}>
              <View
                style={[
                  styles.growthBadge,
                  {
                    backgroundColor: isPositive
                      ? 'rgba(16, 185, 129, 0.14)'
                      : 'rgba(239, 68, 68, 0.14)',
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
                    styles.growthPercentage,
                    { color: isPositive ? '#10B981' : '#EF4444' },
                  ]}
                >
                  {Math.abs(card.growth)}%
                </Text>
              </View>

              <Text
                style={[
                  styles.growthLabel,
                  isDarkMode ? styles.textMutedDark : styles.textMutedLight,
                ]}
                numberOfLines={1}
              >
                {card.growthLabel}
              </Text>
            </View>

            {/* Active Filter Pill indicator */}
            {isSelected && (
              <View
                style={[
                  styles.activeFilterIndicator,
                  { backgroundColor: card.accentColor },
                ]}
              >
                <Ionicons name="checkmark-circle" size={10} color="#FFFFFF" />
                <Text style={styles.activeFilterText}>تصفية مفعلة</Text>
              </View>
            )}
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  // Grid layout container conforming to 8px base gap & margin
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Gap.md, // 16px gap
    marginBottom: Spacing.lg, // 24px bottom margin
    width: '100%',
  },

  // KPI Card item with strict 8px tokens
  card: {
    flex: 1,
    minWidth: 240,
    minHeight: 144, // Generous height respecting 8px multiple (18 * 8)
    padding: Spacing.md, // 16px internal padding
    borderRadius: BorderRadius.lg, // 16px rounded corners
    borderWidth: 1,
    justifyContent: 'space-between',
  },

  cardDark: {
    backgroundColor: '#0F172A', // Slate 900
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },

  cardLight: {
    backgroundColor: '#FFFFFF',
    borderColor: '#E2E8F0', // Slate 200
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 6,
    elevation: 2,
  },

  // Header row with 8px margin
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs, // 8px margin
  },

  cardTitle: {
    fontSize: 13,
    fontWeight: '600',
    flex: 1,
    letterSpacing: 0.1,
  },

  // Icon container conforming to 8px geometry
  iconContainer: {
    width: 36,
    height: 36,
    borderRadius: BorderRadius.md, // 12px
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Main KPI Value Typography
  cardValue: {
    fontSize: 24, // 3 * 8
    lineHeight: 32, // 4 * 8
    fontWeight: '700',
    marginVertical: Spacing.xs, // 8px margin
    letterSpacing: -0.5,
  },

  // Footer trend row
  cardFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs, // 8px gap
    marginTop: Spacing.xs, // 8px margin
  },

  // Growth percentage pill
  growthBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.xs, // 8px padding
    paddingVertical: 2,
    borderRadius: BorderRadius.xs, // 4px radius
    gap: 4,
  },

  growthPercentage: {
    fontSize: 12,
    fontWeight: '700',
  },

  growthLabel: {
    fontSize: 11,
    flex: 1,
  },

  // Filter indicator badge
  activeFilterIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: Spacing.xs, // 8px margin
    paddingVertical: 2,
    paddingHorizontal: Spacing.xs, // 8px padding
    borderRadius: BorderRadius.xs, // 4px radius
    alignSelf: 'flex-start',
  },

  activeFilterText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },

  // Typography color variants
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

export default DashboardGrid;
