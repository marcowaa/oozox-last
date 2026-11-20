import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import type {
  AdminConfig,
  Chat,
  ChatAccount,
  Contact,
  GoldPrices,
  GoldPurchase,
  GoldManufacturer,
  GoldSale,
  GroupInvitation,
  GroupMember,
  InventoryAudit,
  Message,
  NewsArticle,
  Project,
  TraderVerificationRequest,
  Transaction,
  User,
  WorkGroup,
} from '@/types';
import { DEFAULT_GOLD_PRICES, calculateProfitLoss } from '@/utils/gold';
import { generateId } from '@/utils/id';
import {
  createChatAccount,
  createHashtagAccount,
  addHashtagEntry as addHashtagEntryUtil,
  addHashtagComment as addHashtagCommentUtil,
  settleHashtagAccount,
} from '@/utils/hashtag';
import { getItem, setItem, StorageKeys } from '@/utils/storage';

interface AppContextType {
  // Core
  user: User | null;
  isLoading: boolean;
  setUser: (user: User) => Promise<void>;
  logout: () => Promise<void>;
  resetApp: () => Promise<void>;

  // Contacts & Chats
  contacts: Contact[];
  chats: Chat[];
  messages: Record<string, Message[]>;
  addContact: (contact: Omit<Contact, 'id'>) => Promise<Contact>;
  createChat: (contact: Contact) => Promise<Chat>;
  sendMessage: (chatId: string, content: string, type?: Message['type'], hashtagId?: string, transactionId?: string) => Promise<Message>;
  markChatRead: (chatId: string) => Promise<void>;
  getChatById: (chatId: string) => Chat | undefined;

  // Chat Accounts (multi-account per chat)
  chatAccounts: ChatAccount[];
  createChatAccount: (chatId: string, name: string) => Promise<ChatAccount>;
  getChatAccounts: (chatId: string) => ChatAccount[];
  addHashtagToAccount: (chatId: string, accountId: string, tag: string) => Promise<void>;
  getAccountHashtags: (chatId: string, accountId: string) => import('@/types').HashtagAccount[];
  getAllChatHashtags: (chatId: string) => import('@/types').HashtagAccount[];
  addHashtagEntry: (chatId: string, accountId: string, hashtagId: string, type: 'invoice' | 'payment', description: string, amount: number, direction: 'debit' | 'credit', selectedAccountIds?: string[]) => Promise<void>;
  settleHashtag: (chatId: string, accountId: string, hashtagId: string) => Promise<void>;
  addHashtagComment: (chatId: string, accountId: string, hashtagId: string, text: string) => Promise<void>;
  addEntryComment: (chatId: string, accountId: string, hashtagId: string, entryId: string, text: string) => Promise<void>;

  // Work Groups & Projects
  workGroups: WorkGroup[];
  projects: Project[];
  createWorkGroup: (name: string, description?: string, parentGroupId?: string) => Promise<WorkGroup>;
  addGroupMember: (groupId: string, userId: string, username: string, phone: string, roleLabel: string) => Promise<void>;
  createProject: (name: string, workGroupId: string, estimatedCapital: number) => Promise<Project>;
  updateProjectCapital: (projectId: string, capital: number) => Promise<void>;

  // Group chat (auto-created with project)
  createGroupChat: (groupId: string, groupName: string, members: GroupMember[]) => Promise<Chat>;
  getGroupChat: (groupId: string) => Chat | undefined;

  // Invitations
  invitations: GroupInvitation[];
  sendInvitation: (groupId: string, groupName: string, inviteePhone: string, roleLabel: string) => Promise<void>;
  respondToInvitation: (invitationId: string, accept: boolean) => Promise<void>;
  getPendingInvitations: () => GroupInvitation[];

  // Gold
  transactions: Transaction[];
  goldPrices: GoldPrices;
  goldPurchases: GoldPurchase[];
  manufacturers: GoldManufacturer[];
  updateGoldPrices: (prices: Partial<GoldPrices>) => Promise<void>;
  addTransaction: (transactionData: Omit<Transaction, 'id'>) => Promise<Transaction>;
  updateTransactionStatus: (transactionId: string, status: Transaction['status']) => Promise<void>;
  addGoldPurchase: (data: Omit<GoldPurchase, 'id'>) => Promise<GoldPurchase>;
  updateGoldPurchasePrice: (purchaseId: string, currentPricePerGram: number) => Promise<void>;
  addManufacturer: (name: string, defaultManufacturingCost: number) => Promise<void>;
  getTransactionById: (id: string) => Transaction | undefined;

