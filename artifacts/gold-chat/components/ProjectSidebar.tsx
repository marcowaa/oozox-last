import React, { useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  Animated,
  Dimensions,
  ScrollView,
  Platform,
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors } from '../../constants/colors';

const { width } = Dimensions.get('window');
const SIDEBAR_WIDTH = Math.min(width * 0.82, 320);

export interface ProjectSidebarItem {
  id: string;
  title: string;
  subtitle?: string;
  icon: keyof typeof MaterialIcons.glyphMap;
  badge?: string | number;
  type?: 'card_popup' | 'full_screen' | 'action';
  onPress: () => void;
}

export interface ProjectSidebarSection {
  title: string;
  items: ProjectSidebarItem[];
}

interface ProjectSidebarProps {
  visible: boolean;
  onClose: () => void;
  projectName?: string;
  projectType?: string;
  sections: ProjectSidebarSection[];
  quickActions?: {
    id: string;
    label: string;
    icon: keyof typeof MaterialIcons.glyphMap;
    onPress: () => void;
  }[];
}

export default function ProjectSidebar({
  visible,
  onClose,
  projectName = 'إدارة القسم',
  projectType,
  sections,
  quickActions,
}: ProjectSidebarProps) {
  const insets = useSafeAreaInsets();
  const slideAnim = useRef(new Animated.Value(-SIDEBAR_WIDTH)).current;
  const overlayAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (visible) {
      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 260,
          useNativeDriver: true,
        }),
        Animated.timing(overlayAnim, {
          toValue: 1,
          duration: 260,
          useNativeDriver: true,
        }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: -SIDEBAR_WIDTH,
          duration: 220,
          useNativeDriver: true,
        }),
        Animated.timing(overlayAnim, {
          toValue: 0,
          duration: 220,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [visible, slideAnim, overlayAnim]);

  if (!visible && (slideAnim as any)._value === -SIDEBAR_WIDTH) {
    return null;
  }

  return (
    <View style={StyleSheet.absoluteFillObject} pointerEvents={visible ? 'auto' : 'none'}>
      {/* Backdrop overlay */}
      <Animated.View
        style={[
          styles.backdrop,
          {
            opacity: overlayAnim,
          },
        ]}
      >
        <Pressable style={StyleSheet.absoluteFill} onPress={onClose} />
      </Animated.View>

      {/* Drawer Panel */}
      <Animated.View
        style={[
          styles.drawer,
          {
            width: SIDEBAR_WIDTH,
            paddingTop: Math.max(insets.top, 16),
            paddingBottom: Math.max(insets.bottom, 16),
            transform: [{ translateX: slideAnim }],
          },
        ]}
      >
        {/* Drawer Header with Brand Gold & Charcoal Palette */}
        <View style={styles.header}>
          <Pressable
            style={styles.closeBtn}
            onPress={onClose}
            hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
          >
            <MaterialIcons name="close" size={22} color={Colors.textSecondary} />
          </Pressable>

          <View style={styles.headerTitleWrap}>
            <View style={styles.brandBadge}>
              <MaterialIcons name="business-center" size={16} color={Colors.accent} />
              <Text style={styles.brandBadgeText}>قائمة القسم المخصصة</Text>
            </View>
            <Text style={styles.headerTitle} numberOfLines={1}>
              {projectName}
            </Text>
            {projectType ? (
              <Text style={styles.headerSubtitle}>{projectType}</Text>
            ) : null}
          </View>
        </View>

        {/* Quick Top Actions Bar if provided */}
        {quickActions && quickActions.length > 0 && (
          <View style={styles.quickActionsRow}>
            {quickActions.map(action => (
              <Pressable
                key={action.id}
                style={styles.quickActionBtn}
                onPress={() => {
                  onClose();
                  action.onPress();
                }}
              >
                <MaterialIcons name={action.icon} size={18} color={Colors.accent} />
                <Text style={styles.quickActionLabel}>{action.label}</Text>
              </Pressable>
            ))}
          </View>
        )}

        {/* Scrollable sections list */}
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.scrollContent}
        >
          {sections.map((section, sIdx) => (
            <View key={sIdx} style={styles.sectionBlock}>
              <View style={styles.sectionHeaderRow}>
                <Text style={styles.sectionTitle}>{section.title}</Text>
                <View style={styles.sectionDivider} />
              </View>

              <View style={styles.itemsCard}>
                {section.items.map((item, iIdx) => {
                  const isLast = iIdx === section.items.length - 1;
                  return (
                    <Pressable
                      key={item.id}
                      style={({ pressed }) => [
                        styles.itemRow,
                        pressed && styles.itemRowPressed,
                        !isLast && styles.itemBorderBottom,
                      ]}
                      onPress={() => {
                        onClose();
                        item.onPress();
                      }}
                    >
                      {/* Left icon & badge */}
                      <View style={styles.itemLeftCol}>
                        <MaterialIcons
                          name="chevron-left"
                          size={18}
                          color={Colors.textMuted}
                        />
                        {item.type && (
                          <View
                            style={[
                              styles.typeBadge,
                              item.type === 'card_popup'
                                ? styles.typeBadgePopup
                                : item.type === 'full_screen'
                                ? styles.typeBadgeFull
                                : styles.typeBadgeAction,
                            ]}
                          >
                            <Text
                              style={[
                                styles.typeBadgeText,
                                item.type === 'card_popup'
                                  ? styles.typeBadgeTextPopup
                                  : item.type === 'full_screen'
                                  ? styles.typeBadgeTextFull
                                  : styles.typeBadgeTextAction,
                              ]}
                            >
                              {item.type === 'card_popup'
                                ? 'نافذة سريعة'
                                : item.type === 'full_screen'
                                ? 'شاشة كاملة'
                                : 'إجراء'}
                            </Text>
                          </View>
                        )}
                        {item.badge !== undefined && (
                          <View style={styles.countBadge}>
                            <Text style={styles.countBadgeText}>{item.badge}</Text>
                          </View>
                        )}
                      </View>

                      {/* Right info (Arabic RTL layout) */}
                      <View style={styles.itemRightCol}>
                        <View style={styles.itemTextWrap}>
                          <Text style={styles.itemTitle}>{item.title}</Text>
                          {item.subtitle ? (
                            <Text style={styles.itemSubtitle}>{item.subtitle}</Text>
                          ) : null}
                        </View>
                        <View style={styles.itemIconWrap}>
                          <MaterialIcons
                            name={item.icon}
                            size={20}
                            color={Colors.accent}
                          />
                        </View>
                      </View>
                    </Pressable>
                  );
                })}
              </View>
            </View>
          ))}

          {/* Clean branding footer */}
          <View style={styles.drawerFooter}>
            <View style={styles.footerGlow} />
            <Text style={styles.footerBrand}>🪙 تطبيق ذهبي — مساحة مريحة للمشاريع</Text>
            <Text style={styles.footerNote}>
              جميع الإعدادات والمميزات مجمعة هنا لتوفير شاشة رئيسية واسعة
            </Text>
          </View>
        </ScrollView>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(5, 7, 10, 0.65)',
  },
  drawer: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    right: 0, // RTL Drawer opens from the right
    backgroundColor: Colors.surface,
    borderLeftWidth: 1,
    borderLeftColor: Colors.border,
    shadowColor: '#000',
    shadowOffset: { width: -4, height: 0 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 16,
  },
  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  closeBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.surfaceAlt,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  headerTitleWrap: {
    flex: 1,
    alignItems: 'flex-end',
    marginRight: 10,
  },
  brandBadge: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 4,
    backgroundColor: Colors.accentGlow,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
    marginBottom: 4,
  },
  brandBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.accent,
  },
  headerTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  headerSubtitle: {
    fontSize: 11,
    color: Colors.textMuted,
    marginTop: 2,
  },
  quickActionsRow: {
    flexDirection: 'row-reverse',
    gap: 8,
    paddingHorizontal: 14,
    paddingVertical: 10,
    backgroundColor: Colors.background,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  quickActionBtn: {
    flex: 1,
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: Colors.surfaceAlt,
    paddingVertical: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  quickActionLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  scrollContent: {
    paddingHorizontal: 14,
    paddingTop: 14,
    paddingBottom: 24,
  },
  sectionBlock: {
    marginBottom: 16,
  },
  sectionHeaderRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
    paddingHorizontal: 4,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.accent,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  sectionDivider: {
    flex: 1,
    height: 1,
    backgroundColor: Colors.border,
  },
  itemsCard: {
    backgroundColor: Colors.surfaceAlt,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: 'hidden',
  },
  itemRow: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    paddingHorizontal: 12,
  },
  itemRowPressed: {
    backgroundColor: Colors.surfaceHover,
  },
  itemBorderBottom: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  itemRightCol: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  itemIconWrap: {
    width: 34,
    height: 34,
    borderRadius: 10,
    backgroundColor: Colors.accentGlow,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(212, 175, 55, 0.25)',
  },
  itemTextWrap: {
    flex: 1,
    alignItems: 'flex-end',
  },
  itemTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  itemSubtitle: {
    fontSize: 10,
    color: Colors.textMuted,
    marginTop: 2,
  },
  itemLeftCol: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  typeBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 5,
  },
  typeBadgePopup: {
    backgroundColor: 'rgba(14, 165, 233, 0.15)',
  },
  typeBadgeFull: {
    backgroundColor: 'rgba(168, 85, 247, 0.15)',
  },
  typeBadgeAction: {
    backgroundColor: 'rgba(16, 185, 129, 0.15)',
  },
  typeBadgeText: {
    fontSize: 9,
    fontWeight: '700',
  },
  typeBadgeTextPopup: {
    color: Colors.infoText,
  },
  typeBadgeTextFull: {
    color: '#C084FC',
  },
  typeBadgeTextAction: {
    color: Colors.successText,
  },
  countBadge: {
    backgroundColor: Colors.surface,
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  countBadgeText: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.textSecondary,
  },
  drawerFooter: {
    marginTop: 10,
    padding: 14,
    borderRadius: 12,
    backgroundColor: Colors.background,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: 'center',
  },
  footerGlow: {
    width: 32,
    height: 3,
    borderRadius: 2,
    backgroundColor: Colors.accent,
    marginBottom: 8,
  },
  footerBrand: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.accent,
    textAlign: 'center',
  },
  footerNote: {
    fontSize: 10,
    color: Colors.textMuted,
    textAlign: 'center',
    marginTop: 4,
    lineHeight: 14,
  },
});
