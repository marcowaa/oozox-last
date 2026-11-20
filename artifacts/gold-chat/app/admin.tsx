import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import {
  Alert, FlatList, Modal, Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../constants/colors';
import { formatCurrency, formatShortDate } from '../utils/gold';

type Tab = 'overview' | 'requests' | 'api' | 'audits';

export default function AdminScreen() {
  const insets = useSafeAreaInsets();
  const {
    traderRequests, approveTraderRequest, rejectTraderRequest,
    adminConfig, updateAdminConfig,
    goldSales, audits,
  } = useApp();
  const [tab, setTab] = useState<Tab>('overview');
  const [selectedRequest, setSelectedRequest] = useState<string | null>(null);
  const [adminNotes, setAdminNotes] = useState('');
  const [configDraft, setConfigDraft] = useState(adminConfig);

  const pendingRequests = traderRequests.filter(r => r.status === 'pending');
  const approvedRequests = traderRequests.filter(r => r.status === 'approved');
  const completedSales = goldSales.filter(s => s.status === 'completed');
  const totalSalesAmount = completedSales.reduce((s, sale) => s + sale.totalAmount, 0);
  const openAudits = audits.filter(a => a.status === 'open');

  const tabs: { key: Tab; label: string; icon: string; badge?: number }[] = [
    { key: 'overview', label: 'نظرة عامة', icon: 'dashboard' },
    { key: 'requests', label: 'طلبات التجار', icon: 'verified', badge: pendingRequests.length },
    { key: 'api', label: 'إعدادات API', icon: 'api' },
    { key: 'audits', label: 'الجرد', icon: 'inventory-2', badge: openAudits.length },
  ];

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.backBtn}>
          <MaterialIcons name="arrow-forward-ios" size={20} color={Colors.textInverse} />
        </Pressable>
        <Text style={styles.headerTitle}>لوحة الإدارة</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Tabs */}
      <View style={styles.tabsRow}>
        {tabs.map(t => (
          <Pressable
            key={t.key}
            style={[styles.tab, tab === t.key && styles.tabActive]}
            onPress={() => setTab(t.key)}
          >
            <MaterialIcons name={t.icon as keyof typeof MaterialIcons.glyphMap} size={18} color={tab === t.key ? '#fff' : Colors.textMuted} />
            <Text style={[styles.tabText, tab === t.key && styles.tabTextActive]}>{t.label}</Text>
            {t.badge ? (
              <View style={styles.badge}><Text style={styles.badgeText}>{t.badge}</Text></View>
            ) : null}
          </Pressable>
        ))}
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 40 }}>
        {tab === 'overview' && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>إحصائيات النظام</Text>
            <View style={styles.statsGrid}>
              <StatCard label="طلبات تاجر معلّقة" value={String(pendingRequests.length)} icon="hourglass-empty" color={Colors.warning} />
              <StatCard label="تجار موثّقون" value={String(approvedRequests.length)} icon="verified" color={Colors.profit} />
              <StatCard label="مبيعات ذهب مكتملة" value={String(completedSales.length)} icon="shopping-cart" color={Colors.accent} />
              <StatCard label="إجمالي المبيعات" value={formatCurrency(totalSalesAmount)} icon="payments" color={Colors.accent} />
              <StatCard label="عمليات جرد مفتوحة" value={String(openAudits.length)} icon="inventory-2" color={Colors.info} />
              <StatCard label="حالة التشفير" value={adminConfig.encryptionEnabled ? 'مُفعّل 🔒' : 'معطّل'} icon="lock" color={adminConfig.encryptionEnabled ? Colors.profit : Colors.error} />
            </View>

            <View style={styles.privacyNotice}>
              <MaterialIcons name="security" size={20} color={Colors.info} />
              <Text style={styles.privacyText}>
                البيانات مشفّرة بأقصى نظام حماية. لا يتم عرض أي بيانات حساسة للعملاء
                (محادثات، أرقام هواتف، صور). لوحة الإدارة تعرض إحصائيات وطلبات توثيق فقط.
              </Text>
            </View>
          </View>
        )}

        {tab === 'requests' && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>طلبات توثيق تجار الذهب ({pendingRequests.length})</Text>
            {pendingRequests.length === 0 ? (
              <Text style={styles.emptyText}>لا توجد طلبات معلّقة</Text>
            ) : (
              pendingRequests.map(req => (
                <View key={req.id} style={styles.requestCard}>
                  <View style={styles.requestHeader}>
                    <View style={styles.requestInfo}>
                      <Text style={styles.requestName}>{req.name}</Text>
                      <Text style={styles.requestShop}>🏪 {req.shopName}</Text>
                      <Text style={styles.requestDetail}>📍 {req.shopAddress}</Text>
                      <Text style={styles.requestDetail}>📄 ترخيص: {req.license}</Text>
                      <Text style={styles.requestDetail}>📞 {req.phone}</Text>
                      <Text style={styles.requestDetail}>@{req.username}</Text>
                    </View>
                    <Text style={styles.requestDate}>{formatShortDate(req.submittedAt)}</Text>
                  </View>
                  <TextInput
                    style={styles.notesInput}
                    placeholder="ملاحظات الإدارة..."
                    placeholderTextColor={Colors.placeholder}
                    value={adminNotes}
                    onChangeText={setAdminNotes}
                    textAlign="right"
                  />
                  <View style={styles.requestActions}>
                    <Pressable style={styles.rejectBtn} onPress={() => { rejectTraderRequest(req.id); setAdminNotes(''); }}>
                      <MaterialIcons name="close" size={16} color={Colors.error} />
                      <Text style={styles.rejectText}>رفض</Text>
                    </Pressable>
                    <Pressable style={styles.approveBtn} onPress={() => { approveTraderRequest(req.id, adminNotes); setAdminNotes(''); }}>
                      <MaterialIcons name="check" size={16} color="#fff" />
                      <Text style={styles.approveText}>موافقة وتفعيل</Text>
                    </Pressable>
                  </View>
                </View>
              ))
            )}

            {/* Approved traders */}
            {approvedRequests.length > 0 && (
              <View style={styles.subSection}>
                <Text style={styles.subSectionTitle}>تجار موثّقون ({approvedRequests.length})</Text>
                {approvedRequests.map(req => (
                  <View key={req.id} style={styles.approvedCard}>
                    <View style={styles.approvedInfo}>
                      <Text style={styles.approvedName}>{req.name}</Text>
                      <Text style={styles.approvedShop}>🏪 {req.shopName}</Text>
                    </View>
                    <View style={styles.approvedBadge}>
                      <MaterialIcons name="verified" size={14} color={Colors.profit} />
                      <Text style={styles.approvedBadgeText}>موثّق</Text>
                    </View>
                  </View>
                ))}
              </View>
            )}
          </View>
        )}

        {tab === 'api' && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>إعدادات API الخارجية</Text>

            <View style={styles.apiCard}>
              <View style={styles.apiCardHeader}>
                <MaterialIcons name="notifications" size={20} color={Colors.accent} />
                <Text style={styles.apiCardTitle}>إشعارات Push</Text>
              </View>
              <Text style={styles.apiLabel}>رابط API</Text>
              <TextInput style={styles.apiInput} placeholder="https://api.example.com/notify" placeholderTextColor={Colors.placeholder} value={configDraft.notificationApiUrl ?? ''} onChangeText={v => setConfigDraft({ ...configDraft, notificationApiUrl: v })} textAlign="left" />
              <Text style={styles.apiLabel}>مفتاح API</Text>
              <TextInput style={styles.apiInput} placeholder="API Key" placeholderTextColor={Colors.placeholder} value={configDraft.notificationApiKey ?? ''} onChangeText={v => setConfigDraft({ ...configDraft, notificationApiKey: v })} textAlign="left" secureTextEntry />
            </View>

            <View style={styles.apiCard}>
              <View style={styles.apiCardHeader}>
                <MaterialIcons name="article" size={20} color={Colors.info} />
                <Text style={styles.apiCardTitle}>الأخبار والتحديثات</Text>
              </View>
              <Text style={styles.apiLabel}>رابط API الأخبار</Text>
              <TextInput style={styles.apiInput} placeholder="https://api.example.com/news" placeholderTextColor={Colors.placeholder} value={configDraft.newsApiUrl ?? ''} onChangeText={v => setConfigDraft({ ...configDraft, newsApiUrl: v })} textAlign="left" />
            </View>

            <View style={styles.apiCard}>
              <View style={styles.apiCardHeader}>
                <MaterialIcons name="trending-up" size={20} color={Colors.profit} />
                <Text style={styles.apiCardTitle}>أسعار الذهب المباشرة</Text>
              </View>
              <Text style={styles.apiLabel}>رابط API الأسعار</Text>
              <TextInput style={styles.apiInput} placeholder="https://api.example.com/gold-prices" placeholderTextColor={Colors.placeholder} value={configDraft.goldPriceApiUrl ?? ''} onChangeText={v => setConfigDraft({ ...configDraft, goldPriceApiUrl: v })} textAlign="left" />
              <Text style={styles.apiLabel}>مفتاح API</Text>
              <TextInput style={styles.apiInput} placeholder="API Key" placeholderTextColor={Colors.placeholder} value={configDraft.goldPriceApiKey ?? ''} onChangeText={v => setConfigDraft({ ...configDraft, goldPriceApiKey: v })} textAlign="left" secureTextEntry />
            </View>

            <View style={styles.apiCard}>
              <View style={styles.apiCardHeader}>
                <MaterialIcons name="lock" size={20} color={adminConfig.encryptionEnabled ? Colors.profit : Colors.error} />
                <Text style={styles.apiCardTitle}>التشفير والأمان</Text>
              </View>
              <View style={styles.switchRow}>
                <Switch
                  value={configDraft.encryptionEnabled}
                  onValueChange={v => setConfigDraft({ ...configDraft, encryptionEnabled: v })}
                  trackColor={{ false: Colors.border, true: Colors.profit }}
                />
                <Text style={styles.switchLabel}>تشفير البيانات (AES-256)</Text>
              </View>
            </View>

            <Pressable style={styles.saveConfigBtn} onPress={async () => { await updateAdminConfig(configDraft); Alert.alert('تم', 'تم حفظ الإعدادات'); }}>
              <Text style={styles.saveConfigText}>حفظ الإعدادات</Text>
            </Pressable>
          </View>
        )}

        {tab === 'audits' && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>عمليات الجرد ({audits.length})</Text>
            {audits.length === 0 ? (
              <Text style={styles.emptyText}>لا توجد عمليات جرد</Text>
            ) : (
              audits.map(audit => (
                <View key={audit.id} style={styles.auditCard}>
                  <View style={styles.auditHeader}>
                    <Text style={styles.auditType}>
                      {audit.type === 'annual' ? 'جرد سنوي' : audit.type === 'semiannual' ? 'جرد نصف سنوي' : 'جرد ربع سنوي'}
                    </Text>
                    <View style={[styles.auditStatus, { backgroundColor: audit.status === 'open' ? Colors.warningLight : Colors.profitBg }]}>
                      <Text style={[styles.auditStatusText, { color: audit.status === 'open' ? Colors.warning : Colors.profit }]}>
                        {audit.status === 'open' ? 'مفتوح' : 'مكتمل'}
                      </Text>
                    </View>
                  </View>
                  <Text style={styles.auditDetail}>الوزن المتوقع: {audit.expectedWeight} جم</Text>
                  {audit.actualWeight != null && (
                    <>
                      <Text style={styles.auditDetail}>الوزن الفعلي: {audit.actualWeight} جم</Text>
                      {audit.discrepancy != null && (
                        <Text style={[styles.auditDetail, { color: audit.discrepancy === 0 ? Colors.profit : Colors.error }]}>
                          الفرق: {audit.discrepancy} جم
                        </Text>
                      )}
                    </>
                  )}
                  <Text style={styles.auditItems}>{audit.items.length} صنف</Text>
                </View>
              ))
            )}
          </View>
        )}
      </ScrollView>
    </View>
  );
}

