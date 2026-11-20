import { Platform } from 'react-native';
import { Contact } from '@/types';
import { generateId } from './id';

export interface PhoneContact {
  id: string;
  name: string;
  phoneNumbers: string[];
}

export async function requestContactsPermission(): Promise<boolean> {
  if (Platform.OS === 'web') {
    return false;
  }
  try {
    const Contacts = (await import('expo-contacts')).default;
    const { status } = await Contacts.requestPermissionsAsync();
    return status === 'granted';
  } catch {
    return false;
  }
}

export async function getPhoneContacts(): Promise<PhoneContact[]> {
  if (Platform.OS === 'web') {
    // On web, return empty — feature requires native module
    return [];
  }

  try {
    const Contacts = (await import('expo-contacts')).default;

    const permission = await Contacts.getPermissionsAsync();
    if (permission.status !== 'granted') {
      const requested = await Contacts.requestPermissionsAsync();
      if (requested.status !== 'granted') return [];
    }

    const { data } = await Contacts.getContactsAsync({
      fields: [Contacts.Fields.PhoneNumbers, Contacts.Fields.Name],
      sort: Contacts.SortTypes.FirstName,
    });

    const result: PhoneContact[] = [];
    for (const c of data) {
      const phones = (c.phoneNumbers ?? [])
        .map(p => p.number?.replace(/\s/g, '') ?? '')
        .filter(n => n.length > 0);

      if (phones.length > 0) {
        result.push({
          id: c.id,
          name: c.name ?? c.firstName ?? 'بدون اسم',
          phoneNumbers: phones,
        });
      }
    }

    return result;
  } catch {
    return [];
  }
}

export function isContactsSupported(): boolean {
  return Platform.OS !== 'web';
}

export function phoneContactsToAppContacts(
  phoneContacts: PhoneContact[],
  existingContacts: Contact[],
): { newContacts: Contact[]; matched: Contact[] } {
  const existingPhones = new Set(existingContacts.map(c => c.phone.replace(/\s/g, '')));
  const newContacts: Contact[] = [];
  const matched: Contact[] = [];

  for (const pc of phoneContacts) {
    for (const phone of pc.phoneNumbers) {
      const existing = existingContacts.find(c => c.phone.replace(/\s/g, '') === phone);
      if (existing) {
        matched.push(existing);
      } else if (!existingPhones.has(phone)) {
        newContacts.push({
          id: generateId(),
          name: pc.name,
          phone,
          type: 'businessman',
          isVerified: false,
          fromPhoneBook: true,
        });
        existingPhones.add(phone);
      }
    }
  }

  return { newContacts, matched };
}
