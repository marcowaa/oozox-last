import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
  StyleSheet,
  ActivityIndicator,
  Animated,
  Dimensions,
  Alert,
} from 'react-native';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/api';
import { getErrorMessage } from '../utils/errorHandler';

const { width } = Dimensions.get('window');

/**
 * Shimmer placeholder component for dashboard skeleton loading state
 */
function DashboardSkeleton() {
  const animatedValue = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(animatedValue, {
          toValue: 1,
          duration: 1000,
          useNativeDriver: true,
        }),
        Animated.timing(animatedValue, {
          toValue: 0,
          duration: 1000,
          useNativeDriver: true,
        }),
      ])
    );
    animation.start();
    return () => animation.stop();
  }, [animatedValue]);

  const opacity = animatedValue.interpolate({
    inputRange: [0, 1],
    outputRange: [0.3, 0.75],
  });

  return (
    <View style={styles.skeletonContainer}>
      <Animated.View style={[styles.skeletonCard, { height: 110, opacity }]} />
      <Animated.View style={[styles.skeletonCard, { height: 160, opacity }]} />
      <Animated.View style={[styles.skeletonCard, { height: 90, opacity }]} />
      <Animated.View style={[styles.skeletonCard, { height: 180, opacity }]} />
    </View>
  );
}