function StatCard({ label, value, icon, color }: { label: string; value: string; icon: string; color: string }) {
  return (
    <View style={statStyles.card}>
      <View style={[statStyles.iconBox, { backgroundColor: color + '18' }]}>
        <MaterialIcons name={icon as keyof typeof MaterialIcons.glyphMap} size={20} color={color} />
      </View>
      <Text style={statStyles.value}>{value}</Text>
      <Text style={statStyles.label}>{label}</Text>
    </View>
  );
}

const statStyles = StyleSheet.create({
  card: { flex: 1, minWidth: '46%', backgroundColor: Colors.surface, borderRadius: 16, padding: 14, alignItems: 'center', shadowColor: Colors.primary, shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.07, shadowRadius: 4, elevation: 2 },
  iconBox: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center', marginBottom: 8 },
  value: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary, textAlign: 'center' },
  label: { fontSize: 11, color: Colors.textMuted, marginTop: 3, textAlign: 'center' },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  header: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', backgroundColor: Colors.primary, paddingHorizontal: 16, paddingVertical: 14 },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },

  tabsRow: { flexDirection: 'row-reverse', backgroundColor: Colors.surface, paddingHorizontal: 8, paddingVertical: 6, gap: 4 },
  tab: { flexDirection: 'row-reverse', alignItems: 'center', gap: 4, paddingHorizontal: 12, paddingVertical: 8, borderRadius: 10 },
  tabActive: { backgroundColor: Colors.primary },
  tabText: { fontSize: 12, fontWeight: '600', color: Colors.textMuted },
  tabTextActive: { color: '#E6EDF3' },
  badge: { backgroundColor: Colors.error, borderRadius: 8, minWidth: 16, height: 16, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 4 },
  badgeText: { fontSize: 9, color: '#0B0F14', fontWeight: '700' },

  scroll: { flex: 1 },
  section: { padding: 16 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary, marginBottom: 12, textAlign: 'right' },
  subSection: { marginTop: 16 },
  subSectionTitle: { fontSize: 14, fontWeight: '700', color: Colors.textSecondary, marginBottom: 8, textAlign: 'right' },

  statsGrid: { flexDirection: 'row-reverse', flexWrap: 'wrap', gap: 10 },

  privacyNotice: { flexDirection: 'row-reverse', alignItems: 'flex-start', gap: 8, backgroundColor: Colors.infoLight, borderRadius: 12, padding: 12, marginTop: 12 },
  privacyText: { flex: 1, fontSize: 12, color: Colors.info, lineHeight: 18, textAlign: 'right' },

  requestCard: { backgroundColor: Colors.surface, borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: Colors.border },
  requestHeader: { flexDirection: 'row-reverse', justifyContent: 'space-between', marginBottom: 8 },
  requestInfo: { flex: 1, alignItems: 'flex-end', gap: 3 },
  requestName: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  requestShop: { fontSize: 13, color: Colors.accent, fontWeight: '600' },
  requestDetail: { fontSize: 12, color: Colors.textSecondary },
  requestDate: { fontSize: 11, color: Colors.textMuted },
  notesInput: { backgroundColor: Colors.inputBg, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8, fontSize: 13, color: Colors.inputText, borderWidth: 1, borderColor: Colors.inputBorder, marginBottom: 8 },
  requestActions: { flexDirection: 'row-reverse', gap: 8 },
  rejectBtn: { flex: 1, flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'center', gap: 4, backgroundColor: Colors.errorLight, borderRadius: 10, paddingVertical: 10, borderWidth: 1, borderColor: Colors.error },
  rejectText: { fontSize: 13, fontWeight: '600', color: Colors.error },
  approveBtn: { flex: 1, flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'center', gap: 4, backgroundColor: Colors.profit, borderRadius: 10, paddingVertical: 10 },
  approveText: { fontSize: 13, fontWeight: '700', color: '#E6EDF3' },

  approvedCard: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', backgroundColor: Colors.surface, borderRadius: 12, padding: 12, marginBottom: 6, borderWidth: 1, borderColor: Colors.border },
  approvedInfo: { flex: 1, alignItems: 'flex-end' },
  approvedName: { fontSize: 14, fontWeight: '600', color: Colors.textPrimary },
  approvedShop: { fontSize: 12, color: Colors.textSecondary, marginTop: 2 },
  approvedBadge: { flexDirection: 'row-reverse', alignItems: 'center', gap: 3, backgroundColor: Colors.profitBg, borderRadius: 8, paddingHorizontal: 8, paddingVertical: 3 },
  approvedBadgeText: { fontSize: 11, color: Colors.profit, fontWeight: '600' },

  apiCard: { backgroundColor: Colors.surface, borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: Colors.border },
  apiCardHeader: { flexDirection: 'row-reverse', alignItems: 'center', gap: 8, marginBottom: 10 },
  apiCardTitle: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  apiLabel: { fontSize: 12, color: Colors.textMuted, marginBottom: 4, textAlign: 'right' },
  apiInput: { backgroundColor: Colors.inputBg, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: Colors.inputText, borderWidth: 1, borderColor: Colors.inputBorder, marginBottom: 8 },

  switchRow: { flexDirection: 'row-reverse', alignItems: 'center', gap: 10 },
  switchLabel: { fontSize: 14, color: Colors.textPrimary },

  saveConfigBtn: { backgroundColor: Colors.accent, borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginTop: 8 },
  saveConfigText: { color: '#0B0F14', fontSize: 15, fontWeight: '700' },

  auditCard: { backgroundColor: Colors.surface, borderRadius: 12, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: Colors.border },
  auditHeader: { flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 },
  auditType: { fontSize: 14, fontWeight: '700', color: Colors.textPrimary },
  auditStatus: { borderRadius: 8, paddingHorizontal: 8, paddingVertical: 2 },
  auditStatusText: { fontSize: 11, fontWeight: '600' },
  auditDetail: { fontSize: 12, color: Colors.textSecondary, marginTop: 2 },
  auditItems: { fontSize: 11, color: Colors.textMuted, marginTop: 4 },

  emptyText: { fontSize: 14, color: Colors.textMuted, textAlign: 'center', paddingVertical: 20 },
});
