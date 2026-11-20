import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  Alert,
  StyleSheet,
} from 'react-native';
import { invoiceAPI, authAPI } from '../api/endpoints';
import { getErrorMessage } from '../utils/errorHandler';

export default function CreateInvoiceScreen({ navigation }) {
  const [receiverSearch, setReceiverSearch] = useState('');
  const [selectedReceiver, setSelectedReceiver] = useState(null);
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);

  // Items State
  const [items, setItems] = useState([
    { name: '', quantity: '1', unitPrice: '', karat: 0 },
  ]);

  const [taxAmount, setTaxAmount] = useState('0');
  const [discount, setDiscount] = useState('0');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Search for recipient
  const handleSearchReceiver = async (query) => {
    setReceiverSearch(query);
    if (query.trim().length >= 3) {
      setSearching(true);
      try {
        const res = await authAPI.searchUsers(query);
        if (res.data?.data?.users) {
          setSearchResults(res.data.data.users);
        }
      } catch (err) {
        console.warn('[SEARCH-USERS-ERROR]', err);
      } finally {
        setSearching(false);
      }
    } else {
      setSearchResults([]);
    }
  };

  const addItem = () => {
    setItems([...items, { name: '', quantity: '1', unitPrice: '', karat: 0 }]);
  };

  const updateItem = (index, field, value) => {
    const updated = [...items];
    updated[index][field] = value;
    setItems(updated);
  };

  const removeItem = (index) => {
    if (items.length <= 1) {
      Alert.alert('تنبيه', 'يجب أن تحتوي الفاتورة على بند واحد على الأقل');
      return;
    }
    setItems(items.filter((_, i) => i !== index));
  };

  const calculateSubtotal = () => {
    return items.reduce((sum, item) => {
      const q = parseFloat(item.quantity) || 0;
      const p = parseFloat(item.unitPrice) || 0;
      return sum + q * p;
    }, 0);
  };

  const calculateTotal = () => {
    const sub = calculateSubtotal();
    const t = parseFloat(taxAmount) || 0;
    const d = parseFloat(discount) || 0;
    return Math.max(0, sub + t - d);
  };

  const handleSubmit = async () => {
    if (!selectedReceiver) {
      Alert.alert('تنبيه', 'يرجى اختيار الطرف المستلم للفاتورة');
      return;
    }

    for (let i = 0; i < items.length; i++) {
      if (!items[i].name.trim() || !items[i].unitPrice) {
        Alert.alert('تنبيه', `يرجى إكمال بيانات البند رقم ${i + 1}`);
        return;
      }
    }

    setSubmitting(true);
    try {
      const payload = {
        receiverId: selectedReceiver._id,
        items: items.map((it) => ({
          name: it.name.trim(),
          quantity: parseFloat(it.quantity) || 1,
          unitPrice: parseFloat(it.unitPrice) || 0,
          karat: parseInt(it.karat, 10) || 0,
        })),
        taxAmount: parseFloat(taxAmount) || 0,
        discount: parseFloat(discount) || 0,
        paymentMethod: 'INSTAPAY',
        dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        notes: notes.trim(),
      };

      const res = await invoiceAPI.createInvoice(payload);
      Alert.alert(
        'نجاح 🎉',
        `تم إصدار الفاتورة رقم ${res.data.data.invoice.invoiceNumber} وإرسال إشعار للمستلم`,
        [
          {
            text: 'حسناً',
            onPress: () => {
              if (navigation && navigation.goBack) {
                navigation.goBack();
              }
            },
          },
        ]
      );
    } catch (error) {
      Alert.alert('خطأ في الإنشاء', getErrorMessage(error, 'فشل إصدار الفاتورة'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.headerTitle}>إصدار فاتورة ومعاملة جديدة 📝</Text>

      {/* Recipient Selection */}
      <Text style={styles.sectionLabel}>الطرف المستلم (البحث بالاسم أو الهاتف):</Text>
      {selectedReceiver ? (
        <View style={styles.selectedUserCard}>
          <Text style={styles.selectedUserName}>{selectedReceiver.name}</Text>
          <Text style={styles.selectedUserPhone}>{selectedReceiver.phone}</Text>
          <TouchableOpacity onPress={() => setSelectedReceiver(null)}>
            <Text style={styles.changeUserText}>تغيير المستلم 🔄</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <View>
          <TextInput
            style={styles.input}
            placeholder="ابحث باسم المستلم أو هاتفه..."
            placeholderTextColor="#8B949E"
            value={receiverSearch}
            onChangeText={handleSearchReceiver}
          />
          {searching && <ActivityIndicator color="#D4AF37" style={{ marginVertical: 8 }} />}
          {searchResults.map((u) => (
            <TouchableOpacity
              key={u._id}
              style={styles.searchResultItem}
              onPress={() => {
                setSelectedReceiver(u);
                setSearchResults([]);
                setReceiverSearch('');
              }}
            >
              <Text style={styles.searchResultName}>{u.name}</Text>
              <Text style={styles.searchResultPhone}>{u.phone}</Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Items Section */}
      <View style={styles.itemsHeaderRow}>
        <TouchableOpacity style={styles.addItemBtn} onPress={addItem}>
          <Text style={styles.addItemBtnText}>+ إضافة بند</Text>
        </TouchableOpacity>
        <Text style={styles.sectionLabel}>بنود الفاتورة:</Text>
      </View>

      {items.map((item, idx) => (
        <View key={idx} style={styles.itemBox}>
          <View style={styles.itemBoxHeader}>
            <Text style={styles.itemBoxTitle}>البند {idx + 1}</Text>
            {items.length > 1 && (
              <TouchableOpacity onPress={() => removeItem(idx)}>
                <Text style={styles.removeItemText}>حذف 🗑️</Text>
              </TouchableOpacity>
            )}
          </View>

          <TextInput
            style={styles.input}
            placeholder="اسم الصنف / البيان (مثال: سبيكة ذهب 24k)"
            placeholderTextColor="#8B949E"
            value={item.name}
            onChangeText={(val) => updateItem(idx, 'name', val)}
          />

          <View style={styles.row}>
            <View style={{ flex: 1, marginLeft: 8 }}>
              <Text style={styles.fieldLabel}>السعر الإفرادي (ج.م):</Text>
              <TextInput
                style={styles.input}
                placeholder="0.00"
                placeholderTextColor="#8B949E"
                keyboardType="numeric"
                value={item.unitPrice}
                onChangeText={(val) => updateItem(idx, 'unitPrice', val)}
              />
            </View>

            <View style={{ flex: 1 }}>
              <Text style={styles.fieldLabel}>الكمية:</Text>
              <TextInput
                style={styles.input}
                placeholder="1"
                placeholderTextColor="#8B949E"
                keyboardType="numeric"
                value={item.quantity}
                onChangeText={(val) => updateItem(idx, 'quantity', val)}
              />
            </View>
          </View>
        </View>
      ))}

      {/* Total Calculations */}
      <View style={styles.summaryCard}>
        <View style={styles.summaryRow}>
          <Text style={styles.summaryValue}>{calculateSubtotal().toLocaleString()} ج.م</Text>
          <Text style={styles.summaryLabel}>المجموع الفرعي:</Text>
        </View>
        <View style={styles.summaryRow}>
          <Text style={[styles.summaryValue, styles.totalHighlight]}>
            {calculateTotal().toLocaleString()} ج.م
          </Text>
          <Text style={[styles.summaryLabel, styles.totalHighlight]}>الإجمالي النهائي:</Text>
        </View>
      </View>

      {/* Submit Button */}
      <TouchableOpacity
        style={[styles.submitBtn, submitting && { opacity: 0.7 }]}
        disabled={submitting}
        onPress={handleSubmit}
      >
        {submitting ? (
          <ActivityIndicator color="#000" />
        ) : (
          <Text style={styles.submitBtnText}>إصدار وحفظ الفاتورة مباشرة 🚀</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0D1117' },
  content: { padding: 16 },
  headerTitle: { color: '#F0F6FC', fontSize: 20, fontWeight: 'bold', textAlign: 'right', marginBottom: 16 },
  sectionLabel: { color: '#8B949E', fontSize: 14, textAlign: 'right', marginBottom: 8, fontWeight: '600' },
  input: { backgroundColor: '#161B22', color: '#F0F6FC', borderRadius: 8, padding: 12, borderWidth: 1, borderColor: '#30363D', textAlign: 'right', marginBottom: 10 },
  selectedUserCard: { backgroundColor: '#1F242C', padding: 12, borderRadius: 8, borderWidth: 1, borderColor: '#D4AF37', marginBottom: 12 },
  selectedUserName: { color: '#D4AF37', fontWeight: 'bold', fontSize: 16, textAlign: 'right' },
  selectedUserPhone: { color: '#8B949E', fontSize: 13, textAlign: 'right', marginTop: 4 },
  changeUserText: { color: '#58A6FF', textAlign: 'left', marginTop: 6, fontSize: 12 },
  searchResultItem: { backgroundColor: '#21262D', padding: 10, borderRadius: 6, marginBottom: 6 },
  searchResultName: { color: '#F0F6FC', textAlign: 'right', fontWeight: 'bold' },
  searchResultPhone: { color: '#8B949E', textAlign: 'right', fontSize: 12 },
  itemsHeaderRow: { flexDirection: 'row-reverse', justifyContent: 'space-between', alignItems: 'center', marginTop: 12, marginBottom: 8 },
  addItemBtn: { backgroundColor: '#238636', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 6 },
  addItemBtnText: { color: '#FFF', fontSize: 12, fontWeight: 'bold' },
  itemBox: { backgroundColor: '#161B22', padding: 12, borderRadius: 8, marginBottom: 12, borderWidth: 1, borderColor: '#30363D' },
  itemBoxHeader: { flexDirection: 'row-reverse', justifyContent: 'space-between', marginBottom: 8 },
  itemBoxTitle: { color: '#D4AF37', fontWeight: 'bold' },
  removeItemText: { color: '#DA3633', fontSize: 12 },
  row: { flexDirection: 'row-reverse' },
  fieldLabel: { color: '#8B949E', fontSize: 12, textAlign: 'right', marginBottom: 4 },
  summaryCard: { backgroundColor: '#161B22', borderRadius: 8, padding: 16, marginVertical: 16, borderWidth: 1, borderColor: '#30363D' },
  summaryRow: { flexDirection: 'row-reverse', justifyContent: 'space-between', marginVertical: 4 },
  summaryLabel: { color: '#8B949E', fontSize: 14 },
  summaryValue: { color: '#F0F6FC', fontSize: 14, fontWeight: 'bold' },
  totalHighlight: { color: '#D4AF37', fontSize: 18, fontWeight: 'bold' },
  submitBtn: { backgroundColor: '#D4AF37', paddingVertical: 14, borderRadius: 8, alignItems: 'center', marginTop: 10, marginBottom: 30 },
  submitBtnText: { color: '#000', fontSize: 16, fontWeight: 'bold' },
});
