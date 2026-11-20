import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import {
  Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../constants/colors';
import { generateId } from '@/utils/id';
import type { GroupMember } from '@/types';

const ROLE_PRESETS = [
  'مهندس مدني', 'مهندس معماري', 'كهربائي', 'سباك',
  'نجار', 'حداد', 'نقاش', 'عامل بناء', 'سائق', 'أخرى',
];

interface PendingMember {
  phone: string;
  roleLabel: string;
}

export default function NewProjectScreen() {
  const insets = useSafeAreaInsets();
  const { createWorkGroup, createProject, addGroupMember, createGroupChat, sendInvitation, user } = useApp();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [capital, setCapital] = useState('');
  const [members, setMembers] = useState<PendingMember[]>([]);
  const [showAddMember, setShowAddMember] = useState(false);
  const [newMemberRole, setNewMemberRole] = useState('');
  const [newMemberPhone, setNewMemberPhone] = useState('');
  const [loading, setLoading] = useState(false);

  function addMember() {
    if (!newMemberPhone.trim() || !newMemberRole.trim()) {
      Alert.alert('تنبيه', 'يرجى إدخال رقم الهاتف والتخصص');
      return;
    }
    // Check duplicate phone
    if (members.some(m => m.phone === newMemberPhone.trim())) {
      Alert.alert('تنبيه', 'هذا الرقم مضاف بالفعل');
      return;
    }
    setMembers([...members, { phone: newMemberPhone.trim(), roleLabel: newMemberRole.trim() }]);
    setNewMemberRole('');
    setNewMemberPhone('');
    setShowAddMember(false);
  }

  async function handleCreate() {
    if (!name.trim()) {
      Alert.alert('تنبيه', 'يرجى إدخال اسم المشروع');
      return;
    }
    if (loading) return; // prevent double-click
    setLoading(true);
    try {
      // 1. Create work group
      const group = await createWorkGroup(name.trim(), description.trim() || undefined);

      // 2. Create project with capital
      const capitalNum = parseFloat(capital) || 0;
      if (capitalNum > 0) {
        await createProject(name.trim(), group.id, capitalNum);
      }

      // 3. Add members with invitations
      const groupMembers: GroupMember[] = [];
      for (const m of members) {
        const memberId = generateId();
        const member: GroupMember = {
          id: memberId,
          userId: memberId,
          username: 'pending',
          phone: m.phone,
          roleLabel: m.roleLabel,
          status: 'invited',
          addedAt: Date.now(),
        };
        groupMembers.push(member);
        await addGroupMember(group.id, member.userId, member.username, m.phone, m.roleLabel);
        await sendInvitation(group.id, name.trim(), m.phone, m.roleLabel);
      }

      // 4. Add current user as owner
      const ownerMember: GroupMember = {
        id: generateId(),
        userId: user?.id ?? '',
        username: user?.username ?? 'owner',
        phone: user?.phone ?? '',
        roleLabel: 'مدير المشروع',
        status: 'accepted',
        addedAt: Date.now(),
      };
      groupMembers.push(ownerMember);

      // 5. Create group chat — createGroupChat checks for duplicates internally
      const groupChat = await createGroupChat(group.id, name.trim(), groupMembers);

      // 6. Navigate DIRECTLY to the group chat — no Alert (web doesn't show it well)
      setLoading(false);
      router.replace(`/chat/${groupChat.id}`);
    } catch (e) {
      console.error('Project creation error:', e);
      Alert.alert('خطأ', 'حدث خطأ أثناء الإنشاء');
      setLoading(false);
    }
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()}>
          <MaterialIcons name="arrow-back" size={24} color={Colors.textPrimary} />
        </Pressable>
        <Text style={styles.headerTitle}>مشروع جديد</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        {/* Name */}
        <Text style={styles.fieldLabel}>اسم المشروع</Text>
        <TextInput
          style={styles.input}
          value={name}
          onChangeText={setName}
          placeholder="مثال: برج التجارة"
          placeholderTextColor={Colors.placeholder}
          textAlign="right"
          autoFocus
        />

        {/* Description */}
        <Text style={styles.fieldLabel}>الوصف (اختياري)</Text>
        <TextInput
          style={[styles.input, { minHeight: 70 }]}
          value={description}
          onChangeText={setDescription}
          placeholder="وصف مختصر للمشروع"
          placeholderTextColor={Colors.placeholder}
          textAlign="right"
          multiline
        />

        {/* Capital */}
        <Text style={styles.fieldLabel}>رأس المال التقديري</Text>
        <TextInput
          style={styles.input}
          value={capital}
          onChangeText={setCapital}
          placeholder="0.00"
          placeholderTextColor={Colors.placeholder}
          keyboardType="decimal-pad"
          textAlign="right"
        />

        {/* Members */}
        <Text style={styles.fieldLabel}>أعضاء المشروع</Text>
        <Text style={styles.hintText}>
          🔒 الأعضاء يظهرون بأسماء مستخدميهم وتخصصاتهم فقط — الهوية مخفية
        </Text>

        {members.map((m, i) => (
          <View key={i} style={styles.memberRow}>
            <Pressable onPress={() => setMembers(members.filter((_, idx) => idx !== i))}>
              <MaterialIcons name="remove-circle-outline" size={20} color={Colors.error} />
            </Pressable>
            <View style={styles.memberInfo}>
              <Text style={styles.memberRole}>{m.roleLabel}</Text>
              <Text style={styles.memberPhone}>📞 {m.phone}</Text>
              <View style={styles.pendingBadge}>
                <MaterialIcons name="hourglass-empty" size={10} color={Colors.warning} />
                <Text style={styles.pendingText}>بانتظار القبول</Text>
              </View>
            </View>
            <View style={styles.memberAvatar}>
              <MaterialIcons name="person" size={18} color={Colors.textPrimary} />
            </View>
          </View>
        ))}

        {/* Add member form */}
        {showAddMember ? (
          <View style={styles.addMemberBox}>
            <Text style={styles.addMemberLabel}>رقم هاتف العضو *</Text>
            <View style={styles.phoneInputWrap}>
              <MaterialIcons name="phone" size={18} color={Colors.muted} />
              <TextInput
                style={styles.phoneInput}
                value={newMemberPhone}
                onChangeText={setNewMemberPhone}
                placeholder="01xxxxxxxxx"
                placeholderTextColor={Colors.placeholder}
                keyboardType="phone-pad"
                textAlign="right"
                autoFocus
              />
            </View>

            <Text style={styles.addMemberLabel}>التخصص *</Text>
            <View style={styles.rolePresets}>
              {ROLE_PRESETS.map(r => (
                <Pressable
                  key={r}
                  style={[styles.roleChip, newMemberRole === r && styles.roleChipActive]}
                  onPress={() => setNewMemberRole(r)}
                >
                  <Text style={[styles.roleChipText, newMemberRole === r && styles.roleChipTextActive]}>{r}</Text>
                </Pressable>
              ))}
            </View>
            <TextInput
              style={styles.input}
              value={newMemberRole}
              onChangeText={setNewMemberRole}
              placeholder="أو اكتب تخصصاً مخصصاً"
              placeholderTextColor={Colors.placeholder}
              textAlign="right"
            />

            <View style={styles.addMemberActions}>
              <Pressable style={styles.cancelAddBtn} onPress={() => setShowAddMember(false)}>
                <Text style={styles.cancelAddText}>إلغاء</Text>
              </Pressable>
              <Pressable style={styles.confirmAddBtn} onPress={addMember}>
                <Text style={styles.confirmAddText}>إضافة ودعوة</Text>
              </Pressable>
            </View>
          </View>
        ) : (
          <Pressable style={styles.addMemberBtn} onPress={() => setShowAddMember(true)}>
            <MaterialIcons name="person-add" size={18} color={Colors.accent} />
            <Text style={styles.addMemberBtnText}>إضافة عضو برقم الهاتف</Text>
          </Pressable>
        )}
      </ScrollView>

      {/* Footer */}
      <View style={[styles.footer, { paddingBottom: Math.max(insets.bottom, 16) }]}>
        <Pressable
          style={[styles.createBtn, loading && styles.createBtnDisabled]}
          onPress={handleCreate}
          disabled={loading}
        >
          <Text style={styles.createBtnText}>
            {loading ? 'جارٍ الإنشاء...' : 'إنشاء المشروع والمجموعة'}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },

  header: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.primary,
    paddingHorizontal: 16, paddingVertical: 14,
  },
  headerTitle: { fontSize: 17, fontWeight: '700', color: Colors.textPrimary },

  fieldLabel: {
    fontSize: 14, fontWeight: '700', color: Colors.textPrimary,
    textAlign: 'right', marginBottom: 8, marginTop: 16,
    paddingHorizontal: 16,
  },
  hintText: {
    fontSize: 12, color: Colors.textMuted,
    textAlign: 'right', marginBottom: 10, paddingHorizontal: 16,
  },
  input: {
    backgroundColor: Colors.surface, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 14,
    fontSize: 16, color: Colors.textPrimary,
    borderWidth: 1, borderColor: Colors.border,
    marginHorizontal: 16,
  },

  memberRow: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.surface, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 12,
    marginHorizontal: 16, marginBottom: 6,
    borderWidth: 1, borderColor: Colors.border,
  },
  memberAvatar: {
    width: 40, height: 40, borderRadius: 20,
    backgroundColor: Colors.surfaceAlt,
    alignItems: 'center', justifyContent: 'center',
  },
  memberInfo: { flex: 1, alignItems: 'flex-end', marginRight: 12, gap: 3 },
  memberRole: { fontSize: 15, fontWeight: '600', color: Colors.textPrimary },
  memberPhone: { fontSize: 12, color: Colors.textSecondary },
  pendingBadge: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 3,
    backgroundColor: Colors.warningLight, borderRadius: 8,
    paddingHorizontal: 6, paddingVertical: 1,
  },
  pendingText: { fontSize: 9, color: Colors.warning, fontWeight: '600' },

  addMemberBtn: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'center', gap: 8,
    backgroundColor: Colors.accentGlow, borderRadius: 12,
    paddingVertical: 12, marginHorizontal: 16, marginTop: 6,
    borderWidth: 1, borderColor: Colors.accentDark + '40',
  },
  addMemberBtnText: { fontSize: 14, fontWeight: '600', color: Colors.accentLight },

  addMemberBox: {
    backgroundColor: Colors.surface, borderRadius: 14,
    padding: 14, marginHorizontal: 16, marginTop: 6,
    borderWidth: 1, borderColor: Colors.border,
  },
  addMemberLabel: {
    fontSize: 13, fontWeight: '600', color: Colors.textSecondary,
    marginBottom: 6, textAlign: 'right',
  },
  phoneInputWrap: {
    flexDirection: 'row-reverse', alignItems: 'center',
    backgroundColor: Colors.inputBg, borderRadius: 12,
    borderWidth: 1, borderColor: Colors.inputBorder,
    paddingHorizontal: 14, gap: 8, marginBottom: 10,
  },
  phoneInput: {
    flex: 1, paddingVertical: 12,
    color: Colors.textPrimary, fontSize: 16,
  },
  rolePresets: {
    flexDirection: 'row-reverse', flexWrap: 'wrap',
    gap: 6, marginBottom: 10,
  },
  roleChip: {
    backgroundColor: Colors.background, borderRadius: 20,
    paddingHorizontal: 12, paddingVertical: 6,
    borderWidth: 1, borderColor: Colors.border,
  },
  roleChipActive: { backgroundColor: Colors.accent, borderColor: Colors.accent },
  roleChipText: { fontSize: 12, color: Colors.textSecondary },
  roleChipTextActive: { color: '#0B0F14', fontWeight: '600' },

  addMemberActions: { flexDirection: 'row-reverse', gap: 10, marginTop: 8 },
  cancelAddBtn: {
    flex: 1, backgroundColor: Colors.background, borderRadius: 10,
    paddingVertical: 10, alignItems: 'center',
    borderWidth: 1, borderColor: Colors.border,
  },
  cancelAddText: { fontSize: 14, color: Colors.textSecondary },
  confirmAddBtn: {
    flex: 1, backgroundColor: Colors.accent, borderRadius: 10,
    paddingVertical: 10, alignItems: 'center',
  },
  confirmAddText: { fontSize: 14, color: '#0B0F14', fontWeight: '700' },

  footer: {
    paddingHorizontal: 16, paddingTop: 12,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  createBtn: {
    backgroundColor: Colors.accent, borderRadius: 16,
    paddingVertical: 16, alignItems: 'center',
  },
  createBtnDisabled: { opacity: 0.5 },
  createBtnText: { fontSize: 16, fontWeight: '700', color: '#E6EDF3' },
});
