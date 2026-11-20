import { MaterialIcons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Image,
  Modal,
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
import { calculateGoldPrice, formatCurrency } from '../utils/gold';

interface Props {
  visible: boolean;
  chatId: string;
  onClose: () => void;
}

const GOLD_TYPES = ['مصاغ', 'سبائك', 'عملات'];
const KARATS = ['18K', '21K', '24K'];
const KARAT_KEYS: Record<string, string> = { '18K': 'k18', '21K': 'k21', '24K': 'k24' };
const QUICK_WEIGHTS = ['1', '2', '5', '10', '20', '50'];

export function GoldCalculatorSheet({ visible, chatId, onClose }: Props) {
  const insets = useSafeAreaInsets();
  const { user, goldPrices, addTransaction, sendMessage, chats } = useApp();
  const isTrader = user?.type === 'gold_trader';

  const [goldType, setGoldType] = useState('مصاغ');
  const [karat, setKarat] = useState('21K');
  const [weight, setWeight] = useState('');
  const [mfgCost, setMfgCost] = useState('');
  const [traderCost, setTraderCost] = useState('');
  const [photoUri, setPhotoUri] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const chat = chats.find(c => c.id === chatId);

  useEffect(() => {
    if (visible) {
      setGoldType('مصاغ');
      setKarat('21K');
      setWeight('');
      setMfgCost('');
      setTraderCost('');
      setPhotoUri(null);
    }
  }, [visible]);

  const karatKey = KARAT_KEYS[karat] ?? 'k21';
  const pricePerGram = goldPrices[karatKey] ?? 0;
  const weightNum = parseFloat(weight) || 0;
  const mfgNum = parseFloat(mfgCost) || 0;
  const traderCostNum = parseFloat(traderCost) || 0;

  const goldValue = pricePerGram * weightNum;
  const totalPrice = goldValue + mfgNum;
  const profit = isTrader && traderCostNum > 0
    ? totalPrice - (traderCostNum * weightNum)
    : null;

  async function handleCamera() {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('الإذن مطلوب', 'يرجى السماح للتطبيق بالوصول إلى الكاميرا');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
      allowsEditing: false,
    });
    if (!result.canceled && result.assets[0]) {
      setPhotoUri(result.assets[0].uri);
    }
  }

  async function handleDocument() {
    if (!weightNum || weightNum <= 0) {
      Alert.alert('تنبيه', 'يرجى إدخال الوزن أولاً');
      return;
    }
    if (!chatId) {
      Alert.alert('تنبيه', 'لا يمكن التوثيق خارج المحادثة');
      return;
    }

    setLoading(true);
    try {
      const txData = {
        chatId,
        currentUserId: user?.id ?? '',
        currentUserName: user?.name ?? '',
        currentUserType: (user?.type ?? 'businessman') as 'businessman' | 'gold_trader',
        contactId: '',
        contactName: '',
        goldKarat: karat as '18k' | '21k' | '24k',
        goldType,
        weight: weightNum,
        pricePerGram,
        manufacturingCostPerGram: 0,
        totalGoldPrice: goldValue,
        totalManufacturing: mfgNum,
        totalPrice,
        photoUri: photoUri ?? undefined,
        status: 'purchased' as const,
        traderCostPerGram: isTrader && traderCostNum > 0 ? traderCostNum : undefined,
        profit: profit ?? undefined,
        timestamp: Date.now(),
        karat,
        mfgCost: mfgNum,
        createdAt: Date.now(),
      };
      const txResult = await addTransaction(txData);
      if (txResult) {
        const label = isTrader ? 'توثيق بيع' : 'توثيق شراء';
        await sendMessage(chatId, label, 'gold_purchase', undefined, txResult.id);
      }
      onClose();
    } catch (e) {
      Alert.alert('خطأ', 'حدث خطأ أثناء التوثيق');
    } finally {
      setLoading(false);
    }
  }

  const docLabel = isTrader ? 'توثيق البيع' : 'توثيق الشراء';

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={[styles.container, { paddingTop: insets.top || 16 }]}>
        {/* Handle bar */}
        <View style={styles.handle} />

        {/* Header */}
        <View style={styles.header}>
          <Pressable onPress={onClose} style={styles.closeBtn}>
            <MaterialIcons name="close" size={22} color={Colors.textPrimary} />
          </Pressable>
          <Text style={styles.title}>حاسبة الذهب</Text>
          <View style={{ width: 38 }} />
        </View>

        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
          {/* Gold type tabs */}
          <Text style={styles.fieldLabel}>نوع الذهب</Text>
          <View style={styles.typeTabs}>
            {GOLD_TYPES.map(t => (
              <Pressable
                key={t}
                style={[styles.typeTab, goldType === t && styles.typeTabActive]}
                onPress={() => setGoldType(t)}
              >
                <Text style={[styles.typeTabText, goldType === t && styles.typeTabTextActive]}>{t}</Text>
              </Pressable>
            ))}
          </View>

          {/* Karat selection */}
          <Text style={styles.fieldLabel}>العيار</Text>
          <View style={styles.karatRow}>
            {KARATS.map(k => {
              const key = KARAT_KEYS[k];
              const price = goldPrices[key] ?? 0;
              return (
                <Pressable
                  key={k}
                  style={[styles.karatBtn, karat === k && styles.karatBtnActive]}
                  onPress={() => setKarat(k)}
                >
                  <Text style={[styles.karatBtnLabel, karat === k && styles.karatBtnLabelActive]}>{k}</Text>
                  <Text style={[styles.karatBtnPrice, karat === k && styles.karatBtnPriceActive]}>
                    {formatCurrency(price)}
                  </Text>
                </Pressable>
              );
            })}
          </View>

          {/* Weight input + quick weights */}
          <Text style={styles.fieldLabel}>الوزن (غرام)</Text>
          <TextInput
            style={styles.input}
            value={weight}
            onChangeText={setWeight}
            keyboardType="decimal-pad"
            placeholder="0.00"
            placeholderTextColor={Colors.placeholder}
            textAlign="right"
          />
          <View style={styles.quickWeights}>
            {QUICK_WEIGHTS.map(w => (
              <Pressable
                key={w}
                style={[styles.quickWeight, weight === w && styles.quickWeightActive]}
                onPress={() => setWeight(w)}
              >
                <Text style={[styles.quickWeightText, weight === w && styles.quickWeightTextActive]}>
                  {w}g
                </Text>
              </Pressable>
            ))}
          </View>

          {/* Manufacturing cost */}
          <Text style={styles.fieldLabel}>أجرة الصنعة (اختياري)</Text>
          <TextInput
            style={styles.input}
            value={mfgCost}
            onChangeText={setMfgCost}
            keyboardType="decimal-pad"
            placeholder="0.00 ر.س"
            placeholderTextColor={Colors.placeholder}
            textAlign="right"
          />

          {/* Trader private cost */}
          {isTrader && (
            <View style={styles.traderSection}>
              <View style={styles.traderSectionHeader}>
                <MaterialIcons name="lock" size={14} color={Colors.profit} />
                <Text style={styles.traderSectionLabel}>معلومات خاصة (للتاجر فقط)</Text>
              </View>
              <Text style={styles.fieldLabel}>تكلفة الغرام الفعلية</Text>
              <TextInput
                style={[styles.input, styles.traderInput]}
                value={traderCost}
                onChangeText={setTraderCost}
                keyboardType="decimal-pad"
                placeholder="0.00 ر.س"
                placeholderTextColor={Colors.placeholder}
                textAlign="right"
              />
            </View>
          )}

          {/* Photo */}
          <Text style={styles.fieldLabel}>صورة البضاعة</Text>
          {photoUri ? (
            <Pressable onPress={handleCamera} style={styles.photoPreview}>
              <Image source={{ uri: photoUri }} style={styles.photoImage} resizeMode="cover" />
              <View style={styles.retakeOverlay}>
                <MaterialIcons name="camera-alt" size={22} color="#fff" />
                <Text style={styles.retakeText}>إعادة التصوير</Text>
              </View>
            </Pressable>
          ) : (
            <Pressable onPress={handleCamera} style={styles.cameraBtn}>
              <MaterialIcons name="camera-alt" size={32} color={Colors.accent} />
              <Text style={styles.cameraBtnText}>التقط صورة</Text>
              <Text style={styles.cameraBtnSub}>لتوثيق البضاعة على الميزان</Text>
            </Pressable>
          )}

          {/* Live price breakdown */}
          {weightNum > 0 && (
            <View style={styles.breakdown}>
              <Text style={styles.breakdownTitle}>ملخص السعر</Text>
              <BreakdownRow label="سعر الغرام" value={formatCurrency(pricePerGram)} />
              <BreakdownRow label="قيمة المادة" value={formatCurrency(goldValue)} />
              {mfgNum > 0 && <BreakdownRow label="أجرة الصنعة" value={formatCurrency(mfgNum)} />}
              <View style={styles.breakdownTotal}>
                <Text style={styles.breakdownTotalValue}>{formatCurrency(totalPrice)}</Text>
                <Text style={styles.breakdownTotalLabel}>الإجمالي</Text>
              </View>
              {isTrader && profit !== null && (
                <View style={styles.profitRow}>
                  <MaterialIcons name="lock" size={13} color={Colors.profit} />
                  <Text style={styles.profitLabel}>صافي الربح المتوقع:</Text>
                  <Text style={[styles.profitValue, { color: profit >= 0 ? Colors.profit : Colors.loss }]}>
                    {formatCurrency(profit)}
                  </Text>
                </View>
              )}
            </View>
          )}
        </ScrollView>

        {/* Footer */}
        <View style={[styles.footer, { paddingBottom: Math.max(insets.bottom, 16) }]}>
          {chatId ? (
            <Pressable
              style={[styles.docBtn, loading && styles.docBtnDisabled]}
              onPress={handleDocument}
              disabled={loading || !weightNum}
            >
              <MaterialIcons name="receipt-long" size={20} color="#fff" />
              <Text style={styles.docBtnText}>{loading ? 'جارٍ التوثيق...' : docLabel}</Text>
            </Pressable>
          ) : (
            <View style={styles.calcOnlyNote}>
              <MaterialIcons name="info-outline" size={16} color={Colors.textMuted} />
              <Text style={styles.calcOnlyText}>للتوثيق، افتح الحاسبة من داخل محادثة</Text>
            </View>
          )}
        </View>
      </View>
    </Modal>
  );
}

function BreakdownRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={breakdownStyles.row}>
      <Text style={breakdownStyles.value}>{value}</Text>
      <Text style={breakdownStyles.label}>{label}</Text>
    </View>
  );
}

const breakdownStyles = StyleSheet.create({
  row: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  label: { fontSize: 13, color: Colors.textSecondary },
  value: { fontSize: 14, fontWeight: '600', color: Colors.textPrimary },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.surface },
  handle: {
    width: 40, height: 4, borderRadius: 2,
    backgroundColor: Colors.border,
    alignSelf: 'center',
    marginBottom: 8,
  },

  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  title: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },
  closeBtn: {
    width: 38, height: 38, borderRadius: 19,
    backgroundColor: Colors.background,
    alignItems: 'center', justifyContent: 'center',
  },

  scroll: { padding: 16, paddingBottom: 24 },

  fieldLabel: {
    fontSize: 13, fontWeight: '600', color: Colors.textSecondary,
    textAlign: 'right', marginBottom: 8, marginTop: 16,
  },

  typeTabs: { flexDirection: 'row-reverse', gap: 8 },
  typeTab: {
    flex: 1, paddingVertical: 12, borderRadius: 12,
    backgroundColor: Colors.background, alignItems: 'center',
    borderWidth: 1, borderColor: Colors.border,
  },
  typeTabActive: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  typeTabText: { fontSize: 14, color: Colors.textSecondary, fontWeight: '600' },
  typeTabTextActive: { color: Colors.accent },

  karatRow: { flexDirection: 'row-reverse', gap: 10 },
  karatBtn: {
    flex: 1, paddingVertical: 12, borderRadius: 14, alignItems: 'center',
    backgroundColor: Colors.background, borderWidth: 1, borderColor: Colors.border,
  },
  karatBtnActive: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  karatBtnLabel: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  karatBtnLabelActive: { color: Colors.accent },
  karatBtnPrice: { fontSize: 11, color: Colors.textMuted, marginTop: 3 },
  karatBtnPriceActive: { color: 'rgba(201,168,76,0.7)' },

  input: {
    backgroundColor: Colors.inputBg,
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 16,
    color: Colors.inputText,
    borderWidth: 1,
    borderColor: Colors.inputBorder,
    fontWeight: '600',
  },

  quickWeights: { flexDirection: 'row-reverse', gap: 8, marginTop: 10, flexWrap: 'wrap' },
  quickWeight: {
    paddingHorizontal: 14, paddingVertical: 7, borderRadius: 20,
    backgroundColor: Colors.background, borderWidth: 1, borderColor: Colors.border,
  },
  quickWeightActive: { backgroundColor: Colors.accent, borderColor: Colors.accent },
  quickWeightText: { fontSize: 13, color: Colors.textSecondary, fontWeight: '600' },
  quickWeightTextActive: { color: '#E6EDF3' },

  traderSection: {
    backgroundColor: Colors.profitBg,
    borderRadius: 14,
    padding: 14,
    marginTop: 16,
    borderWidth: 1,
    borderColor: Colors.successLight,
  },
  traderSectionHeader: { flexDirection: 'row-reverse', alignItems: 'center', gap: 6, marginBottom: 4 },
  traderSectionLabel: { fontSize: 13, color: Colors.profit, fontWeight: '600' },
  traderInput: { backgroundColor: Colors.surface, marginTop: 4 },

  cameraBtn: {
    alignItems: 'center', justifyContent: 'center',
    borderRadius: 16, borderWidth: 2, borderColor: Colors.border,
    borderStyle: 'dashed', paddingVertical: 32,
    backgroundColor: Colors.background, gap: 8,
  },
  cameraBtnText: { fontSize: 16, fontWeight: '700', color: Colors.accent },
  cameraBtnSub: { fontSize: 12, color: Colors.textMuted },

  photoPreview: { borderRadius: 16, overflow: 'hidden', height: 180 },
  photoImage: { width: '100%', height: '100%' },
  retakeOverlay: {
    position: 'absolute', bottom: 0, left: 0, right: 0,
    flexDirection: 'row-reverse', alignItems: 'center', gap: 8,
    backgroundColor: 'rgba(0,0,0,0.5)', padding: 12,
  },
  retakeText: { color: '#0B0F14', fontSize: 14, fontWeight: '600' },

  breakdown: {
    backgroundColor: Colors.background,
    borderRadius: 16, padding: 16, marginTop: 16,
    borderWidth: 1, borderColor: Colors.border,
  },
  breakdownTitle: {
    fontSize: 14, fontWeight: '700', color: Colors.textPrimary,
    textAlign: 'right', marginBottom: 10,
  },
  breakdownTotal: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 10, marginTop: 4,
  },
  breakdownTotalLabel: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  breakdownTotalValue: { fontSize: 26, fontWeight: '700', color: Colors.accent },
  profitRow: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 6,
    backgroundColor: Colors.profitBg, borderRadius: 10,
    paddingHorizontal: 12, paddingVertical: 8, marginTop: 10,
  },
  profitLabel: { fontSize: 13, color: Colors.profit },
  profitValue: { fontSize: 15, fontWeight: '700', marginRight: 4 },

  footer: {
    paddingHorizontal: 16, paddingTop: 12,
    borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  docBtn: {
    flexDirection: 'row-reverse', alignItems: 'center', justifyContent: 'center',
    gap: 10, backgroundColor: Colors.accent, borderRadius: 16,
    paddingVertical: 16,
  },
  docBtnDisabled: { opacity: 0.5 },
  docBtnText: { fontSize: 16, fontWeight: '700', color: '#E6EDF3' },
  calcOnlyNote: {
    flexDirection: 'row-reverse', alignItems: 'center', gap: 8,
    justifyContent: 'center', paddingVertical: 12,
  },
  calcOnlyText: { fontSize: 13, color: Colors.textMuted },
});
