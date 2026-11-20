import React from 'react';
import { Text, View, StyleSheet, Platform } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../context/AuthContext';

// Import Screens
import LoginScreen from '../screens/LoginScreen';
import DashboardScreen from '../screens/DashboardScreen';
import InvoicesScreen from '../screens/InvoicesScreen';
import CreateInvoiceScreen from '../screens/CreateInvoiceScreen';
import ProfileScreen from '../screens/ProfileScreen';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

function TabIconEmoji({ emoji, focused, label }) {
  return (
    <View style={styles.tabIconWrapper}>
      <Text style={[styles.emoji, focused && styles.emojiFocused]}>{emoji}</Text>
      <Text style={[styles.tabLabel, focused && styles.tabLabelFocused]}>{label}</Text>
      {focused && <View style={styles.activeDot} />}
    </View>
  );
}

// Bottom Tabs Navigator
function MainTabsNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarShowLabel: false,
        tabBarStyle: {
          backgroundColor: '#161B22',
          borderTopWidth: 1,
          borderTopColor: '#30363D',
          height: Platform.OS === 'ios' ? 84 : 64,
          paddingBottom: Platform.OS === 'ios' ? 24 : 8,
          paddingTop: 8,
          elevation: 8,
        },
      }}
    >
      <Tab.Screen
        name="DashboardTab"
        component={DashboardScreen}
        options={{
          tabBarIcon: ({ focused }) => (
            <TabIconEmoji emoji="📊" focused={focused} label="الرئيسية" />
          ),
        }}
      />
      <Tab.Screen
        name="InvoicesTab"
        component={InvoicesScreen}
        options={{
          tabBarIcon: ({ focused }) => (
            <TabIconEmoji emoji="📋" focused={focused} label="الفواتير" />
          ),
        }}
      />
      <Tab.Screen
        name="ProfileTab"
        component={ProfileScreen}
        options={{
          tabBarIcon: ({ focused }) => (
            <TabIconEmoji emoji="👤" focused={focused} label="حسابي" />
          ),
        }}
      />
    </Tab.Navigator>
  );
}

// Root App Navigator with Stack & Authentication Routing
export default function AppNavigator() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <View style={styles.splashContainer}>
        <Text style={styles.splashBrand}>🪙 OOZOX</Text>
        <Text style={styles.splashSub}>جاري تحميل التطبيق...</Text>
      </View>
    );
  }

  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: '#161B22' },
        headerTintColor: '#D4AF37',
        headerTitleStyle: { fontWeight: '700' },
        headerBackTitleVisible: false,
        contentStyle: { backgroundColor: '#0D1117' },
      }}
    >
      {!isAuthenticated ? (
        <Stack.Screen
          name="Login"
          component={LoginScreen}
          options={{ headerShown: false }}
        />
      ) : (
        <>
          <Stack.Screen
            name="Main"
            component={MainTabsNavigator}
            options={{ headerShown: false }}
          />
          <Stack.Screen
            name="CreateInvoice"
            component={CreateInvoiceScreen}
            options={{
              title: 'إصدار فاتورة جديدة',
              headerShown: true,
              presentation: 'modal',
            }}
          />
          <Stack.Screen
            name="Invoices"
            component={InvoicesScreen}
            options={{
              title: 'سجل الفواتير',
              headerShown: true,
            }}
          />
        </>
      )}
    </Stack.Navigator>
  );
}

const styles = StyleSheet.create({
  tabIconWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 50,
  },
  emoji: {
    fontSize: 18,
    opacity: 0.6,
  },
  emojiFocused: {
    opacity: 1,
    transform: [{ scale: 1.15 }],
  },
  tabLabel: {
    fontSize: 10,
    color: '#8B949E',
    fontWeight: '500',
    marginTop: 2,
  },
  tabLabelFocused: {
    color: '#D4AF37',
    fontWeight: '700',
  },
  activeDot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#D4AF37',
    marginTop: 3,
  },
  splashContainer: {
    flex: 1,
    backgroundColor: '#0D1117',
    alignItems: 'center',
    justifyContent: 'center',
  },
  splashBrand: {
    fontSize: 34,
    fontWeight: '800',
    color: '#D4AF37',
  },
  splashSub: {
    color: '#8B949E',
    marginTop: 8,
    fontSize: 13,
  },
});
