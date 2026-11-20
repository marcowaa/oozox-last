import { Tabs } from 'expo-router';
import { Platform, View, Text } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors } from '../../constants/colors';

type TabIconProps = {
  name: React.ComponentProps<typeof MaterialIcons>['name'];
  color: string;
  size: number;
  label: string;
  focused: boolean;
};

function TabIcon({ name, color, size, label, focused }: TabIconProps) {
  return (
    <View style={{ alignItems: 'center', justifyContent: 'center', paddingTop: 6 }}>
      <MaterialIcons name={name} size={size} color={color} />
      <Text style={{
        fontSize: 10,
        color,
        marginTop: 2,
        fontWeight: focused ? '700' : '500',
        fontFamily: 'System',
      }}>{label}</Text>
      {focused && <View style={styles.activeDot} />}
    </View>
  );
}

const styles = {
  activeDot: {
    width: 4, height: 4, borderRadius: 2,
    backgroundColor: Colors.accent, marginTop: 3,
  },
};

export default function TabLayout() {
  const insets = useSafeAreaInsets();
  const tabBarHeight = Platform.OS === 'ios' ? 56 + insets.bottom : 56;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: Colors.tabBar,
          borderTopWidth: 1,
          borderTopColor: Colors.border,
          height: tabBarHeight,
          paddingBottom: Platform.OS === 'ios' ? insets.bottom : 8,
          paddingTop: 0,
          elevation: 0,
          shadowOpacity: 0,
        },
        tabBarShowLabel: false,
        tabBarActiveTintColor: Colors.accent,
        tabBarInactiveTintColor: Colors.tabBarInactive,
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="chat-bubble" color={color} size={22} label="المحادثات" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="projects"
        options={{
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="business-center" color={color} size={22} label="المشاريع" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="gold"
        options={{
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="diamond" color={color} size={22} label="الذهب" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="transactions"
        options={{
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="receipt-long" color={color} size={22} label="السجلات" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="person" color={color} size={22} label="حسابي" focused={focused} />
          ),
        }}
      />
    </Tabs>
  );
}
