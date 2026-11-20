import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';
import { DashboardChartsResponse, ChartPeriod } from '../../types/dashboard';

interface ChartsSectionProps {
  charts: DashboardChartsResponse | null;
  selectedPeriod: ChartPeriod;
  onSelectPeriod: (period: ChartPeriod) => void;
  isLoading: boolean;
  isDarkMode: boolean;
}

export const ChartsSection: React.FC<ChartsSectionProps> = ({
  charts,
  selectedPeriod,
  onSelectPeriod,
  isLoading,
  isDarkMode,
}) => {
  const periods: { key: ChartPeriod; label: string }[] = [
    { key: '7d', label: 'آخر 7 أيام' },
    { key: 'month', label: 'هذا الشهر' },
    { key: 'year', label: 'هذا العام' },
  ];

  const trendData = charts?.trend || [];
  const distributionData = charts?.distribution || [];

  const maxRevenue = Math.max(...trendData.map((d) => d.revenue), 100);

  return (
    <View style={styles.container}>
      {/* 1. Main Trend Chart Card */}
      <View style={[styles.card, { flex: 2 }, isDarkMode ? styles.cardDark : styles.cardLight]}>
        {/* Card Header & Period Filters */}
        <View style={styles.cardHeader}>
          <View>
            <Text style={[styles.cardTitle, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
              اتجاهات الإيرادات والطلبات
            </Text>
            <Text style={[styles.cardSubtitle, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
              مخطط بياني حي مسترجع من الـ API
            </Text>
          </View>

          <View style={[styles.periodSelector, isDarkMode ? styles.selectorDark : styles.selectorLight]}>
            {periods.map((p) => {
              const active = selectedPeriod === p.key;
              return (
                <TouchableOpacity
                  key={p.key}
                  style={[
                    styles.periodTab,
                    active && (isDarkMode ? styles.periodTabActiveDark : styles.periodTabActiveLight),
                  ]}
                  onPress={() => onSelectPeriod(p.key)}
                  disabled={isLoading}
                >
                  <Text
                    style={[
                      styles.periodTabText,
                      active ? styles.periodTabTextActive : isDarkMode ? styles.textMutedDark : styles.textMutedLight,
                    ]}
                  >
                    {p.label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        {/* Dynamic Bar/Line Visualizer */}
        <View style={styles.chartCanvasArea}>
          {trendData.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={isDarkMode ? styles.textMutedDark : styles.textMutedLight}>
                {isLoading ? 'جاري جلب بيانات المخطط...' : 'لا توجد بيانات متاحة لهذا النطاق'}
              </Text>
            </View>
          ) : (
            <View style={styles.barsRow}>
              {trendData.map((item, index) => {
                const heightPercentage = Math.max(12, Math.min(100, (item.revenue / maxRevenue) * 100));
                return (
                  <View key={index} style={styles.barColumn}>
                    {/* Tooltip / value preview */}
                    <Text style={[styles.barValueText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}>
                      {item.revenue > 1000 ? `${(item.revenue / 1000).toFixed(1)}k` : item.revenue}
                    </Text>

                    {/* Bar Pillar */}
                    <View style={styles.barTrack}>
                      <View
                        style={[
                          styles.barFill,
                          {
                            height: `${heightPercentage}%`,
                            backgroundColor: index === trendData.length - 1 ? '#F59E0B' : '#3B82F6',
                          },
                        ]}
                      />
                    </View>

                    {/* X-axis date label */}
                    <Text
                      style={[styles.barLabelText, isDarkMode ? styles.textMutedDark : styles.textMutedLight]}
                      numberOfLines={1}
                    >
                      {item.date}
                    </Text>
                  </View>
                );
              })}
            </View>
          )}
        </View>
      </View>

      {/* 2. Distribution Pie/Donut Chart Card */}
      <View style={[styles.card, { flex: 1 }, isDarkMode ? styles.cardDark : styles.cardLight]}>
        <View style={styles.cardHeader}>
          <Text style={[styles.cardTitle, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
            توزيع الإيرادات
          </Text>
        </View>

        <View style={styles.distributionContainer}>
          {distributionData.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={isDarkMode ? styles.textMutedDark : styles.textMutedLight}>
                {isLoading ? 'جاري جلب التوزيع...' : 'لا توجد بيانات توزيع'}
              </Text>
            </View>
          ) : (
            <View style={styles.distributionList}>
              {distributionData.map((item, index) => {
                const colors = ['#10B981', '#3B82F6', '#F59E0B', '#8B5CF6', '#EC4899'];
                const itemColor = item.color || colors[index % colors.length];

                return (
                  <View key={index} style={styles.distributionRow}>
                    <View style={styles.distributionHeaderRow}>
                      <View style={styles.categoryNameRow}>
                        <View style={[styles.colorDot, { backgroundColor: itemColor }]} />
                        <Text style={[styles.categoryText, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                          {item.category}
                        </Text>
                      </View>
                      <Text style={[styles.percentageText, isDarkMode ? styles.textPrimaryDark : styles.textPrimaryLight]}>
                        {item.percentage}%
                      </Text>
                    </View>

                    {/* Progress Track */}
                    <View style={[styles.progressTrack, isDarkMode ? styles.trackDark : styles.trackLight]}>
                      <View
                        style={[
                          styles.progressFill,
                          {
                            width: `${Math.min(100, Math.max(2, item.percentage))}%`,
                            backgroundColor: itemColor,
                          },
                        ]}
                      />
                    </View>
                  </View>
                );
              })}
            </View>
          )}
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.md,
    marginBottom: Spacing.lg,
  },
  card: {
    minWidth: 300,
    padding: Spacing.lg,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
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
    marginBottom: Spacing.md,
    flexWrap: 'wrap',
    gap: Spacing.sm,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
  },
  cardSubtitle: {
    fontSize: 12,
    marginTop: 2,
  },
  periodSelector: {
    flexDirection: 'row',
    padding: 3,
    borderRadius: BorderRadius.sm,
    borderWidth: 1,
  },
  selectorDark: {
    backgroundColor: '#1E293B',
    borderColor: 'rgba(255,255,255,0.08)',
  },
  selectorLight: {
    backgroundColor: '#F1F5F9',
    borderColor: '#CBD5E1',
  },
  periodTab: {
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xxs,
    borderRadius: BorderRadius.xs,
  },
  periodTabActiveDark: {
    backgroundColor: '#3B82F6',
  },
  periodTabActiveLight: {
    backgroundColor: '#FFFFFF',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  periodTabText: {
    fontSize: 12,
    fontWeight: '600',
  },
  periodTabTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  chartCanvasArea: {
    height: 200,
    justifyContent: 'flex-end',
    marginTop: Spacing.md,
  },
  barsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    height: 180,
    gap: Spacing.xs,
  },
  barColumn: {
    flex: 1,
    alignItems: 'center',
    height: '100%',
    justifyContent: 'flex-end',
  },
  barTrack: {
    width: '60%',
    maxWidth: 24,
    height: 130,
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderRadius: BorderRadius.xs,
    justifyContent: 'flex-end',
    overflow: 'hidden',
  },
  barFill: {
    width: '100%',
    borderRadius: BorderRadius.xs,
  },
  barValueText: {
    fontSize: 10,
    fontWeight: '600',
    marginBottom: 4,
  },
  barLabelText: {
    fontSize: 10,
    marginTop: 6,
  },
  distributionContainer: {
    marginTop: Spacing.sm,
  },
  distributionList: {
    gap: Spacing.md,
  },
  distributionRow: {
    gap: Spacing.xxs,
  },
  distributionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  categoryNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  colorDot: {
    width: 10,
    height: 10,
    borderRadius: BorderRadius.full,
  },
  categoryText: {
    fontSize: 13,
    fontWeight: '600',
  },
  percentageText: {
    fontSize: 13,
    fontWeight: '700',
  },
  progressTrack: {
    height: 8,
    borderRadius: BorderRadius.full,
    overflow: 'hidden',
  },
  trackDark: {
    backgroundColor: '#1E293B',
  },
  trackLight: {
    backgroundColor: '#E2E8F0',
  },
  progressFill: {
    height: '100%',
    borderRadius: BorderRadius.full,
  },
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
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
