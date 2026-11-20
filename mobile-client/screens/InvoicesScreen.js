import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  StyleSheet,
  Alert,
  Animated,
  Dimensions,
} from 'react-native';
import api from '../api/api';
import { getErrorMessage } from '../utils/errorHandler';
import { useAuth } from '../context/AuthContext';

const { width } = Dimensions.get('window');

/**
 * Polished animated shimmer placeholder for skeleton loading states
 */
function ShimmerPlaceholder({ style }) {
  const animatedValue = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(animatedValue, {
          toValue: 1,
          duration: 1100,
          useNativeDriver: true,
        }),
        Animated.timing(animatedValue, {
          toValue: 0,
          duration: 1100,
          useNativeDriver: true,
        }),
      ])
    );
    animation.start();
    return () => animation.stop();
  }, [animatedValue]);

  const opacity = animatedValue.interpolate({
    inputRange: [0, 1],
    outputRange: [0.35, 0.85],
  });

  return (
    <Animated.View
      style={[
        styles.skeletonBase,
        style,
        { opacity },
      ]}
    />
  );
}

/**
 * Realistic Invoice Card Skeleton Mock
 */
function InvoiceCardSkeleton() {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <ShimmerPlaceholder style={{ width: 140, height: 20, borderRadius: 6 }} />
        <ShimmerPlaceholder style={{ width: 80, height: 22, borderRadius: 12 }} />
      </View>

      <View style={{ marginVertical: 12 }}>
        <ShimmerPlaceholder style={{ width: 190, height: 14, borderRadius: 4, alignSelf: 'flex-end' }} />
      </View>

      <View style={styles.itemsSummary}>
        <ShimmerPlaceholder style={{ width: 90, height: 14, borderRadius: 4 }} />
        <ShimmerPlaceholder style={{ width: 110, height: 22, borderRadius: 6 }} />
      </View>

      <View style={styles.cardFooter}>
        <ShimmerPlaceholder style={{ width: 120, height: 12, borderRadius: 4 }} />
        <ShimmerPlaceholder style={{ width: 95, height: 28, borderRadius: 6 }} />
      </View>
    </View>
  );
}

