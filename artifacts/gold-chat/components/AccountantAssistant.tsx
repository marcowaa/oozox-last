import { MaterialIcons } from '@expo/vector-icons';
import { useState } from 'react';
import {
  FlatList,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Colors } from '../constants/colors';
import { ChatAccount } from '../types';
import {
  askAccountant, generateChatReport, generateSmartAlerts,
  calculateHealthScore,
} from '../utils/accountant';
import { formatCurrency } from '../utils/gold';

interface Props {
  visible: boolean;
  accounts: ChatAccount[];
  chatName: string;
  onClose: () => void;
}

const QUICK_QUESTIONS = [
  'تنبيهات',
  'صحة الحساب',
  'كم المتبقي؟',
  'ما هي الحسابات؟',
  'ملخص',
  'أكبر دين',
];

interface ChatMsg {
  id: string;
  text: string;
  isUser: boolean;
}

export function AccountantAssistant({ visible, accounts, chatName, onClose }: Props) {
  const [messages, setMessages] = useState<ChatMsg[]>([]);
  const [input, setInput] = useState('');

  // Auto-generate welcome with alerts
  const report = accounts.length > 0 ? generateChatReport('', chatName, accounts) : null;
  const alerts = accounts.length > 0 ? generateSmartAlerts(accounts, chatName) : [];
  const analysis = accounts.length > 0 ? calculateHealthScore(accounts, chatName) : null;

  // Initialize welcome message
  useState(() => {
    let welcomeText = `🤖 أهلاً! أنا محاسبك الذكي في محادثة "${chatName}".\n\n`;
    if (analysis) {
      const scoreEmoji = analysis.healthScore >= 80 ? '🟢' : analysis.healthScore >= 50 ? '🟡' : '🔴';
      welcomeText += `${scoreEmoji} صحة الحسابات: ${analysis.healthScore}/100\n`;
      welcomeText += `💰 المتبقي: ${formatCurrency(report?.grandTotalRemaining ?? 0)}\n`;
      welcomeText += `📊 المعاملات: ${report?.totalTransactions ?? 0}\n`;
      if (alerts.length > 0) {
        welcomeText += `🔔 تنبيهات: ${alerts.length}\n\n`;
        welcomeText += `اكتب "تنبيهات" لعرضها أو اسألني أي شيء.`;
      } else {
        welcomeText += `\nاسألني عن حساباتك!`;
      }
    } else {
      welcomeText += `لا توجد حسابات بعد. اكتب #هاشتاج في المحادثة لإنشاء حساب.`;
    }
    setMessages([{ id: 'welcome', text: welcomeText, isUser: false }]);
  });

  function handleAsk(question: string) {
    if (!question.trim()) return;
    const userMsg: ChatMsg = { id: Date.now().toString(), text: question, isUser: true };
    const answer = askAccountant(question, accounts, chatName);
    const botMsg: ChatMsg = { id: (Date.now() + 1).toString(), text: answer, isUser: false };
    setMessages(prev => [...prev, userMsg, botMsg]);
    setInput('');
  }

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable onPress={onClose} style={styles.closeBtn}>
            <MaterialIcons name="close" size={22} color={Colors.textPrimary} />
          </Pressable>
          <View style={styles.headerInfo}>
            <View style={styles.headerIcon}>
              <MaterialIcons name="psychology" size={20} color={Colors.accent} />
            </View>
            <View>
              <Text style={styles.title}>المحاسب الذكي</Text>
              <Text style={styles.subtitle}>{chatName}</Text>
            </View>
          </View>
          <View style={{ width: 38 }} />
        </View>

        {/* Smart stats bar */}
        {report && (
          <View style={styles.statsBar}>
            <View style={styles.statItem}>
              <Text style={[styles.statValue, { color: report.grandTotalRemaining > 0 ? Colors.error : Colors.profit }]}>
                {formatCurrency(report.grandTotalRemaining)}
              </Text>
              <Text style={styles.statLabel}>المتبقي</Text>
            </View>
            <View style={styles.statDivider} />
            <View style={styles.statItem}>
              <Text style={styles.statValue}>{report.totalTransactions}</Text>
              <Text style={styles.statLabel}>معاملة</Text>
            </View>
            <View style={styles.statDivider} />
            <View style={styles.statItem}>
              <View style={[
                styles.healthBadge,
                {
                  backgroundColor: analysis && analysis.healthScore >= 80 ? Colors.successLight :
                                   analysis && analysis.healthScore >= 50 ? Colors.warningLight :
                                   Colors.errorLight,
                },
              ]}>
                <Text style={[
                  styles.healthText,
                  {
                    color: analysis && analysis.healthScore >= 80 ? Colors.success :
                           analysis && analysis.healthScore >= 50 ? Colors.warning :
                           Colors.error,
                  },
                ]}>
                  {analysis?.healthScore ?? 0}
                </Text>
              </View>
              <Text style={styles.statLabel}>الصحة</Text>
            </View>
            {alerts.length > 0 && (
              <>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                  <View style={[styles.alertBadge, { backgroundColor: alerts.some(a => a.type === 'critical' || a.type === 'error') ? Colors.errorLight : Colors.warningLight }]}>
                    <Text style={[styles.alertText, { color: alerts.some(a => a.type === 'critical' || a.type === 'error') ? Colors.error : Colors.warning }]}>
                      {alerts.length}
                    </Text>
                  </View>
                  <Text style={styles.statLabel}>تنبيهات</Text>
                </View>
              </>
            )}
          </View>
        )}

        {/* Chat messages */}
        <FlatList
          data={messages}
          keyExtractor={m => m.id}
          renderItem={({ item }) => (
            <View style={[styles.msgRow, item.isUser ? styles.msgRowUser : styles.msgRowBot]}>
              <View style={[styles.msgBubble, item.isUser ? styles.msgUser : styles.msgBot]}>
                {!item.isUser && <Text style={styles.botIcon}>🤖</Text>}
                <Text style={[styles.msgText, item.isUser ? styles.msgTextUser : styles.msgTextBot]}>
                  {item.text}
                </Text>
              </View>
            </View>
          )}
          contentContainerStyle={{ padding: 12, gap: 8 }}
          showsVerticalScrollIndicator={false}
        />

        {/* Quick questions */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.quickRow} style={styles.quickScroll}>
          {QUICK_QUESTIONS.map(q => (
            <Pressable key={q} style={styles.quickBtn} onPress={() => handleAsk(q)}>
              <Text style={styles.quickBtnText}>{q}</Text>
            </Pressable>
          ))}
        </ScrollView>

        {/* Input */}
        <View style={styles.inputBar}>
          <Pressable style={styles.sendBtn} onPress={() => handleAsk(input)}>
            <MaterialIcons name="send" size={18} color="#0B0F14" />
          </Pressable>
          <TextInput
            style={styles.input}
            placeholder="اسأل محاسبك... أو اكتب رقماً للتحقق"
            placeholderTextColor={Colors.placeholder}
            value={input}
            onChangeText={setInput}
            textAlign="right"
            onSubmitEditing={() => handleAsk(input)}
          />
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bg },

  header: {
    flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'space-between',
    backgroundColor: Colors.primary, paddingHorizontal: 16, paddingVertical: 14,
  },
  closeBtn: {
    width: 38, height: 38, borderRadius: 19,
    backgroundColor: Colors.surfaceAlt, alignItems: 'center', justifyContent: 'center',
  },
  headerInfo: { flexDirection: 'row-reverse', alignItems: 'center', gap: 10 },
  headerIcon: {
    width: 40, height: 40, borderRadius: 12,
    backgroundColor: Colors.accentGlow, alignItems: 'center', justifyContent: 'center',
    borderWidth: 1, borderColor: Colors.accentDark + '40',
  },
  title: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  subtitle: { fontSize: 12, color: Colors.textMuted, marginTop: 1 },

  statsBar: {
    flexDirection: 'row-reverse', backgroundColor: Colors.surface,
    paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: Colors.border,
  },
  statItem: { flex: 1, alignItems: 'center' },
  statValue: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  statLabel: { fontSize: 10, color: Colors.textMuted, marginTop: 2 },
  statDivider: { width: 1, backgroundColor: Colors.border },
  healthBadge: {
    minWidth: 32, borderRadius: 10, paddingHorizontal: 8, paddingVertical: 2,
    alignItems: 'center',
  },
  healthText: { fontSize: 14, fontWeight: '700' },
  alertBadge: {
    minWidth: 32, borderRadius: 10, paddingHorizontal: 8, paddingVertical: 2,
    alignItems: 'center',
  },
  alertText: { fontSize: 14, fontWeight: '700' },

  msgRow: { flexDirection: 'row', paddingHorizontal: 4 },
  msgRowUser: { justifyContent: 'flex-end' },
  msgRowBot: { justifyContent: 'flex-start' },
  msgBubble: { maxWidth: '85%', borderRadius: 16, padding: 12 },
  msgUser: { backgroundColor: Colors.accent },
  msgBot: { backgroundColor: Colors.surface, borderWidth: 1, borderColor: Colors.border },
  botIcon: { fontSize: 16, marginBottom: 4 },
  msgText: { fontSize: 14, lineHeight: 20, textAlign: 'right' },
  msgTextUser: { color: '#0B0F14' },
  msgTextBot: { color: Colors.textPrimary },

  quickScroll: { maxHeight: 44, backgroundColor: Colors.surface, borderTopWidth: 1, borderTopColor: Colors.border },
  quickRow: { flexDirection: 'row-reverse', paddingHorizontal: 8, paddingVertical: 6, gap: 6 },
  quickBtn: {
    backgroundColor: Colors.accentGlow, borderRadius: 16,
    paddingHorizontal: 12, paddingVertical: 6,
    borderWidth: 1, borderColor: Colors.accentDark + '30',
  },
  quickBtnText: { fontSize: 12, color: Colors.accentLight, fontWeight: '600' },

  inputBar: {
    flexDirection: 'row-reverse', alignItems: 'center',
    backgroundColor: Colors.surface, paddingHorizontal: 10, paddingVertical: 8, gap: 8,
    borderTopWidth: 1, borderTopColor: Colors.border,
  },
  sendBtn: {
    width: 38, height: 38, borderRadius: 12,
    backgroundColor: Colors.accent, alignItems: 'center', justifyContent: 'center',
  },
  input: {
    flex: 1, backgroundColor: Colors.inputBg, borderRadius: 14,
    paddingHorizontal: 14, paddingVertical: 10,
    fontSize: 14, color: Colors.textPrimary,
    borderWidth: 1, borderColor: Colors.inputBorder,
  },
});
