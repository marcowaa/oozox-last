import { MaterialIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../constants/colors';
import { Chat } from '../types';
import { formatShortTime } from '../utils/gold';
import { useApp } from '@/context/AppContext';

interface Props {
  chat: Chat;
  onPress: () => void;
}

function getAvatarColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return Colors.avatarPalette[Math.abs(hash) % Colors.avatarPalette.length];
}

function getInitials(name: string): string {
  const parts = name.trim().split(' ');
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

export function ChatListItem({ chat, onPress }: Props) {
  const { user } = useApp();
  const avatarColor = getAvatarColor(chat.contactName);
  const initials = getInitials(chat.contactName);
  const isUnread = (chat.unreadCount ?? 0) > 0;
  const isSentByMe = chat.lastMessageSenderId === user?.id;

  function renderTicks() {
    if (!isSentByMe) return null;
    const status = chat.lastMessageStatus;
    if (status === 'read') {
      return <MaterialIcons name="done-all" size={14} color={Colors.accent} style={{ marginRight: 2 }} />;
    } else if (status === 'delivered') {
      return <MaterialIcons name="done-all" size={14} color={Colors.textMuted} style={{ marginRight: 2 }} />;
    }
    return <MaterialIcons name="done" size={14} color={Colors.textMuted} style={{ marginRight: 2 }} />;
  }

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.container, pressed && styles.pressed]}
    >
      {/* Avatar */}
      <View style={[styles.avatar, { backgroundColor: avatarColor }]}>
        <Text style={styles.initials}>{initials}</Text>
        {chat.contactVerified && (
          <View style={styles.verifiedDot}>
            <MaterialIcons name="verified" size={12} color={Colors.accent} />
          </View>
        )}
      </View>

      {/* Content */}
      <View style={styles.content}>
        {/* Top row: name + time */}
        <View style={styles.topRow}>
          <Text style={[styles.time, isUnread && styles.timeUnread]}>
            {chat.lastMessageTime ? formatShortTime(chat.lastMessageTime) : ''}
          </Text>
          <Text style={styles.name} numberOfLines={1}>{chat.contactName}</Text>
        </View>

        {/* Bottom row: preview + badge */}
        <View style={styles.bottomRow}>
          <View style={styles.badgeArea}>
            {isUnread ? (
              <View style={styles.unreadBadge}>
                <Text style={styles.unreadText}>{chat.unreadCount}</Text>
              </View>
            ) : null}
          </View>
          <View style={styles.previewRow}>
            {renderTicks()}
            <Text style={[styles.preview, isUnread && styles.previewUnread]} numberOfLines={1}>
              {chat.lastMessage || ''}
            </Text>
          </View>
        </View>
      </View>

      {/* Divider */}
      <View style={styles.divider} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: Colors.surface,
    minHeight: 72,
  },
  pressed: {
    backgroundColor: Colors.surfaceAlt,
  },
  avatar: {
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 12,
  },
  initials: {
    color: '#E6EDF3',
    fontSize: 18,
    fontWeight: '700',
  },
  verifiedDot: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
  },
  topRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
    color: Colors.textPrimary,
    flex: 1,
    textAlign: 'right',
  },
  time: {
    fontSize: 12,
    color: Colors.textMuted,
    marginLeft: 0,
  },
  timeUnread: {
    color: Colors.accent,
    fontWeight: '600',
  },
  bottomRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  previewRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    flex: 1,
  },
  preview: {
    fontSize: 14,
    color: Colors.textSecondary,
    flex: 1,
    textAlign: 'right',
  },
  previewUnread: {
    color: Colors.textPrimary,
    fontWeight: '500',
  },
  badgeArea: {
    marginLeft: 6,
    minWidth: 20,
    alignItems: 'flex-end',
  },
  unreadBadge: {
    backgroundColor: Colors.accent,
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 5,
  },
  unreadText: {
    color: '#E6EDF3',
    fontSize: 12,
    fontWeight: '700',
  },
  divider: {
    position: 'absolute',
    bottom: 0,
    left: 16,
    right: 80,
    height: StyleSheet.hairlineWidth,
    backgroundColor: Colors.divider,
  },
});