  // Gold trader verification
  traderRequests: TraderVerificationRequest[];
  submitTraderRequest: (data: { shopName: string; shopAddress: string; license: string }) => Promise<void>;
  approveTraderRequest: (requestId: string, adminNotes?: string) => Promise<void>;
  rejectTraderRequest: (requestId: string, adminNotes?: string) => Promise<void>;

  // Gold sales
  goldSales: GoldSale[];
  confirmGoldSale: (saleId: string) => Promise<void>;

  // Inventory audits
  audits: InventoryAudit[];

  // Admin config
  adminConfig: AdminConfig;
  updateAdminConfig: (config: Partial<AdminConfig>) => Promise<void>;

  // ThemeManager (Single Source of Truth)
  themeManager: {
    isDarkMode: boolean;
    toggleTheme: () => Promise<void>;
    setDarkMode: (dark: boolean) => Promise<void>;
  };
  isDarkMode: boolean;
  toggleTheme: () => Promise<void>;
}

const AppContext = createContext<AppContextType | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<User | null>(null);
  const [isDarkMode, setIsDarkMode] = useState<boolean>(true);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [chats, setChats] = useState<Chat[]>([]);
  const [messages, setMessages] = useState<Record<string, Message[]>>({});
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [goldPrices, setGoldPricesState] = useState<GoldPrices>(DEFAULT_GOLD_PRICES);
  const [chatAccounts, setChatAccounts] = useState<ChatAccount[]>([]);
  const [workGroups, setWorkGroups] = useState<WorkGroup[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [goldPurchases, setGoldPurchases] = useState<GoldPurchase[]>([]);
  const [manufacturers, setManufacturers] = useState<GoldManufacturer[]>([]);
  const [traderRequests, setTraderRequests] = useState<TraderVerificationRequest[]>([]);
  const [goldSales, setGoldSales] = useState<GoldSale[]>([]);
  const [audits, setAudits] = useState<InventoryAudit[]>([]);
  const [adminConfig, setAdminConfig] = useState<AdminConfig>({ encryptionEnabled: true });
  const [invitations, setInvitations] = useState<GroupInvitation[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Load all from storage
  useEffect(() => {
    async function load() {
      const [u, c, ch, m, t, gp, ca, wg, pj, gpur, mf, tr, gs, au, ac, inv, darkTheme] = await Promise.all([
        getItem<User>(StorageKeys.USER),
        getItem<Contact[]>(StorageKeys.CONTACTS),
        getItem<Chat[]>(StorageKeys.CHATS),
        getItem<Record<string, Message[]>>(StorageKeys.MESSAGES),
        getItem<Transaction[]>(StorageKeys.TRANSACTIONS),
        getItem<GoldPrices>(StorageKeys.GOLD_PRICES),
        getItem<ChatAccount[]>(StorageKeys.CHAT_ACCOUNTS),
        getItem<WorkGroup[]>(StorageKeys.WORKGROUPS),
        getItem<Project[]>(StorageKeys.PROJECTS),
        getItem<GoldPurchase[]>(StorageKeys.GOLD_PURCHASES),
        getItem<GoldManufacturer[]>(StorageKeys.MANUFACTURERS),
        getItem<TraderVerificationRequest[]>(StorageKeys.TRADER_REQUESTS),
        getItem<GoldSale[]>(StorageKeys.GOLD_SALES),
        getItem<InventoryAudit[]>(StorageKeys.AUDITS),
        getItem<AdminConfig>(StorageKeys.ADMIN_CONFIG),
        getItem<GroupInvitation[]>(StorageKeys.INVITATIONS),
        getItem<boolean>('is_dark_theme'),
      ]);
      if (darkTheme !== null && darkTheme !== undefined) setIsDarkMode(darkTheme);
      if (u) setUserState(u);
      if (c) setContacts(c);
      if (ch) setChats(ch);
      if (m) setMessages(m);
      if (t) setTransactions(t);
      if (gp) setGoldPricesState(gp);
      if (ca) setChatAccounts(ca);
      if (wg) setWorkGroups(wg);
      if (pj) setProjects(pj);
      if (gpur) setGoldPurchases(gpur);
      if (mf) setManufacturers(mf);
      if (tr) setTraderRequests(tr);
      if (gs) setGoldSales(gs);
      if (au) setAudits(au);
      if (ac) setAdminConfig(ac);
      if (inv) setInvitations(inv);
      setIsLoading(false);
    }
    load();
  }, []);

  // ===================== User =====================
  const setUser = useCallback(async (newUser: User) => {
    setUserState(newUser);
    await setItem(StorageKeys.USER, newUser);
  }, []);

  const logout = useCallback(async () => {
    setUserState(null);
    setContacts([]); setChats([]); setMessages({}); setTransactions([]);
    setGoldPricesState(DEFAULT_GOLD_PRICES);
    setChatAccounts([]); setWorkGroups([]); setProjects([]);
    setGoldPurchases([]); setManufacturers([]);
    setTraderRequests([]); setGoldSales([]); setAudits([]);
    await Promise.all([
      setItem(StorageKeys.USER, null),
      setItem(StorageKeys.CONTACTS, []),
      setItem(StorageKeys.CHATS, []),
      setItem(StorageKeys.MESSAGES, {}),
      setItem(StorageKeys.TRANSACTIONS, []),
      setItem(StorageKeys.CHAT_ACCOUNTS, []),
      setItem(StorageKeys.WORKGROUPS, []),
      setItem(StorageKeys.PROJECTS, []),
      setItem(StorageKeys.GOLD_PURCHASES, []),
      setItem(StorageKeys.MANUFACTURERS, []),
      setItem(StorageKeys.TRADER_REQUESTS, []),
      setItem(StorageKeys.GOLD_SALES, []),
      setItem(StorageKeys.AUDITS, []),
    ]);
  }, []);

  const resetApp = useCallback(async () => {
    await logout();
    await setItem(StorageKeys.GOLD_PRICES, DEFAULT_GOLD_PRICES);
  }, [logout]);

  // ===================== Contacts & Chats =====================
  const addContact = useCallback(async (contact: Omit<Contact, 'id'>): Promise<Contact> => {
    const newContact: Contact = { ...contact, id: generateId() };
    const updated = [...contacts, newContact];
    setContacts(updated);
    await setItem(StorageKeys.CONTACTS, updated);
    return newContact;
  }, [contacts]);

  const createChat = useCallback(async (contact: Contact): Promise<Chat> => {
    const existing = chats.find(c => c.contactId === contact.id);
    if (existing) return existing;
    const newChat: Chat = {
      id: generateId(),
      contactId: contact.id,
      contactName: contact.name,
      contactType: contact.type,
      contactVerified: contact.isVerified,
      unreadCount: 0,
      createdAt: Date.now(),
    };
    const updated = [newChat, ...chats];
    setChats(updated);
    await setItem(StorageKeys.CHATS, updated);
    return newChat;
  }, [chats]);

  const sendMessage = useCallback(async (
    chatId: string,
    content: string,
    type: Message['type'] = 'text',
    hashtagId?: string,
    transactionId?: string,
  ): Promise<Message> => {
    const msg: Message = {
      id: generateId(),
      chatId,
      senderId: user?.id ?? '',
      senderName: user?.name ?? '',
      type,
      content,
      hashtagId,
      transactionId,
      timestamp: Date.now(),
      status: 'sent',
    };
    const chatMsgs = messages[chatId] ?? [];
    const updatedMessages = { ...messages, [chatId]: [msg, ...chatMsgs] };
    setMessages(updatedMessages);
    await setItem(StorageKeys.MESSAGES, updatedMessages);

    const updatedChats = chats
      .map(c => c.id === chatId
        ? { ...c, lastMessage: content, lastMessageTime: msg.timestamp, lastMessageStatus: 'sent' as const }
        : c)
      .sort((a, b) => (b.lastMessageTime ?? 0) - (a.lastMessageTime ?? 0));
    setChats(updatedChats);
    await setItem(StorageKeys.CHATS, updatedChats);
    return msg;
  }, [messages, chats, user]);

  const markChatRead = useCallback(async (chatId: string) => {
    const updatedChats = chats.map(c => c.id === chatId ? { ...c, unreadCount: 0 } : c);
    setChats(updatedChats);
    await setItem(StorageKeys.CHATS, updatedChats);
  }, [chats]);

  const getChatById = useCallback((chatId: string) => chats.find(c => c.id === chatId), [chats]);

  // ===================== Chat Accounts (multi-account) =====================
  const createChatAccountFn = useCallback(async (chatId: string, name: string) => {
    const account = createChatAccount(chatId, name, user?.id ?? '');
    const updated = [...chatAccounts, account];
    setChatAccounts(updated);
    await setItem(StorageKeys.CHAT_ACCOUNTS, updated);
    return account;
  }, [chatAccounts, user]);

  const getChatAccountsFn = useCallback((chatId: string) =>
    chatAccounts.filter(a => a.chatId === chatId), [chatAccounts]);

  const addHashtagToAccount = useCallback(async (chatId: string, accountId: string, tag: string) => {
    const updated = chatAccounts.map(a => {
      if (a.id !== accountId) return a;
      const newHashtag = createHashtagAccount(chatId, accountId, tag, user?.id ?? '');
      return { ...a, hashtags: [...a.hashtags, newHashtag] };
    });
    setChatAccounts(updated);
    await setItem(StorageKeys.CHAT_ACCOUNTS, updated);
  }, [chatAccounts, user]);

  const getAccountHashtags = useCallback((chatId: string, accountId: string) => {
    const account = chatAccounts.find(a => a.id === accountId && a.chatId === chatId);
    return account ? account.hashtags : [];
  }, [chatAccounts]);

  const getAllChatHashtags = useCallback((chatId: string) => {
    return chatAccounts
      .filter(a => a.chatId === chatId)
      .flatMap(a => a.hashtags);
  }, [chatAccounts]);

  const addHashtagEntry = useCallback(async (
    chatId: string,
    accountId: string,
    hashtagId: string,
    type: 'invoice' | 'payment',
    description: string,
    amount: number,
    direction: 'debit' | 'credit',
    selectedAccountIds?: string[],
  ) => {
    const updated = chatAccounts.map(a => {
      if (a.id !== accountId) return a;
      return {
        ...a,
        hashtags: a.hashtags.map(h => {
          if (h.id !== hashtagId) return h;
          return addHashtagEntryUtil(h, type, description, amount, direction, user?.id ?? '', undefined, selectedAccountIds);
        }),
      };
    });
    setChatAccounts(updated);
    await setItem(StorageKeys.CHAT_ACCOUNTS, updated);
  }, [chatAccounts, user]);

  const settleHashtag = useCallback(async (chatId: string, accountId: string, hashtagId: string) => {
    const updated = chatAccounts.map(a => {
      if (a.id !== accountId) return a;
      return {
        ...a,
        hashtags: a.hashtags.map(h => h.id === hashtagId ? settleHashtagAccount(h) : h),
      };
    });
    setChatAccounts(updated);
    await setItem(StorageKeys.CHAT_ACCOUNTS, updated);
  }, [chatAccounts]);

  const addHashtagComment = useCallback(async (chatId: string, accountId: string, hashtagId: string, text: string) => {
    const updated = chatAccounts.map(a => {
      if (a.id !== accountId) return a;
      return {
        ...a,
        hashtags: a.hashtags.map(h => h.id === hashtagId ? addHashtagCommentUtil(h, text, user?.id ?? '') : h),
      };
    });
    setChatAccounts(updated);
    await setItem(StorageKeys.CHAT_ACCOUNTS, updated);
  }, [chatAccounts, user]);

  const addEntryComment = useCallback(async (chatId: string, accountId: string, hashtagId: string, entryId: string, text: string) => {
    const updated = chatAccounts.map(a => {
      if (a.id !== accountId) return a;
      return {
        ...a,
        hashtags: a.hashtags.map(h => {
          if (h.id !== hashtagId) return h;
          return {
            ...h,
            entries: h.entries.map(e => e.id === entryId ? addHashtagCommentUtil(e, text, user?.id ?? '') : e),
          };
        }),
      };
    });
    setChatAccounts(updated);
    await setItem(StorageKeys.CHAT_ACCOUNTS, updated);
  }, [chatAccounts, user]);

  // ===================== Work Groups & Projects =====================
  const createWorkGroup = useCallback(async (name: string, description?: string, parentGroupId?: string): Promise<WorkGroup> => {
    const group: WorkGroup = {
      id: generateId(), name, description,
      ownerId: user?.id ?? '', parentGroupId,
      members: [], createdAt: Date.now(),
    };
    const updated = [...workGroups, group];
    setWorkGroups(updated);
    await setItem(StorageKeys.WORKGROUPS, updated);
    return group;
  }, [workGroups, user]);

  const addGroupMember = useCallback(async (groupId: string, userId: string, username: string, phone: string, roleLabel: string) => {
    const member: GroupMember = {
      id: generateId(), userId, username, phone, roleLabel,
      status: 'invited', addedAt: Date.now(),
    };
    const updated = workGroups.map(g => {
      if (g.id !== groupId) return g;
      return { ...g, members: [...g.members, member] };
    });
    setWorkGroups(updated);
    await setItem(StorageKeys.WORKGROUPS, updated);
  }, [workGroups]);

  // Create group chat when project is created
  const createGroupChat = useCallback(async (groupId: string, groupName: string, members: GroupMember[]): Promise<Chat> => {
    // Build the group chat object
    const groupChat: Chat = {
      id: generateId(),
      contactId: groupId,
      contactName: groupName,
      contactType: 'businessman',
      contactVerified: false,
      lastMessage: `تم إنشاء مجموعة "${groupName}"`,
      lastMessageTime: Date.now(),
      lastMessageStatus: 'sent',
      unreadCount: 0,
      createdAt: Date.now(),
      isGroup: true,
      groupId,
      groupName,
      groupMembers: members,
    };

    // Use functional updates to avoid stale state
    let created = groupChat;
    setChats(prev => {
      // Check if already exists
      const existing = prev.find(c => c.isGroup && c.groupId === groupId);
      if (existing) {
        created = existing;
        return prev;
      }
      const updated = [groupChat, ...prev];
      setItem(StorageKeys.CHATS, updated);
      return updated;
    });

    // Send system message
    const sysMsg: Message = {
      id: generateId(),
      chatId: created.id,
      senderId: 'system',
      senderName: 'النظام',
      type: 'system',
      content: `تم إنشاء مجموعة "${groupName}" — ${members.length} عضو`,
      timestamp: Date.now(),
    };
    setMessages(prev => {
      if (prev[created.id]) return prev; // already has messages
      const updated = { ...prev, [created.id]: [sysMsg] };
      setItem(StorageKeys.MESSAGES, updated);
      return updated;
    });

    return created;
  }, []);

  const getGroupChat = useCallback((groupId: string) =>
    chats.find(c => c.isGroup && c.groupId === groupId), [chats]);

  // Invitations
  const sendInvitation = useCallback(async (groupId: string, groupName: string, inviteePhone: string, roleLabel: string) => {
    const inv: GroupInvitation = {
      id: generateId(),
      groupId, groupName,
      inviterId: user?.id ?? '',
      inviterName: user?.name ?? '',
      inviteePhone,
      roleLabel,
      status: 'pending',
      createdAt: Date.now(),
    };
    const updated = [...invitations, inv];
    setInvitations(updated);
    await setItem(StorageKeys.INVITATIONS, updated);
  }, [invitations, user]);

  const respondToInvitation = useCallback(async (invitationId: string, accept: boolean) => {
    const updated = invitations.map(inv =>
      inv.id === invitationId
        ? { ...inv, status: accept ? 'accepted' as const : 'declined' as const, respondedAt: Date.now() }
        : inv
    );
    setInvitations(updated);
    await setItem(StorageKeys.INVITATIONS, updated);

    if (accept) {
      // Find the invitation and update member status
      const inv = updated.find(i => i.id === invitationId);
      if (inv) {
        const wgUpdated = workGroups.map(g => {
          if (g.id !== inv.groupId) return g;
          return {
            ...g,
            members: g.members.map(m =>
              m.phone === inv.inviteePhone
                ? { ...m, status: 'accepted' as const }
                : m
            ),
          };
        });
        setWorkGroups(wgUpdated);
        await setItem(StorageKeys.WORKGROUPS, wgUpdated);
      }
    }
  }, [invitations, workGroups]);

  const getPendingInvitations = useCallback(() =>
    invitations.filter(i => i.status === 'pending'), [invitations]);

  const createProject = useCallback(async (name: string, workGroupId: string, estimatedCapital: number): Promise<Project> => {
    const project: Project = {
      id: generateId(), name, workGroupId, type: 'construction',
      estimatedCapital, totalExpenses: 0, totalIncome: 0, createdAt: Date.now(),
    };
    const updated = [...projects, project];
    setProjects(updated);
    await setItem(StorageKeys.PROJECTS, updated);
    return project;
  }, [projects]);

  const updateProjectCapital = useCallback(async (projectId: string, capital: number) => {
    const updated = projects.map(p => p.id === projectId ? { ...p, estimatedCapital: capital } : p);
    setProjects(updated);
    await setItem(StorageKeys.PROJECTS, updated);
  }, [projects]);

  // ===================== Gold =====================
  const updateGoldPrices = useCallback(async (prices: Partial<GoldPrices>) => {
    const updated = { ...goldPrices, ...prices, updatedAt: Date.now() };
    setGoldPricesState(updated);
    await setItem(StorageKeys.GOLD_PRICES, updated);
  }, [goldPrices]);

  const addTransaction = useCallback(async (transactionData: Omit<Transaction, 'id'>): Promise<Transaction> => {
    const transaction: Transaction = { ...transactionData, id: generateId() };
    const updated = [...transactions, transaction];
    setTransactions(updated);
    await setItem(StorageKeys.TRANSACTIONS, updated);
    return transaction;
  }, [transactions]);

  const updateTransactionStatus = useCallback(async (transactionId: string, status: Transaction['status']) => {
    const updated = transactions.map(t => t.id === transactionId ? { ...t, status } : t);
    setTransactions(updated);
    await setItem(StorageKeys.TRANSACTIONS, updated);
  }, [transactions]);

  const addGoldPurchase = useCallback(async (data: Omit<GoldPurchase, 'id'>): Promise<GoldPurchase> => {
    const purchase: GoldPurchase = { ...data, id: generateId() };
    const updated = [...goldPurchases, purchase];
    setGoldPurchases(updated);
    await setItem(StorageKeys.GOLD_PURCHASES, updated);
    return purchase;
  }, [goldPurchases]);

  const updateGoldPurchasePrice = useCallback(async (purchaseId: string, currentPricePerGram: number) => {
    const updated = goldPurchases.map(p => {
      if (p.id !== purchaseId) return p;
      const { profitLoss, profitLossPercent, currentValue } = calculateProfitLoss({
        weight: p.extractedWeight, purchasePricePerGram: p.pricePerGram,
        currentPricePerGram, manufacturingCost: p.manufacturingCost, stampDuty: p.stampDuty,
      });
      return { ...p, currentPricePerGram, currentValue, profitLoss, profitLossPercent };
    });
    setGoldPurchases(updated);
    await setItem(StorageKeys.GOLD_PURCHASES, updated);
  }, [goldPurchases]);

  const addManufacturer = useCallback(async (name: string, defaultManufacturingCost: number) => {
    const m: GoldManufacturer = { id: generateId(), name, defaultManufacturingCost, createdAt: Date.now() };
    const updated = [...manufacturers, m];
    setManufacturers(updated);
    await setItem(StorageKeys.MANUFACTURERS, updated);
  }, [manufacturers]);

  const getTransactionById = useCallback((id: string) => transactions.find(t => t.id === id), [transactions]);

  // ===================== Trader Verification =====================
  const submitTraderRequest = useCallback(async (data: { shopName: string; shopAddress: string; license: string }) => {
    if (!user) return;
    const req: TraderVerificationRequest = {
      id: generateId(),
      userId: user.id,
      name: user.name,
      phone: user.phone,
      username: user.username,
      shopName: data.shopName,
      shopAddress: data.shopAddress,
      license: data.license,
      status: 'pending',
      submittedAt: Date.now(),
    };
    const updated = [...traderRequests, req];
    setTraderRequests(updated);
    await setItem(StorageKeys.TRADER_REQUESTS, updated);
    // Update user's traderStatus to pending
    const updatedUser = { ...user, traderStatus: 'pending' as const, traderShopName: data.shopName, traderShopAddress: data.shopAddress, traderLicense: data.license };
    setUserState(updatedUser);
    await setItem(StorageKeys.USER, updatedUser);
  }, [user, traderRequests]);

  const approveTraderRequest = useCallback(async (requestId: string, adminNotes?: string) => {
    const req = traderRequests.find(r => r.id === requestId);
    if (!req) return;
    const updated = traderRequests.map(r => r.id === requestId ? { ...r, status: 'approved' as const, reviewedAt: Date.now(), adminNotes } : r);
    setTraderRequests(updated);
    await setItem(StorageKeys.TRADER_REQUESTS, updated);
    // If this is the current user, update their type
    if (user && req.userId === user.id) {
      const updatedUser = { ...user, type: 'gold_trader' as const, isVerified: true, traderStatus: 'approved' as const, traderApprovedAt: Date.now(), traderTotalGoldWeight: 0 };
      setUserState(updatedUser);
      await setItem(StorageKeys.USER, updatedUser);
    }
  }, [traderRequests, user]);

  const rejectTraderRequest = useCallback(async (requestId: string, adminNotes?: string) => {
    const updated = traderRequests.map(r => r.id === requestId ? { ...r, status: 'rejected' as const, reviewedAt: Date.now(), adminNotes } : r);
    setTraderRequests(updated);
    await setItem(StorageKeys.TRADER_REQUESTS, updated);
    const req = traderRequests.find(r => r.id === requestId);
    if (user && req?.userId === user.id) {
      const updatedUser = { ...user, traderStatus: 'rejected' as const };
      setUserState(updatedUser);
      await setItem(StorageKeys.USER, updatedUser);
    }
  }, [traderRequests, user]);

  // ===================== Gold Sales =====================
  const confirmGoldSale = useCallback(async (saleId: string) => {
    const updated = goldSales.map(s => {
      if (s.id !== saleId) return s;
      return { ...s, status: 'completed' as const, traderConfirmedAt: Date.now() };
    });
    setGoldSales(updated);
    await setItem(StorageKeys.GOLD_SALES, updated);
    // Decrease trader's gold weight
    const sale = goldSales.find(s => s.id === saleId);
    if (sale && user) {
      const newWeight = Math.max(0, (user.traderTotalGoldWeight ?? 0) - sale.weight);
      const updatedUser = { ...user, traderTotalGoldWeight: newWeight };
      setUserState(updatedUser);
      await setItem(StorageKeys.USER, updatedUser);
    }
  }, [goldSales, user]);

  // ===================== Admin Config =====================
  const updateAdminConfig = useCallback(async (config: Partial<AdminConfig>) => {
    const updated = { ...adminConfig, ...config };
    setAdminConfig(updated);
    await setItem(StorageKeys.ADMIN_CONFIG, updated);
  }, [adminConfig]);

  // ===================== Theme Manager =====================
  const setDarkMode = useCallback(async (dark: boolean) => {
    setIsDarkMode(dark);
    await setItem('is_dark_theme', dark);
  }, []);

  const toggleTheme = useCallback(async () => {
    const nextMode = !isDarkMode;
    setIsDarkMode(nextMode);
    await setItem('is_dark_theme', nextMode);
  }, [isDarkMode]);

  const themeManager = {
    isDarkMode,
    toggleTheme,
    setDarkMode,
  };

  return (
    <AppContext.Provider
      value={{
        user, isLoading, setUser, logout, resetApp,
        contacts, chats, messages, addContact, createChat, sendMessage, markChatRead, getChatById,
        chatAccounts, createChatAccount: createChatAccountFn, getChatAccounts: getChatAccountsFn,
        addHashtagToAccount, getAccountHashtags, getAllChatHashtags,
        addHashtagEntry, settleHashtag, addHashtagComment, addEntryComment,
        workGroups, projects, createWorkGroup, addGroupMember, createProject, updateProjectCapital,
        createGroupChat, getGroupChat,
        invitations, sendInvitation, respondToInvitation, getPendingInvitations,
        transactions, goldPrices, goldPurchases, manufacturers,
        updateGoldPrices, addTransaction, updateTransactionStatus,
        addGoldPurchase, updateGoldPurchasePrice, addManufacturer, getTransactionById,
        traderRequests, submitTraderRequest, approveTraderRequest, rejectTraderRequest,
        goldSales, confirmGoldSale,
        audits,
        adminConfig, updateAdminConfig,
        themeManager, isDarkMode, toggleTheme,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
}
