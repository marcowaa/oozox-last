import { MaterialIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useState } from 'react';
import {
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
import { generateId } from '@/utils/id';

export default function OnboardingScreen() {
  const insets = useSafeAreaInsets();
  const { setUser } = useApp();

  const [step, setStep] = useState<'info' | 'username'>('info');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [username, setUsername] = useState('');

  function generateUsernameSuggestion(n: string): string {
    const clean = n.trim().replace(/\s+/g, '_').replace(/[^\u0600-\u06FF\w]/g, '');
    const suffix = Math.floor(Math.random() * 9999).toString().padStart(2, '0');
    return clean || 'user_' + suffix;
  }

  async function handleContinue() {
    if (step === 'info') {
      if (!name.trim() || !phone.trim()) return;
      setUsername(generateUsernameSuggestion(name));
      setStep('username');
      return;
    }
    if (!username.trim()) return;
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    await setUser({
      id: generateId(),
      name: name.trim(),
      phone: phone.trim(),
      username: username.trim().replace(/^@/, ''),
      type: 'businessman',
      isVerified: false,
      createdAt: Date.now(),
    });
    router.replace('/(tabs)');
  }

  const canContinue =
    step === 'info' ? name.trim().length > 0 && phone.trim().length > 0 : username.trim().length > 0;

  return (
    <LinearGradient colors={Colors.onboardingGradient as [string, string, string]} style={styles.gradient}>
      <ScrollView
        contentContainerStyle={[
          styles.container,
          { paddingTop: insets.top + 60, paddingBottom: insets.bottom + 40 },
        ]}
        keyboardShouldPersistTaps="handled"
      >
        {/* Logo */}
        <View style={styles.logoArea}>
          <View style={styles.logoCircle}>
            <LinearGradient
              colors={Colors.goldGradient as [string, string, string]}
              style={styles.logoGradient}
            >
              <MaterialIcons name="diamond" size={48} color="#0B0F14" />
            </LinearGradient>
          </View>
          <Text style={styles.appName}>ذهبي</Text>
          <View style={styles.taglineBar} />
          <Text style={styles.tagline}>منصة إدارة الأعمال والذهب</Text>
        </View>

        {step === 'info' ? (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>بياناتك الشخصية</Text>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>الاسم</Text>
              <View style={styles.inputWrap}>
                <MaterialIcons name="person-outline" size={20} color={Colors.muted} />
                <TextInput
                  style={styles.input}
                  placeholder="أدخل اسمك"
                  placeholderTextColor={Colors.placeholder}
                  value={name}
                  onChangeText={setName}
                  textAlign="right"
                  autoFocus
                />
              </View>
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>رقم الهاتف</Text>
              <View style={styles.inputWrap}>
                <MaterialIcons name="phone" size={20} color={Colors.muted} />
                <TextInput
                  style={styles.input}
                  placeholder="01xxxxxxxxx"
                  placeholderTextColor={Colors.placeholder}
                  value={phone}
                  onChangeText={setPhone}
                  keyboardType="phone-pad"
                  textAlign="right"
                />
              </View>
            </View>
          </View>
        ) : (
          <View style={styles.card}>
            <Pressable style={styles.backBtn} onPress={() => setStep('info')}>
              <MaterialIcons name="arrow-back" size={20} color={Colors.textSecondary} />
              <Text style={styles.backText}>رجوع</Text>
            </Pressable>

            <Text style={styles.cardTitle}>اسم المستخدم</Text>
            <View style={styles.usernameHintRow}>
              <MaterialIcons name="lock" size={16} color={Colors.accent} />
              <Text style={styles.usernameHint}>
                يُستخدم في مجموعات العمل فقط — لا يظهر في البحث
              </Text>
            </View>
            <View style={styles.inputGroup}>
              <View style={styles.usernameWrap}>
                <Text style={styles.atSymbol}>@</Text>
                <TextInput
                  style={styles.usernameInput}
                  placeholder="username"
                  placeholderTextColor={Colors.placeholder}
                  value={username}
                  onChangeText={setUsername}
                  textAlign="left"
                  autoCapitalize="none"
                  autoCorrect={false}
                />
              </View>
            </View>
            <View style={styles.privacyCard}>
              <MaterialIcons name="shield" size={20} color={Colors.accent} />
              <Text style={styles.privacyText}>
                الاسم الحقيقي ورقم الهاتف مخفيان في المجموعات. يظهر فقط اسم المستخدم والتخصص — يحمي الوسطاء.
              </Text>
            </View>
          </View>
        )}

        <Pressable
          style={({ pressed }) => [
            styles.continueBtn,
            pressed && { transform: [{ scale: 0.98 }] },
            !canContinue && { opacity: 0.4 },
          ]}
          onPress={handleContinue}
          disabled={!canContinue}
        >
          <LinearGradient
            colors={canContinue ? (Colors.goldGradient as [string, string, string]) : (['#21262D', '#21262D', '#21262D'] as [string, string, string])}
            style={styles.continueGradient}
          >
            <Text style={[styles.continueText, { color: canContinue ? '#0B0F14' : Colors.textMuted }]}>{step === 'info' ? 'التالي' : 'ابدأ التطبيق'}</Text>
            <MaterialIcons name="arrow-forward" size={20} color={canContinue ? '#0B0F14' : Colors.muted} />
          </LinearGradient>
        </Pressable>
      </ScrollView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  gradient: { flex: 1 },
  container: { paddingHorizontal: 24, gap: 28 },

  logoArea: { alignItems: 'center', gap: 8 },
  logoCircle: {
    width: 100, height: 100, borderRadius: 30,
    overflow: 'hidden',
    marginBottom: 4,
  },
  logoGradient: {
    width: '100%', height: '100%',
    alignItems: 'center', justifyContent: 'center',
  },
  appName: {
    fontSize: 42, fontFamily: 'Inter_700Bold',
    color: Colors.accent, letterSpacing: 4,
  },
  taglineBar: {
    width: 60, height: 2,
    backgroundColor: Colors.accent, borderRadius: 1, marginVertical: 4,
  },
  tagline: {
    fontSize: 14, color: Colors.textSecondary,
    fontFamily: 'Inter_400Regular',
  },

  card: {
    backgroundColor: Colors.surface,
    borderRadius: 20, padding: 20,
    borderWidth: 1, borderColor: Colors.border,
    gap: 16,
  },
  cardTitle: {
    fontSize: 20, fontFamily: 'Inter_700Bold',
    color: Colors.textPrimary, textAlign: 'right',
  },

  inputGroup: { gap: 8 },
  inputLabel: {
    color: Colors.textSecondary, fontSize: 13,
    fontFamily: 'Inter_500Medium', textAlign: 'right',
  },
  inputWrap: {
    flexDirection: 'row-reverse', alignItems: 'center',
    backgroundColor: Colors.inputBg, borderRadius: 14,
    borderWidth: 1, borderColor: Colors.inputBorder,
    paddingHorizontal: 14, gap: 10,
  },
  input: {
    flex: 1, paddingVertical: 14,
    color: Colors.textPrimary, fontSize: 16,
    fontFamily: 'Inter_400Regular',
  },

  usernameHintRow: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 6,
  },
  usernameHint: {
    fontSize: 12, color: Colors.textSecondary,
    fontFamily: 'Inter_400Regular',
  },
  usernameWrap: {
    flexDirection: 'row', alignItems: 'center',
    backgroundColor: Colors.inputBg, borderRadius: 14,
    borderWidth: 1, borderColor: Colors.accentDark,
    paddingHorizontal: 16,
  },
  atSymbol: { fontSize: 20, color: Colors.accent, fontWeight: '700' },
  usernameInput: {
    flex: 1, paddingVertical: 14, marginLeft: 4,
    color: Colors.textPrimary, fontSize: 16,
    fontFamily: 'Inter_400Regular',
  },

  privacyCard: {
    flexDirection: 'row-reverse', alignItems: 'flex-start', gap: 10,
    backgroundColor: Colors.accentGlow, borderRadius: 14, padding: 14,
    borderWidth: 1, borderColor: Colors.accentDark + '40',
  },
  privacyText: {
    flex: 1, color: Colors.accentLight, fontSize: 12,
    fontFamily: 'Inter_400Regular', textAlign: 'right', lineHeight: 18,
  },

  backBtn: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 6,
    alignSelf: 'flex-start',
  },
  backText: { color: Colors.textSecondary, fontSize: 14, fontFamily: 'Inter_500Medium' },

  continueBtn: { borderRadius: 16, overflow: 'hidden' },
  continueGradient: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center',
    gap: 8, paddingVertical: 18,
  },
  continueText: {
    fontSize: 17, fontFamily: 'Inter_700Bold', color: '#E6EDF3',
  },
});
