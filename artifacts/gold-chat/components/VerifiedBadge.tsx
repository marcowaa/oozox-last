import { MaterialIcons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';
import { useColors } from '@/hooks/useColors';

interface Props {
  size?: number;
  showLabel?: boolean;
}

export function VerifiedBadge({ size = 16, showLabel = false }: Props) {
  const colors = useColors();
  return (
    <View style={styles.row}>
      <MaterialIcons name="verified" size={size} color={colors.accent} />
      {showLabel && (
        <Text style={[styles.label, { color: colors.accent, fontSize: size - 2 }]}>
          تاجر موثق
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  label: {
    fontFamily: 'Inter_600SemiBold',
  },
});