export default function DashboardScreen({ navigation }) {
  const { user, logout, reloadUserProfile } = useAuth();

  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  // Live Simulated Gold Rates for quick actions
  const [goldRates] = useState({
    k24: 4250,
    k21: 3720,
    k18: 3188,
    change24h: '+1.45%',
  });

  // Calculate Metrics from Invoices
  const metrics = React.useMemo(() => {
    let pendingCount = 0;
    let pendingAmount = 0;
    let paidCount = 0;
    let paidAmount = 0;

    invoices.forEach((inv) => {
      if (inv.status === 'PENDING') {
        pendingCount += 1;
        pendingAmount += inv.totalAmount || 0;
      } else if (inv.status === 'PAID') {
        paidCount += 1;
        paidAmount += inv.totalAmount || 0;
      }
    });

    return {
      pendingCount,
      pendingAmount,
      paidCount,
      paidAmount,
      totalCount: invoices.length,
    };
  }, [invoices]);

  const fetchDashboardData = useCallback(async () => {
    setErrorMessage(null);
    try {
      // 1. Fetch recent invoices
      const response = await api.getInvoices({ limit: 5 });
      if (response.data?.data?.invoices) {
        setInvoices(response.data.data.invoices);
      }
      // 2. Silently reload user profile in background
      await reloadUserProfile();
    } catch (error) {
      const msg = getErrorMessage(error, 'تعذر جلب بيانات لوحة التحكم من السيرفر');
      setErrorMessage(msg);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [reloadUserProfile]);

  useEffect(() => {
    setLoading(true);
    fetchDashboardData();
  }, [fetchDashboardData]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchDashboardData();
  };

  const handleLogout = () => {
    Alert.alert(
      'تسجيل الخروج',
      'هل أنت متأكد من رغبتك في تسجيل الخروج من تطبيق OOZOX؟',
      [
        { text: 'إلغاء', style: 'cancel' },
        {
          text: 'تسجيل الخروج',
          style: 'destructive',
          onPress: () => logout(),
        },
      ]
    );
  };

  const renderStatusBadge = (status) => {
    let bg = '#F39C12';
    let label = 'قيد الانتظار';
    if (status === 'PAID') {
      bg = '#27AE60';
      label = 'مسددة ✅';
    } else if (status === 'OVERDUE') {
      bg = '#E74C3C';
      label = 'متأخرة ⚠️';
    } else if (status === 'CANCELLED') {
      bg = '#7F8C8D';
      label = 'ملغاة ❌';
    }
    return (
      <View style={[styles.statusBadge, { backgroundColor: bg }]}>
        <Text style={styles.statusBadgeText}>{label}</Text>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      {/* Top Header Bar */}
      <View style={styles.headerBar}>
        <TouchableOpacity
          style={styles.logoutBtn}
          onPress={handleLogout}
          accessibilityLabel="تسجيل الخروج"
        >
          <Text style={styles.logoutBtnText}>خروج 🚪</Text>
        </TouchableOpacity>

        <View style={styles.userInfo}>
          <Text style={styles.greetingText}>مرحباً بك، 👋</Text>
          <Text style={styles.userNameText}>{user?.name || 'مستخدم OOZOX'}</Text>
          <View style={styles.roleTag}>
            <Text style={styles.roleTagText}>{user?.role || 'USER'}</Text>
          </View>
        </View>
      </View>

      {/* Error Alert Banner */}
      {errorMessage && (
        <View style={styles.errorBanner}>
          <Text style={styles.errorText}>{errorMessage}</Text>
          <TouchableOpacity onPress={fetchDashboardData} style={styles.retryBtn}>
            <Text style={styles.retryBtnText}>إعادة المحاولة 🔄</Text>
          </TouchableOpacity>
        </View>
      )}

      {loading && !refreshing ? (
        <DashboardSkeleton />
      ) : (
        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              colors={['#D4AF37']}
              tintColor="#D4AF37"
            />
          }
        >
          {/* Live Gold Price Ticker */}
          <View style={styles.tickerCard}>
            <View style={styles.tickerHeader}>
              <Text style={styles.tickerTag}>{goldRates.change24h}</Text>
              <Text style={styles.tickerTitle}>✨ أسعار الذهب الحية (سوق الصاغة)</Text>
            </View>
            <View style={styles.tickerRow}>
              <View style={styles.tickerItem}>
                <Text style={styles.tickerKarat}>عيار 18</Text>
                <Text style={styles.tickerPrice}>{goldRates.k18.toLocaleString()} ج.م</Text>
              </View>
              <View style={styles.tickerItem}>
                <Text style={styles.tickerKarat}>عيار 21 (الرسمي)</Text>
                <Text style={[styles.tickerPrice, styles.goldHighlight]}>
                  {goldRates.k21.toLocaleString()} ج.م
                </Text>
              </View>
              <View style={styles.tickerItem}>
                <Text style={styles.tickerKarat}>عيار 24</Text>
                <Text style={styles.tickerPrice}>{goldRates.k24.toLocaleString()} ج.م</Text>
              </View>
            </View>
          </View>

          {/* Balance & Invoices Financial Summary */}
          <View style={styles.summaryCard}>
            <Text style={styles.cardSectionTitle}>ملخص المعاملات المالية 💼</Text>
            <View style={styles.summaryGrid}>
              <View style={styles.summaryMetric}>
                <Text style={styles.metricLabel}>المستحقات المعلقة</Text>
                <Text style={[styles.metricValue, { color: '#F39C12' }]}>
                  {metrics.pendingAmount.toLocaleString()} ج.م
                </Text>
                <Text style={styles.metricSub}>{metrics.pendingCount} فواتير</Text>
              </View>

              <View style={styles.summaryMetric}>
                <Text style={styles.metricLabel}>المبالغ المسددة</Text>
                <Text style={[styles.metricValue, { color: '#27AE60' }]}>
                  {metrics.paidAmount.toLocaleString()} ج.م
                </Text>
                <Text style={styles.metricSub}>{metrics.paidCount} فواتير</Text>
              </View>
            </View>
          </View>

          {/* Gold Quick Actions */}
          <View style={styles.actionsSection}>
            <Text style={styles.sectionHeader}>الإجراءات السريعة للمعاملات ⚡</Text>
            <View style={styles.actionGrid}>
              <TouchableOpacity
                style={[styles.actionCard, { backgroundColor: '#1A2332', borderColor: '#58A6FF' }]}
                onPress={() => navigation?.navigate?.('CreateInvoice')}
              >
                <Text style={styles.actionIcon}>📝</Text>
                <Text style={styles.actionTitle}>إصدار فاتورة</Text>
                <Text style={styles.actionSubtitle}>إنشاء وتوثيق فوري</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.actionCard, { backgroundColor: '#242014', borderColor: '#D4AF37' }]}
                onPress={() => navigation?.navigate?.('Invoices')}
              >
                <Text style={styles.actionIcon}>📋</Text>
                <Text style={styles.actionTitle}>سجل الفواتير</Text>
                <Text style={styles.actionSubtitle}>عرض ومتابعة السداد</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.actionCard, { backgroundColor: '#1C281F', borderColor: '#2EA043' }]}
                onPress={() => Alert.alert('صفقة جديدة', 'جاهز لبدء صفقة شراء/بيع ذهب جديدة')}
              >
                <Text style={styles.actionIcon}>🪙</Text>
                <Text style={styles.actionTitle}>تسجيل صفقة ذهب</Text>
                <Text style={styles.actionSubtitle}>شراء / بيع / مقايضة</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.actionCard, { backgroundColor: '#2A1B28', borderColor: '#BC8CFF' }]}
                onPress={() => Alert.alert('المحفظة الذهبية', 'إجمالي رصيد الذهب والسيولة النقدية')}
              >
                <Text style={styles.actionIcon}>⚖️</Text>
                <Text style={styles.actionTitle}>الخزنة والجرامات</Text>
                <Text style={styles.actionSubtitle}>جرد الأرصدة والعيارات</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Recent Invoices List Section */}
          <View style={styles.recentSection}>
            <View style={styles.sectionHeaderRow}>
              <TouchableOpacity onPress={() => navigation?.navigate?.('Invoices')}>
                <Text style={styles.viewAllText}>عرض الكل ({metrics.totalCount}) ←</Text>
              </TouchableOpacity>
              <Text style={styles.sectionHeader}>أحدث الفواتير المسجلة</Text>
            </View>

            {invoices.length === 0 ? (
              <View style={styles.emptyCard}>
                <Text style={styles.emptyIcon}>📂</Text>
                <Text style={styles.emptyTitle}>لا توجد فواتير حديثة</Text>
                <Text style={styles.emptyText}>ابدأ بإصدار أول فاتورة عبر زر الإجراء السريع أعلاه</Text>
              </View>
            ) : (
              invoices.slice(0, 4).map((inv) => {
                const isSender = inv.sender?._id === user?.id;
                const partyName = isSender ? inv.receiver?.name : inv.sender?.name;
                return (
                  <TouchableOpacity
                    key={inv._id}
                    style={styles.invoiceItemCard}
                    onPress={() => navigation?.navigate?.('Invoices')}
                  >
                    <View style={styles.invoiceItemTop}>
                      <Text style={styles.invoiceNumber}>{inv.invoiceNumber}</Text>
                      {renderStatusBadge(inv.status)}
                    </View>
                    <View style={styles.invoiceItemBottom}>
                      <Text style={styles.invoiceAmount}>
                        {inv.totalAmount?.toLocaleString()} ج.م
                      </Text>
                      <Text style={styles.invoiceParty}>
                        {isSender ? `إلى: ${partyName || 'طرف مستلم'}` : `من: ${partyName || 'طرف مرسل'}`}
                      </Text>
                    </View>
                  </TouchableOpacity>
                );
              })
            )}
          </View>
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0D1117' },
  headerBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 14,
    backgroundColor: '#161B22',
    borderBottomWidth: 1,
    borderBottomColor: '#30363D',
  },
  userInfo: { alignItems: 'flex-end' },
  greetingText: { color: '#8B949E', fontSize: 12 },
  userNameText: { color: '#F0F6FC', fontSize: 18, fontWeight: 'bold', marginTop: 2 },
  roleTag: {
    backgroundColor: '#21262D',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
    marginTop: 4,
    borderWidth: 1,
    borderColor: '#30363D',
  },
  roleTagText: { color: '#D4AF37', fontSize: 11, fontWeight: 'bold' },
  logoutBtn: {
    backgroundColor: '#21262D',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#DA3633',
  },
  logoutBtnText: { color: '#FF7B72', fontSize: 13, fontWeight: 'bold' },
  scrollView: { flex: 1 },
  scrollContent: { padding: 16, paddingBottom: 40 },
  errorBanner: {
    backgroundColor: '#3E1F1F',
    padding: 12,
    margin: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#DA3633',
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  errorText: { color: '#FF7B72', fontSize: 13, flex: 1, textAlign: 'right' },
  retryBtn: {
    backgroundColor: '#DA3633',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 4,
    marginLeft: 8,
  },
  retryBtnText: { color: '#FFF', fontSize: 12, fontWeight: 'bold' },
  tickerCard: {
    backgroundColor: '#161B22',
    borderRadius: 12,
    padding: 14,
    borderWidth: 1,
    borderColor: '#D4AF37',
    marginBottom: 16,
  },
  tickerHeader: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  tickerTitle: { color: '#F0F6FC', fontSize: 14, fontWeight: 'bold' },
  tickerTag: {
    color: '#3FB950',
    backgroundColor: '#12261E',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    fontSize: 12,
    fontWeight: 'bold',
  },
  tickerRow: { flexDirection: 'row-reverse', justifyContent: 'space-between' },
  tickerItem: { alignItems: 'center', flex: 1 },
  tickerKarat: { color: '#8B949E', fontSize: 12 },
  tickerPrice: { color: '#F0F6FC', fontSize: 14, fontWeight: 'bold', marginTop: 4 },
  goldHighlight: { color: '#D4AF37', fontSize: 16 },
  summaryCard: {
    backgroundColor: '#161B22',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#30363D',
    marginBottom: 16,
  },
  cardSectionTitle: {
    color: '#F0F6FC',
    fontSize: 15,
    fontWeight: 'bold',
    textAlign: 'right',
    marginBottom: 12,
  },
  summaryGrid: { flexDirection: 'row-reverse', justifyContent: 'space-between' },
  summaryMetric: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: '#21262D',
    padding: 12,
    borderRadius: 8,
    marginHorizontal: 4,
  },
  metricLabel: { color: '#8B949E', fontSize: 12 },
  metricValue: { fontSize: 16, fontWeight: 'bold', marginVertical: 4 },
  metricSub: { color: '#8B949E', fontSize: 11 },
  actionsSection: { marginBottom: 20 },
  sectionHeader: {
    color: '#F0F6FC',
    fontSize: 16,
    fontWeight: 'bold',
    textAlign: 'right',
    marginBottom: 12,
  },
  sectionHeaderRow: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  viewAllText: { color: '#58A6FF', fontSize: 13, fontWeight: 'bold' },
  actionGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
  actionCard: {
    width: (width - 44) / 2,
    borderRadius: 10,
    padding: 14,
    borderWidth: 1,
    marginBottom: 12,
    alignItems: 'flex-end',
  },
  actionIcon: { fontSize: 24, marginBottom: 8 },
  actionTitle: { color: '#F0F6FC', fontSize: 14, fontWeight: 'bold', marginBottom: 2 },
  actionSubtitle: { color: '#8B949E', fontSize: 11, textAlign: 'right' },
  recentSection: { marginBottom: 20 },
  invoiceItemCard: {
    backgroundColor: '#161B22',
    borderRadius: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: '#30363D',
    marginBottom: 10,
  },
  invoiceItemTop: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  invoiceNumber: { color: '#F0F6FC', fontWeight: 'bold', fontSize: 14 },
  statusBadge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 10 },
  statusBadgeText: { color: '#FFF', fontSize: 10, fontWeight: 'bold' },
  invoiceItemBottom: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  invoiceAmount: { color: '#D4AF37', fontWeight: 'bold', fontSize: 15 },
  invoiceParty: { color: '#8B949E', fontSize: 12 },
  emptyCard: {
    backgroundColor: '#161B22',
    borderRadius: 10,
    padding: 24,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#30363D',
  },
  emptyIcon: { fontSize: 32, marginBottom: 8 },
  emptyTitle: { color: '#F0F6FC', fontSize: 16, fontWeight: 'bold' },
  emptyText: { color: '#8B949E', fontSize: 12, marginTop: 4, textAlign: 'center' },
  skeletonContainer: { padding: 16 },
  skeletonCard: { backgroundColor: '#21262D', borderRadius: 12, marginBottom: 16 },
});
