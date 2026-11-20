import { MaterialIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  Alert,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../../constants/colors';
import { formatCurrency } from '../../utils/gold';
import ProjectSidebar, { ProjectSidebarSection } from '@/components/ProjectSidebar';
import FeatureCardPopup from '@/components/FeatureCardPopup';

export default function ProjectDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const {
    workGroups,
    projects,
    createWorkGroup,
    addGroupMember,
    updateProjectCapital,
    user,
    sendInvitation,
  } = useApp();

  // Project Dedicated Sidebar state (3-bars hamburger)
  const [sidebarVisible, setSidebarVisible] = useState(false);

  // Active feature popup state for card popups
  const [activePopup, setActivePopup] = useState<string | null>(null);

  // Modals & Inputs
  const [newSubGroupName, setNewSubGroupName] = useState('');
  const [newMemberPhone, setNewMemberPhone] = useState('');
  const [newMemberRole, setNewMemberRole] = useState('');
  const [capitalInput, setCapitalInput] = useState('');

  const group = workGroups.find(g => g.id === id);
  const project = projects.find(p => p.workGroupId === id);
  const subGroups = useMemo(
    () => workGroups.filter(g => g.parentGroupId === id),
    [workGroups, id],
  );

  if (!group) {
    return (
      <View style={[styles.center, { paddingTop: insets.top }]}>
        <MaterialIcons name="error-outline" size={48} color={Colors.textMuted} />
        <Text style={styles.notFound}>لم يتم العثور على المشروع</Text>
        <Pressable style={styles.backBtn} onPress={() => router.back()}>
          <Text style={styles.backBtnText}>العودة</Text>
        </Pressable>
      </View>
    );
  }

  const remaining = project
    ? project.estimatedCapital - project.totalExpenses + project.totalIncome
    : 0;
  const spendingRate =
    project && project.estimatedCapital > 0
      ? (project.totalExpenses / project.estimatedCapital) * 100
      : 0;

  async function handleCreateSubGroup() {
    if (!newSubGroupName.trim() || !id) return;
    await createWorkGroup(newSubGroupName.trim(), undefined, id);
    setNewSubGroupName('');
    setActivePopup(null);
  }

  async function handleAddMember() {
    if (!newMemberRole.trim() || !id) return;
    const memberId =
      Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
    await addGroupMember(
      id,
      memberId,
      'pending',
      newMemberPhone,
      newMemberRole.trim(),
    );
    if (newMemberPhone.trim()) {
      await sendInvitation(
        id,
        group!.name,
        newMemberPhone.trim(),
        newMemberRole.trim(),
      );
    }
    setNewMemberRole('');
    setNewMemberPhone('');
    setActivePopup(null);
  }

  async function handleUpdateCapital() {
    const val = parseFloat(capitalInput);
    if (!val || val <= 0 || !project) return;
    await updateProjectCapital(project.id, val);
    setCapitalInput('');
    setActivePopup(null);
  }

  // Define dedicated project sidebar sections
  const projectSidebarSections: ProjectSidebarSection[] = [
    {
      title: 'إدارة المالية ورأس المال',
      items: [
        {
          id: 'edit_capital',
          title: 'تعديل رأس المال',
          subtitle: `الحالي: ${project ? formatCurrency(project.estimatedCapital) : 'غير محدد'}`,
          icon: 'monetization-on',
          type: 'card_popup',
          onPress: () => {
            if (project) setCapitalInput(String(project.estimatedCapital));
            setActivePopup('edit_capital');
          },
        },
        {
          id: 'capital_audit',
          title: 'تدقيق المصروفات والنسبة',
          subtitle: `نسبة الصرف: ${spendingRate.toFixed(1)}%`,
          icon: 'pie-chart',
          type: 'card_popup',
          onPress: () => setActivePopup('capital_audit'),
        },
      ],
    },
    {
      title: 'فريق العمل والتشكيل',
      items: [
        {
          id: 'add_member',
          title: 'إضافة عضو جديد بالتخصص',
          subtitle: 'دعوة مهندس أو فني برقم الهاتف مع إخفاء الاسم',
          icon: 'person-add',
          type: 'card_popup',
          onPress: () => setActivePopup('add_member'),
        },
        {
          id: 'add_subgroup',
          title: 'إنشاء مجموعة فرعية جديدة',
          subtitle: 'تنظيم مراحل المشروع كأقسام مستقلة',
          icon: 'create-new-folder',
          type: 'card_popup',
          onPress: () => setActivePopup('add_subgroup'),
        },
        {
          id: 'members_audit',
          title: 'قائمة التخصصات الكاملة',
          subtitle: `${group.members.length} عضو مسجل`,
          badge: group.members.length,
          icon: 'badge',
          type: 'card_popup',
          onPress: () => setActivePopup('members_audit'),
        },
      ],
    },
    {
      title: 'العمليات والاتصال',
      items: [
        {
          id: 'group_chat',
          title: 'محادثة فريق المشروع',
          subtitle: 'غرفة المحادثة الخاصة بهذا المشروع فقط',
          icon: 'chat',
          type: 'full_screen',
          onPress: () => {
            // Find or navigate to project chat
            router.push(`/chat/${id}`);
          },
        },
        {
          id: 'project_info',
          title: 'معلومات وهوية المشروع',
          subtitle: 'الاسم، المعرف، والوصف التأسيسي',
          icon: 'info-outline',
          type: 'card_popup',
          onPress: () => setActivePopup('project_info'),
        },
      ],
    },
  ];

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Top Header with Back Button on Left and Hamburger 3-Bars on Right */}
      <View style={styles.header}>
        <Pressable
          onPress={() => router.back()}
          style={styles.headerBackBtn}
          hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
        >
          <MaterialIcons
            name="arrow-forward-ios"
            size={18}
            color={Colors.textPrimary}
          />
        </Pressable>

        <View style={styles.headerTitleWrap}>
          <Text style={styles.headerTitle} numberOfLines={1}>
            {group.name}
          </Text>
          <Text style={styles.headerSubtitle}>
            {group.members.length} عضو • {subGroups.length} مجموعة فرعية
          </Text>
        </View>

        {/* 3-Bars Hamburger Button to open project-specific sidebar */}
        <Pressable
          onPress={() => setSidebarVisible(true)}
          style={styles.menuBtn}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        >
          <View style={styles.hamburgerBar} />
          <View style={[styles.hamburgerBar, { width: 16 }]} />
          <View style={styles.hamburgerBar} />
        </Pressable>
      </View>

      {/* Main Spacious Content */}
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.mainScrollContent}
      >
        {/* Breathing Space Hero Card - Capital Dashboard */}
        {project && (
          <View style={styles.capitalHeroCard}>
            <View style={styles.capitalHeroHeader}>
              <View style={styles.capitalHeroBadge}>
                <MaterialIcons name="account-balance-wallet" size={16} color={Colors.accent} />
                <Text style={styles.capitalHeroBadgeText}>رأس مال المشروع</Text>
              </View>

              <Pressable
                onPress={() => {
                  setCapitalInput(String(project.estimatedCapital));
                  setActivePopup('edit_capital');
                }}
                style={styles.heroEditPill}
              >
                <MaterialIcons name="edit" size={14} color={Colors.accent} />
                <Text style={styles.heroEditPillText}>تعديل الميزانية</Text>
              </Pressable>
            </View>

            {/* Clean 3-Metric Numbers Row */}
            <View style={styles.capitalHeroMetrics}>
              <View style={styles.metricCol}>
                <Text style={styles.metricLabel}>رأس المال المخصص</Text>
                <Text style={[styles.metricValue, { color: Colors.accent }]}>
                  {formatCurrency(project.estimatedCapital)}
                </Text>
              </View>

              <View style={styles.metricDivider} />

              <View style={styles.metricCol}>
                <Text style={styles.metricLabel}>المصروفات</Text>
                <Text style={[styles.metricValue, { color: Colors.error }]}>
                  {formatCurrency(project.totalExpenses)}
                </Text>
              </View>

              <View style={styles.metricDivider} />

              <View style={styles.metricCol}>
                <Text style={styles.metricLabel}>المتبقي</Text>
                <Text
                  style={[
                    styles.metricValue,
                    { color: remaining >= 0 ? Colors.profit : Colors.error },
                  ]}
                >
                  {formatCurrency(remaining)}
                </Text>
              </View>
            </View>

            {/* Progress Track */}
            <View style={styles.heroProgressWrap}>
              <View style={styles.heroProgressTrack}>
                <View
                  style={[
                    styles.heroProgressFill,
                    {
                      width: `${Math.min(spendingRate, 100)}%`,
                      backgroundColor:
                        spendingRate > 80 ? Colors.error : Colors.accent,
                    },
                  ]}
                />
              </View>
              <View style={styles.heroProgressLabels}>
                <Text style={styles.heroProgressText}>
                  {spendingRate.toFixed(1)}% تم صرفها
                </Text>
                <Text style={styles.heroProgressText}>
                  {formatCurrency(remaining)} متبقية
                </Text>
              </View>
            </View>
          </View>
        )}

        {/* Sub-groups Section (Spacious Cards) */}
        <View style={styles.sectionWrap}>
          <View style={styles.sectionHeader}>
            <Pressable
              style={styles.quickAddBtn}
              onPress={() => setActivePopup('add_subgroup')}
            >
              <MaterialIcons name="add" size={16} color={Colors.accent} />
              <Text style={styles.quickAddText}>إضافة</Text>
            </Pressable>
            <View style={styles.sectionTitleRow}>
              <Text style={styles.sectionTitle}>المجموعات الفرعية</Text>
              <View style={styles.badgeCount}>
                <Text style={styles.badgeCountText}>{subGroups.length}</Text>
              </View>
            </View>
          </View>

          {subGroups.length === 0 ? (
            <View style={styles.emptyCard}>
              <MaterialIcons name="folder-open" size={28} color={Colors.textMuted} />
              <Text style={styles.emptyCardText}>
                لا توجد مجموعات فرعية بعد. أنشئ مرحلة لتوزيع المهام.
              </Text>
            </View>
          ) : (
            subGroups.map(sg => (
              <View key={sg.id} style={styles.subGroupCard}>
                <View style={styles.subGroupIconWrap}>
                  <MaterialIcons name="folder" size={22} color={Colors.accent} />
                </View>
                <View style={styles.subGroupInfo}>
                  <Text style={styles.subGroupName}>{sg.name}</Text>
                  <Text style={styles.subGroupSubtitle}>
                    {sg.members.length} عضو مسجل
                  </Text>
                </View>
                <MaterialIcons
                  name="chevron-left"
                  size={20}
                  color={Colors.textMuted}
                />
              </View>
            ))
          )}
        </View>

        {/* Team Members Section (Clean Anonymous Listing) */}
        <View style={styles.sectionWrap}>
          <View style={styles.sectionHeader}>
            <Pressable
              style={styles.quickAddBtn}
              onPress={() => setActivePopup('add_member')}
            >
              <MaterialIcons name="person-add" size={16} color={Colors.accent} />
              <Text style={styles.quickAddText}>إضافة عضو</Text>
            </Pressable>
            <View style={styles.sectionTitleRow}>
              <Text style={styles.sectionTitle}>فريق العمل والتخصصات</Text>
              <View style={styles.badgeCount}>
                <Text style={styles.badgeCountText}>{group.members.length}</Text>
              </View>
            </View>
          </View>

          {/* Privacy Note */}
          <View style={styles.privacyNote}>
            <MaterialIcons name="lock" size={13} color={Colors.info} />
            <Text style={styles.privacyNoteText}>
              الأسماء الحقيقية مخفية لحماية سرية المقاولين والشركات — يظهر التخصص فقط
            </Text>
          </View>

          {group.members.length === 0 ? (
            <View style={styles.emptyCard}>
              <MaterialIcons name="people-outline" size={28} color={Colors.textMuted} />
              <Text style={styles.emptyCardText}>
                لم تتم إضافة أعضاء بعد. انقر على 'إضافة عضو' أو استخدم القائمة الجانبية.
              </Text>
            </View>
          ) : (
            group.members.map(m => (
              <View key={m.id} style={styles.memberCard}>
                <View style={styles.memberAvatar}>
                  <MaterialIcons name="badge" size={18} color={Colors.accent} />
                </View>
                <View style={styles.memberInfo}>
                  <Text style={styles.memberRole}>{m.roleLabel}</Text>
                  {m.status === 'accepted' && m.username !== 'pending' ? (
                    <Text style={styles.memberUsername}>@{m.username}</Text>
                  ) : null}
                  {m.phone && m.status !== 'accepted' ? (
                    <Text style={styles.memberPhone}>📞 {m.phone}</Text>
                  ) : null}
                </View>
                <View
                  style={[
                    styles.statusPill,
                    m.status === 'accepted'
                      ? styles.statusPillAccepted
                      : styles.statusPillInvited,
                  ]}
                >
                  <Text
                    style={[
                      styles.statusPillText,
                      m.status === 'accepted'
                        ? styles.statusPillTextAccepted
                        : styles.statusPillTextInvited,
                    ]}
                  >
                    {m.status === 'accepted' ? 'نشط' : 'مدعو'}
                  </Text>
                </View>
              </View>
            ))
          )}
        </View>

        {/* Wide Bottom Space for peaceful scrolling */}
        <View style={{ height: 40 }} />
      </ScrollView>

      {/* Dedicated Project Sidebar (3-Bars Drawer) */}
      <ProjectSidebar
        visible={sidebarVisible}
        onClose={() => setSidebarVisible(false)}
        projectName={group.name}
        projectType="إعدادات ومميزات المشروع المخصصة"
        sections={projectSidebarSections}
        quickActions={[
          {
            id: 'qa_add_mem',
            label: 'إضافة عضو',
            icon: 'person-add',
            onPress: () => setActivePopup('add_member'),
          },
          {
            id: 'qa_add_sub',
            label: 'مجموعة فرعية',
            icon: 'create-new-folder',
            onPress: () => setActivePopup('add_subgroup'),
          },
          {
            id: 'qa_chat',
            label: 'المحادثة',
            icon: 'chat',
            onPress: () => router.push(`/chat/${id}`),
          },
        ]}
      />

      {/* Feature Card Popup: Edit Capital */}
      <FeatureCardPopup
        visible={activePopup === 'edit_capital'}
        onClose={() => setActivePopup(null)}
        title="تعديل رأس مال المشروع"
        subtitle="حدد ميزانية العمل التقديرية"
        icon="monetization-on"
      >
        <View style={styles.popupInputWrap}>
          <Text style={styles.popupInputLabel}>مبلغ رأس المال (ج.م)</Text>
          <TextInput
            style={styles.popupInput}
            value={capitalInput}
            onChangeText={setCapitalInput}
            placeholder="مثال: 250000"
            placeholderTextColor={Colors.placeholder}
            keyboardType="decimal-pad"
            textAlign="right"
            autoFocus
          />
          <View style={styles.popupButtonsRow}>
            <Pressable
              style={styles.popupCancelBtn}
              onPress={() => setActivePopup(null)}
            >
              <Text style={styles.popupCancelText}>إلغاء</Text>
            </Pressable>
            <Pressable
              style={styles.popupConfirmBtn}
              onPress={handleUpdateCapital}
            >
              <Text style={styles.popupConfirmText}>حفظ الميزانية</Text>
            </Pressable>
          </View>
        </View>
      </FeatureCardPopup>

      {/* Feature Card Popup: Capital Audit */}
      <FeatureCardPopup
        visible={activePopup === 'capital_audit'}
        onClose={() => setActivePopup(null)}
        title="تدقيق ميزانية المشروع"
        subtitle="تفاصيل الإنفاق والرصيد المتبقي"
        icon="pie-chart"
      >
        {project && (
          <View style={styles.auditBody}>
            <View style={styles.auditRow}>
              <Text style={[styles.auditVal, { color: Colors.accent }]}>
                {formatCurrency(project.estimatedCapital)}
              </Text>
              <Text style={styles.auditLabel}>رأس المال المعتمد:</Text>
            </View>
            <View style={styles.auditRow}>
              <Text style={[styles.auditVal, { color: Colors.error }]}>
                {formatCurrency(project.totalExpenses)}
              </Text>
              <Text style={styles.auditLabel}>إجمالي المصروفات:</Text>
            </View>
            <View style={styles.auditRow}>
              <Text
                style={[
                  styles.auditVal,
                  { color: remaining >= 0 ? Colors.profit : Colors.error },
                ]}
              >
                {formatCurrency(remaining)}
              </Text>
              <Text style={styles.auditLabel}>صافي المتبقي:</Text>
            </View>
            <View style={styles.auditRow}>
              <Text style={styles.auditVal}>{spendingRate.toFixed(1)}%</Text>
              <Text style={styles.auditLabel}>نسبة الاستهلاك:</Text>
            </View>

            <Pressable
              style={styles.popupDoneBtn}
              onPress={() => setActivePopup(null)}
            >
              <Text style={styles.popupDoneText}>إغلاق</Text>
            </Pressable>
          </View>
        )}
      </FeatureCardPopup>

      {/* Feature Card Popup: Add Subgroup */}
      <FeatureCardPopup
        visible={activePopup === 'add_subgroup'}
        onClose={() => setActivePopup(null)}
        title="إنشاء مجموعة فرعية"
        subtitle="قسم المشروع إلى مراحل عمل أو تخصصات"
        icon="create-new-folder"
      >
        <View style={styles.popupInputWrap}>
          <Text style={styles.popupInputLabel}>اسم المجموعة الفرعية</Text>
          <TextInput
            style={styles.popupInput}
            value={newSubGroupName}
            onChangeText={setNewSubGroupName}
            placeholder="مثال: مرحلة الأساسات، تشطيب الكهرباء"
            placeholderTextColor={Colors.placeholder}
            textAlign="right"
            autoFocus
          />
          <View style={styles.popupButtonsRow}>
            <Pressable
              style={styles.popupCancelBtn}
              onPress={() => setActivePopup(null)}
            >
              <Text style={styles.popupCancelText}>إلغاء</Text>
            </Pressable>
            <Pressable
              style={styles.popupConfirmBtn}
              onPress={handleCreateSubGroup}
            >
              <Text style={styles.popupConfirmText}>إنشاء المجموعة</Text>
            </Pressable>
          </View>
        </View>
      </FeatureCardPopup>

      {/* Feature Card Popup: Add Member */}
      <FeatureCardPopup
        visible={activePopup === 'add_member'}
        onClose={() => setActivePopup(null)}
        title="إضافة عضو لفريق المشروع"
        subtitle="دعوة بسرية تامة (يظهر التخصص فقط)"
        icon="person-add"
      >
        <View style={styles.popupInputWrap}>
          <Text style={styles.popupInputLabel}>تخصص العضو أو دوره</Text>
          <TextInput
            style={styles.popupInput}
            value={newMemberRole}
            onChangeText={setNewMemberRole}
            placeholder="مثال: مهندس استشاري، حداد، مقاول دهانات"
            placeholderTextColor={Colors.placeholder}
            textAlign="right"
          />

          <Text style={[styles.popupInputLabel, { marginTop: 10 }]}>
            رقم الهاتف لإرسال الدعوة (اختياري)
          </Text>
          <TextInput
            style={styles.popupInput}
            value={newMemberPhone}
            onChangeText={setNewMemberPhone}
            placeholder="01xxxxxxxxx"
            placeholderTextColor={Colors.placeholder}
            keyboardType="phone-pad"
            textAlign="right"
          />

          <View style={styles.popupButtonsRow}>
            <Pressable
              style={styles.popupCancelBtn}
              onPress={() => setActivePopup(null)}
            >
              <Text style={styles.popupCancelText}>إلغاء</Text>
            </Pressable>
            <Pressable style={styles.popupConfirmBtn} onPress={handleAddMember}>
              <Text style={styles.popupConfirmText}>إضافة ودعوة</Text>
            </Pressable>
          </View>
        </View>
      </FeatureCardPopup>

      {/* Feature Card Popup: Members Audit */}
      <FeatureCardPopup
        visible={activePopup === 'members_audit'}
        onClose={() => setActivePopup(null)}
        title="قائمة أعضاء وتخصصات المشروع"
        subtitle={`إجمالي الفريق: ${group.members.length} عضو`}
        icon="badge"
      >
        <ScrollView style={{ maxHeight: 260 }} showsVerticalScrollIndicator={false}>
          {group.members.map(m => (
            <View key={m.id} style={styles.auditMemberRow}>
              <Text style={styles.auditMemberRole}>{m.roleLabel}</Text>
              <Text style={styles.auditMemberStatus}>
                {m.status === 'accepted' ? 'نشط' : 'مدعو'}
              </Text>
            </View>
          ))}
        </ScrollView>
        <Pressable
          style={[styles.popupDoneBtn, { marginTop: 12 }]}
          onPress={() => setActivePopup(null)}
        >
          <Text style={styles.popupDoneText}>إغلاق</Text>
        </Pressable>
      </FeatureCardPopup>

      {/* Feature Card Popup: Project Info */}
      <FeatureCardPopup
        visible={activePopup === 'project_info'}
        onClose={() => setActivePopup(null)}
        title="معلومات وهوية المشروع"
        subtitle="البيانات المسجلة للمشروع"
        icon="info-outline"
      >
        <View style={styles.auditBody}>
          <View style={styles.auditRow}>
            <Text style={styles.auditVal}>{group.name}</Text>
            <Text style={styles.auditLabel}>اسم المشروع:</Text>
          </View>
          {group.description ? (
            <View style={styles.auditRow}>
              <Text style={styles.auditVal}>{group.description}</Text>
              <Text style={styles.auditLabel}>الوصف:</Text>
            </View>
          ) : null}
          <View style={styles.auditRow}>
            <Text style={styles.auditVal}>
              {new Date(group.createdAt).toLocaleDateString('ar-EG')}
            </Text>
            <Text style={styles.auditLabel}>تاريخ التأسيس:</Text>
          </View>
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
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  notFound: { fontSize: 18, color: Colors.textSecondary, marginTop: 12 },
  backBtn: {
    marginTop: 16,
    backgroundColor: Colors.accent,
    borderRadius: 20,
    paddingHorizontal: 24,
    paddingVertical: 10,
  },
  backBtnText: { color: '#0B0F14', fontWeight: '800' },

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
  headerBackBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  headerTitleWrap: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  headerTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  headerSubtitle: {
    fontSize: 10,
    color: Colors.textMuted,
    marginTop: 2,
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

  mainScrollContent: {
    paddingHorizontal: 16,
    paddingTop: 12,
  },

  capitalHeroCard: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    padding: 18,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.25)',
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
    elevation: 4,
  },
  capitalHeroHeader: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  capitalHeroBadge: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 6,
    backgroundColor: Colors.accentGlow,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
  },
  capitalHeroBadgeText: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.accent,
  },
  heroEditPill: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: Colors.surfaceAlt,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  heroEditPillText: {
    fontSize: 11,
    color: Colors.accent,
    fontWeight: '700',
  },

  capitalHeroMetrics: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.background,
    borderRadius: 14,
    padding: 14,
    marginBottom: 14,
  },
  metricCol: {
    flex: 1,
    alignItems: 'center',
    gap: 4,
  },
  metricLabel: {
    fontSize: 10,
    color: Colors.textMuted,
  },
  metricValue: {
    fontSize: 13,
    fontWeight: '800',
  },
  metricDivider: {
    width: 1,
    height: 28,
    backgroundColor: Colors.border,
  },

  heroProgressWrap: {
    gap: 6,
  },
  heroProgressTrack: {
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.surfaceAlt,
    overflow: 'hidden',
  },
  heroProgressFill: {
    height: '100%',
    borderRadius: 4,
  },
  heroProgressLabels: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
  },
  heroProgressText: {
    fontSize: 10,
    color: Colors.textMuted,
  },

  sectionWrap: {
    marginBottom: 16,
  },
  sectionHeader: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  sectionTitleRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 8,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  badgeCount: {
    backgroundColor: Colors.surfaceAlt,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  badgeCountText: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.accent,
  },
  quickAddBtn: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    backgroundColor: Colors.accentGlow,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.25)',
  },
  quickAddText: {
    fontSize: 11,
    color: Colors.accent,
    fontWeight: '700',
  },

  emptyCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 18,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
    gap: 6,
  },
  emptyCardText: {
    fontSize: 12,
    color: Colors.textMuted,
    textAlign: 'center',
  },

  subGroupCard: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  subGroupIconWrap: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.25)',
  },
  subGroupInfo: {
    flex: 1,
    alignItems: 'flex-end',
    marginRight: 12,
  },
  subGroupName: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  subGroupSubtitle: {
    fontSize: 11,
    color: Colors.textMuted,
    marginTop: 2,
  },

  privacyNote: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 6,
    backgroundColor: Colors.infoLight,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 10,
    marginBottom: 8,
  },
  privacyNoteText: {
    fontSize: 11,
    color: Colors.info,
    flex: 1,
    textAlign: 'right',
  },

  memberCard: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  memberAvatar: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: Colors.surfaceAlt,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  memberInfo: {
    flex: 1,
    alignItems: 'flex-end',
    marginRight: 12,
  },
  memberRole: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  memberUsername: {
    fontSize: 11,
    color: Colors.accent,
    marginTop: 2,
  },
  memberPhone: {
    fontSize: 11,
    color: Colors.textMuted,
    marginTop: 2,
  },
  statusPill: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  statusPillAccepted: {
    backgroundColor: Colors.successLight,
  },
  statusPillInvited: {
    backgroundColor: Colors.warningLight,
  },
  statusPillText: {
    fontSize: 10,
    fontWeight: '800',
  },
  statusPillTextAccepted: {
    color: Colors.success,
  },
  statusPillTextInvited: {
    color: Colors.warning,
  },

  popupInputWrap: {
    gap: 8,
  },
  popupInputLabel: {
    fontSize: 12,
    color: Colors.textSecondary,
    textAlign: 'right',
  },
  popupInput: {
    backgroundColor: Colors.inputBg,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: Colors.inputText,
    borderWidth: 1,
    borderColor: Colors.inputBorder,
  },
  popupButtonsRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 14,
  },
  popupCancelBtn: {
    flex: 1,
    backgroundColor: Colors.surfaceAlt,
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  popupCancelText: {
    fontSize: 13,
    color: Colors.textSecondary,
    fontWeight: '700',
  },
  popupConfirmBtn: {
    flex: 1,
    backgroundColor: Colors.accent,
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  popupConfirmText: {
    fontSize: 13,
    color: '#0B0F14',
    fontWeight: '800',
  },

  auditBody: {
    gap: 12,
  },
  auditRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  auditLabel: {
    fontSize: 12,
    color: Colors.textMuted,
  },
  auditVal: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  auditMemberRow: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  auditMemberRole: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  auditMemberStatus: {
    fontSize: 11,
    color: Colors.accent,
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
});
