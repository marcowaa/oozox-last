import AsyncStorage from '@react-native-async-storage/async-storage';

export const StorageKeys = {
  USER: '@goldchat/user',
  CONTACTS: '@goldchat/contacts',
  CHATS: '@goldchat/chats',
  MESSAGES: '@goldchat/messages',
  TRANSACTIONS: '@goldchat/transactions',
  GOLD_PRICES: '@goldchat/gold_prices',
  HASHTAGS: '@goldchat/hashtags',
  CHAT_ACCOUNTS: '@goldchat/chat_accounts',
  WORKGROUPS: '@goldchat/workgroups',
  PROJECTS: '@goldchat/projects',
  GOLD_PURCHASES: '@goldchat/gold_purchases',
  MANUFACTURERS: '@goldchat/manufacturers',
  TRADER_REQUESTS: '@goldchat/trader_requests',
  GOLD_SALES: '@goldchat/gold_sales',
  PORTFOLIO: '@goldchat/portfolio',
  PRICE_ALERTS: '@goldchat/price_alerts',
  NEWS: '@goldchat/news',
  AUDITS: '@goldchat/audits',
  ADMIN_CONFIG: '@goldchat/admin_config',
  IS_ADMIN: '@goldchat/is_admin',
  INVITATIONS: '@goldchat/invitations',
} as const;

export async function getItem<T>(key: string): Promise<T | null> {
  try {
    const value = await AsyncStorage.getItem(key);
    return value ? (JSON.parse(value) as T) : null;
  } catch {
    return null;
  }
}

export async function setItem<T>(key: string, value: T): Promise<void> {
  try {
    await AsyncStorage.setItem(key, JSON.stringify(value));
  } catch {}
}
