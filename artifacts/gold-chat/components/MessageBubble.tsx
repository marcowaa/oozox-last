import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { Alert, Pressable, StyleSheet, Text, View, Platform } from 'react-native';
import { Colors } from '../constants/colors';
import { Message } from '../types';
import { formatShortTime } from '../utils/gold';

interface Props {
  message: Message;
  isSent: boolean;
}

function TickIcon({ status }: { status?: string }) {
  if (status === 'read') {
    return <MaterialIcons name="done-all" size={13} color="rgba(11,15,20,0.9)" />;
  } else if (status === 'delivered') {
    return <MaterialIcons name="done-all" size={13} color="rgba(11,15,20,0.6)" />;
  }
  return <MaterialIcons name="done" size={13} color="rgba(11,15,20,0.6)" />;
}

async function copyToClipboard(text: string) {
  if (Platform.OS === 'web') {
    try { await navigator.clipboard.writeText(text); } catch {}
    return;
  }
  try {
    const Clipboard = await import('expo-clipboard');
    await Clipboard.setStringAsync(text);
  } catch {}
}

export function MessageBubble({ message, isSent }: Props) {
  const router = useRouter();

  if (message.type === 'system') {
    return (
      <View style={styles.systemRow}>
        <View style={styles.systemChip}>
          <Text style={styles.systemText}>{message.content}</Text>
        </View>
      </View>
    );
  }

  if (message.type === 'day-separator') {
    return (
      <View style={styles.separatorRow}>
        <View style={styles.separatorChip}>
          <Text style={styles.separatorText}>{message.content}</Text>
        </View>
      </View>
    );
  }

  const handleLongPress = () => {
    Alert.alert('رسالة', undefined, [
      { text: 'نسخ', onPress: () => copyToClipboard(message.content || '') },
      { text: 'إلغاء', style: 'cancel' },
    ]);
  };

  if (message.type === 'transaction' && message.transactionId) {
    return (
      <View style={[styles.row, isSent ? styles.rowSent : styles.rowReceived]}>
        <Pressable
          onPress={() => router.push(`/transaction/${message.transactionId}`)}
          style={[
            styles.transactionCard,
            isSent ? styles.transactionSent : styles.transactionReceived,
          ]}
        >
          <View style={styles.transactionHeader}>
            <MaterialIcons name="receipt-long" size={16} color={isSent ? '#0B0F14' : Colors.accent} />
            <Text style={[styles.transactionLabel, isSent && styles.transactionLabelSent]}>
              {isSent ? 'توثيق بيع' : 'توثيق شراء'}
            </Text>
          </View>
          <Text style={[styles.transactionHint, isSent && styles.transactionHintSent]}>
            اضغط للعرض التفاصيل
          </Text>
          <View style={styles.metaRow}>
            {isSent && <TickIcon status={message.status} />}
            <Text style={[styles.timeText, isSent ? styles.timeTextSent : styles.timeTextReceived]}>
              {formatShortTime(message.timestamp)}
            </Text>
          </View>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={[styles.row, isSent ? styles.rowSent : styles.rowReceived]}>
      <Pressable
        onLongPress={handleLongPress}
        style={[styles.bubble, isSent ? styles.bubbleSent : styles.bubbleReceived]}
      >
        <Text style={[styles.text, isSent ? styles.textSent : styles.textReceived]}>
          {message.content}
        </Text>
        <View style={styles.metaRow}>
          {isSent && <TickIcon status={message.status} />}
          <Text style={[styles.timeText, isSent ? styles.timeTextSent : styles.timeTextReceived]}>
            {formatShortTime(message.timestamp)}
          </Text>
        </View>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { marginVertical: 2, paddingHorizontal: 12 },
  rowSent: { alignItems: 'flex-end' },
  rowReceived: { alignItems: 'flex-start' },
  bubble: {
    maxWidth: '80%', borderRadius: 18,
    paddingHorizontal: 14, paddingVertical: 8, paddingBottom: 6,
  },
  bubbleSent: { backgroundColor: Colors.sentBubble, borderBottomRightRadius: 4 },
  bubbleReceived: {
    backgroundColor: Colors.receivedBubble, borderBottomLeftRadius: 4,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06, shadowRadius: 2, elevation: 1,
  },
  text: { fontSize: 15, lineHeight: 22, textAlign: 'right' },
  textSent: { color: Colors.sentBubbleText },
  textReceived: { color: Colors.receivedBubbleText },
  metaRow: {
    flexDirection: 'row', alignItems: 'center',
    justifyContent: 'flex-end', gap: 3, marginTop: 2,
  },
  timeText: { fontSize: 11 },
  timeTextSent: { color: 'rgba(11,15,20,0.75)' },
  timeTextReceived: { color: Colors.textMuted },
  separatorRow: { alignItems: 'center', marginVertical: 10 },
  separatorChip: {
    backgroundColor: Colors.systemMessage, borderRadius: 12,
    paddingHorizontal: 14, paddingVertical: 4,
  },
  separatorText: { fontSize: 12, color: Colors.textSecondary },
  systemRow: { alignItems: 'center', marginVertical: 6 },
  systemChip: { paddingHorizontal: 12 },
  systemText: { fontSize: 12, color: Colors.textMuted, textAlign: 'center' },
  transactionCard: { maxWidth: '80%', borderRadius: 16, padding: 14 },
  transactionSent: { backgroundColor: Colors.sentBubble, borderBottomRightRadius: 4 },
  transactionReceived: {
    backgroundColor: Colors.receivedBubble, borderBottomLeftRadius: 4,
    borderWidth: 1, borderColor: Colors.border,
  },
  transactionHeader: {
    flexDirection: 'row-reverse', alignItems: 'center',
    gap: 6, marginBottom: 4,
  },
  transactionLabel: { fontSize: 14, fontWeight: '700', color: Colors.textSecondary, textAlign: 'right' },
  transactionLabelSent: { color: '#E6EDF3' },
  transactionHint: { fontSize: 12, color: Colors.textMuted, textAlign: 'right', marginBottom: 6 },
  transactionHintSent: { color: 'rgba(11,15,20,0.7)' },
});