export default function InvoicesScreen({ navigation }) {
  const { user } = useAuth();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filterRole, setFilterRole] = useState('all'); // all, sent, received
  const [errorMessage, setErrorMessage] = useState(null);

  const fetchInvoices = useCallback(async () => {
    setErrorMessage(null);
    try {
      const params = {};
      if (filterRole !== 'all') {
        params.role = filterRole;
      }
      const response = await api.getInvoices(params);
      if (response.data?.data?.invoices) {
        setInvoices(response.data.data.invoices);
      }
    } catch (error) {
      const msg = getErrorMessage(error, 'تعذر جلب الفواتير من الخادم');
      setErrorMessage(msg);
      Alert.alert('خطأ في الاتصال', msg);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [filterRole]);

  useEffect(() => {
    setLoading(true);
    fetchInvoices();
  }, [fetchInvoices]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchInvoices();
  };

  const handleUpdateStatus = async (invoiceId, newStatus) => {
    try {
      await api.updateStatus(invoiceId, newStatus);
      Alert.alert('نجاح', 'تم تحديث حالة الفاتورة بنجاح ✅');
      fetchInvoices();
    } catch (error) {
      Alert.alert('خطأ', getErrorMessage(error, 'فشل تحديث حالة الفاتورة'));
    }
  };

  const renderStatusBadge = (status) => {
    let bg = '#F39C12';
    let label = 'قيد الانتظار';
    if (status === 'PAID') {
      bg = '#27AE60';
      label = 'مسددة بالكامل';
    } else if (status === 'OVERDUE') {
      bg = '#E74C3C';
      label = 'متأخرة';
    } else if (status === 'CANCELLED') {
      bg = '#7F8C8D';
      label = 'ملغاة';
    }
    return (
      <View style={[styles.badge, { backgroundColor: bg }]}>
        <Text style={styles.badgeText}>{label}</Text>
      </View>
    );
  };

  const renderInvoiceItem = ({ item }) => {
    const isSender = item.sender?._id === user?.id;
    const counterpartName = isSender ? item.receiver?.name : item.sender?.name;

    return (
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.invoiceNumber}>{item.invoiceNumber}</Text>
          {renderStatusBadge(item.status)}
        </View>

        <Text style={styles.partyText}>
          {isSender ? `المستلم: ${counterpartName || 'غير محدد'}` : `المرسل: ${counterpartName || 'غير محدد'}`}
        </Text>

        <View style={styles.itemsSummary}>
          <Text style={styles.itemsCount}>عدد البنود: {item.items?.length || 0}</Text>
          <Text style={styles.totalAmount}>{item.totalAmount?.toLocaleString()} ج.م</Text>
        </View>

        <View style={styles.cardFooter}>
          <Text style={styles.dateText}>
            الاستحقاق: {new Date(item.dueDate).toLocaleDateString('ar-EG')}
          </Text>

          {item.status === 'PENDING' && (
            <TouchableOpacity
              style={styles.payButton}
              onPress={() => handleUpdateStatus(item._id, 'PAID')}
            >
              <Text style={styles.payButtonText}>تأكيد السداد ✅</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      {/* Role Filter Tabs */}
      <View style={styles.filterRow}>
        {['all', 'received', 'sent'].map((tab) => (
          <TouchableOpacity
            key={tab}
            style={[styles.filterTab, filterRole === tab && styles.filterTabActive]}
            onPress={() => setFilterRole(tab)}
          >
            <Text
              style={[
                styles.filterTabText,
                filterRole === tab && styles.filterTabTextActive,
              ]}
            >
              {tab === 'all' ? 'الكل' : tab === 'received' ? 'الواردة' : 'الصادرة'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {errorMessage && (
        <View style={styles.errorBanner}>
          <Text style={styles.errorBannerText}>{errorMessage}</Text>
          <TouchableOpacity onPress={fetchInvoices} style={styles.retryBtn}>
            <Text style={styles.retryBtnText}>إعادة المحاولة 🔄</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Skeleton Loading State or Actual Content */}
      {loading && !refreshing ? (
        <View style={styles.listContent}>
          <InvoiceCardSkeleton />
          <InvoiceCardSkeleton />
          <InvoiceCardSkeleton />
          <InvoiceCardSkeleton />
        </View>
      ) : (
        <FlatList
          data={invoices}
          keyExtractor={(item) => item._id}
          renderItem={renderInvoiceItem}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#D4AF37']} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyTitle}>لا توجد فواتير مسجلة</Text>
              <Text style={styles.emptySubtitle}>اسحب للأسفل للتحديث أو أنشئ فاتورة جديدة</Text>
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0D1117' },
  skeletonBase: { backgroundColor: '#21262D' },
  filterRow: {
    flexDirection: 'row-reverse',
    padding: 12,
    backgroundColor: '#161B22',
    borderBottomWidth: 1,
    borderBottomColor: '#30363D',
  },
  filterTab: {
    flex: 1,
    paddingVertical: 8,
    alignItems: 'center',
    borderRadius: 8,
    marginHorizontal: 4,
  },
  filterTabActive: { backgroundColor: '#D4AF37' },
  filterTabText: { color: '#8B949E', fontWeight: 'bold' },
  filterTabTextActive: { color: '#000' },
  listContent: { padding: 16 },
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
  errorBannerText: { color: '#FF7B72', fontSize: 13, flex: 1, textAlign: 'right' },
  retryBtn: {
    backgroundColor: '#DA3633',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 4,
    marginLeft: 8,
  },
  retryBtnText: { color: '#FFF', fontSize: 12, fontWeight: 'bold' },
  card: {
    backgroundColor: '#161B22',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#30363D',
  },
  cardHeader: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  invoiceNumber: { color: '#F0F6FC', fontWeight: 'bold', fontSize: 16 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  badgeText: { color: '#FFF', fontSize: 11, fontWeight: 'bold' },
  partyText: { color: '#8B949E', marginVertical: 8, textAlign: 'right' },
  itemsSummary: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    marginVertical: 8,
    alignItems: 'center',
  },
  itemsCount: { color: '#8B949E' },
  totalAmount: { color: '#D4AF37', fontWeight: 'bold', fontSize: 18 },
  cardFooter: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#21262D',
    paddingTop: 8,
  },
  dateText: { color: '#8B949E', fontSize: 12 },
  payButton: {
    backgroundColor: '#238636',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  payButtonText: { color: '#FFF', fontSize: 12, fontWeight: 'bold' },
  emptyContainer: { alignItems: 'center', marginTop: 60 },
  emptyTitle: { color: '#F0F6FC', fontSize: 18, fontWeight: 'bold' },
  emptySubtitle: { color: '#8B949E', fontSize: 14, marginTop: 6 },
});
