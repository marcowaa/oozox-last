import { reloadAppAsync } from 'expo';
import { Feather } from '@expo/vector-icons';
import React, { useState } from 'react';
import {
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors } from '@/constants/colors';

export type ErrorFallbackProps = {
  error: Error;
  resetError: () => void;
};

export function ErrorFallback({ error, resetError }: ErrorFallbackProps) {
  const insets = useSafeAreaInsets();
  const [isModalVisible, setIsModalVisible] = useState(false);

  const handleRestart = async () => {
    try {
      await reloadAppAsync();
    } catch {
      resetError();
    }
  };

  const formatErrorDetails = (): string => {
    let details = `Error: ${error.message}\n\n`;
    if (error.stack) details += `Stack Trace:\n${error.stack}`;
    return details;
  };

  const monoFont = Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' });

  return (
    <View style={[styles.container, { backgroundColor: Colors.background }]}>
      {__DEV__ ? (
        <Pressable
          onPress={() => setIsModalVisible(true)}
          style={({ pressed }) => [
            styles.topButton,
            { top: insets.top + 16, backgroundColor: Colors.surface, opacity: pressed ? 0.8 : 1 },
          ]}
        >
          <Feather name="alert-circle" size={20} color={Colors.textPrimary} />
        </Pressable>
      ) : null}

      <View style={styles.content}>
        <Text style={[styles.title, { color: Colors.textPrimary }]}>حدث خطأ</Text>
        <Text style={[styles.message, { color: Colors.textSecondary }]}>
          يرجى إعادة تشغيل التطبيق للمتابعة.
        </Text>
        <Pressable
          onPress={handleRestart}
          style={({ pressed }) => [
            styles.button,
            { backgroundColor: Colors.accent, opacity: pressed ? 0.9 : 1 },
          ]}
        >
          <Text style={[styles.buttonText, { color: '#E6EDF3' }]}>إعادة المحاولة</Text>
        </Pressable>
      </View>

      {__DEV__ ? (
        <Modal
          visible={isModalVisible}
          animationType="slide"
          transparent
          onRequestClose={() => setIsModalVisible(false)}
        >
          <View style={styles.modalOverlay}>
            <View style={[styles.modalContainer, { backgroundColor: Colors.background }]}>
              <View style={[styles.modalHeader, { borderBottomColor: Colors.border }]}>
                <Text style={[styles.modalTitle, { color: Colors.textPrimary }]}>تفاصيل الخطأ</Text>
                <Pressable
                  onPress={() => setIsModalVisible(false)}
                  style={({ pressed }) => [styles.closeButton, { opacity: pressed ? 0.6 : 1 }]}
                >
                  <Feather name="x" size={24} color={Colors.textPrimary} />
                </Pressable>
              </View>
              <ScrollView
                style={styles.modalScrollView}
                contentContainerStyle={[styles.modalScrollContent, { paddingBottom: insets.bottom + 16 }]}
              >
                <View style={[styles.errorContainer, { backgroundColor: Colors.surface }]}>
                  <Text style={[styles.errorText, { color: Colors.textPrimary, fontFamily: monoFont }]} selectable>
                    {formatErrorDetails()}
                  </Text>
                </View>
              </ScrollView>
            </View>
          </View>
        </Modal>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, width: '100%', height: '100%', justifyContent: 'center', alignItems: 'center', padding: 24 },
  content: { alignItems: 'center', justifyContent: 'center', gap: 16, width: '100%', maxWidth: 600 },
  title: { fontSize: 28, fontWeight: '700', textAlign: 'center', lineHeight: 40 },
  message: { fontSize: 16, textAlign: 'center', lineHeight: 24 },
  topButton: {
    position: 'absolute', right: 16, width: 44, height: 44, borderRadius: 8,
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', zIndex: 10,
  },
  button: { paddingVertical: 16, borderRadius: 12, paddingHorizontal: 24, minWidth: 200 },
  buttonText: { fontWeight: '700', textAlign: 'center', fontSize: 16 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  modalContainer: { width: '100%', height: '90%', borderTopLeftRadius: 16, borderTopRightRadius: 16 },
  modalHeader: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 16, paddingTop: 16, paddingBottom: 12, borderBottomWidth: 1,
  },
  modalTitle: { fontSize: 20, fontWeight: '600' },
  closeButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  modalScrollView: { flex: 1 },
  modalScrollContent: { padding: 16 },
  errorContainer: { width: '100%', borderRadius: 8, overflow: 'hidden', padding: 16 },
  errorText: { fontSize: 12, lineHeight: 18, width: '100%' },
});
