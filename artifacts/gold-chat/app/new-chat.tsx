import { MaterialIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  FlatList,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useApp } from '@/context/AppContext';
import { Colors } from '../constants/colors';
import { VerifiedBadge } from '@/components/VerifiedBadge';
import type { Contact, UserType } from '@/types';
import { generateId } from '@/utils/id';
import { getInitials } from '@/utils/gold';
import {
  getPhoneContacts,
  phoneContactsToAppContacts,
  requestContactsPermission,
  isContactsSupported,
} from '@/utils/phone-contacts';

export default function NewChatScreen() {
  const insets = useSafeAreaInsets();
  const { contacts, addContact, createChat } = useApp();

  const [mode, setMode] = useState<'list' | 'add' | 'phone'>('list');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [contactType, setContactType] = useState<UserType>('businessman');
  const [search, setSearch] = useState('');
  const [phoneContacts, setPhoneContacts] = useState<Contact[]>([]);
  const [importing, setImporting] = useState(false);

  const topPad = Platform.OS === 'web' ? 67 : insets.top;

  const filtered = contacts.filter(
    (c) =>
      c.name.toLowerCase().includes(search.toLowerCase()) ||
      c.phone.includes(search),
  );

  // NOTE: username is intentionally NOT searchable — protects intermediaries
  // from clients contacting each other directly

  async function handleStart(contact: Contact) {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    const chat = await createChat(contact);
    router.replace(`/chat/${chat.id}`);
  }

  async function handleAdd() {
    if (!name.trim() || !phone.trim()) return;
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    const contact = await addContact({
      name: name.trim(),
      phone: phone.trim(),
      type: contactType,
      isVerified: contactType === 'gold_trader',
    });
    await handleStart(contact);
  }

  async function handleImportContacts() {
    if (!isContactsSupported()) {
      Alert.alert('غير متاح', 'استيراد جهات الاتصال متاح على الموبايل فقط — أضف جهة اتصال يدوياً');
      return;
    }
    setImporting(true);
    try {
      const granted = await requestContactsPermission();
      if (!granted) {
        Alert.alert('الإذن مطلوب', 'يرجى السماح للتطبيق بالوصول إلى جهات الاتصال');
        setImporting(false);
        return;
      }

      const phoneBook = await getPhoneContacts();
      const { newContacts } = phoneContactsToAppContacts(phoneBook, contacts);

      // Add all new contacts
      for (const c of newContacts) {
        await addContact({
          name: c.name,
          phone: c.phone,
          type: c.type,
          isVerified: c.isVerified,
          fromPhoneBook: true,
        });
      }

      setPhoneContacts(newContacts);
      if (newContacts.length === 0) {
        Alert.alert('لا جهات جديدة', 'كل جهات الاتصال في هاتفك موجودة بالفعل في التطبيق');
      }
      setMode('phone');
    } catch {
      Alert.alert('خطأ', 'حدث خطأ أثناء استيراد جهات الاتصال');
    } finally {
      setImporting(false);
    }
  }

  const displayList = mode === 'phone' ? phoneContacts : filtered;

  return (
    <View style={[styles.container, { backgroundColor: Colors.background }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: topPad + 12 }]}>
        <Pressable onPress={() => (mode !== 'list' ? setMode('list') : router.back())} hitSlop={8}>
          <MaterialIcons name="arrow-back" size={24} color={Colors.headerText} />
        </Pressable>
        <Text style={styles.headerTitle}>
          {mode === 'add' ? 'إضافة جهة اتصال' : mode === 'phone' ? 'جهات الهاتف' : 'محادثة جديدة'}
        </Text>
        {mode === 'list' && (
          <View style={styles.headerActions}>
            <Pressable onPress={handleImportContacts} disabled={importing} hitSlop={8}>
              <MaterialIcons
                name={importing ? 'hourglass-top' : 'contacts'}
                size={22}
                color={Colors.accent}
              />
            </Pressable>
            <Pressable onPress={() => setMode('add')} hitSlop={8}>
              <MaterialIcons name="person-add" size={22} color={Colors.accent} />
            </Pressable>
          </View>
        )}
        {mode !== 'list' && <View style={{ width: 22 }} />}
      </View>

      {mode === 'list' && (
        <>
          {/* Search */}
          <View style={[styles.searchContainer, { backgroundColor: Colors.header }]}>
            <View style={styles.searchBar}>
              <MaterialIcons name="search" size={18} color="rgba(255,255,255,0.5)" />
              <TextInput
                style={styles.searchInput}
                placeholder="ابحث بالاسم أو رقم الهاتف"
                placeholderTextColor="rgba(255,255,255,0.4)"
                value={search}
                onChangeText={setSearch}
                textAlign="right"
              />
            </View>
          </View>

          {/* Import banner */}
          <Pressable style={styles.importBanner} onPress={handleImportContacts} disabled={importing}>
            <MaterialIcons name="contacts" size={24} color={Colors.accent} />
            <View style={styles.importText}>
              <Text style={styles.importTitle}>استيراد من دليل الهاتف</Text>
              <Text style={styles.importSubtitle}>
                {importing ? 'جارٍ الاستيراد...' : 'أضف جهات اتصالك من هاتفك مثل واتساب'}
              </Text>
            </View>
            <MaterialIcons name="chevron-left" size={20} color={Colors.textMuted} />
          </Pressable>
        </>
      )}

      {mode !== 'add' && (
        <FlatList
          data={displayList}
          keyExtractor={(c) => c.id}
          renderItem={({ item }) => (
            <Pressable
              style={({ pressed }) => [
                styles.contactRow,
                pressed && { backgroundColor: Colors.surfaceAlt },
              ]}
              onPress={() => handleStart(item)}
            >
              <MaterialIcons name="chevron-left" size={20} color={Colors.textMuted} />
              <View style={styles.contactInfo}>
                <View style={styles.contactNameRow}>
                  <Text style={styles.contactName}>{item.name}</Text>
                  {item.isVerified && <VerifiedBadge size={14} />}
                  {item.fromPhoneBook && (
                    <View style={styles.phoneBadge}>
                      <MaterialIcons name="phone" size={10} color={Colors.accent} />
                      <Text style={styles.phoneBadgeText}>هاتف</Text>
                    </View>
                  )}
                </View>
                <Text style={styles.contactPhone}>{item.phone}</Text>
              </View>
              <View style={[styles.avatar, { backgroundColor: Colors.primary }]}>
                <Text style={styles.avatarText}>{getInitials(item.name)}</Text>
              </View>
            </Pressable>
          )}
          ItemSeparatorComponent={() => <View style={[styles.sep, { backgroundColor: Colors.border }]} />}
          ListEmptyComponent={
            <View style={styles.empty}>
              <MaterialIcons
                name={mode === 'phone' ? 'contact-phone' : 'people-outline'}
                size={52}
                color={Colors.textMuted}
              />
              <Text style={styles.emptyTitle}>
                {mode === 'phone' ? 'لا جهات هاتف مستوردة' : 'لا توجد جهات اتصال'}
              </Text>
              <Text style={styles.emptyText}>
                {mode === 'phone'
                  ? 'استورد جهات الاتصال من هاتفك أو أضف جهة جديدة يدوياً'
                  : 'أضف جهة اتصال جديدة بالضغط على أيقونة الشخص أعلاه'}
              </Text>
            </View>
          }
          contentContainerStyle={displayList.length === 0 && styles.emptyFlex}
        />
      )}

      {mode === 'add' && (
        <ScrollView contentContainerStyle={styles.addForm} keyboardShouldPersistTaps="handled">
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>الاسم</Text>
            <TextInput
              style={styles.input}
              placeholder="اسم جهة الاتصال"
              placeholderTextColor={Colors.placeholder}
              value={name}
              onChangeText={setName}
              textAlign="right"
              autoFocus
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>رقم الهاتف</Text>
            <TextInput
              style={styles.input}
              placeholder="رقم الهاتف"
              placeholderTextColor={Colors.placeholder}
              value={phone}
              onChangeText={setPhone}
              keyboardType="phone-pad"
              textAlign="right"
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>النوع</Text>
            <View style={styles.typeRow}>
              {(['businessman', 'gold_trader'] as UserType[]).map((t) => (
                <Pressable
                  key={t}
                  style={[
                    styles.typeBtn,
                    {
                      backgroundColor: contactType === t ? Colors.accent : Colors.secondary,
                      borderColor: contactType === t ? Colors.accent : Colors.border,
                      flex: 1,
                    },
                  ]}
                  onPress={() => setContactType(t)}
                >
                  <Text style={[styles.typeBtnText, { color: contactType === t ? '#fff' : Colors.foreground }]}>
                    {t === 'gold_trader' ? 'تاجر ذهب' : 'رجل أعمال'}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <Pressable
            style={({ pressed }) => [
              styles.addBtn,
              {
                backgroundColor: name.trim() && phone.trim() ? Colors.accent : Colors.muted,
                opacity: pressed ? 0.85 : 1,
              },
            ]}
            onPress={handleAdd}
            disabled={!name.trim() || !phone.trim()}
          >
            <MaterialIcons name="chat" size={20} color="#fff" />
            <Text style={styles.addBtnText}>إضافة وبدء المحادثة</Text>
          </Pressable>
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 14,
    backgroundColor: Colors.header,
  },
  headerTitle: { fontSize: 17, fontFamily: 'Inter_700Bold', color: Colors.headerText },
  headerActions: { flexDirection: 'row', gap: 16 },

  searchContainer: { paddingHorizontal: 14, paddingBottom: 12, backgroundColor: Colors.header },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.12)',
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 8,
    gap: 6,
  },
  searchInput: { flex: 1, color: '#E6EDF3', fontSize: 14, fontFamily: 'Inter_400Regular' },

  importBanner: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 12,
    backgroundColor: Colors.accentLight,
    paddingHorizontal: 16,
    paddingVertical: 12,
    marginHorizontal: 12,
    marginVertical: 8,
    borderRadius: 14,
  },
  importText: { flex: 1 },
  importTitle: { fontSize: 14, fontWeight: '700', color: Colors.accentDark },
  importSubtitle: { fontSize: 12, color: Colors.textSecondary, marginTop: 2 },

  contactRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 12,
    backgroundColor: Colors.card,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { color: '#E6EDF3', fontSize: 17, fontFamily: 'Inter_600SemiBold' },
  contactInfo: { flex: 1, gap: 2, alignItems: 'flex-end' },
  contactNameRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  contactName: { fontSize: 15, fontFamily: 'Inter_600SemiBold', color: Colors.foreground },
  contactPhone: { fontSize: 13, fontFamily: 'Inter_400Regular', color: Colors.textMuted },
  phoneBadge: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 3,
    backgroundColor: Colors.accentLight,
    borderRadius: 6,
    paddingHorizontal: 5,
    paddingVertical: 1,
  },
  phoneBadgeText: { fontSize: 9, color: Colors.accentDark, fontWeight: '600' },
  sep: { height: StyleSheet.hairlineWidth, marginLeft: 76 },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    paddingHorizontal: 40,
    paddingTop: 80,
  },
  emptyFlex: { flex: 1 },
  emptyTitle: { fontSize: 18, fontFamily: 'Inter_600SemiBold', color: Colors.foreground },
  emptyText: { fontSize: 14, fontFamily: 'Inter_400Regular', color: Colors.textMuted, textAlign: 'center', lineHeight: 22 },

  addForm: { padding: 20, gap: 16 },
  inputGroup: { gap: 8 },
  inputLabel: { fontSize: 13, fontFamily: 'Inter_500Medium', textAlign: 'right', color: Colors.textSecondary },
  input: {
    borderRadius: 12,
    borderWidth: 1,
    padding: 14,
    fontSize: 15,
    fontFamily: 'Inter_400Regular',
    backgroundColor: Colors.card,
    borderColor: Colors.border,
    color: Colors.foreground,
  },
  typeRow: { flexDirection: 'row', gap: 10 },
  typeBtn: {
    borderRadius: 12,
    borderWidth: 1,
    padding: 12,
    alignItems: 'center',
  },
  typeBtnText: { fontSize: 12, fontFamily: 'Inter_600SemiBold' },
  addBtn: {
    borderRadius: 14,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginTop: 8,
  },
  addBtnText: { color: '#E6EDF3', fontSize: 16, fontFamily: 'Inter_700Bold' },
});
