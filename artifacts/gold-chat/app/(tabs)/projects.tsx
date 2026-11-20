import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
  Alert,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { formatCurrency } from '../../utils/gold';
import ProjectSidebar, { ProjectSidebarSection } from '@/components/ProjectSidebar';
import FeatureCardPopup from '@/components/FeatureCardPopup';

export default function ProjectsScreen() {
  const insets = useSafeAreaInsets();
  const { user, workGroups, projects } = useApp();

  // Sidebar visibility state
  const [sidebarVisible, setSidebarVisible] = useState(false);

  // Popup states for quick features
  const [activePopup, setActivePopup] = useState<string | null>(null);
  const [filterMode, setFilterMode] = useState<'all' | 'high_budget' | 'warning'>('all');

  const rootGroups = useMemo(
    () => workGroups.filter(g => !g.parentGroupId),
    [workGroups],
  );

  function getProjectForGroup(groupId: string) {
    return projects.find(p => p.workGroupId === groupId);
  }

  function getSubGroups(groupId: string) {
    return workGroups.filter(g => g.parentGroupId === groupId);
  }

  // Calculate high level summaries
  const totalCapital = useMemo(() => {
    return projects.reduce((acc, p) => acc + (p.estimatedCapital || 0), 0);
  }, [projects]);

  const totalExpenses = useMemo(() => {
    return projects.reduce((acc, p) => acc + (p.totalExpenses || 0), 0);
  }, [projects]);

  const totalMembers = useMemo(() => {
    return rootGroups.reduce((acc, g) => acc + (g.members?.length || 0), 0);
  }, [rootGroups]);

  // Filtered groups based on quick view setting
  const displayedGroups = useMemo(() => {
    if (filterMode === 'high_budget') {
      return rootGroups.filter(g => {
        const p = getProjectForGroup(g.id);
        return p && p.estimatedCapital >= 100000;
      });
    }
    if (filterMode === 'warning') {
      return rootGroups.filter(g => {
        const p = getProjectForGroup(g.id);
        return p && p.estimatedCapital > 0 && (p.totalExpenses / p.estimatedCapital) >= 0.75;
      });
    }
    return rootGroups;
  }, [rootGroups, filterMode, projects]);

  // Sidebar sections configuration for General Projects Section
  const sidebarSections: ProjectSidebarSection[] = [
    {
      title: 'إدارة وتخطيط القسم',
      items: [
        {
          id: 'new_project',
          title: 'إنشاء مشروع جديد',
          subtitle: 'إعداد تفاصيل رأس المال وفريق العمل',
          icon: 'add-business',
          type: 'full_screen',
          onPress: () => router.push('/new-project'),
        },
        {
          id: 'budget_overview',
          title: 'تقرير ميزانيات القسم',
          subtitle: 'عرض إجمالي رؤوس الأموال ومعدلات الصرف',
          icon: 'pie-chart',
          type: 'card_popup',
          onPress: () => setActivePopup('budget_overview'),
        },
        {
          id: 'filter_view',
          title: 'تصفية وترتيب العرض',
          subtitle: filterMode === 'all' ? 'عرض الكل' : filterMode === 'high_budget' ? 'المشاريع الكبرى' : 'المشاريع الحرجة',
          icon: 'filter-list',
          type: 'card_popup',
          onPress: () => setActivePopup('filter_view'),
        },
      ],
    },
    {
      title: 'الأمان والخصوصية والسياسات',
      items: [
        {
          id: 'privacy_policy',
          title: 'إخفاء هوية الأعضاء',
          subtitle: 'إظهار التخصصات فقط وحماية سرية المقاولين',
          icon: 'security',
          type: 'card_popup',
          onPress: () => setActivePopup('privacy_policy'),
        },
        {
          id: 'invitations',
          title: 'سجل الدعوات والصلاحيات',
          subtitle: 'إدارة دعوات المهندسين والعمال برقم الهاتف',
          icon: 'outgoing-mail',
          type: 'card_popup',
          onPress: () => setActivePopup('invitations'),
        },
      ],
    },
    {
      title: 'تفضيلات واجهة القسم',
      items: [
        {
          id: 'display_settings',
          title: 'تهيئة مساحة العرض',
          subtitle: 'إخفاء الأشرطة الجانبية وتوسيع الرؤية',
          icon: 'aspect-ratio',
          type: 'card_popup',
          onPress: () => setActivePopup('display_settings'),
        },
      ],
    },
  ];

  function renderGroup({ item }: { item: typeof workGroups[0] }) {
    const project = getProjectForGroup(item.id);
    const subs = getSubGroups(item.id);
    const remaining = project
      ? project.estimatedCapital - project.totalExpenses + project.totalIncome
      : 0;
    const spendingRate = project && project.estimatedCapital > 0
      ? (project.totalExpenses / project.estimatedCapital) * 100
      : 0;

    return (
      <Pressable
        style={styles.card}
        onPress={() => router.push(`/project/${item.id}`)}
      >
        {/* Header */}
        <View style={styles.cardHeader}>
          <View style={styles.cardIcon}>
            <MaterialIcons name="domain" size={24} color={Colors.accent} />
          </View>
          <View style={styles.cardInfo}>
            <Text style={styles.cardName}>{item.name}</Text>
            {item.description ? <Text style={styles.cardDesc}>{item.description}</Text> : null}
          </View>
          <MaterialIcons name="chevron-left" size={22} color={Colors.textMuted} />
        </View>

        {/* Capital info */}
        {project && (
          <View style={styles.capitalRow}>
            <View style={styles.capitalItem}>
              <Text style={styles.capitalLabel}>رأس المال</Text>
              <Text style={styles.capitalValue}>{formatCurrency(project.estimatedCapital)}</Text>
            </View>
            <View style={styles.capitalItem}>
              <Text style={styles.capitalLabel}>المصروفات</Text>
              <Text style={[styles.capitalValue, { color: Colors.error }]}>
                {formatCurrency(project.totalExpenses)}
              </Text>
            </View>
            <View style={styles.capitalItem}>
              <Text style={styles.capitalLabel}>المتبقي</Text>
              <Text style={[styles.capitalValue, { color: remaining >= 0 ? Colors.profit : Colors.error }]}>
                {formatCurrency(remaining)}
              </Text>
            </View>
          </View>
        )}

        {/* Progress bar */}
        {project && project.estimatedCapital > 0 && (
          <View style={styles.progressContainer}>
            <View style={styles.progressTrack}>
              <View
                style={[
                  styles.progressFill,
                  {
                    width: `${Math.min(spendingRate, 100)}%`,
                    backgroundColor: spendingRate > 80 ? Colors.error : Colors.accent,
                  },
                ]}
              />
            </View>
            <Text style={styles.progressLabel}>{spendingRate.toFixed(0)}% مصروف</Text>
          </View>
        )}

        {/* Sub-groups count + members */}
        <View style={styles.footerRow}>
          {subs.length > 0 && (
            <View style={styles.footerItem}>
              <MaterialIcons name="folder" size={14} color={Colors.textMuted} />
              <Text style={styles.footerText}>{subs.length} مجموعة فرعية</Text>
            </View>
          )}
          <View style={styles.footerItem}>
            <MaterialIcons name="group" size={14} color={Colors.textMuted} />
            <Text style={styles.footerText}>{item.members.length} عضو</Text>
          </View>
        </View>
      </Pressable>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Top Header with Hamburger 3-Bars Button */}
      <View style={styles.header}>
        {/* Left: Quick Add Project */}
        <Pressable
          style={styles.addBtn}
          onPress={() => router.push('/new-project')}
          hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
        >
          <MaterialIcons name="add" size={24} color={Colors.accent} />
        </Pressable>

        {/* Center Title with Breathing Space Indicator */}
        <View style={styles.titleWrap}>
          <Text style={styles.headerTitle}>المشاريع</Text>
          <Text style={styles.headerSub}>مساحة عمل رحبة وواضحة</Text>
        </View>

        {/* Right: Hamburger Button (3-Bars) for Section Sidebar */}
        <Pressable
          style={styles.menuBtn}
          onPress={() => setSidebarVisible(true)}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        >
          <View style={styles.hamburgerBar} />
          <View style={[styles.hamburgerBar, { width: 16 }]} />
          <View style={styles.hamburgerBar} />
        </Pressable>
      </View>

      {/* Active Filter Indicator if non-default */}
      {filterMode !== 'all' && (
        <View style={styles.filterBar}>
          <Pressable onPress={() => setFilterMode('all')} style={styles.resetFilterBtn}>
            <MaterialIcons name="close" size={16} color={Colors.accent} />
            <Text style={styles.resetFilterText}>إلغاء التصفية</Text>
          </Pressable>
          <Text style={styles.filterBarText}>
            تصفية مفعلة: {filterMode === 'high_budget' ? 'المشاريع الكبرى' : 'المشاريع بمصروفات عالية'}
          </Text>
        </View>
      )}

      {/* Main Content Area - Wide, Spacious & Clean */}
      {displayedGroups.length === 0 ? (
        <View style={styles.empty}>
          <View style={styles.emptyIcon}>
            <MaterialIcons name="business-center" size={56} color={Colors.accent} />
          </View>
          <Text style={styles.emptyTitle}>لا توجد مشاريع معروضة</Text>
          <Text style={styles.emptySubtitle}>
            {filterMode !== 'all'
              ? 'لا توجد مشاريع تطابق شروط التصفية المحددة'
              : 'أنشئ مشروعاً جديداً لإدارة مجموعات العمل ورأس المال، أو افتح القائمة الجانبية (☰) للإعدادات'}
          </Text>
          <View style={styles.emptyActionsRow}>
            {filterMode !== 'all' ? (
              <Pressable style={styles.emptyBtn} onPress={() => setFilterMode('all')}>
                <Text style={styles.emptyBtnText}>عرض جميع المشاريع</Text>
              </Pressable>
            ) : (
              <Pressable style={styles.emptyBtn} onPress={() => router.push('/new-project')}>
                <MaterialIcons name="add" size={18} color="#0B0F14" />
                <Text style={styles.emptyBtnText}>مشروع جديد</Text>
              </Pressable>
            )}
          </View>
        </View>
      ) : (
        <FlatList
          data={displayedGroups}
          keyExtractor={i => i.id}
          renderItem={renderGroup}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: 90, paddingTop: 6 }}
        />
      )}

      {/* Dedicated Section Sidebar (3-Bars Drawer) */}
      <ProjectSidebar
        visible={sidebarVisible}
        onClose={() => setSidebarVisible(false)}
        projectName="قسم المشاريع ورأس المال"
        projectType="إدارة المجموعات والاعتمادات"
        sections={sidebarSections}
        quickActions={[
          {
            id: 'qa_add',
            label: 'مشروع جديد',
            icon: 'add-business',
            onPress: () => router.push('/new-project'),
          },
          {
            id: 'qa_summary',
            label: 'ملخص القسم',
            icon: 'assessment',
            onPress: () => setActivePopup('budget_overview'),
          },
        ]}
      />

      {/* Feature Card Popup 1: Budget Overview */}
      <FeatureCardPopup
        visible={activePopup === 'budget_overview'}
        onClose={() => setActivePopup(null)}
        title="تقرير ميزانيات القسم"
        subtitle="ملخص سريع لجميع المشاريع الفعالة"
        icon="pie-chart"
      >
        <View style={styles.popupSummaryGrid}>
          <View style={styles.popupSummaryItem}>
            <Text style={styles.popupSummaryLabel}>إجمالي رأس المال</Text>
            <Text style={[styles.popupSummaryValue, { color: Colors.accent }]}>
              {formatCurrency(totalCapital)}
            </Text>
          </View>
          <View style={styles.popupSummaryItem}>
            <Text style={styles.popupSummaryLabel}>إجمالي المصروفات</Text>
            <Text style={[styles.popupSummaryValue, { color: Colors.error }]}>
              {formatCurrency(totalExpenses)}
            </Text>
          </View>
          <View style={styles.popupSummaryItem}>
            <Text style={styles.popupSummaryLabel}>صافي المتبقي</Text>
            <Text style={[styles.popupSummaryValue, { color: Colors.profit }]}>
              {formatCurrency(totalCapital - totalExpenses)}
            </Text>
          </View>
          <View style={styles.popupSummaryItem}>
            <Text style={styles.popupSummaryLabel}>إجمالي الأعضاء</Text>
            <Text style={styles.popupSummaryValue}>{totalMembers} عضو</Text>
          </View>
        </View>
        <Pressable
          style={styles.popupDoneBtn}
          onPress={() => setActivePopup(null)}
        >
          <Text style={styles.popupDoneText}>إغلاق التقرير</Text>
        </Pressable>
      </FeatureCardPopup>

      {/* Feature Card Popup 2: Filter View */}
      <FeatureCardPopup
        visible={activePopup === 'filter_view'}
        onClose={() => setActivePopup(null)}
        title="تصفية وطريقة العرض"
        subtitle="اختر كيفية ترتيب وتنظيم قائمة المشاريع"
        icon="filter-list"
      >
        <View style={styles.filterOptionsList}>
          <Pressable
            style={[
              styles.filterOptionCard,
              filterMode === 'all' && styles.filterOptionCardActive,
            ]}
            onPress={() => {
              setFilterMode('all');
              setActivePopup(null);
            }}
          >
            <MaterialIcons
              name={filterMode === 'all' ? 'radio-button-checked' : 'radio-button-unchecked'}
              size={20}
              color={filterMode === 'all' ? Colors.accent : Colors.textMuted}
            />
            <View style={{ flex: 1, alignItems: 'flex-end', marginRight: 10 }}>
              <Text style={styles.filterOptionTitle}>عرض جميع المشاريع</Text>
              <Text style={styles.filterOptionDesc}>إظهار كافة المشاريع والمجموعات المسجلة</Text>
            </View>
          </Pressable>

          <Pressable
            style={[
              styles.filterOptionCard,
              filterMode === 'high_budget' && styles.filterOptionCardActive,
            ]}
            onPress={() => {
              setFilterMode('high_budget');
              setActivePopup(null);
            }}
          >
            <MaterialIcons
              name={filterMode === 'high_budget' ? 'radio-button-checked' : 'radio-button-unchecked'}
              size={20}
              color={filterMode === 'high_budget' ? Colors.accent : Colors.textMuted}
            />
            <View style={{ flex: 1, alignItems: 'flex-end', marginRight: 10 }}>
              <Text style={styles.filterOptionTitle}>المشاريع الكبرى</Text>
              <Text style={styles.filterOptionDesc}>مشاريع رأس مال 100,000 ج.م فأكثر</Text>
            </View>
          </Pressable>

          <Pressable
            style={[
              styles.filterOptionCard,
              filterMode === 'warning' && styles.filterOptionCardActive,
            ]}
            onPress={() => {
              setFilterMode('warning');
              setActivePopup(null);
            }}
          >
            <MaterialIcons
              name={filterMode === 'warning' ? 'radio-button-checked' : 'radio-button-unchecked'}
              size={20}
              color={filterMode === 'warning' ? Colors.accent : Colors.textMuted}
            />
            <View style={{ flex: 1, alignItems: 'flex-end', marginRight: 10 }}>
              <Text style={styles.filterOptionTitle}>تجاوز معدل الصرف</Text>
              <Text style={styles.filterOptionDesc}>مشاريع استهلكت 75% أو أكثر من رأس المال</Text>
            </View>
          </Pressable>
        </View>
      </FeatureCardPopup>

      {/* Feature Card Popup 3: Privacy Policy */}
      <FeatureCardPopup
        visible={activePopup === 'privacy_policy'}
        onClose={() => setActivePopup(null)}
        title="سياسة الخصوصية بالقسم"
        subtitle="حماية سرية الأعضاء والمقاولين"
        icon="security"
      >
        <View style={styles.privacyBody}>
          <View style={styles.privacyBullet}>
            <MaterialIcons name="verified-user" size={18} color={Colors.accent} />
            <Text style={styles.privacyBulletText}>
              يتم إخفاء الأسماء الحقيقية للأعضاء داخل المشاريع، ويكتفى بعرض التخصص (مثل: مهندس مدني، كهربائي) واسم المستخدم.
            </Text>
          </View>
          <View style={styles.privacyBullet}>
            <MaterialIcons name="lock" size={18} color={Colors.accent} />
            <Text style={styles.privacyBulletText}>
              أرقام الهواتف تظل مشفرة ولا تظهر إلا للمنشئ في شاشة التوجيه والإشراف.
            </Text>
          </View>
          <Pressable
            style={styles.popupDoneBtn}
            onPress={() => setActivePopup(null)}
          >
            <Text style={styles.popupDoneText}>فهمت ذلك</Text>
          </Pressable>
        </View>
      </FeatureCardPopup>

      {/* Feature Card Popup 4: Invitations */}
      <FeatureCardPopup
        visible={activePopup === 'invitations'}
        onClose={() => setActivePopup(null)}
        title="سجل الدعوات والصلاحيات"
        subtitle="إدارة الأعضاء المدعوين للقسم"
        icon="outgoing-mail"
      >
        <View style={{ paddingVertical: 6 }}>
          <Text style={styles.invitationNotice}>
            يمكنك إرسال دعوة مباشرة لأي فني أو مقاول عبر رقم هاتفه مع تحديد تخصصه من داخل شاشة أي مشروع أو عند إنشاء مشروع جديد.
          </Text>
          <Pressable
            style={styles.popupDoneBtn}
            onPress={() => {
              setActivePopup(null);
              router.push('/new-project');
            }}
          >
            <Text style={styles.popupDoneText}>+ دعوة عضو عبر مشروع جديد</Text>
          </Pressable>
        </View>
      </FeatureCardPopup>

      {/* Feature Card Popup 5: Display Settings */}
      <FeatureCardPopup
        visible={activePopup === 'display_settings'}
        onClose={() => setActivePopup(null)}
        title="تهيئة مساحة العرض"
        subtitle="تجربة بصرية مريحة للعين"
        icon="aspect-ratio"
      >
        <View style={{ paddingVertical: 4 }}>
          <Text style={styles.displayNoteText}>
            تم تصميم هذا القسم ليتيح أوسع مساحة تنفس ممكنة للعميل: تم نقل جميع الخيارات والإعدادات المتقدمة إلى القائمة الجانبية المخصصة (☰) لتظل الشاشة الرئيسية خالية من التكدس وسلسة التصفح.
          </Text>
          <Pressable
            style={styles.popupDoneBtn}
            onPress={() => setActivePopup(null)}
          >
            <Text style={styles.popupDoneText}>تم</Text>
          </Pressable>
        </View>
      </FeatureCardPopup>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.primary,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  menuBtn: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.3)',
    gap: 4,
  },
  hamburgerBar: {
    width: 20,
    height: 2.5,
    borderRadius: 2,
    backgroundColor: Colors.accent,
  },
  titleWrap: {
    alignItems: 'center',
    flex: 1,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  headerSub: {
    fontSize: 10,
    color: Colors.textMuted,
    marginTop: 1,
  },
  addBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(212, 175, 55, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.25)',
  },

  filterBar: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.surfaceAlt,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  filterBarText: {
    fontSize: 11,
    color: Colors.accent,
    fontWeight: '600',
  },
  resetFilterBtn: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
    backgroundColor: Colors.surface,
  },
  resetFilterText: {
    fontSize: 10,
    color: Colors.textSecondary,
  },

  card: {
    backgroundColor: Colors.surface,
    borderRadius: 18,
    marginHorizontal: 16,
    marginVertical: 7,
    padding: 16,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardHeader: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 12,
    marginBottom: 12,
  },
  cardIcon: {
    width: 46,
    height: 46,
    borderRadius: 23,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.3)',
  },
  cardInfo: { flex: 1, alignItems: 'flex-end' },
  cardName: { fontSize: 17, fontWeight: '800', color: Colors.textPrimary },
  cardDesc: { fontSize: 12, color: Colors.textMuted, marginTop: 3, lineHeight: 16 },

  capitalRow: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-around',
    backgroundColor: Colors.background,
    borderRadius: 12,
    padding: 12,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.04)',
  },
  capitalItem: { alignItems: 'center', gap: 3 },
  capitalLabel: { fontSize: 10, color: Colors.textMuted },
  capitalValue: { fontSize: 13, fontWeight: '800', color: Colors.textPrimary },

  progressContainer: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 8,
    marginBottom: 10,
  },
  progressTrack: {
    flex: 1,
    height: 6,
    borderRadius: 3,
    backgroundColor: Colors.surfaceAlt,
    overflow: 'hidden',
  },
  progressFill: { height: '100%', borderRadius: 3 },
  progressLabel: { fontSize: 10, color: Colors.textMuted, minWidth: 50 },

  footerRow: {
    flexDirection: 'row-reverse',
    gap: 16,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
    paddingTop: 10,
  },
  footerItem: { flexDirection: 'row-reverse', alignItems: 'center', gap: 5 },
  footerText: { fontSize: 11, color: Colors.textSecondary },

  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  emptyIcon: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 20,
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.3)',
  },
  emptyTitle: { fontSize: 20, fontWeight: '800', color: Colors.textPrimary, marginBottom: 8 },
  emptySubtitle: {
    fontSize: 13,
    color: Colors.textSecondary,
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 20,
  },
  emptyActionsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  emptyBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: Colors.accent,
    borderRadius: 24,
    paddingHorizontal: 24,
    paddingVertical: 12,
  },
  emptyBtnText: { color: '#0B0F14', fontSize: 15, fontWeight: '800' },

  popupSummaryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 16,
  },
  popupSummaryItem: {
    width: '48%',
    backgroundColor: Colors.surfaceAlt,
    borderRadius: 12,
    padding: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  popupSummaryLabel: {
    fontSize: 11,
    color: Colors.textMuted,
    marginBottom: 4,
  },
  popupSummaryValue: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  popupDoneBtn: {
    backgroundColor: Colors.accent,
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  popupDoneText: {
    color: '#0B0F14',
    fontSize: 14,
    fontWeight: '800',
  },

  filterOptionsList: {
    gap: 10,
  },
  filterOptionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surfaceAlt,
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  filterOptionCardActive: {
    borderColor: Colors.accent,
    backgroundColor: 'rgba(212, 175, 55, 0.08)',
  },
  filterOptionTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  filterOptionDesc: {
    fontSize: 11,
    color: Colors.textMuted,
    marginTop: 2,
  },

  privacyBody: {
    gap: 12,
  },
  privacyBullet: {
    flexDirection: 'row-reverse',
    alignItems: 'flex-start',
    gap: 8,
    backgroundColor: Colors.surfaceAlt,
    padding: 12,
    borderRadius: 12,
  },
  privacyBulletText: {
    flex: 1,
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 18,
    textAlign: 'right',
  },

  invitationNotice: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 18,
    textAlign: 'right',
    marginBottom: 16,
  },
  displayNoteText: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 19,
    textAlign: 'right',
    marginBottom: 16,
  },
});
