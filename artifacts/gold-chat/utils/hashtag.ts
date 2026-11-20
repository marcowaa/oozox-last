import {
  ChatAccount,
  HashtagAccount,
  HashtagEntry,
  HashtagEntryDirection,
  HashtagEntryType,
  Comment,
} from '@/types';
import { generateId } from './id';
import { calculateHashtagTotals } from './gold';
export { calculateHashtagTotals };

// ============================================================================
// Parse hashtag from text
// ============================================================================

export function parseHashtag(text: string): string | null {
  const match = text.match(/#([\u0600-\u06FF\w]+)/);
  return match ? match[1] : null;
}

export function isHashtagInput(text: string): boolean {
  return text.trim().startsWith('#');
}

// ============================================================================
// Chat Account operations
// ============================================================================

export function createChatAccount(
  chatId: string,
  name: string,
  creatorId: string,
): ChatAccount {
  return {
    id: generateId(),
    chatId,
    name,
    creatorId,
    createdAt: Date.now(),
    hashtags: [],
    comments: [],
    status: 'active',
  };
}

export function getChatAccounts(
  accounts: ChatAccount[],
  chatId: string,
): ChatAccount[] {
  return accounts.filter(a => a.chatId === chatId);
}

export function getAccountById(
  accounts: ChatAccount[],
  accountId: string,
): ChatAccount | undefined {
  return accounts.find(a => a.id === accountId);
}

// ============================================================================
// Hashtag operations (within an account)
// ============================================================================

export function createHashtagAccount(
  chatId: string,
  accountId: string,
  tag: string,
  creatorId: string,
): HashtagAccount {
  return {
    id: generateId(),
    chatId,
    accountId,
    tag,
    creatorId,
    createdAt: Date.now(),
    entries: [],
    comments: [],
    status: 'active',
  };
}

export function addHashtagEntry(
  account: HashtagAccount,
  type: HashtagEntryType,
  description: string,
  amount: number,
  direction: HashtagEntryDirection,
  createdBy: string,
  photoUri?: string,
  accountIds?: string[],
): HashtagAccount {
  const entry: HashtagEntry = {
    id: generateId(),
    hashtagId: account.id,
    type,
    description,
    amount,
    direction,
    createdBy,
    createdAt: Date.now(),
    comments: [],
    photoUri,
    accountIds,
  };
  return { ...account, entries: [...account.entries, entry] };
}

export function addHashtagComment<T extends { comments: Comment[] }>(
  target: T,
  text: string,
  createdBy: string,
): T {
  const comment: Comment = {
    id: generateId(),
    text,
    createdBy,
    createdAt: Date.now(),
  };
  return { ...target, comments: [...target.comments, comment] };
}

export function settleHashtagAccount(account: HashtagAccount): HashtagAccount {
  return { ...account, status: 'settled' };
}

// ============================================================================
// Queries
// ============================================================================

export function getHashtagTotals(account: HashtagAccount) {
  return calculateHashtagTotals(account.entries);
}

export function getAllHashtagsForChat(
  accounts: ChatAccount[],
  chatId: string,
): HashtagAccount[] {
  return accounts
    .filter(a => a.chatId === chatId)
    .flatMap(a => a.hashtags);
}

export function getHashtagsForAccount(
  accounts: ChatAccount[],
  chatId: string,
  accountId: string,
): HashtagAccount[] {
  const account = accounts.find(a => a.id === accountId);
  return account ? account.hashtags : [];
}

export function getHashtagsByTag(
  accounts: ChatAccount[],
  chatId: string,
  tag: string,
): { account: ChatAccount; hashtag: HashtagAccount }[] {
  const result: { account: ChatAccount; hashtag: HashtagAccount }[] = [];
  for (const account of accounts.filter(a => a.chatId === chatId)) {
    for (const hashtag of account.hashtags) {
      if (hashtag.tag === tag) {
        result.push({ account, hashtag });
      }
    }
  }
  return result;
}

export function getUniqueTags(accounts: ChatAccount[], chatId: string): string[] {
  const tags: string[] = [];
  for (const account of accounts.filter(a => a.chatId === chatId)) {
    for (const hashtag of account.hashtags) {
      if (!tags.includes(hashtag.tag)) tags.push(hashtag.tag);
    }
  }
  return tags;
}

// ============================================================================
// Entry helpers
// ============================================================================

export function getEntryLabel(entry: HashtagEntry): string {
  return entry.type === 'invoice' ? 'فاتورة' : 'دفعة';
}

export function getEntryDirectionLabel(direction: HashtagEntryDirection): string {
  return direction === 'debit' ? 'مدين' : 'دائن';
}

// ============================================================================
// Compute totals across multiple accounts (for multi-account invoice)
// ============================================================================

export function getMultiAccountTotals(
  accounts: ChatAccount[],
  accountIds: string[],
): { totalDebit: number; totalCredit: number; remaining: number } {
  let totalDebit = 0;
  let totalCredit = 0;
  for (const acc of accounts) {
    if (!accountIds.includes(acc.id)) continue;
    for (const hashtag of acc.hashtags) {
      const totals = calculateHashtagTotals(hashtag.entries);
      totalDebit += totals.totalDebit;
      totalCredit += totals.totalCredit;
    }
  }
  return { totalDebit, totalCredit, remaining: totalDebit - totalCredit };
}
