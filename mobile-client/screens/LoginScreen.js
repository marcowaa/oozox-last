import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen({ navigation }) {
  const { login, isLoading, authError } = useAuth();
  const [emailOrPhone, setEmailOrPhone] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = async () => {
    if (!emailOrPhone.trim() || !password.trim()) {
      Alert.alert('تنبيه', 'يرجى إدخال اسم المستخدم/البريد الإلكتروني وكلمة المرور');
      return;
    }
    const res = await login(emailOrPhone.trim(), password);
    if (!res.success) {
      Alert.alert('خطأ', res.message || 'فشل تسجيل الدخول');
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.container}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.brandBox}>
          <Text style={styles.logoText}>🪙 OOZOX</Text>
          <Text style={styles.subtitle}>بوابة المعاملات والفواتير والذهب الذكية</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>تسجيل الدخول</Text>

          {authError && (
            <View style={styles.errorBanner}>
              <Text style={styles.errorText}>{authError}</Text>
            </View>
          )}

          <Text style={styles.label}>البريد الإلكتروني أو الهاتف</Text>
          <TextInput
            style={styles.input}
            placeholder="example@mail.com / 01000000000"
            placeholderTextColor="#8B949E"
            value={emailOrPhone}
            onChangeText={setEmailOrPhone}
            autoCapitalize="none"
            keyboardType="email-address"
          />

          <Text style={styles.label}>كلمة المرور</Text>
          <TextInput
            style={styles.input}
            placeholder="••••••••"
            placeholderTextColor="#8B949E"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
          />

          <TouchableOpacity
            style={[styles.button, isLoading && { opacity: 0.7 }]}
            onPress={handleLogin}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#000" />
            ) : (
              <Text style={styles.buttonText}>دخول إلى حسابي</Text>
            )}
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0D1117' },
  content: {
    padding: 20,
    justifyContent: 'center',
    flexGrow: 1,
  },
  brandBox: {
    alignItems: 'center',
    marginBottom: 32,
  },
  logoText: {
    fontSize: 32,
    fontWeight: '800',
    color: '#D4AF37',
    letterSpacing: 1,
  },
  subtitle: {
    fontSize: 13,
    color: '#8B949E',
    marginTop: 6,
    textAlign: 'center',
  },
  card: {
    backgroundColor: '#161B22',
    borderRadius: 16,
    padding: 24,
    borderWidth: 1,
    borderColor: '#30363D',
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#F0F6FC',
    textAlign: 'right',
    marginBottom: 20,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#8B949E',
    textAlign: 'right',
    marginBottom: 6,
  },
  input: {
    backgroundColor: '#0D1117',
    color: '#F0F6FC',
    borderRadius: 8,
    padding: 12,
    borderWidth: 1,
    borderColor: '#30363D',
    textAlign: 'right',
    marginBottom: 16,
    fontSize: 15,
  },
  button: {
    backgroundColor: '#D4AF37',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonText: {
    color: '#0D1117',
    fontSize: 16,
    fontWeight: '700',
  },
  errorBanner: {
    backgroundColor: '#3E1F1F',
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#DA3633',
    marginBottom: 16,
  },
  errorText: {
    color: '#FF7B72',
    fontSize: 13,
    textAlign: 'right',
  },
});
