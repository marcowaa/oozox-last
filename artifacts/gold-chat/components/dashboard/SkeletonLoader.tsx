import React from 'react';
import { View, StyleSheet, Animated } from 'react-native';
import { Spacing, BorderRadius } from '../../../constants/DesignSystem';

interface SkeletonProps {
  width?: number | string;
  height?: number | string;
  borderRadius?: number;
  style?: object;
}

export const SkeletonBox: React.FC<SkeletonProps> = ({
  width = '100%',
  height = 20,
  borderRadius = BorderRadius.sm,
  style,
}) => {
  const animatedValue = React.useRef(new Animated.Value(0.3)).current;

  React.useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(animatedValue, {
          toValue: 0.8,
          duration: 900,
          useNativeDriver: false,
        }),
        Animated.timing(animatedValue, {
          toValue: 0.3,
          duration: 900,
          useNativeDriver: false,
        }),
      ])
    );
    animation.start();
    return () => animation.stop();
  }, [animatedValue]);

  return (
    <Animated.View
      style={[
        styles.skeletonBase,
        {
          width: width as any,
          height: height as any,
          borderRadius,
          opacity: animatedValue,
        },
        style,
      ]}
    />
  );
};

export const KpiCardSkeleton: React.FC = () => (
  <View style={styles.kpiSkeletonCard}>
    <View style={styles.kpiHeaderRow}>
      <SkeletonBox width={100} height={16} />
      <SkeletonBox width={32} height={32} borderRadius={BorderRadius.md} />
    </View>
    <SkeletonBox width={140} height={32} style={{ marginVertical: Spacing.sm }} />
    <View style={styles.kpiFooterRow}>
      <SkeletonBox width={60} height={14} />
      <SkeletonBox width={80} height={14} />
    </View>
  </View>
);

export const ChartsSectionSkeleton: React.FC = () => (
  <View style={styles.chartsContainer}>
    <View style={[styles.chartCard, { flex: 2 }]}>
      <View style={styles.chartHeader}>
        <SkeletonBox width={160} height={20} />
        <SkeletonBox width={120} height={32} borderRadius={BorderRadius.sm} />
      </View>
      <SkeletonBox width="100%" height={240} style={{ marginTop: Spacing.md }} />
    </View>
    <View style={[styles.chartCard, { flex: 1 }]}>
      <SkeletonBox width={140} height={20} />
      <SkeletonBox width="100%" height={240} borderRadius={BorderRadius.full} style={{ marginTop: Spacing.md }} />
    </View>
  </View>
);

export const TableSkeleton: React.FC = () => (
  <View style={styles.tableCard}>
    <View style={styles.tableHeader}>
      <SkeletonBox width={180} height={24} />
      <SkeletonBox width={200} height={36} borderRadius={BorderRadius.sm} />
    </View>
    {[1, 2, 3, 4, 5].map((i) => (
      <View key={i} style={styles.tableRow}>
        <SkeletonBox width={80} height={16} />
        <SkeletonBox width={120} height={16} />
        <SkeletonBox width={90} height={16} />
        <SkeletonBox width={70} height={24} borderRadius={BorderRadius.sm} />
        <SkeletonBox width={60} height={24} borderRadius={BorderRadius.sm} />
      </View>
    ))}
  </View>
);

const styles = StyleSheet.create({
  skeletonBase: {
    backgroundColor: '#334155',
  },
  kpiSkeletonCard: {
    flex: 1,
    minWidth: 220,
    backgroundColor: '#1E293B',
    padding: Spacing.md,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  kpiHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  kpiFooterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  chartsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.lg,
    marginVertical: Spacing.lg,
  },
  chartCard: {
    minWidth: 280,
    backgroundColor: '#1E293B',
    padding: Spacing.lg,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  chartHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  tableCard: {
    backgroundColor: '#1E293B',
    padding: Spacing.lg,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
    marginTop: Spacing.lg,
  },
  tableHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  tableRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.04)',
  },
});
