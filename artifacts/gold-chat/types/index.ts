// ============================================================================
// Core Types
// ============================================================================

export type UserType = 'businessman' | 'gold_trader';
export type GoldKarat = '18k' | '21k' | '24k' | '14k';
export type TransactionStatus = 'purchased' | 'returned' | 'resold';
export type MessageType = 'text' | 'hashtag' | 'invoice' | 'payment' | 'system' | 'gold_purchase' | 'transaction' | 'day-separator';
export type MessageStatus = 'sent' | 'delivered' | 'read';
export type HashtagStatus = 'active' | 'settled';
export type HashtagEntryType = 'invoice' | 'payment';
export type HashtagEntryDirection = 'debit' | 'credit';
export type ProjectType = 'construction' | 'finishing' | 'contracting' | 'other';
export type TraderStatus = 'none' | 'pending' | 'approved' | 'rejected';
export type SaleStatus = 'pending' | 'confirmed_by_buyer' | 'confirmed_by_trader' | 'completed' | 'cancelled';

// ============================================================================
// User — simplified: everyone is a regular user, gold_trader is a role applied for
// ============================================================================

export interface User {
  id: string;
  name: string;
  phone: string;
  username: string;
  type: UserType;              // 'businessman' = default, 'gold_trader' = after approval
  isVerified: boolean;
  createdAt: number;
  // Gold trader fields (only if type === 'gold_trader')
  traderStatus?: TraderStatus;
  traderLicense?: string;
  traderShopName?: string;
  traderShopAddress?: string;
  traderTotalGoldWeight?: number;   // total gold owned (grams)
  traderApprovedAt?: number;
}

// ============================================================================
// Contact
// ============================================================================

export interface Contact {
  id: string;
  name: string;
  phone: string;
  username?: string;
  type: UserType;
  isVerified: boolean;
  fromPhoneBook?: boolean;
}

// ============================================================================
// Chat & Message
// ============================================================================

export interface Chat {
  id: string;
  contactId: string;
  contactName: string;
  contactType: UserType;
  contactVerified: boolean;
  lastMessage?: string;
  lastMessageTime?: number;
  lastMessageStatus?: MessageStatus;
  lastMessageSenderId?: string;
  lastMessageSender?: string;
  unreadCount: number;
  createdAt: number;
  /** Group chat fields */
  isGroup?: boolean;
  groupId?: string;
  groupName?: string;
  groupMembers?: GroupMember[];
}

export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  senderName: string;
  type: MessageType;
  content: string;
  transactionId?: string;
  hashtagId?: string;
  timestamp: number;
  status?: MessageStatus;
  text?: string;
  createdAt?: number;
}

// ============================================================================
// Transaction (legacy gold)
// ============================================================================

export interface Transaction {
  id: string;
  chatId: string;
  currentUserId: string;
  currentUserName: string;
  currentUserType: UserType;
  contactId: string;
  contactName: string;
  goldKarat: GoldKarat;
  goldType: string;
  weight: number;
  pricePerGram: number;
  manufacturingCostPerGram: number;
  totalGoldPrice: number;
  totalManufacturing: number;
  totalPrice: number;
  traderCostPerGram?: number;
  profit?: number;
  photoUri?: string;
  timestamp: number;
  status: TransactionStatus;
  originalTransactionId?: string;
  notes?: string;
  karat?: string;
  createdAt?: number;
  mfgCost?: number;
}

// ============================================================================
// Comment
// ============================================================================

export interface Comment {
  id: string;
  text: string;
  createdBy: string;
  createdAt: number;
}

// ============================================================================
// Chat Account & Hashtag System
// ============================================================================

export interface ChatAccount {
  id: string;
  chatId: string;
  name: string;
  creatorId: string;
  createdAt: number;
  hashtags: HashtagAccount[];
  comments: Comment[];
  status: 'active' | 'settled';
}

export interface HashtagEntry {
  id: string;
  hashtagId: string;
  type: HashtagEntryType;
  description: string;
  amount: number;
  direction: HashtagEntryDirection;
  createdBy: string;
  createdAt: number;
  comments: Comment[];
  photoUri?: string;
  accountIds?: string[];
}

export interface HashtagAccount {
  id: string;
  chatId: string;
  accountId: string;
  tag: string;
  creatorId: string;
  createdAt: number;
  entries: HashtagEntry[];
  comments: Comment[];
  status: HashtagStatus;
}

// ============================================================================
// Work Groups & Projects
// ============================================================================

export interface GroupMember {
  id: string;
  userId: string;
  username: string;
  phone: string;          // phone for invitation
  roleLabel: string;
  status: 'invited' | 'accepted' | 'declined';
  addedAt: number;
}

