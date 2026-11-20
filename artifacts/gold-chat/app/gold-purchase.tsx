import { MaterialIcons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { router } from 'expo-router';
import { useState } from 'react';
import {
  Alert,
  Image,
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
import { GoldKarat } from '../types';
import {
  calculateFullGoldPrice,
  calculateProfitLoss,
  DEFAULT_MANUFACTURERS,
  formatCurrency,
  KARAT_LABELS,
} from '../utils/gold';
import { generateId } from '../utils/id';

const KARATS: GoldKarat[] = ['18k', '21k', '24k'];

export default function GoldPurchaseScreen() {
  const insets = useSafeAreaInsets();
  const { user, goldPrices, addGoldPurchase, manufacturers, addManufacturer } = useApp();

  const [photoUri, setPhotoUri] = useState<string | null>(null);
  const [weight, setWeight] = useState('');
  const [karat, setKarat] = useState<GoldKarat>('21k');
  const [manufacturer, setManufacturer] = useState('');
  const [mfgCost, setMfgCost] = useState('');
  const [stampDuty, setStampDuty] = useState('');
  const [traderHashtag, setTraderHashtag] = useState('');
  const [traderConfirmed, setTraderConfirmed] = useState(false);
  const [showManufacturerInput, setShowManufacturerInput] = useState(false);
  const [newManufacturerName, setNewManufacturerName] = useState('');
  const [newManufacturerCost, setNewManufacturerCost] = useState('');
  const [loading, setLoading] = useState(false);

  const pricePerGram = goldPrices[karat];
  const weightNum = parseFloat(weight) || 0;
  const mfgNum = parseFloat(mfgCost) || 0;
  const stampNum = parseFloat(stampDuty) || 0;

  const calc = calculateFullGoldPrice({
    weight: weightNum,
    pricePerGram,
    manufacturingCost: mfgNum,
    stampDuty: stampNum,
  });

  const profitCalc = calculateProfitLoss({
    weight: weightNum,
    purchasePricePerGram: pricePerGram,
    currentPricePerGram: pricePerGram,
    manufacturingCost: mfgNum,
    stampDuty: stampNum,
  });

  async function handleCamera() {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('الإذن مطلوب', 'يرجى السماح للتطبيق بالوصول إلى الكاميرا');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 1,
      allowsEditing: false,
    });
    if (!result.canceled && result.assets[0]) {
      setPhotoUri(result.assets[0].uri);
    }
  }

  function handleSelectManufacturer(name: string) {
    setManufacturer(name);
    const existing = manufacturers.find(m => m.name === name);
    if (existing) {
      setMfgCost(String(existing.defaultManufacturingCost));
    }
  }

  async function handleAddManufacturer() {
    if (!newManufacturerName.trim() || !newManufacturerCost) return;
    await addManufacturer(newManufacturerName.trim(), parseFloat(newManufacturerCost) || 0);
    setManufacturer(newManufacturerName.trim());
    setMfgCost(newManufacturerCost);
    setShowManufacturerInput(false);
    setNewManufacturerName('');
    setNewManufacturerCost('');
  }

  async function handlePurchase() {
    if (!weightNum || weightNum <= 0) {
      Alert.alert('تنبيه', 'يرجى إدخال الوزن');
      return;
    }
    if (!photoUri) {
      Alert.alert('تنبيه', 'يرجى التقاط صورة للميزان والقطعة');
      return;
    }
    if (!manufacturer) {
      Alert.alert('تنبيه', 'يرجى اختيار الشركة المصنّعة');
      return;
    }
    if (!traderHashtag.trim()) {
      Alert.alert('تنبيه', 'يرجى إنشاء هاشتاج للتاجر');
      return;
    }

    setLoading(true);
    try {
      const tag = traderHashtag.trim().startsWith('#')
        ? traderHashtag.trim()
        : `#${traderHashtag.trim()}`;

      await addGoldPurchase({
        buyerId: user?.id ?? '',
        traderHashtag: tag,
        traderConfirmed,
        photoUri,
        extractedWeight: weightNum,
        manuallyEdited: true,
        karat,
        pricePerGram,
        currentPricePerGram: pricePerGram,
        manufacturer,
        manufacturingCost: mfgNum,
        stampDuty: stampNum,
        totalPaid: calc.totalPaid,
        currentValue: profitCalc.currentValue,
        profitLoss: profitCalc.profitLoss,
        profitLossPercent: profitCalc.profitLossPercent,
        status: 'purchased',
        createdAt: Date.now(),
      });

      Alert.alert('تم', 'تم حفظ عملية الشراء في سجل المشتريات', [
        { text: 'حسناً', onPress: () => router.back() },
      ]);
    } catch {
      Alert.alert('خطأ', 'حدث خطأ أثناء الحفظ');
    } finally {
      setLoading(false);
    }
  }

  const allManufacturers = [
    ...DEFAULT_MANUFACTURERS,
    ...manufacturers.map(m => m.name),
  ];
  const uniqueManufacturers = [...new Set(allManufacturers)];

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.closeBtn}>
          <MaterialIcons name="close" size={22} color={Colors.textPrimary} />
        </Pressable>
        <Text style={styles.title}>توثيق شراء ذهب</Text>
        <View style={{ width: 38 }} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        {/* Camera section */}
        <Text style={styles.fieldLabel}>📷 صورة الميزان والقطعة</Text>
        {photoUri ? (
          <Pressable onPress={handleCamera} style={styles.photoPreview}>
            <Image source={{ uri: photoUri }} style={styles.photo} resizeMode="cover" />
            <View style={styles.retakeOverlay}>
              <MaterialIcons name="camera-alt" size={22} color="#fff" />
              <Text style={styles.retakeText}>إعادة التصوير</Text>
            </View>
          </Pressable>
        ) : (
          <Pressable onPress={handleCamera} style={styles.cameraBtn}>
            <MaterialIcons name="camera-alt" size={40} color={Colors.accent} />
            <Text style={styles.cameraBtnText}>التقط صورة</Text>
            <Text style={styles.cameraBtnSub}>شاشة الميزان + القطعة الذهبية معاً</Text>
          </Pressable>
        )}

        {/* Weight */}
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

        {/* Karat */}
        <Text style={styles.fieldLabel}>العيار</Text>
        <View style={styles.karatRow}>
          {KARATS.map(k => (
            <Pressable
              key={k}
              style={[styles.karatBtn, karat === k && styles.karatBtnActive]}
              onPress={() => setKarat(k)}
            >
              <Text style={[styles.karatLabel, karat === k && styles.karatLabelActive]}>
                {KARAT_LABELS[k]}
              </Text>
              <Text style={[styles.karatPrice, karat === k && styles.karatPriceActive]}>
                {formatCurrency(goldPrices[k])}
              </Text>
            </Pressable>
          ))}
        </View>

        {/* Manufacturer */}
        <Text style={styles.fieldLabel}>الشركة المصنّعة</Text>
        {manufacturer ? (
          <View style={styles.selectedManufacturer}>
            <Text style={styles.selectedManufacturerText}>{manufacturer}</Text>
            <Pressable onPress={() => setManufacturer('')}>
              <MaterialIcons name="close" size={18} color={Colors.textMuted} />
            </Pressable>
          </View>
        ) : (
          <View style={styles.manufacturerList}>
            {uniqueManufacturers.slice(0, 6).map(m => (
              <Pressable
                key={m}
                style={styles.manufacturerChip}
                onPress={() => handleSelectManufacturer(m)}
              >
                <Text style={styles.manufacturerChipText}>{m}</Text>
              </Pressable>
            ))}
            <Pressable
              style={[styles.manufacturerChip, { borderColor: Colors.accent, borderWidth: 1 }]}
              onPress={() => setShowManufacturerInput(true)}
            >
              <Text style={[styles.manufacturerChipText, { color: Colors.accent }]}>+ أخرى</Text>
            </Pressable>
          </View>
        )}

        {/* Add new manufacturer */}
        {showManufacturerInput && (
          <View style={styles.newManufacturer}>
            <TextInput
              style={styles.input}
              value={newManufacturerName}
              onChangeText={setNewManufacturerName}
              placeholder="اسم الشركة"
              placeholderTextColor={Colors.placeholder}
              textAlign="right"
            />
            <TextInput
              style={styles.input}
              value={newManufacturerCost}
              onChangeText={setNewManufacturerCost}
              placeholder="أجرة الصنعة الافتراضية"
              placeholderTextColor={Colors.placeholder}
              keyboardType="decimal-pad"
              textAlign="right"
            />
            <Pressable style={styles.addMfgBtn} onPress={handleAddManufacturer}>
              <Text style={styles.addMfgBtnText}>إضافة</Text>
            </Pressable>
          </View>
        )}

        {/* Manufacturing cost */}
        <Text style={styles.fieldLabel}>أجرة الصنعة (إجمالي)</Text>
        <TextInput
          style={styles.input}
          value={mfgCost}
          onChangeText={setMfgCost}
          keyboardType="decimal-pad"
          placeholder="0.00"
          placeholderTextColor={Colors.placeholder}
          textAlign="right"
        />

        {/* Stamp duty */}
        <Text style={styles.fieldLabel}>الدمغة الحكومية</Text>
        <TextInput
          style={styles.input}
          value={stampDuty}
          onChangeText={setStampDuty}
          keyboardType="decimal-pad"
          placeholder="0.00"
          placeholderTextColor={Colors.placeholder}
          textAlign="right"
        />

        {/* Trader hashtag */}
        <Text style={styles.fieldLabel}>هاشتاج التاجر</Text>
        <View style={styles.traderRow}>
          <TextInput
            style={[styles.input, { flex: 1 }]}
            value={traderHashtag}
            onChangeText={setTraderHashtag}
            placeholder="#اسم_التاجر"
            placeholderTextColor={Colors.placeholder}
            textAlign="right"
          />
          <Pressable
            style={[styles.confirmBtn, traderConfirmed && styles.confirmBtnActive]}
            onPress={() => setTraderConfirmed(!traderConfirmed)}
          >
            <MaterialIcons
              name={traderConfirmed ? 'check-circle' : 'radio-button-unchecked'}
              size={24}
              color={traderConfirmed ? Colors.profit : Colors.textMuted}
            />
            <Text style={[styles.confirmBtnText, traderConfirmed && styles.confirmBtnTextActive]}>
              {traderConfirmed ? 'مؤكد' : 'تأكيد'}
            </Text>
          </Pressable>
        </View>

        {/* Price breakdown */}
        {weightNum > 0 && (
          <View style={styles.breakdown}>
            <Text style={styles.breakdownTitle}>ملخص السعر</Text>
            <BreakdownRow label="سعر الغرام" value={formatCurrency(pricePerGram)} />
            <BreakdownRow label="قيمة الذهب" value={formatCurrency(calc.goldValue)} />
            {mfgNum > 0 && <BreakdownRow label="أجرة الصنعة" value={formatCurrency(mfgNum)} />}
            {stampNum > 0 && <BreakdownRow label="الدمغة" value={formatCurrency(stampNum)} />}
            <View style={styles.breakdownTotal}>
              <Text style={styles.breakdownTotalLabel}>الإجمالي</Text>
              <Text style={styles.breakdownTotalValue}>{formatCurrency(calc.totalPaid)}</Text>
            </View>
          </View>
        )}
      </ScrollView>

      {/* Footer */}
      <View style={[styles.footer, { paddingBottom: Math.max(insets.bottom, 16) }]}>
        <Pressable
          style={[styles.purchaseBtn, loading && styles.purchaseBtnDisabled]}
          onPress={handlePurchase}
          disabled={loading || !weightNum}
        >
          <MaterialIcons name="shopping-cart" size={20} color="#fff" />
          <Text style={styles.purchaseBtnText}>
            {loading ? 'جارٍ الحفظ...' : 'شراء وحفظ'}
          </Text>
        </Pressable>
        <Pressable style={styles.cancelBtn} onPress={() => router.back()}>
          <Text style={styles.cancelBtnText}>رجوع</Text>
        </Pressable>
      </View>
    </View>
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
    flexDirection: 'row-reverse', justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  label: { fontSize: 13, color: Colors.textSecondary },
  value: { fontSize: 14, fontWeight: '600', color: Colors.textPrimary },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.surface },

  header: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16, paddingBottom: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  title: { fontSize: 18, fontWeight: '700', color: Colors.textPrimary },
  closeBtn: {
    width: 38, height: 38, borderRadius: 19,
    backgroundColor: Colors.background,
    alignItems: 'center', justifyContent: 'center',
  },

  fieldLabel: {
    fontSize: 13, fontWeight: '600', color: Colors.textSecondary,
    textAlign: 'right', marginBottom: 8, marginTop: 16,
    paddingHorizontal: 16,
  },

  cameraBtn: {
    alignItems: 'center', justifyContent: 'center',
    borderRadius: 16, borderWidth: 2, borderColor: Colors.border,
    borderStyle: 'dashed', paddingVertical: 40,
    backgroundColor: Colors.background, gap: 8, marginHorizontal: 16,
  },
  cameraBtnText: { fontSize: 16, fontWeight: '700', color: Colors.accent },
  cameraBtnSub: { fontSize: 12, color: Colors.textMuted },

  photoPreview: {
    borderRadius: 16, overflow: 'hidden', marginHorizontal: 16, height: 200,
  },
  photo: { width: '100%', height: '100%' },
  retakeOverlay: {
    position: 'absolute', bottom: 0, left: 0, right: 0,
    flexDirection: 'row-reverse', alignItems: 'center', gap: 8,
    backgroundColor: 'rgba(0,0,0,0.5)', padding: 12,
  },
  retakeText: { color: '#0B0F14', fontSize: 14, fontWeight: '600' },

  input: {
    backgroundColor: Colors.inputBg, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 14,
    fontSize: 16, color: Colors.inputText,
    borderWidth: 1, borderColor: Colors.inputBorder,
    fontWeight: '600', marginHorizontal: 16,
  },

  karatRow: {
    flexDirection: 'row-reverse', gap: 10,
    paddingHorizontal: 16,
  },
  karatBtn: {
    flex: 1, paddingVertical: 12, borderRadius: 14,
    alignItems: 'center', backgroundColor: Colors.background,
    borderWidth: 1, borderColor: Colors.border,
  },
  karatBtnActive: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  karatLabel: { fontSize: 16, fontWeight: '700', color: Colors.textPrimary },
  karatLabelActive: { color: Colors.accent },
  karatPrice: { fontSize: 11, color: Colors.textMuted, marginTop: 3 },
  karatPriceActive: { color: 'rgba(201,168,76,0.7)' },

  manufacturerList: {
    flexDirection: 'row-reverse', flexWrap: 'wrap',
    gap: 8, paddingHorizontal: 16,
  },
  manufacturerChip: {
    backgroundColor: Colors.background, borderRadius: 20,
    paddingHorizontal: 14, paddingVertical: 8,
    borderWidth: 1, borderColor: Colors.border,
  },
  manufacturerChipText: { fontSize: 13, color: Colors.textSecondary, fontWeight: '500' },

  selectedManufacturer: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.accentLight, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 12,
    marginHorizontal: 16,
  },
  selectedManufacturerText: { fontSize: 15, fontWeight: '700', color: Colors.accentDark },

  newManufacturer: { gap: 8, paddingHorizontal: 16, marginTop: 8 },
  addMfgBtn: {
    backgroundColor: Colors.accent, borderRadius: 12,
    paddingVertical: 10, alignItems: 'center',
  },
  addMfgBtnText: { color: '#0B0F14', fontSize: 14, fontWeight: '700' },

  traderRow: {
    flexDirection: 'row-reverse', alignItems: 'center',
    gap: 8, paddingHorizontal: 16,
  },
  confirmBtn: {
    flexDirection: 'row-reverse', alignItems: 'center',
    gap: 4, backgroundColor: Colors.background,
    borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10,
    borderWidth: 1, borderColor: Colors.border,
  },
  confirmBtnActive: { borderColor: Colors.profit, backgroundColor: Colors.profitBg },
  confirmBtnText: { fontSize: 13, color: Colors.textMuted, fontWeight: '600' },
  confirmBtnTextActive: { color: Colors.profit },

  breakdown: {
    backgroundColor: Colors.background, borderRadius: 16,
    padding: 16, marginTop: 16, marginHorizontal: 16,
    borderWidth: 1, borderColor: Colors.border,
  },
  breakdownTitle: {
    fontSize: 14, fontWeight: '700', color: Colors.textPrimary,
    textAlign: 'right', marginBottom: 10,
  },
  breakdownTotal: {
    flexDirection: 'row-reverse', justifyContent: 'space-between',
    alignItems: 'center', paddingTop: 10, marginTop: 4,
  },
  breakdownTotalLabel: { fontSize: 15, fontWeight: '700', color: Colors.textPrimary },
  breakdownTotalValue: { fontSize: 26, fontWeight: '700', color: Colors.accent },

  footer: {
    paddingHorizontal: 16, paddingTop: 12,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  purchaseBtn: {
    flexDirection: 'row-reverse', alignItems: 'center',
    justifyContent: 'center', gap: 10,
    backgroundColor: Colors.accent, borderRadius: 16,
    paddingVertical: 16, marginBottom: 8,
  },
  purchaseBtnDisabled: { opacity: 0.5 },
  purchaseBtnText: { fontSize: 16, fontWeight: '700', color: '#E6EDF3' },
  cancelBtn: {
    alignItems: 'center', paddingVertical: 10,
  },
  cancelBtnText: { fontSize: 15, color: Colors.textMuted },
});
