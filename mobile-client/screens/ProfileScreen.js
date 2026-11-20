import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native';
import { useAuth } from '../context/AuthContext';

export default function ProfileScreen({ navigation }) {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    Alert.alert('تسجيل الخروج', 'هل تود تسجيل الخروج من تطبيق OOZOX؟', [
      { text: 'إلغاء', style: 'cancel' },
      { text: 'خروج', style: 'destructive', onPress: () => logout() },
    ]);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.avatarCard}>
        <View style={styles.avatarCircle}>
          <Text style={styles.avatarInitial}>
            {user?.name ? user.name[0].toUpperCase() : 'U'}
          </Text>
        </View>
        <Text style={styles.userName}>{user?.name || 'مستخدم OOZOX'}</Text>
        <Text style={styles.userRole}>{user?.role || 'تاجر / عميل'}</Text>
        <Text style={styles.userContact}>{user?.email || user?.phone || ''}</Text>
      </View>

      <View style={styles.sectionCard}>
        <Text style={styles.sectionTitle}>معلومات الحساب</Text>
        <View style={styles.row}>
          <Text style={styles.rowValue}>{user?.phone || 'غير مسجل'}</Text>
          <Text style={styles.rowLabel}>رقم الهاتف</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.rowValue}>{user?.businessName || 'مؤسسة الذهب'}</Text>
          <Text style={styles.rowLabel}>اسم النشاط</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.rowValue}>مفعل وموثق ✅</Text>
          <Text style={styles.rowLabel}>حالة الحساب</Text>
        </View>
      </View>

      <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
        <Text style={styles.logoutText}>تسجيل الخروج 🚪</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0D1117' },
  content: { padding: 16 },
  avatarCard: {
    backgroundColor: '#161B22',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#30363D',
  },
  avatarCircle: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#D4AF37',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  avatarInitial: { fontSize: 28, fontWeight: '800', color: '#0D1117' },
  userName: { fontSize: 20, fontWeight: '700', color: '#F0F6FC' },
  userRole: { fontSize: 13, color: '#D4AF37', fontWeight: '600', marginTop: 4 },
  userContact: { fontSize: 13, color: '#8B949E', marginTop: 2 },
  sectionCard: {
    backgroundColor: '#161B22',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#30363D',
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#F0F6FC',
    textAlign: 'right',
    marginBottom: 12,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#21262D',
  },
  rowLabel: { color: '#8B949E', fontSize: 13 },
  rowValue: { color: '#F0F6FC', fontSize: 13, fontWeight: '600' },
  logoutBtn: {
    backgroundColor: '#21262D',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#DA3633',
    marginTop: 8,
  },
  logoutText: { color: '#FF7B72', fontSize: 15, fontWeight: '700' },
});