// ============================================================================
// Group Invitation
// ============================================================================

export interface GroupInvitation {
  id: string;
  groupId: string;
  groupName: string;
  inviterId: string;
  inviterName: string;
  inviteePhone: string;
  roleLabel: string;
  status: 'pending' | 'accepted' | 'declined';
  createdAt: number;
  respondedAt?: number;
}

export interface WorkGroup {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  parentGroupId?: string;
  members: GroupMember[];
  createdAt: number;
}

export interface Project {
  id: string;
  name: string;
  workGroupId: string;
  type: ProjectType;
  estimatedCapital: number;
  totalExpenses: number;
  totalIncome: number;
  createdAt: number;
}

// ============================================================================
// Gold Trader Verification Request
// ============================================================================

export interface TraderVerificationRequest {
  id: string;
  userId: string;
  name: string;
  phone: string;
  username: string;
  shopName: string;
  shopAddress: string;
  license: string;
  status: 'pending' | 'approved' | 'rejected';
  submittedAt: number;
  reviewedAt?: number;
  adminNotes?: string;
}

// ============================================================================
// Gold Sale (between buyer and trader)
// ============================================================================

export interface GoldSale {
  id: string;
  buyerId: string;
  buyerName: string;
  traderId: string;
  traderName: string;
  goldKarat: GoldKarat;
  goldType: string;
  weight: number;
  pricePerGram: number;
  manufacturingCost: number;
  stampDuty: number;
  totalAmount: number;
  photoUri?: string;
  status: SaleStatus;
  createdAt: number;
  buyerConfirmedAt?: number;
  traderConfirmedAt?: number;
  notes?: string;
}

// ============================================================================
// Gold Portfolio (user's gold holdings)
// ============================================================================

export interface GoldPortfolioItem {
  id: string;
  karat: GoldKarat;
  goldType: string;          // مصاغ، سبيكة، جنيه
  weight: number;
  purchasePricePerGram: number;
  purchaseDate: number;
  photoUri?: string;
  notes?: string;
}

// ============================================================================
// Price Alert
// ============================================================================

export interface PriceAlert {
  id: string;
  karat: GoldKarat;
  targetPrice: number;
  direction: 'above' | 'below';
  active: boolean;
  createdAt: number;
  triggeredAt?: number;
}

// ============================================================================
// News Article (from admin API)
// ============================================================================

export interface NewsArticle {
  id: string;
  title: string;
  summary: string;
  body?: string;
  imageUrl?: string;
  publishedAt: number;
  source?: string;
}

// ============================================================================
// Inventory Audit (for gold traders)
// ============================================================================

export interface InventoryAudit {
  id: string;
  traderId: string;
  type: 'quarterly' | 'semiannual' | 'annual';
  startDate: number;
  endDate?: number;
  status: 'open' | 'completed';
  expectedWeight: number;
  actualWeight?: number;
  discrepancy?: number;
  items: InventoryAuditItem[];
  notes?: string;
}

export interface InventoryAuditItem {
  id: string;
  karat: GoldKarat;
  goldType: string;
  expectedWeight: number;
  actualWeight: number;
  photoUri?: string;
  notes?: string;
}

// ============================================================================
// Gold Manufacturer & Purchase (legacy)
// ============================================================================

export interface GoldManufacturer {
  id: string;
  name: string;
  defaultManufacturingCost: number;
  createdAt: number;
}

export interface GoldPurchase {
  id: string;
  buyerId: string;
  traderHashtag: string;
  traderConfirmed: boolean;
  photoUri: string;
  extractedWeight: number;
  manuallyEdited: boolean;
  karat: GoldKarat;
  pricePerGram: number;
  currentPricePerGram: number;
  manufacturer: string;
  manufacturingCost: number;
  stampDuty: number;
  totalPaid: number;
  currentValue: number;
  profitLoss: number;
  profitLossPercent: number;
  status: 'purchased' | 'returned';
  createdAt: number;
}

// ============================================================================
// Gold Prices (with buy/sell spread + gold pound + ounce)
// ============================================================================

export interface GoldPrices {
  '14k': number;
  '18k': number;
  '21k': number;
  '24k': number;
  goldPound: number;           // سعر الجنيه الذهب
  ounceUSD: number;            // سعر الأوقية عالمياً بالدولار
  updatedAt: number;
  [key: string]: number | undefined;
}

// ============================================================================
// Admin Config (API settings)
// ============================================================================

export interface AdminConfig {
  notificationApiUrl?: string;
  notificationApiKey?: string;
  newsApiUrl?: string;
  goldPriceApiUrl?: string;
  goldPriceApiKey?: string;
  encryptionEnabled: boolean;
  lastBackupAt?: number;
}
