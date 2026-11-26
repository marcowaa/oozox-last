import React, { useState, useMemo, useEffect } from 'react';
import { Button } from './components/ui/button';
import { Input } from './components/ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './components/ui/card';
import { Badge } from './components/ui/badge';
import { JobOpportunity, WorkAssignment, AttendanceRecord } from './types';
import { 
  Plus, 
  Send, 
  FileText, 
  TrendingUp, 
  Lock, 
  Unlock, 
  Smartphone, 
  Download, 
  Share2, 
  Code, 
  CheckCircle2, 
  ShieldCheck, 
  Sparkles, 
  Coins, 
  QrCode, 
  Calculator, 
  ArrowLeft, 
  Search, 
  Copy,
  Receipt,
  UserCheck,
  User,
  X,
  AlertCircle,
  Info,
  CheckCircle,
  Save,
  Trash2,
  Building2,
  Briefcase,
  Palette,
  Heart,
  GraduationCap,
  Hammer,
  Scale,
  Truck,
  Camera,
  Utensils,
  MapPin,
  Clock,
  MessageCircle,
  MessageSquare,
  Laptop,
  LogOut,
  Phone,
  Video
} from 'lucide-react';
import { WebAuthModal, WebTrustedDevicesModal } from './components/WebAuthPortal';
import { WebRtcCallModal, ActiveWebCallSession } from './components/WebRtcCallModal';
import {
  WebChatMessageItem,
  WebConversationItem,
  WebUserDto,
  apiFetchMe,
  apiListConversations,
  apiListMessages,
  apiSendMessage,
  clearWebAuthSession,
  getSignalingWsUrl,
  getWebAccessToken,
} from './lib/webAuthClient';

interface ToastMessage {
  id: string;
  title: string;
  description?: string;
  type: 'success' | 'info' | 'warning' | 'error';
}

interface UserProfile {
  name: string;
  businessName: string;
  phone: string;
  taxNumber: string;
  currency: string;
  city: string;
  accountType: 'individual' | 'business' | 'professional' | 'freelancer';
  jobTitle: string;
  bio: string;
  skills: string[];
  education: string;
  experience: string;
}

interface InvoiceItem {
  id: string;
  name: string;
  quantity: number;
  unitPrice: number;
  total: number;
}

interface InvoiceRecord {
  id: string;
  invoiceNumber: string;
  clientName: string;
  clientPhone: string;
  items: InvoiceItem[];
  subtotal: number;
  vatAmount: number;
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  status: 'paid' | 'partial' | 'unpaid';
  date: string;
  qrCodeUrl: string;
}

interface HashtagEntry {
  id: string;
  rawText: string;
  tag: string;
  category: 'gold' | 'materials' | 'service' | 'general';
  amount: number;
  quantity?: number;
  unitPrice?: number;
  type: 'sale' | 'purchase' | 'payment';
  date: string;
}

export const UserAppPortal: React.FC<{
  onNavigateHome: () => void;
  onNavigateToAdmin: () => void;
}> = ({ onNavigateHome, onNavigateToAdmin }) => {
  const [activeView, setActiveView] = useState<'dashboard' | 'chats' | 'invoices' | 'hashtag_engine' | 'gold_calc' | 'stealth_vault' | 'developer_api' | 'career_cv' | 'work_tracking'>('dashboard');

  // Server Authenticated User Session & Trusted Devices State
  const [authToken, setAuthToken] = useState<string | null>(() => getWebAccessToken('user'));
  const [authUser, setAuthUser] = useState<WebUserDto | null>(null);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [showTrustedDevicesModal, setShowTrustedDevicesModal] = useState(false);

  useEffect(() => {
    const token = getWebAccessToken('user');
    if (!token) return;
    void apiFetchMe(token).then((res) => {
      if (res.ok && res.data?.user) {
        setAuthToken(token);
        setAuthUser(res.data.user);
      } else {
        clearWebAuthSession('user', false);
        setAuthToken(null);
        setAuthUser(null);
      }
    });
  }, []);

  // Floating Chat Bubble State
  const [showChatBubble, setShowChatBubble] = useState(false);
  const [bubbleActive, setBubbleActive] = useState(false);
  const [bubbleInput, setBubbleInput] = useState('');
  const [bubbleMessages, setBubbleMessages] = useState<Array<{ id: string; sender: 'bot' | 'user'; text: string }>>([
    { id: 'welcome', sender: 'bot', text: 'أهلاً بك! اكتب أمرك أو سؤالك المحاسبي وسنساعدك فوراً.' }
  ]);

  const handleSendBubbleMessage = (e?: React.FormEvent) => {
    e?.preventDefault();
    const trimmed = bubbleInput.trim();
    if (!trimmed) return;
    const userMsg = { id: `u_${Date.now()}`, sender: 'user' as const, text: trimmed };
    setBubbleMessages(prev => [
      ...prev,
      userMsg,
      {
        id: `b_${Date.now() + 1}`,
        sender: 'bot' as const,
        text: trimmed.startsWith('#')
          ? 'يمكنك اعتماد القيد المحاسبي مباشرة عبر تبويب "محرك الهاشتاج الذكي" لحفظه في دفتر الأستاذ.'
          : 'تم استلام رسالتك بنجاح. للتواصل الرسمي أو الدعم الفني الفوري استخدم صفحة اتصل بنا أو نظام الإشعارات.'
      }
    ]);
    setBubbleInput('');
  };

  // Attendance & Tracking State (populated dynamically from active work assignments)
  const [assignments] = useState<WorkAssignment[]>([]);

  const [attendanceLogs, setAttendanceLogs] = useState<AttendanceRecord[]>([]);

  const calculateDistanceMeters = (lat1: number, lon1: number, lat2: number, lon2: number) => {
    const toRad = (v: number) => (v * Math.PI) / 180;
    const R = 6371000;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  };

  // Real GPS Geolocation Verification
  const checkEmployeeLocation = (assignmentId: string) => {
    const assignment = assignments.find((a) => a.id === assignmentId);
    if (!assignment) {
      addToast('تعذر العثور على التكليف', 'بيانات موقع العمل غير متوفرة حالياً.', 'error');
      return;
    }
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      addToast('المتصفح لا يدعم GPS', 'يرجى السماح بالوصول إلى الموقع الجغرافي من متصفح أو جهاز يدعم GPS.', 'error');
      return;
    }

    addToast(
      'طلب التحقق من الموقع',
      'جاري قراءة إحداثيات GPS الفعلية ومطابقتها مع نطاق العمل المحدد...',
      'info'
    );

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const dist = Math.round(
          calculateDistanceMeters(
            pos.coords.latitude,
            pos.coords.longitude,
            assignment.location.lat,
            assignment.location.lng
          )
        );
        const radius = assignment.location.radius || 100;
        const isMatched = dist <= radius;
        const newRecord: AttendanceRecord = {
          id: `att-${Date.now()}`,
          assignmentId,
          timestamp: new Date().toLocaleTimeString('ar-EG'),
          type: isMatched ? 'check_in' : 'violation',
          locationMatch: isMatched,
          notes: isMatched
            ? `تم التوثيق ضمن النطاق (${dist} متر من موقع العمل)`
            : `خارج النطاق المحدد (${dist} متر / المسموح ${radius} متر)`
        };
        setAttendanceLogs((prev) => [newRecord, ...prev]);
        if (!isMatched) {
          addToast('تنبيه مخالفة موقع', `الموقع الحالي يبعد ${dist} متر عن موقع العمل المحدد.`, 'warning');
        } else {
          addToast('تم التحقق بنجاح', 'تم توثيق إحداثيات الموقع الفعلية في سجل الحضور.', 'success');
        }
      },
      (err) => {
        addToast('تعذر تحديد الموقع', `فشل الحصول على إحداثيات GPS: ${err.message}`, 'error');
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  };

  const [conversations, setConversations] = useState<WebConversationItem[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [activeMessages, setActiveMessages] = useState<WebChatMessageItem[]>([]);
  const [chatComposer, setChatComposer] = useState('');
  const [activeCallSession, setActiveCallSession] = useState<ActiveWebCallSession | null>(null);
  const [incomingCallSignal, setIncomingCallSignal] = useState<Record<string, unknown> | null>(null);
  const wsRef = React.useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!authToken || !authUser) {
      wsRef.current?.close();
      wsRef.current = null;
      return undefined;
    }
    void apiListConversations(authToken).then((res) => {
      if (res.ok && res.data?.conversations) {
        setConversations(res.data.conversations);
      }
    });

    const ws = new WebSocket(getSignalingWsUrl(authToken));
    wsRef.current = ws;
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(String(ev.data)) as Record<string, unknown>;
        setIncomingCallSignal(msg);
        if (msg.type === 'CALL_OFFER' && typeof msg.callId === 'string' && typeof msg.senderId === 'string') {
          setActiveCallSession({
            callId: msg.callId,
            peerId: msg.senderId,
            peerName: String(msg.callerName || msg.senderId),
            callType: String(msg.callType || 'AUDIO').toUpperCase() === 'VIDEO' ? 'VIDEO' : 'AUDIO',
            isIncoming: true,
            offerSdp: typeof msg.sdp === 'string' ? msg.sdp : undefined,
          });
        } else if (msg.type === 'force_logout' || msg.type === 'session_revoked') {
          clearWebAuthSession('user', true);
          setAuthToken(null);
          setAuthUser(null);
          addToast('تم نقل الحساب', String(msg.message || 'تم تسجيل الخروج من هذا الجهاز بعد نقل الحساب.'), 'warning');
        }
      } catch {
        // ignore non-json frames
      }
    };
    return () => {
      ws.close();
      wsRef.current = null;
    };
  }, [authToken, authUser?.id]);

  useEffect(() => {
    if (!authToken || !selectedConversationId) return;
    void apiListMessages(authToken, selectedConversationId).then((res) => {
      if (res.ok && res.data?.messages) {
        setActiveMessages(res.data.messages);
      }
    });
  }, [authToken, selectedConversationId]);

  const chats = conversations.map((c) => {
    const peer = c.participantDetails?.find((p) => p.id !== authUser?.id) || c.participantDetails?.[0];
    return {
      id: c.conversationId || c._id,
      peerId: peer?.id || '',
      name: peer?.name || peer?.phone || 'محادثة مباشرة',
      lastMessage: c.lastMessagePreview || 'ابدأ المحادثة...',
      time: c.lastMessageAt ? new Date(c.lastMessageAt).toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' }) : '',
      unreadCount: c.unreadCount || 0,
      avatarColor: 'bg-amber-500',
      isOnline: true,
    };
  });

  // Toast Notifications State
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (title: string, description?: string, type: 'success' | 'info' | 'warning' | 'error' = 'success') => {
    const id = `toast-${Date.now()}-${toasts.length}`;
    const newToast: ToastMessage = { id, title, description, type };
    setToasts((prev) => [...prev, newToast]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  // User Profile State
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [profile, setProfile] = useState<UserProfile>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('oozox_user_profile');
      if (saved) {
        try {
          return JSON.parse(saved);
        } catch {
          // ignore malformed storage
        }
      }
    }
    return {
      name: '',
      businessName: '',
      phone: '',
      taxNumber: '',
      currency: 'ج.م (EGP)',
      city: '',
      accountType: 'professional',
      jobTitle: '',
      bio: '',
      skills: [],
      education: '',
      experience: ''
    };
  });

  const [profileForm, setProfileForm] = useState<UserProfile>({ ...profile });
  const [isCustomJob, setIsCustomJob] = useState(false);

  const professionalCategories = [
    {
      id: 'gold_trade',
      title: 'الذهب والتجارة',
      icon: <Coins className="w-4 h-4" />,
      jobs: ['تاجر ذهب', 'صائغ مجوهرات', 'مثمن معادن نفيسة', 'تاجر جملة', 'مندوب مبيعات', 'خبير أحجار كريمة']
    },
    {
      id: 'engineering',
      title: 'الهندسة والإنشاءات',
      icon: <Building2 className="w-4 h-4" />,
      jobs: ['مهندس مدني', 'مقاول إنشائات', 'فني كهرباء', 'فني سباكة', 'نجار محترف', 'حداد مسلح', 'فني دهانات']
    },
    {
      id: 'tech_digital',
      title: 'التكنولوجيا والتصميم',
      icon: <Code className="w-4 h-4" />,
      jobs: ['مبرمج برمجيات', 'مصمم جرافيك', 'كاتب محتوى', 'خبير تسويق رقمي', 'محلل بيانات', 'مصور فوتوغرافي']
    },
    {
      id: 'finance_legal',
      title: 'المالية والقانون',
      icon: <Scale className="w-4 h-4" />,
      jobs: ['محاسب قانوني', 'محلل مالي', 'محامي', 'مدير إداري', 'أمين مخزن']
    },
    {
      id: 'services',
      title: 'الخدمات والحرف',
      icon: <Truck className="w-4 h-4" />,
      jobs: ['شيف عمومي', 'خبير تجميل', 'سائق نقل', 'خياط', 'حلاق', 'فني تبريد وتكييف']
    },
    {
      id: 'medical_edu',
      title: 'الطب والتعليم',
      icon: <GraduationCap className="w-4 h-4" />,
      jobs: ['طبيب', 'صيدلي', 'ممرض', 'معلم', 'مترجم', 'باحث أكاديمي']
    }
  ];

  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);

  const [jobs, setJobs] = useState<JobOpportunity[]>(() => {
    if (typeof window === 'undefined') return [];
    try {
      const saved = localStorage.getItem('oozox_jobs');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  // Job Notification Listener
  useEffect(() => {
    const handleNewJob = (event: any) => {
      const job = event.detail;
      // Update local jobs list
      setJobs(prev => [job, ...prev]);
      
      // Simple match: check if the job title contains the user's job title or vice versa
      if (profile.jobTitle && (
          job.title.toLowerCase().includes(profile.jobTitle.toLowerCase()) || 
          profile.jobTitle.toLowerCase().includes(job.title.toLowerCase())
      )) {
        addToast(
          '🔔 فرصة عمل جديدة مطابقة!',
          `تم نشر وظيفة "${job.title}" في شركة "${job.company}" تطابق تخصصك المهني.`,
          'info'
        );
      }
    };

    window.addEventListener('oozox_new_job', handleNewJob);
    return () => window.removeEventListener('oozox_new_job', handleNewJob);
  }, [profile.jobTitle]);

  const handleUpdateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    if (!profileForm.name.trim()) {
      addToast('خطأ في البيانات', 'يرجى إدخال الاسم بالكامل', 'error');
      return;
    }
    setProfile({ ...profileForm });
    if (typeof window !== 'undefined') {
      localStorage.setItem('oozox_user_profile', JSON.stringify(profileForm));
    }
    setShowProfileModal(false);
    addToast('تم تحديث البيانات المهنية', 'تم حفظ التغييرات وتحديث السيرة الذاتية الرقمية.', 'success');
  };

  const handleShareCV = () => {
    const slug = (profile.name || 'profile').trim().replace(/\s+/g, '-');
    const cvUrl = `https://oozox.com/cv/${slug}`;
    handleCopyText(cvUrl, 'رابط السيرة الذاتية');
    addToast('رابط CV جاهز للمشاركة', 'تم نسخ رابط سيرتك الذاتية المنسقة، يمكنك الآن إرساله للشركات.', 'success');
  };

  // Invoices State
  const [invoices, setInvoices] = useState<InvoiceRecord[]>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('oozox_invoices');
      if (saved) {
        try {
          return JSON.parse(saved);
        } catch {
          // ignore malformed storage
        }
      }
    }
    return [];
  });

  // Modal for New Invoice
  const [showNewInvoiceModal, setShowNewInvoiceModal] = useState(false);
  const [newClientName, setNewClientName] = useState('');
  const [newClientPhone, setNewClientPhone] = useState('');
  const [newItemName, setNewItemName] = useState('');
  const [newItemQty, setNewItemQty] = useState('1');
  const [newItemPrice, setNewItemPrice] = useState('');
  const [newPaidAmount, setNewPaidAmount] = useState('');
  const [includeVat, setIncludeVat] = useState(false);

  // Hashtag Chat Engine State
  const [hashtagInput, setHashtagInput] = useState('');
  const [hashtagEntries, setHashtagEntries] = useState<HashtagEntry[]>([]);

  // Gold Calculator State
  const [goldWeight, setGoldWeight] = useState('');
  const [goldKarat, setGoldKarat] = useState<'18' | '21' | '24'>('21');
  const [craftsmanshipPerGram, setCraftsmanshipPerGram] = useState('');
  const [goldPricesBase, setGoldPricesBase] = useState<Record<'18' | '21' | '24', number>>({
    '18': 0,
    '21': 0,
    '24': 0
  });

  const calculatedGoldTotal = useMemo(() => {
    const w = parseFloat(goldWeight) || 0;
    const baseP = goldPricesBase[goldKarat] || 0;
    const craft = parseFloat(craftsmanshipPerGram) || 0;
    const gramTotal = baseP + craft;
    const total = w * gramTotal;
    return {
      rawGoldPrice: w * baseP,
      craftsmanshipTotal: w * craft,
      grandTotal: total,
      gramTotal
    };
  }, [goldWeight, goldKarat, craftsmanshipPerGram, goldPricesBase]);

  // Stealth Vault State
  const [isVaultLocked, setIsVaultLocked] = useState(true);
  const [vaultPin, setVaultPin] = useState('');
  const [vaultError, setVaultError] = useState('');
  const [configuredVaultPin, setConfiguredVaultPin] = useState<string>(() =>
    typeof window !== 'undefined' ? localStorage.getItem('oozox_vault_pin') || '' : ''
  );
  const [stealthNotes] = useState<Array<{ id: string; label: string; balance: string; date: string }>>([]);

  // Handle New Invoice Creation
  const handleCreateInvoice = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newClientName || !newItemName || !newItemPrice) {
      addToast('بيانات غير مكتملة', 'يرجى ملء كافة الحقول الإلزامية لإنشاء الفاتورة', 'warning');
      return;
    }

    const qty = parseFloat(newItemQty) || 1;
    const price = parseFloat(newItemPrice) || 0;
    const sub = qty * price;
    const vat = includeVat ? sub * 0.14 : 0;
    const total = sub + vat;
    const paid = parseFloat(newPaidAmount) || total;
    const rem = total - paid;

    const seq = String(Date.now()).slice(-6);
    const invId = `INV-${new Date().getFullYear()}-${seq}`;
    const newInv: InvoiceRecord = {
      id: `inv-${Date.now()}`,
      invoiceNumber: invId,
      clientName: newClientName,
      clientPhone: newClientPhone.trim(),
      items: [
        { id: '1', name: newItemName, quantity: qty, unitPrice: price, total: sub }
      ],
      subtotal: sub,
      vatAmount: vat,
      totalAmount: total,
      paidAmount: paid,
      remainingAmount: rem > 0 ? rem : 0,
      status: rem <= 0 ? 'paid' : paid > 0 ? 'partial' : 'unpaid',
      date: new Date().toISOString().split('T')[0],
      qrCodeUrl: `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=OOZOX-${invId}-${total}EGP`
    };

    const updatedInvoices = [newInv, ...invoices];
    setInvoices(updatedInvoices);
    if (typeof window !== 'undefined') {
      localStorage.setItem('oozox_invoices', JSON.stringify(updatedInvoices));
    }
    setShowNewInvoiceModal(false);
    setNewClientName('');
    setNewClientPhone('');
    setNewItemName('');
    setNewItemPrice('');
    setNewPaidAmount('');

    addToast(
      `تم إصدار وحفظ الفاتورة (${invId})`,
      `للعميل: ${newInv.clientName} بقيمة ${newInv.totalAmount.toLocaleString()} ج.م مع كود QR الضريبي`,
      'success'
    );
  };

  // Delete Invoice
  const handleDeleteInvoice = (inv: InvoiceRecord) => {
    const updated = invoices.filter((i) => i.id !== inv.id);
    setInvoices(updated);
    if (typeof window !== 'undefined') {
      localStorage.setItem('oozox_invoices', JSON.stringify(updated));
    }
    addToast('تم حذف الفاتورة', `تم حذف الفاتورة رقم ${inv.invoiceNumber} بنجاح من السجلات`, 'info');
  };

  // Handle Hashtag Submission
  const handleProcessHashtag = (e: React.FormEvent) => {
    e.preventDefault();
    if (!hashtagInput.trim() || !hashtagInput.includes('#')) {
      addToast('صيغة غير صالحة', 'يجب أن تبدأ الرسالة بوسم هاشتاج مثل #ذهب_21 أو #حديد', 'warning');
      return;
    }

    const tagMatch = hashtagInput.match(/#([\p{L}\p{N}_]+)/u);
    const tag = tagMatch ? `#${tagMatch[1]}` : '#معاملة';
    const numbers = hashtagInput.match(/\d+(\.\d+)?/g)?.map(Number) || [];

    let amount = 0;
    let qty: number | undefined;
    let unitPrice: number | undefined;
    let category: 'gold' | 'materials' | 'service' | 'general' = 'general';

    if (tag.includes('ذهب') || tag.includes('عيار')) {
      category = 'gold';
      if (numbers.length >= 2) {
        qty = numbers[0];
        unitPrice = numbers[1];
        amount = qty * unitPrice;
      } else if (numbers.length === 1) {
        amount = numbers[0];
      }
    } else {
      amount = numbers[0] || 0;
    }

    const entry: HashtagEntry = {
      id: `ht-${Date.now()}`,
      rawText: hashtagInput,
      tag,
      category,
      amount,
      quantity: qty,
      unitPrice,
      type: hashtagInput.includes('شراء') ? 'purchase' : 'sale',
      date: 'الآن'
    };

    setHashtagEntries([entry, ...hashtagEntries]);
    setHashtagInput('');

    addToast(
      `تم تسجيل القيد المحاسبي (${tag})`,
      `المبلغ المسجل: ${amount.toLocaleString()} ج.م بالدفتر الآلي`,
      'success'
    );
  };

  // Unlock Vault (creates user PIN on first setup or verifies configured PIN)
  const handleUnlockVault = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = vaultPin.trim();
    if (cleaned.length < 4) {
      setVaultError('يجب أن يتكون رمز PIN من 4 أرقام على الأقل');
      return;
    }
    if (!configuredVaultPin) {
      if (typeof window !== 'undefined') {
        localStorage.setItem('oozox_vault_pin', cleaned);
      }
      setConfiguredVaultPin(cleaned);
      setIsVaultLocked(false);
      setVaultError('');
      addToast('تم تعيين رمز الخزينة وفتحها', 'تم حفظ رمز PIN الخاص بك محلياً وفك قفل الخزينة بأمان', 'success');
      return;
    }
    if (cleaned === configuredVaultPin) {
      setIsVaultLocked(false);
      setVaultError('');
      addToast('تم فك تشفير الخزينة', 'وضع الحسابات السرية والتخفي نشط الآن بأمان', 'success');
    } else {
      setVaultError('رمز PIN غير صحيح.');
      addToast('فشل فتح الخزينة', 'رمز PIN المدخل غير مطابق للرمز المحفوظ', 'error');
    }
  };

  // Lock Vault
  const handleLockVault = () => {
    setIsVaultLocked(true);
    setVaultPin('');
    addToast('تم قفل الخزينة بنجاح', 'تم إخفاء السجلات السرية وإعادة تشفير البيانات', 'info');
  };

  // Copy helper
  const handleCopyText = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    addToast('تم النسخ إلى الحافظة', `تم نسخ ${label} بنجاح`, 'info');
  };

  // WhatsApp Share Helper
  const shareInvoiceWhatsApp = (inv: InvoiceRecord) => {
    const text = `فاتورة إلكترونية معتمدة من OOZOX 🧾\nرقم الفاتورة: ${inv.invoiceNumber}\nالعميل: ${inv.clientName}\nالإجمالي: ${inv.totalAmount.toLocaleString()} ج.م\nالمدفوع: ${inv.paidAmount.toLocaleString()} ج.م\nالمتبقي: ${inv.remainingAmount.toLocaleString()} ج.م\n\nتاريخ الإصدار: ${inv.date}\nعرض وطباعة الفاتورة الأصلية: https://oozox.com/inv/${inv.invoiceNumber}`;
    const cleanPhone = inv.clientPhone.replace(/[^0-9]/g, '');
    const url = `https://wa.me/${cleanPhone}?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank');
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans selection:bg-amber-500 selection:text-black transition-colors duration-300" dir="rtl">
      {/* Top Application Bar */}
      <header className="sticky top-0 z-40 bg-white/80 dark:bg-slate-950/90 border-b border-slate-200 dark:border-slate-800/80 backdrop-blur-xl px-4 sm:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={onNavigateHome}
              className="px-3.5 py-2 rounded-xl bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-300 hover:text-amber-600 dark:hover:text-amber-400 hover:border-amber-500/40 transition-all text-xs font-bold tracking-wide"
              title="العودة للصفحة الرئيسية"
            >
              موقع OOZOX
            </button>
            <div className="h-5 w-px bg-slate-200 dark:bg-slate-800 hidden sm:block" />
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-amber-500 to-amber-300 text-slate-950 font-black text-xs flex items-center justify-center shadow-md">
                OX
              </div>
              <span className="text-sm font-black text-slate-900 dark:text-white tracking-wide">بوابة الإنتاج والمحاسبة الذكية</span>
              <Badge className="bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30 text-[10px] hidden md:inline-flex">
                Production App v3.0
              </Badge>
            </div>
          </div>

          <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
            {/* Transport & Server Field Encryption Status Badge */}
            <div className="hidden md:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 text-xs font-bold shrink-0">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse shrink-0" />
              <span>تشفير TLS / AES-GCM</span>
            </div>

            {authUser && authToken ? (
              <>
                <button
                  onClick={() => setShowTrustedDevicesModal(true)}
                  className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-600 dark:text-amber-300 text-xs font-bold hover:bg-amber-500/25 transition-all shrink-0"
                  title="إدارة الأجهزة الموثوقة وتغيير كلمة المرور"
                >
                  <Laptop className="w-4 h-4" />
                  <span className="hidden sm:inline">{authUser.name || authUser.phone}</span>
                </button>
                <button
                  onClick={() => {
                    clearWebAuthSession('user', false);
                    setAuthToken(null);
                    setAuthUser(null);
                    addToast('تم تسجيل الخروج', 'تم إنهاء الجلسة الحالية مع الإبقاء على ثقة الجهاز وفق السياسة.', 'info');
                  }}
                  className="p-2 rounded-xl bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-300 hover:text-rose-500 transition-all"
                  title="تسجيل الخروج"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </>
            ) : (
              <button
                onClick={() => setShowAuthModal(true)}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-xs transition-all shadow-sm shrink-0"
              >
                <ShieldCheck className="w-4 h-4" />
                <span>دخول / تحقق OTP</span>
              </button>
            )}

            {/* Floating Bubble Summoner */}
            <button
              onClick={() => {
                setBubbleActive(true);
                addToast('تم تفعيل فقاعة المحادثة', 'يمكنك الآن الوصول السريع للمحادثات من أي مكان في النظام.', 'info');
              }}
              className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-amber-600 dark:text-amber-400 hover:bg-amber-500 hover:text-slate-950 transition-all shadow-sm"
              title="فتح فقاعة المحادثة العائمة"
            >
              <MessageCircle className="w-5 h-5" />
            </button>

            {/* Merchant Business Profile */}
            <button
              onClick={() => {
                setProfileForm({ ...profile });
                setShowProfileModal(true);
              }}
              className="px-4 py-2 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-amber-500/50 text-xs font-bold text-slate-700 dark:text-slate-200 hover:text-amber-600 dark:hover:text-amber-400 transition-all duration-200 shrink-0 shadow-sm tracking-wide"
              title="الملف التجاري وإعدادات الفواتير"
            >
              <span className="hidden sm:inline">{profile.businessName || 'الملف التجاري'}</span>
              <span className="sm:hidden">الملف</span>
            </button>

            {/* Central Admin Gateway */}
            <button
              onClick={onNavigateToAdmin}
              className="px-4 py-2 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-amber-500 text-xs font-bold text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white transition-all duration-200 shrink-0 shadow-sm tracking-wide"
              title="الانتقال إلى لوحة التحكم المركزية"
            >
              لوحة الإدارة
            </button>
          </div>
        </div>
      </header>

      {/* Navigation Sub-Tabs */}
      <div className="bg-white/50 dark:bg-slate-900/50 border-b border-slate-200 dark:border-slate-800/60 px-4 sm:px-8 py-3 overflow-x-auto">
        <div className="max-w-7xl mx-auto flex items-center gap-2.5 min-w-max">
          {[
            { id: 'dashboard', label: 'نظرة عامة والسيولة' },
            { id: 'chats', label: 'المحادثات والرسائل', badge: chats.reduce((s, c) => s + c.unreadCount, 0) > 0 ? String(chats.reduce((s, c) => s + c.unreadCount, 0)) : undefined },
            { id: 'invoices', label: 'الفواتير والواتساب', badge: `${invoices.length}` },
            { id: 'hashtag_engine', label: 'الهاشتاج المحاسبي (#)' },
            { id: 'gold_calc', label: 'حاسبة الصاغة والذهب' },
            { id: 'stealth_vault', label: 'الخزينة المشفرة' },
            { id: 'work_tracking', label: 'تتبع الموظفين والموقع', badge: 'GPS' },
            { id: 'career_cv', label: 'السيرة الذاتية والوظائف', badge: 'جديد' },
            { id: 'developer_api', label: 'دليل المطور وAPIs', badge: 'REST' },
          ].map((tab) => {
            const isActive = activeView === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveView(tab.id as any)}
                className={`flex items-center gap-2 px-4 sm:px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide ${
                  isActive
                    ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-500/20'
                    : 'bg-white dark:bg-slate-950/60 text-slate-500 dark:text-slate-400 border border-slate-200 dark:border-slate-800/60 hover:text-slate-900 dark:hover:text-white hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                <span>{tab.label}</span>
                {tab.badge && (
                  <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-bold ${
                    isActive ? 'bg-slate-950 text-amber-400' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                  }`}>
                    {tab.badge}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-8 py-8 sm:py-10">
        
        {/* VIEW 0: CHATS / CONVERSATIONS (INBOX) */}
        {activeView === 'chats' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="border-b border-slate-200 dark:border-slate-800 pb-5 flex items-center justify-between">
              <div>
                <h2 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight leading-snug">مركز المحادثات والرسائل</h2>
                <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5 leading-relaxed">تواصل مع العملاء والموردين وقم بتحويل الدردشة إلى قيود محاسبية فورية.</p>
              </div>
              <Button className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs px-5 rounded-xl hidden sm:flex items-center gap-2">
                <Plus className="w-4 h-4" />
                محادثة جديدة
              </Button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Inbox List Side */}
              <div className="lg:col-span-1 space-y-3">
                <div className="flex items-center justify-between mb-4 px-1">
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                    <span className="text-[10px] font-black text-slate-500 dark:text-slate-400 uppercase tracking-[0.2em]">الرسائل الواردة</span>
                  </div>
                  <span className="text-[10px] bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 font-black px-2.5 py-1 rounded-full shadow-sm">
                    {chats.reduce((s, c) => s + c.unreadCount, 0)} غير مقروء
                  </span>
                </div>
                {chats.length === 0 ? (
                  <div className="p-6 rounded-[24px] border border-dashed border-slate-200 dark:border-slate-800 text-center text-xs text-slate-500 dark:text-slate-400">
                    لا توجد محادثات واردة بعد
                  </div>
                ) : (
                  chats.map((chat) => (
                    <div
                      key={chat.id}
                      onClick={() => setSelectedConversationId(chat.id)}
                      className={`p-4 rounded-[24px] border transition-all cursor-pointer flex items-center gap-4 group relative ${
                        selectedConversationId === chat.id
                          ? 'bg-amber-500/10 border-amber-500/50'
                          : 'bg-white/80 dark:bg-slate-900/40 border-slate-200 dark:border-slate-800 hover:bg-white dark:hover:bg-slate-900'
                      }`}
                    >
                      <div className="relative shrink-0">
                        <div className={`w-14 h-14 rounded-[18px] ${chat.avatarColor} text-white flex items-center justify-center text-xl font-black shadow-lg overflow-hidden`}>
                          <span className="relative z-10">{chat.name.charAt(0)}</span>
                        </div>
                        {chat.isOnline && (
                          <span className="absolute -bottom-0.5 -left-0.5 w-4 h-4 bg-emerald-500 border-2 border-white dark:border-slate-900 rounded-full shadow-sm" />
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-1">
                          <h4 className="text-[15px] font-extrabold text-slate-900 dark:text-white truncate">{chat.name}</h4>
                          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 px-2 py-0.5 rounded-lg">{chat.time}</span>
                        </div>
                        <p className="text-[13px] text-slate-600 dark:text-slate-400 truncate leading-relaxed font-medium">
                          {chat.lastMessage}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* Conversation Thread + 1-on-1 Audio/Video Calls */}
              {selectedConversationId && authToken ? (
                (() => {
                  const activeChat = chats.find((c) => c.id === selectedConversationId);
                  return (
                    <div className="lg:col-span-2 bg-slate-900/80 border border-slate-800 rounded-[32px] flex flex-col min-h-[500px] overflow-hidden">
                      <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
                        <div>
                          <h3 className="text-base font-black text-white">{activeChat?.name || 'محادثة'}</h3>
                          <span className="text-[11px] text-emerald-400">مراسلة ومكالمات 1-on-1 مشفرة</span>
                        </div>
                        {activeChat?.peerId && (
                          <div className="flex items-center gap-2">
                            <Button
                              size="sm"
                              onClick={() =>
                                setActiveCallSession({
                                  callId: `web_${Date.now()}`,
                                  peerId: activeChat.peerId,
                                  peerName: activeChat.name,
                                  callType: 'AUDIO',
                                  isIncoming: false,
                                })
                              }
                              className="bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs rounded-xl gap-1.5"
                            >
                              <Phone className="w-3.5 h-3.5" />
                              صوتية
                            </Button>
                            <Button
                              size="sm"
                              onClick={() =>
                                setActiveCallSession({
                                  callId: `web_${Date.now()}`,
                                  peerId: activeChat.peerId,
                                  peerName: activeChat.name,
                                  callType: 'VIDEO',
                                  isIncoming: false,
                                })
                              }
                              className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-xs rounded-xl gap-1.5"
                            >
                              <Video className="w-3.5 h-3.5" />
                              فيديو
                            </Button>
                          </div>
                        )}
                      </div>
                      <div className="flex-1 p-5 space-y-3 overflow-y-auto max-h-[380px]">
                        {activeMessages.length === 0 ? (
                          <p className="text-xs text-slate-500 text-center py-10">لا توجد رسائل في هذه المحادثة بعد.</p>
                        ) : (
                          activeMessages.map((m) => {
                            const isMe = m.senderId === authUser?.id;
                            return (
                              <div key={m._id} className={`flex ${isMe ? 'justify-start' : 'justify-end'}`}>
                                <div className={`px-4 py-2.5 rounded-2xl text-xs max-w-[75%] ${
                                  isMe ? 'bg-amber-400 text-slate-950 font-bold' : 'bg-slate-800 text-slate-100'
                                }`}>
                                  {m.content}
                                </div>
                              </div>
                            );
                          })
                        )}
                      </div>
                      <form
                        onSubmit={async (e) => {
                          e.preventDefault();
                          const trimmed = chatComposer.trim();
                          if (!trimmed || !authToken || !selectedConversationId) return;
                          const res = await apiSendMessage(authToken, selectedConversationId, trimmed);
                          if (res.ok && res.data?.message) {
                            setActiveMessages((prev) => [...prev, res.data!.message]);
                            setChatComposer('');
                          } else {
                            addToast('تعذر الإرسال', res.message, 'error');
                          }
                        }}
                        className="p-4 border-t border-slate-800 flex gap-2"
                      >
                        <Input
                          value={chatComposer}
                          onChange={(e) => setChatComposer(e.target.value)}
                          placeholder="اكتب رسالتك هنا..."
                          className="bg-slate-950 border-slate-800 text-xs text-white"
                        />
                        <Button type="submit" className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-xs">
                          <Send className="w-4 h-4" />
                        </Button>
                      </form>
                    </div>
                  );
                })()
              ) : (
                <div className="lg:col-span-2 bg-white/40 dark:bg-slate-900/40 border border-slate-200 dark:border-slate-800 rounded-[32px] p-10 flex flex-col items-center justify-center text-center space-y-8 min-h-[500px] shadow-sm backdrop-blur-sm relative overflow-hidden">
                  <div className="absolute top-0 right-0 w-64 h-64 bg-amber-500/5 blur-[100px] -mr-32 -mt-32" />
                  <div className="absolute bottom-0 left-0 w-64 h-64 bg-teal-500/5 blur-[100px] -ml-32 -mb-32" />
                  <div className="relative">
                    <div className="w-24 h-24 rounded-[32px] bg-white dark:bg-slate-950 border border-slate-100 dark:border-slate-800 flex items-center justify-center text-amber-500 shadow-xl shadow-amber-500/5 rotate-3">
                      <MessageSquare className="w-10 h-10" />
                    </div>
                    <div className="absolute -bottom-2 -right-2 w-10 h-10 rounded-2xl bg-teal-500 text-white flex items-center justify-center shadow-lg -rotate-12 border-2 border-white dark:border-slate-900">
                      <Sparkles className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="max-w-xs space-y-3">
                    <h3 className="text-xl font-black text-slate-900 dark:text-white tracking-tight">ابدأ المحادثة الآن</h3>
                    <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed font-medium">
                      سجل الدخول برقم هاتفك واختر محادثة لعرض الرسائل وإجراء مكالمات صوت وفيديو 1-on-1.
                    </p>
                  </div>
                  <div className="flex flex-col sm:flex-row items-center gap-3">
                    <Button
                      onClick={() => setActiveView('hashtag_engine')}
                      className="bg-slate-900 dark:bg-white text-white dark:text-slate-950 font-black text-xs px-8 py-3 rounded-2xl shadow-xl hover:scale-105 transition-all"
                    >
                      فتح محرك الهاشتاج
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* VIEW 1: DASHBOARD */}
        {activeView === 'dashboard' && (
          <div className="space-y-8 animate-fadeIn">
            {/* Top Metrics Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
              <Card className="bg-white dark:bg-slate-900/80 border-slate-200 dark:border-slate-800 shadow-xl dark:shadow-none">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 text-xs sm:text-sm font-bold mb-2.5">
                    <span>إجمالي المبيعات المؤكدة</span>
                    <span className="text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-md text-xs">محدث فورياً</span>
                  </div>
                  <div className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-white tracking-tight font-mono tabular-nums">
                    {invoices.reduce((sum, inv) => sum + inv.totalAmount, 0).toLocaleString()} <span className="text-xs sm:text-sm text-amber-600 dark:text-amber-400 font-sans font-bold">ج.م</span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 font-medium">من إجمالي {invoices.length} فواتير مسجلة</p>
                </CardContent>
              </Card>

              <Card className="bg-white dark:bg-slate-900/80 border-slate-200 dark:border-slate-800 shadow-xl dark:shadow-none">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 text-xs sm:text-sm font-bold mb-2.5">
                    <span>الديون والمستحقات الآجلة</span>
                    <span className="text-red-500 dark:text-red-400 bg-red-500/10 px-2 py-0.5 rounded-md text-xs">
                      {invoices.filter((i) => i.remainingAmount > 0).length} عميل
                    </span>
                  </div>
                  <div className="text-3xl sm:text-4xl font-black text-red-600 dark:text-red-400 tracking-tight font-mono tabular-nums">
                    {invoices.reduce((sum, inv) => sum + inv.remainingAmount, 0).toLocaleString()} <span className="text-xs sm:text-sm text-slate-400 dark:text-slate-300 font-sans font-bold">ج.م</span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 font-medium">تذكير واتساب بنقرة واحدة</p>
                </CardContent>
              </Card>

              <Card className="bg-white dark:bg-slate-900/80 border-slate-200 dark:border-slate-800 shadow-xl dark:shadow-none">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 text-xs sm:text-sm font-bold mb-2.5">
                    <span>ذهب عيار 21 اليوم</span>
                    <span className="text-amber-600 dark:text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-md text-xs">الصاغة</span>
                  </div>
                  <div className="text-3xl sm:text-4xl font-black text-amber-500 tracking-tight font-mono tabular-nums">
                    {goldPricesBase['21'] > 0 ? goldPricesBase['21'].toLocaleString() : '—'} <span className="text-xs sm:text-sm text-slate-400 dark:text-slate-300 font-sans font-bold">ج.م / جم</span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 font-medium">أدخل أسعار اليوم في حاسبة الذهب</p>
                </CardContent>
              </Card>

              <Card className="bg-white dark:bg-slate-900/80 border-slate-200 dark:border-slate-800 shadow-xl dark:shadow-none">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 text-xs sm:text-sm font-bold mb-2.5">
                    <span>حالة النظام والتشفير</span>
                    <span className="text-emerald-600 dark:text-emerald-400 text-xs font-mono font-bold">● AES-256</span>
                  </div>
                  <div className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2 mt-1 tracking-tight">
                    <ShieldCheck className="w-6 h-6 text-emerald-600 dark:text-emerald-400" />
                    <span>جاهز للإنتاج</span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 font-medium">قاعدة بيانات Room مشفرة محلياً (Offline-First)</p>
                </CardContent>
              </Card>
            </div>

            {/* Quick Action Banner */}
            <div className="p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-amber-500/5 via-white dark:via-slate-900 to-teal-500/5 border border-slate-200 dark:border-amber-500/30 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-5">
              <div className="space-y-1">
                <h3 className="text-lg sm:text-xl font-bold text-slate-900 dark:text-white tracking-tight">إصدار فاتورة إلكترونية سريعة ومشاركتها عبر واتساب</h3>
                <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed">أنشئ الفاتورة، وقم بحساب الضرائب، وإرسال رابط PDF المباشر للعميل خلال ثوانٍ.</p>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <Button
                  onClick={() => setShowNewInvoiceModal(true)}
                  className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs sm:text-sm px-6 py-3 rounded-xl shadow-md shadow-amber-500/20 tracking-wide transition-all"
                >
                  فاتورة جديدة الآن
                </Button>
                <Button
                  onClick={() => setActiveView('hashtag_engine')}
                  variant="outline"
                  className="bg-white dark:bg-transparent border-slate-200 dark:border-slate-700 hover:border-amber-500 text-xs sm:text-sm text-slate-700 dark:text-slate-200 px-5 py-3 rounded-xl font-bold tracking-wide transition-all"
                >
                  تجربة الهاشتاج (#)
                </Button>
              </div>
            </div>

            {/* Job Matches (Conditional) */}
            {jobs.filter(j => 
              profile.jobTitle && (
                j.title.toLowerCase().includes(profile.jobTitle.toLowerCase()) || 
                profile.jobTitle.toLowerCase().includes(j.title.toLowerCase())
              )
            ).length > 0 && (
              <div className="p-6 rounded-3xl bg-amber-500/5 border border-amber-500/20 shadow-sm animate-fadeIn">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white">وظائف جديدة تناسب ملفك الشخصي</h3>
                  </div>
                  <span className="text-[10px] bg-amber-500/10 text-amber-600 dark:text-amber-400 font-black px-2 py-0.5 rounded-lg border border-amber-500/20">تنبيه ذكي</span>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {jobs.filter(j => 
                    profile.jobTitle && (
                      j.title.toLowerCase().includes(profile.jobTitle.toLowerCase()) || 
                      profile.jobTitle.toLowerCase().includes(j.title.toLowerCase())
                    )
                  ).slice(0, 2).map(job => (
                    <div key={job.id} className="bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl flex items-center justify-between group">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 flex items-center justify-center text-amber-500 shadow-inner">
                          <Briefcase className="w-5 h-5" />
                        </div>
                        <div>
                          <h4 className="text-xs font-bold text-slate-900 dark:text-white">{job.title}</h4>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400">{job.company} • {job.location}</p>
                        </div>
                      </div>
                      <Button size="sm" variant="ghost" className="text-[10px] font-bold text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 h-8">عرض التفاصيل</Button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Recent Invoices Feed */}
            <Card className="bg-white dark:bg-slate-900/60 border-slate-200 dark:border-slate-800 shadow-xl dark:shadow-none">
              <CardHeader className="flex flex-row items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800/80 px-6 pt-6">
                <div>
                  <CardTitle className="text-base sm:text-lg text-slate-900 dark:text-white font-bold tracking-tight">آخر الفواتير الصادرة</CardTitle>
                  <CardDescription className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-0.5">سجل الفواتير المتزامنة مع التقرير المحاسبي</CardDescription>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setActiveView('invoices')}
                  className="text-xs sm:text-sm text-amber-600 dark:text-amber-400 hover:text-slate-900 dark:hover:text-white font-bold tracking-wide px-3 py-1.5"
                >
                  عرض كافة الفواتير
                </Button>
              </CardHeader>
              <CardContent className="p-4 sm:p-6">
                {invoices.length === 0 ? (
                  <div className="p-8 text-center text-xs sm:text-sm text-slate-500 dark:text-slate-400 border border-dashed border-slate-200 dark:border-slate-800 rounded-2xl">
                    لا توجد فواتير صادرة حتى الآن. اضغط على «فاتورة جديدة الآن» لإصدار أول فاتورة.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {invoices.map((inv) => (
                      <div key={inv.id} className="p-4 bg-white dark:bg-slate-950 border border-slate-100 dark:border-slate-800/80 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 flex items-center justify-center text-amber-600 dark:text-amber-400 shadow-inner">
                            <FileText className="w-5 h-5" />
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-slate-900 dark:text-white">{inv.clientName}</span>
                              <span className="text-[10px] text-slate-400 dark:text-slate-400 font-mono">({inv.invoiceNumber})</span>
                            </div>
                            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                              {inv.items.map(i => i.name).join(' • ')}
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end">
                          <div className="text-left">
                            <div className="text-xs font-bold text-slate-900 dark:text-white">{inv.totalAmount.toLocaleString()} ج.م</div>
                            <span className={`text-[10px] font-bold ${
                              inv.status === 'paid' ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-500 dark:text-red-400'
                            }`}>
                              {inv.status === 'paid' ? 'تم السداد بالكامل' : `متبقي ${inv.remainingAmount.toLocaleString()} ج.م`}
                            </span>
                          </div>
                          <Button
                            size="sm"
                            onClick={() => shareInvoiceWhatsApp(inv)}
                            className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs h-8 px-4 rounded-xl font-bold"
                          >
                            واتساب
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        )}

        {/* VIEW 2: INVOICES MANAGER */}
        {activeView === 'invoices' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-5">
              <div>
                <h2 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight leading-snug">إدارة الفواتير والتحصيل الذكي</h2>
                <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5 leading-relaxed">توليد ومتابعة الفواتير المعتمدة ومشاركتها عبر الواتساب مع كود QR المعتمد.</p>
              </div>
              <Button
                onClick={() => setShowNewInvoiceModal(true)}
                className="bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 font-black text-xs sm:text-sm px-6 py-3 rounded-xl shadow-lg shadow-amber-500/20 tracking-wide transition-all self-start sm:self-auto"
              >
                إنشاء فاتورة جديدة
              </Button>
            </div>

            {invoices.length === 0 ? (
              <div className="p-10 rounded-3xl border border-dashed border-slate-200 dark:border-slate-800 text-center space-y-3">
                <p className="text-sm font-bold text-slate-700 dark:text-slate-200">سجل الفواتير فارغ</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">ابدأ بإنشاء أول فاتورة إلكترونية لتظهر هنا مع رمز التحقق.</p>
              </div>
            ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {invoices.map((inv) => (
                <div key={inv.id} className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-7 shadow-xl dark:shadow-none flex flex-col justify-between">
                  <div>
                    {/* Invoice Header */}
                    <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4 mb-5">
                      <div>
                        <span className="text-xs text-amber-600 dark:text-amber-400 font-mono font-bold bg-amber-500/10 px-2.5 py-1 rounded-lg border border-amber-500/20">
                          {inv.invoiceNumber}
                        </span>
                        <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white mt-2 tracking-tight">{inv.clientName}</h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400 font-mono mt-0.5">{inv.clientPhone}</p>
                      </div>
                      <div className="text-left">
                        <img 
                          src={inv.qrCodeUrl} 
                          alt="QR Verification" 
                          className="w-14 h-14 bg-white p-1 rounded-xl shadow-md inline-block border border-slate-100"
                        />
                        <div className="text-[10px] text-slate-400 dark:text-slate-400 mt-1 font-mono uppercase tracking-widest">QR ضريبي</div>
                      </div>
                    </div>

                    {/* Items List */}
                    <div className="space-y-2 mb-5">
                      <span className="text-xs font-bold text-slate-400 tracking-wide uppercase">بنود المعاملة:</span>
                      {inv.items.map((item) => (
                        <div key={item.id} className="flex items-center justify-between text-xs sm:text-sm bg-slate-50 dark:bg-slate-950 p-3 rounded-xl border border-slate-100 dark:border-slate-800/80">
                          <span className="text-slate-700 dark:text-slate-200 font-medium">{item.name} (x{item.quantity})</span>
                          <span className="font-bold text-amber-600 dark:text-amber-400 font-mono tabular-nums">{item.total.toLocaleString()} ج.م</span>
                        </div>
                      ))}
                    </div>

                    {/* Financial Summary */}
                    <div className="bg-slate-50/80 dark:bg-slate-950/80 p-4 rounded-2xl border border-slate-100 dark:border-slate-800 space-y-2 text-xs sm:text-sm mb-5">
                      <div className="flex justify-between text-slate-500 dark:text-slate-400">
                        <span>المجموع الفرعي:</span>
                        <span className="font-mono tabular-nums">{inv.subtotal.toLocaleString()} ج.م</span>
                      </div>
                      {inv.vatAmount > 0 && (
                        <div className="flex justify-between text-slate-500 dark:text-slate-400">
                          <span>ضريبة القيمة المضافة (14%):</span>
                          <span className="font-mono tabular-nums">{inv.vatAmount.toLocaleString()} ج.م</span>
                        </div>
                      )}
                      <div className="flex justify-between text-slate-900 dark:text-white font-bold pt-2 border-t border-slate-200 dark:border-slate-800 text-sm sm:text-base">
                        <span>الإجمالي الصافي:</span>
                        <span className="text-amber-600 dark:text-amber-400 font-mono tabular-nums">{inv.totalAmount.toLocaleString()} ج.م</span>
                      </div>
                      <div className="flex justify-between text-xs font-medium pt-1">
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold">المدفوع: {inv.paidAmount.toLocaleString()} ج.م</span>
                        <span className={`font-bold ${inv.remainingAmount > 0 ? 'text-red-500 dark:text-red-400' : 'text-slate-400'}`}>
                          المتبقي: {inv.remainingAmount.toLocaleString()} ج.م
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 pt-4 border-t border-slate-100 dark:border-slate-800">
                    <Button
                      onClick={() => shareInvoiceWhatsApp(inv)}
                      className="flex-1 bg-emerald-600 hover:bg-emerald-500 text-white text-xs sm:text-sm font-bold h-10 px-4 rounded-xl tracking-wide transition-colors"
                    >
                      <span className="hidden sm:inline">إرسال الفاتورة عبر WhatsApp</span>
                      <span className="sm:hidden">واتساب</span>
                    </Button>
                    <Button
                      onClick={() => handleCopyText(`https://oozox.com/inv/${inv.invoiceNumber}`, 'رابط الفاتورة')}
                      variant="outline"
                      className="bg-white dark:bg-transparent border-slate-200 dark:border-slate-700 hover:border-amber-500 text-xs sm:text-sm h-10 px-3.5 font-bold rounded-xl transition-colors text-slate-700 dark:text-slate-200"
                      title="نسخ رابط الفاتورة"
                    >
                      نسخ الرابط
                    </Button>
                    <Button
                      onClick={() => addToast('جاري التوليد', `جاري تجهيز وتحميل ملف PDF للفاتورة (${inv.invoiceNumber})`, 'info')}
                      variant="outline"
                      className="bg-white dark:bg-transparent border-slate-200 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 text-xs sm:text-sm h-10 px-3.5 font-bold rounded-xl transition-colors text-slate-700 dark:text-slate-200"
                      title="تحميل PDF"
                    >
                      تحميل PDF
                    </Button>
                    <Button
                      onClick={() => handleDeleteInvoice(inv)}
                      variant="ghost"
                      className="text-red-500 dark:text-red-400 hover:text-red-600 dark:hover:text-red-300 hover:bg-red-500/5 text-xs sm:text-sm h-10 px-3 font-bold rounded-xl transition-colors"
                      title="حذف الفاتورة"
                    >
                      حذف
                    </Button>
                  </div>
                </div>
              ))}
            </div>
            )}
          </div>
        )}

        {/* VIEW 3: HASHTAG IN-CHAT ACCOUNTING SIMULATOR */}
        {activeView === 'hashtag_engine' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="border-b border-slate-200 dark:border-slate-800 pb-5">
              <span className="text-amber-600 dark:text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20 font-mono">
                Smart In-Chat Accounting
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white mt-2.5 tracking-tight leading-snug">محرك تحويل الهاشتاج إلى قيود محاسبية فورية</h2>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5 leading-relaxed">
                اكتب أي رسالة تبدأ بـ # واسم الصنف والمبلغ وسيقوم نظام OOZOX باحتساب الدائن والمدين والمتبقي آلياً.
              </p>
            </div>

            {/* Input Bar */}
            <Card className="bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 shadow-sm">
              <CardContent className="p-6">
                <form onSubmit={handleProcessHashtag} className="flex flex-col sm:flex-row gap-3">
                  <Input
                    type="text"
                    value={hashtagInput}
                    onChange={(e) => setHashtagInput(e.target.value)}
                    placeholder="مثال: #ذهب_21 <الوزن_جم> <سعر_الجرام> أو #توريد <المبلغ> مدفوع <المبلغ>"
                    className="bg-slate-50 dark:bg-slate-950 border-slate-200 dark:border-slate-800 text-slate-900 dark:text-white font-medium text-xs sm:text-sm py-3"
                  />
                  <Button type="submit" className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs px-6 whitespace-nowrap h-10 sm:h-auto rounded-xl shadow-md shadow-amber-500/10 transition-all">
                    تحليل وإضافة قيد
                  </Button>
                </form>

                <div className="flex flex-wrap items-center gap-2 mt-4 text-[11px] text-slate-400">
                  <span className="font-bold text-slate-500 dark:text-slate-300 uppercase tracking-widest text-[9px]">صيغة سريعة:</span>
                  <button 
                    type="button"
                    onClick={() => setHashtagInput('#ذهب_21 10g <سعر_الجرام> كاش')}
                    className="px-2.5 py-1.5 bg-slate-50 dark:bg-slate-950 hover:bg-white dark:hover:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-800 text-amber-600 dark:text-amber-400 font-bold transition-all"
                  >
                    #ذهب_21 &lt;الوزن&gt;g &lt;السعر&gt; كاش
                  </button>
                  <button 
                    type="button"
                    onClick={() => setHashtagInput('#توريد 100وحده 250 كاش')}
                    className="px-2.5 py-1.5 bg-slate-50 dark:bg-slate-950 hover:bg-white dark:hover:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-800 text-teal-600 dark:text-teal-400 font-bold transition-all"
                  >
                    #توريد &lt;الكمية&gt; &lt;السعر&gt;
                  </button>
                </div>
              </CardContent>
            </Card>

            {/* Generated Ledger Feed */}
            <div className="space-y-3">
              <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest mb-4">القيود المولدة من الدردشة ({hashtagEntries.length}):</h3>
              {hashtagEntries.map((entry) => (
                <div key={entry.id} className="p-4 bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm transition-all hover:shadow-md">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-bold text-amber-600 dark:text-amber-400 bg-amber-500/10 px-3 py-1.5 rounded-xl border border-amber-500/20 font-mono">
                      {entry.tag}
                    </span>
                    <div>
                      <p className="text-xs font-bold text-slate-900 dark:text-white">{entry.rawText}</p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                        التصنيف: {entry.category === 'gold' ? 'صاغة وذهب' : 'مواد وتجارة'} • التوقيت: {entry.date}
                      </p>
                    </div>
                  </div>

                  <div className="text-left w-full sm:w-auto flex sm:flex-col justify-between items-end">
                    <span className="text-sm font-black text-amber-600 dark:text-amber-400 tabular-nums">{entry.amount.toLocaleString()} ج.م</span>
                    <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3" />
                      قيد آلي مسجل
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* VIEW 4: GOLD KARAT & CRAFTSMANSHIP CALCULATOR */}
        {activeView === 'gold_calc' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="border-b border-slate-800 pb-5">
              <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20 font-mono">
                Gold & Jewelry Suite
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-white mt-2.5 tracking-tight leading-snug">حاسبة تسعير الذهب والمصنعية والدمغة الحية</h2>
              <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">حساب دقيق لأسعار الذهب، المصنعيات، والتقاص لصاغة وتجار المشغولات.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Form Controls */}
              <Card className="lg:col-span-2 bg-slate-900 border-slate-800">
                <CardContent className="p-6 sm:p-8 space-y-5">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الوزن بالجرام *</label>
                      <Input
                        type="number"
                        step="0.01"
                        value={goldWeight}
                        onChange={(e) => setGoldWeight(e.target.value)}
                        placeholder="مثال: 12.5"
                        className="bg-slate-950 border-slate-800 text-white font-bold text-xs sm:text-sm py-3"
                      />
                    </div>

                    <div>
                      <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">عيار الذهب *</label>
                      <select
                        value={goldKarat}
                        onChange={(e) => setGoldKarat(e.target.value as any)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:outline-none focus:border-amber-400 font-bold"
                      >
                        <option value="21">عيار 21</option>
                        <option value="18">عيار 18</option>
                        <option value="24">عيار 24</option>
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">سعر جرام عيار {goldKarat} الخام (ج.م) *</label>
                      <Input
                        type="number"
                        value={goldPricesBase[goldKarat] || ''}
                        onChange={(e) => setGoldPricesBase({ ...goldPricesBase, [goldKarat]: parseFloat(e.target.value) || 0 })}
                        placeholder="أدخل سعر الجرام الحالي"
                        className="bg-slate-950 border-slate-800 text-white text-xs sm:text-sm py-3"
                      />
                    </div>
                    <div>
                      <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">المصنعية والدمغة لكل جرام (ج.م)</label>
                      <Input
                        type="number"
                        value={craftsmanshipPerGram}
                        onChange={(e) => setCraftsmanshipPerGram(e.target.value)}
                        placeholder="أدخل قيمة المصنعية"
                        className="bg-slate-950 border-slate-800 text-white text-xs sm:text-sm py-3"
                      />
                    </div>
                  </div>

                  <div className="pt-3 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-xs sm:text-sm text-slate-400 border-t border-slate-800">
                    <span>قراءة شاشة الميزان بكاميرا الذكاء الاصطناعي:</span>
                    <button 
                      onClick={() => alert('ميزة AI Scale OCR تتيح مسح أرقام الميزان الحساس مباشرة بكاميرا الهاتف وتعبئة الوزن آلياً في تطبيق Android.')}
                      className="text-amber-400 hover:underline font-bold"
                    >
                      تجربة الكاميرا الذكية (AI OCR)
                    </button>
                  </div>
                </CardContent>
              </Card>

              {/* Calculation Output Card */}
              <div className="bg-gradient-to-b from-amber-500/20 via-slate-900 to-slate-950 border border-amber-500/40 rounded-3xl p-6 sm:p-7 shadow-2xl flex flex-col justify-between">
                <div>
                  <span className="text-xs text-amber-400 font-mono font-bold uppercase tracking-wider bg-amber-500/10 px-2.5 py-1 rounded-md border border-amber-500/20">
                    التسعير الإجمالي الفوري
                  </span>
                  <div className="text-3xl sm:text-4xl font-black text-white mt-4 font-mono tabular-nums">
                    {calculatedGoldTotal.grandTotal.toLocaleString()} <span className="text-xs sm:text-sm text-amber-400 font-sans font-bold">ج.م</span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1 font-medium">سعر الجرام النهائي: {calculatedGoldTotal.gramTotal.toLocaleString()} ج.م</p>

                  <div className="space-y-2.5 mt-6 pt-6 border-t border-slate-800/80 text-xs sm:text-sm">
                    <div className="flex justify-between text-slate-400">
                      <span>قيمة الذهب الصافي ({goldWeight} جم):</span>
                      <span className="text-white font-bold font-mono tabular-nums">{calculatedGoldTotal.rawGoldPrice.toLocaleString()} ج.م</span>
                    </div>
                    <div className="flex justify-between text-slate-400">
                      <span>إجمالي المصنعية والدمغة:</span>
                      <span className="text-amber-400 font-bold font-mono tabular-nums">{calculatedGoldTotal.craftsmanshipTotal.toLocaleString()} ج.م</span>
                    </div>
                  </div>
                </div>

                <Button
                  onClick={() => {
                    setNewItemName(`ذهب عيار ${goldKarat} (وزن ${goldWeight} جم)`);
                    setNewItemPrice(String(calculatedGoldTotal.grandTotal));
                    setShowNewInvoiceModal(true);
                  }}
                  className="mt-6 w-full bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs sm:text-sm py-3.5 rounded-xl shadow-lg shadow-amber-500/20 tracking-wide transition-all"
                >
                  تحويل لفاتورة إلكترونية سريعة
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 5: ZERO-TRACE STEALTH VAULT */}
        {activeView === 'stealth_vault' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="border-b border-slate-800 pb-5">
              <span className="text-teal-400 text-xs font-bold uppercase tracking-wider bg-teal-500/10 px-3 py-1 rounded-full border border-teal-500/20 font-mono">
                Military-Grade Privacy
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-white mt-2.5 tracking-tight leading-snug">الخزائن المشفرة بنظام التخفي التام (Zero-Trace)</h2>
              <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">دفاتر وحسابات سرية لا تترك أي إشعار أو أثر مرئي ومحمية برمز PIN وتشفير AES-256.</p>
            </div>

            {isVaultLocked ? (
              <div className="max-w-md mx-auto bg-slate-900 border border-slate-800 rounded-3xl p-8 sm:p-10 shadow-2xl text-center">
                <div className="w-16 h-16 rounded-3xl bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center justify-center mx-auto mb-4 text-2xl">
                  <Lock className="w-8 h-8" />
                </div>
                <h3 className="text-lg sm:text-xl font-bold text-white mb-1.5 tracking-tight">
                  {configuredVaultPin ? 'الخزينة المشفرة مقفلة' : 'تعيين رمز قفل الخزينة'}
                </h3>
                <p className="text-xs sm:text-sm text-slate-400 mb-6 leading-relaxed">
                  {configuredVaultPin
                    ? 'أدخل رمز PIN السري لفك تشفير وعرض السجلات السرية'
                    : 'أدخل رمز PIN جديد (4 أرقام على الأقل) لحماية سجلات الخزينة'}
                </p>

                <form onSubmit={handleUnlockVault} className="space-y-4">
                  <Input
                    type="password"
                    maxLength={8}
                    value={vaultPin}
                    onChange={(e) => setVaultPin(e.target.value)}
                    placeholder="أدخل رمز PIN"
                    className="text-center text-lg tracking-widest bg-slate-950 border-slate-800 text-white font-bold py-3"
                  />
                  {vaultError && <p className="text-xs text-red-400 font-semibold">{vaultError}</p>}
                  <Button type="submit" className="w-full bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-xs sm:text-sm py-3.5 rounded-xl shadow-lg shadow-amber-500/20 tracking-wide transition-all">
                    {configuredVaultPin ? 'فتح الخزينة' : 'حفظ الرمز وفتح الخزينة'}
                  </Button>
                </form>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-between bg-emerald-500/10 border border-emerald-500/30 p-4 rounded-2xl">
                  <div className="flex items-center gap-2 text-emerald-400 text-xs sm:text-sm font-bold">
                    <ShieldCheck className="w-5 h-5" />
                    <span>تم فك تشفير الخزينة بنجاح (وضع العرض الآمن نشط)</span>
                  </div>
                  <Button
                    size="sm"
                    onClick={handleLockVault}
                    variant="outline"
                    className="border-emerald-500/40 text-emerald-400 text-xs sm:text-sm hover:bg-emerald-500/20 px-3.5 py-2 font-bold rounded-xl"
                  >
                    قفل الخزينة فورياً
                  </Button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {stealthNotes.map((note) => (
                    <div key={note.id} className="p-5 sm:p-6 bg-slate-900 border border-slate-800 rounded-2xl">
                      <span className="text-xs text-slate-500 font-mono">{note.date}</span>
                      <h4 className="text-base font-bold text-white mt-1.5 tracking-tight">{note.label}</h4>
                      <div className="text-2xl sm:text-3xl font-black text-amber-400 mt-2.5 font-mono tabular-nums">{note.balance}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* VIEW 6: CAREER & CV MANAGER */}
        {activeView === 'career_cv' && (
          <div className="space-y-8 animate-fadeIn">
            <div className="border-b border-slate-800 pb-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20 font-mono">
                  Career Intelligence & CV Builder
                </span>
                <h2 className="text-2xl sm:text-3xl font-black text-white mt-2.5 tracking-tight leading-snug">إدارة المسار المهني والسيرة الذاتية الذكية</h2>
                <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">قم ببناء ملفك المهني، اختر مسمى وظيفي دقيق، وشارك سيرتك الذاتية المنسقة مع الشركات لزيادة فرص العمل.</p>
              </div>
              <Button
                onClick={handleShareCV}
                className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-xs sm:text-sm px-6 py-3 rounded-xl shadow-lg shadow-amber-500/20 tracking-wide transition-all self-start md:self-auto flex items-center gap-2"
              >
                <Share2 className="w-4 h-4" />
                مشاركة الـ CV
              </Button>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-12 gap-8">
              {/* Profile Editor Side */}
              <div className="xl:col-span-5 space-y-6">
                <Card className="bg-slate-900 border-slate-800 overflow-hidden">
                  <CardHeader className="bg-slate-950/50 border-b border-slate-800 pb-4">
                    <CardTitle className="text-sm font-bold text-white flex items-center gap-2">
                      <UserCheck className="w-4 h-4 text-amber-400" />
                      إعدادات الهوية المهنية
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-6 space-y-5">
                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-2">نوع الحساب</label>
                      <div className="grid grid-cols-2 gap-2">
                        {[
                          { id: 'individual', label: 'فرد' },
                          { id: 'freelancer', label: 'عمل حر' },
                          { id: 'professional', label: 'محترف / خبير' },
                          { id: 'business', label: 'منشأة تجارية' }
                        ].map((type) => (
                          <button
                            key={type.id}
                            onClick={() => setProfileForm({ ...profileForm, accountType: type.id as any })}
                            className={`px-3 py-2.5 rounded-xl border text-xs font-bold transition-all ${
                              profileForm.accountType === type.id
                                ? 'bg-amber-500/10 border-amber-500/50 text-amber-400'
                                : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                            }`}
                          >
                            {type.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-2">المسمى الوظيفي / التخصص المهني</label>
                      <div className="space-y-4">
                        {!isCustomJob ? (
                          <div className="space-y-4">
                            {/* Categories Grid */}
                            <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                              {professionalCategories.map((cat) => (
                                <button
                                  key={cat.id}
                                  onClick={() => setSelectedCategoryId(selectedCategoryId === cat.id ? null : cat.id)}
                                  className={`flex flex-col items-center justify-center p-2 rounded-xl border transition-all gap-1.5 ${
                                    selectedCategoryId === cat.id
                                      ? 'bg-amber-500/10 border-amber-500/50 text-amber-400'
                                      : 'bg-slate-950 border-slate-800 text-slate-500 hover:border-slate-700'
                                  }`}
                                  title={cat.title}
                                >
                                  <div className={`p-2 rounded-lg ${selectedCategoryId === cat.id ? 'bg-amber-500 text-slate-950' : 'bg-slate-900 text-slate-400'}`}>
                                    {cat.icon}
                                  </div>
                                  <span className="text-[9px] font-bold truncate w-full text-center leading-tight">{cat.title}</span>
                                </button>
                              ))}
                            </div>

                            {/* Job Selection Dropdown (Inside Selected Category) */}
                            {selectedCategoryId && (
                              <div className="animate-in fade-in slide-in-from-top-2 duration-200">
                                <select
                                  value={profileForm.jobTitle}
                                  onChange={(e) => setProfileForm({ ...profileForm, jobTitle: e.target.value })}
                                  className="w-full bg-slate-950 border border-amber-500/30 rounded-xl px-4 py-3 text-xs text-white focus:outline-none focus:border-amber-400 font-bold appearance-none cursor-pointer"
                                  style={{ backgroundImage: 'url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 24 24\' fill=\'none\' stroke=\'white\' stroke-width=\'2\' stroke-linecap=\'round\' stroke-linejoin=\'round\'%3e%3cpolyline points=\'6 9 12 15 18 9\'%3e%3c/polyline%3e%3c/svg%3e")', backgroundRepeat: 'no-repeat', backgroundPosition: 'left 1rem center', backgroundSize: '1em' }}
                                >
                                  <option value="">-- اختر الوظيفة من {professionalCategories.find(c => c.id === selectedCategoryId)?.title} --</option>
                                  {professionalCategories.find(c => c.id === selectedCategoryId)?.jobs.map(job => (
                                    <option key={job} value={job}>{job}</option>
                                  ))}
                                </select>
                              </div>
                            )}

                            <div className="flex justify-between items-center px-1">
                              <p className="text-[10px] text-slate-500 font-medium">اختر تصنيفاً أولاً ثم حدد وظيفتك بدقة</p>
                              <Button 
                                variant="ghost" 
                                onClick={() => setIsCustomJob(true)}
                                className="text-[10px] font-bold text-amber-400 hover:text-amber-300 h-auto p-0 hover:bg-transparent"
                              >
                                ألا تجد وظيفتك؟ اكتبها يدوياً
                              </Button>
                            </div>
                          </div>
                        ) : (
                          <div className="flex gap-2">
                            <Input
                              value={profileForm.jobTitle}
                              onChange={(e) => setProfileForm({ ...profileForm, jobTitle: e.target.value })}
                              placeholder="اكتب مسماك الوظيفي الخاص هنا..."
                              className="bg-slate-950 border-slate-800 text-white font-bold"
                            />
                            <Button 
                              variant="ghost" 
                              onClick={() => setIsCustomJob(false)}
                              className="text-xs text-slate-500 hover:text-white shrink-0"
                            >
                              رجوع للتصنيفات
                            </Button>
                          </div>
                        )}
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-2">النبذة الشخصية (Bio)</label>
                      <textarea
                        value={profileForm.bio}
                        onChange={(e) => setProfileForm({ ...profileForm, bio: e.target.value })}
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-slate-300 focus:outline-none focus:border-amber-400 min-h-[100px] resize-none leading-relaxed"
                        placeholder="تحدث بإيجاز عن مهاراتك وخبراتك لجذب الشركات..."
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-2">المهارات (افصل بينها بفاصلة)</label>
                      <Input
                        value={profileForm.skills.join(', ')}
                        onChange={(e) => setProfileForm({ ...profileForm, skills: e.target.value.split(',').map(s => s.trim()).filter(s => s !== '') })}
                        placeholder="مثال: تثمين الذهب، المحاسبة، التفاوض..."
                        className="bg-slate-950 border-slate-800 text-xs text-white"
                      />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-bold text-slate-400 mb-2">التعليم</label>
                        <Input
                          value={profileForm.education}
                          onChange={(e) => setProfileForm({ ...profileForm, education: e.target.value })}
                          className="bg-slate-950 border-slate-800 text-xs text-white"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-bold text-slate-400 mb-2">سنوات الخبرة</label>
                        <Input
                          value={profileForm.experience}
                          onChange={(e) => setProfileForm({ ...profileForm, experience: e.target.value })}
                          className="bg-slate-950 border-slate-800 text-xs text-white"
                        />
                      </div>
                    </div>

                    <Button 
                      onClick={handleUpdateProfile}
                      className="w-full bg-slate-100 hover:bg-white text-slate-950 font-black text-xs py-3 rounded-xl transition-all"
                    >
                      حفظ وتحديث السيرة الذاتية
                    </Button>
                  </CardContent>
                </Card>
              </div>

              {/* CV Preview Side */}
              <div className="xl:col-span-7">
                <Card className="bg-white text-slate-950 border-slate-200 shadow-2xl rounded-3xl overflow-hidden min-h-[600px] flex flex-col">
                  {/* CV Header */}
                  <div className="bg-slate-900 p-8 sm:p-10 text-white relative">
                    <div className="absolute top-0 left-0 p-8 opacity-10">
                      <QrCode className="w-24 h-24" />
                    </div>
                    <div className="relative z-10">
                      <div className="flex items-center gap-4 mb-4">
                        <div className="w-16 h-16 rounded-2xl bg-amber-400 text-slate-950 flex items-center justify-center text-3xl font-black">
                          {(profile.name || 'O').charAt(0)}
                        </div>
                        <div>
                          <h1 className="text-2xl sm:text-3xl font-black tracking-tight">{profile.name || 'الاسم غير محدد'}</h1>
                          <p className="text-amber-400 font-bold text-sm sm:text-base mt-1">{profile.jobTitle || 'المسمى الوظيفي غير محدد'}</p>
                        </div>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-6 pt-6 border-t border-slate-800 text-xs text-slate-300">
                        <div className="flex items-center gap-2">
                          <Smartphone className="w-4 h-4 text-slate-500" />
                          <span>{profile.phone || '—'}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                          <span>رقم ضريبي: {profile.taxNumber || '—'}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* CV Body */}
                  <div className="p-8 sm:p-10 flex-1 space-y-8 bg-slate-50">
                    <section>
                      <h2 className="text-xs font-black uppercase tracking-widest text-slate-400 mb-3 border-b border-slate-200 pb-2">الملخص المهني</h2>
                      <p className="text-sm text-slate-700 leading-relaxed font-medium">
                        {profile.bio}
                      </p>
                    </section>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                      <section>
                        <h2 className="text-xs font-black uppercase tracking-widest text-slate-400 mb-3 border-b border-slate-200 pb-2">المهارات والقدرات</h2>
                        <div className="flex flex-wrap gap-2">
                          {profile.skills.map((skill, i) => (
                            <span key={i} className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-[11px] font-bold text-slate-600 shadow-sm">
                              {skill}
                            </span>
                          ))}
                        </div>
                      </section>
                      <section>
                        <h2 className="text-xs font-black uppercase tracking-widest text-slate-400 mb-3 border-b border-slate-200 pb-2">المسار التعليمي</h2>
                        <div className="text-sm text-slate-800 font-bold">
                          {profile.education}
                        </div>
                        <div className="text-xs text-slate-500 mt-1">درجة جامعية معتمدة</div>
                      </section>
                    </div>

                    <section>
                      <h2 className="text-xs font-black uppercase tracking-widest text-slate-400 mb-3 border-b border-slate-200 pb-2">الخبرة العملية</h2>
                      <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
                        <div className="text-sm font-black text-slate-900">{profile.experience}</div>
                        <div className="text-xs text-slate-500 mt-1 flex items-center gap-2">
                          <CheckCircle className="w-3 h-3 text-emerald-500" />
                          <span>سجل عمل موثق عبر منصة OOZOX</span>
                        </div>
                      </div>
                    </section>
                  </div>

                  {/* CV Footer */}
                  <div className="p-6 bg-slate-100 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4">
                    <div className="flex items-center gap-2 text-[10px] text-slate-400 font-bold">
                      <Sparkles className="w-3 h-3" />
                      تم إنشاء هذا الملف آلياً بواسطة بوابة OOZOX الرقمية
                    </div>
                    <div className="flex items-center gap-3">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => typeof window !== 'undefined' && window.print()}
                        className="text-xs font-bold text-slate-600"
                      >
                        طباعة / حفظ PDF
                      </Button>
                      <div className="h-4 w-px bg-slate-300" />
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setProfileForm({ ...profile });
                          setShowProfileModal(true);
                        }}
                        className="text-xs font-bold text-amber-600"
                      >
                        تحديث البيانات
                      </Button>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 7: DEVELOPER API & PRODUCTION GUIDE */}
        {activeView === 'developer_api' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="border-b border-slate-800 pb-5">
              <span className="text-cyan-400 text-xs font-bold uppercase tracking-wider bg-cyan-500/10 px-3 py-1 rounded-full border border-cyan-500/20 font-mono">
                Production REST API
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-white mt-2.5 tracking-tight leading-snug">توثيق ونقاط تكامل REST APIs للمطورين</h2>
              <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">تكامل فعلي ومباشر مع خادم الإنتاج (https://api.oozox.com) لإدارة المصادقة والفواتير والإشعارات.</p>
            </div>

            <div className="space-y-4">
              {/* Endpoint 1: Phone-Only OTP Auth */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 sm:p-6">
                <div className="flex items-center justify-between gap-4 mb-3">
                  <div className="flex items-center gap-2">
                    <span className="bg-amber-500/20 text-amber-400 px-2.5 py-1 rounded-lg text-xs font-mono font-bold">
                      POST
                    </span>
                    <span className="text-xs sm:text-sm font-mono text-white font-semibold">/api/auth/otp/request &amp; /verify</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400 font-mono hidden sm:inline">Phone + OTP Only</span>
                    <button
                      onClick={() => handleCopyText(`curl -X POST https://api.oozox.com/api/auth/otp/request -H "Content-Type: application/json" -d '{"phone":"+201012345678","channel":"TELEGRAM","purpose":"LOGIN"}'`, 'أمر المصادقة بالهاتف')}
                      className="px-3.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs sm:text-sm text-amber-400 font-bold tracking-wide transition-colors"
                      title="نسخ أمر الربط"
                    >
                      نسخ cURL
                    </button>
                  </div>
                </div>
                <div className="bg-slate-950 p-4 rounded-xl font-mono text-xs text-slate-300 overflow-x-auto border border-slate-800 leading-relaxed">
                  <pre>{`curl -X POST https://api.oozox.com/api/auth/otp/request \\
  -H "Content-Type: application/json" \\
  -d '{
    "phone": "+201012345678",
    "channel": "TELEGRAM",
    "purpose": "LOGIN"
  }'`}</pre>
                </div>
              </div>

              {/* Endpoint 2: Create Invoice */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 sm:p-6">
                <div className="flex items-center justify-between gap-4 mb-3">
                  <div className="flex items-center gap-2">
                    <span className="bg-emerald-500/20 text-emerald-400 px-2.5 py-1 rounded-lg text-xs font-mono font-bold">
                      POST
                    </span>
                    <span className="text-xs sm:text-sm font-mono text-white font-semibold">/api/invoices</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400 font-mono hidden sm:inline">Bearer Token Required</span>
                    <button
                      onClick={() => handleCopyText(`curl -X POST https://api.oozox.com/api/invoices -H "Authorization: Bearer <JWT_TOKEN>" -H "Content-Type: application/json" -d '{"receiverId":"<RECEIVER_OBJECT_ID>","items":[{"name":"<ITEM_NAME>","quantity":1,"unitPrice":0,"karat":21}],"paymentMethod":"INSTAPAY","dueDate":"<ISO8601_DATE>"}'`, 'أمر إنشاء الفاتورة')}
                      className="px-3.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs sm:text-sm text-amber-400 font-bold tracking-wide transition-colors"
                      title="نسخ أمر الربط"
                    >
                      نسخ cURL
                    </button>
                  </div>
                </div>
                <div className="bg-slate-950 p-4 rounded-xl font-mono text-xs text-slate-300 overflow-x-auto border border-slate-800 leading-relaxed">
                  <pre>{`curl -X POST https://api.oozox.com/api/invoices \\
  -H "Authorization: Bearer <JWT_TOKEN>" \\
  -H "Content-Type: application/json" \\
  -d '{
    "receiverId": "<RECEIVER_OBJECT_ID>",
    "items": [{"name": "<ITEM_NAME>", "quantity": 1, "unitPrice": 0, "karat": 21}],
    "taxAmount": 0,
    "discount": 0,
    "paymentMethod": "INSTAPAY",
    "dueDate": "<ISO8601_DATE>"
  }'`}</pre>
                </div>
              </div>

              {/* Endpoint 3: Health & Notifications */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 sm:p-6">
                <div className="flex items-center justify-between gap-4 mb-3">
                  <div className="flex items-center gap-2">
                    <span className="bg-cyan-500/20 text-cyan-400 px-2.5 py-1 rounded-lg text-xs font-mono font-bold">
                      GET
                    </span>
                    <span className="text-xs sm:text-sm font-mono text-white font-semibold">/api/health</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400 font-mono hidden sm:inline">Live Service Telemetry</span>
                    <button
                      onClick={() => handleCopyText(`https://api.oozox.com/api/health`, 'رابط فحص الخدمة')}
                      className="px-3.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs sm:text-sm text-cyan-400 font-bold tracking-wide transition-colors"
                      title="نسخ الرابط"
                    >
                      نسخ الرابط
                    </button>
                  </div>
                </div>
                <div className="bg-slate-950 p-4 rounded-xl font-mono text-xs text-slate-300 overflow-x-auto border border-slate-800 leading-relaxed">
                  <pre>{`// Authenticated endpoints also available:
// GET   /api/invoices          (paginated list & filters)
// PATCH /api/invoices/:id/status
// GET   /api/notifications     (in-app alerts)`}</pre>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 8: WORK TRACKING & GPS ATTENDANCE */}
        {activeView === 'work_tracking' && (
          <div className="space-y-6 sm:space-y-8 animate-fadeIn">
            <div className="border-b border-slate-800 pb-5">
              <span className="text-emerald-400 text-xs font-bold uppercase tracking-wider bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20 font-mono">
                Employee Shield & Geofencing
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-white mt-2.5 tracking-tight leading-snug">تتبع الموظفين والتحقق الجغرافي الذكي</h2>
              <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">راقب حضور الموظفين في مواقع العمل المحددة وتأكد من الالتزام بالمواعيد الجغرافية والزمنية.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Active Assignments */}
              <div className="lg:col-span-2 space-y-4">
                <h3 className="text-sm font-bold text-slate-300 flex items-center gap-2">
                  <UserCheck className="w-4 h-4 text-emerald-400" />
                  تعيينات العمل النشطة (الحالية)
                </h3>
                
                {assignments.length === 0 ? (
                  <Card className="bg-slate-900 border-slate-800">
                    <CardContent className="p-8 text-center text-xs sm:text-sm text-slate-400">
                      لا توجد تعيينات عمل نشطة مسجلة حالياً.
                    </CardContent>
                  </Card>
                ) : (
                  assignments.map(as => (
                  <Card key={as.id} className="bg-slate-900 border-slate-800 group hover:border-emerald-500/30 transition-all">
                    <CardContent className="p-6">
                      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                        <div className="flex items-center gap-4">
                          <div className="w-14 h-14 rounded-2xl bg-slate-950 border border-slate-800 flex flex-col items-center justify-center text-emerald-400 shadow-inner group-hover:scale-105 transition-transform">
                            <MapPin className="w-6 h-6" />
                            <span className="text-[9px] font-black mt-1">GPS</span>
                          </div>
                          <div>
                            <h4 className="text-base font-bold text-white">{as.employeeName}</h4>
                            <p className="text-xs text-slate-400 font-medium">{as.jobTitle}</p>
                            <div className="flex items-center gap-3 mt-2">
                              <span className="text-[10px] text-slate-500 flex items-center gap-1">
                                <Clock className="w-3 h-3" /> {as.workingHours.start} - {as.workingHours.end}
                              </span>
                              <span className="text-[10px] text-slate-500 flex items-center gap-1">
                                <MapPin className="w-3 h-3" /> {as.location.address}
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="w-full sm:w-auto flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-3">
                          <div className="text-right">
                            <span className="text-[10px] text-slate-500 block mb-1">نقاط الالتزام (CV Score)</span>
                            <div className="flex items-center gap-2">
                              <div className="w-24 h-1.5 bg-slate-800 rounded-full overflow-hidden">
                                <div className="h-full bg-emerald-500" style={{ width: `${as.presenceScore}%` }} />
                              </div>
                              <span className="text-xs font-black text-emerald-400">{as.presenceScore}%</span>
                            </div>
                          </div>
                          <Button 
                            onClick={() => checkEmployeeLocation(as.id)}
                            className="bg-emerald-600 hover:bg-emerald-500 text-white text-[10px] font-bold px-4 py-2 rounded-xl h-9"
                          >
                            طلب تحقق فوري (GPS)
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                  ))
                )}
              </div>

              {/* Real-time Tracking Logs */}
              <div className="lg:col-span-1 space-y-4">
                <h3 className="text-sm font-bold text-slate-300 flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-cyan-400" />
                  سجل التحقق اللحظي
                </h3>
                <div className="bg-slate-900 border border-slate-800 rounded-3xl p-5 min-h-[300px] max-h-[500px] overflow-y-auto space-y-4">
                  {attendanceLogs.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center p-8 opacity-50">
                      <ShieldCheck className="w-12 h-12 text-slate-700 mb-3" />
                      <p className="text-xs text-slate-500">لا يوجد سجلات حتى الآن. اطلب التحقق من الموظف لبدء التتبع.</p>
                    </div>
                  ) : (
                    attendanceLogs.map(log => (
                      <div key={log.id} className={`p-3 rounded-xl border flex gap-3 animate-slideIn ${
                        log.locationMatch ? 'bg-emerald-500/5 border-emerald-500/20' : 'bg-red-500/5 border-red-500/20'
                      }`}>
                        <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                          log.locationMatch ? 'bg-emerald-500 text-slate-950' : 'bg-red-500 text-white'
                        }`}>
                          {log.locationMatch ? <CheckCircle className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
                        </div>
                        <div>
                          <div className="flex items-center justify-between gap-4">
                            <span className="text-[10px] font-black text-white">{log.type === 'check_in' ? 'تحقق دخول' : 'مخالفة موقع'}</span>
                            <span className="text-[9px] text-slate-500 font-mono">{log.timestamp}</span>
                          </div>
                          <p className="text-[10px] text-slate-400 mt-1 leading-relaxed">{log.notes}</p>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

      </main>

      {/* FLOATING CHAT BUBBLE SYSTEM */}
      {bubbleActive && (
        <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-4 animate-fadeIn">
          {showChatBubble && (
            <div className="w-[340px] sm:w-[380px] h-[500px] bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-[32px] shadow-2xl flex flex-col overflow-hidden animate-slideUp">
              <div className="p-5 bg-gradient-to-r from-amber-500 to-amber-400 text-slate-950 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center border border-white/30 backdrop-blur-sm">
                    <MessageSquare className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-black tracking-tight">مساعد OOZOX الذكي</h4>
                    <span className="text-[10px] font-bold opacity-80 uppercase tracking-wider">Direct Business Support</span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button 
                    onClick={() => setBubbleActive(false)}
                    className="p-1.5 hover:bg-black/10 rounded-full transition-colors"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="flex-1 overflow-y-auto p-5 space-y-4 bg-slate-50/50 dark:bg-slate-950/50 scrollbar-hide">
                {bubbleMessages.map((msg) => (
                  <div key={msg.id} className={`flex gap-2 ${msg.sender === 'user' ? 'flex-row-reverse' : ''}`}>
                    <div className={`w-7 h-7 rounded-lg flex items-center justify-center text-[10px] font-black shrink-0 ${
                      msg.sender === 'bot' ? 'bg-amber-500 text-slate-950' : 'bg-slate-700 text-white'
                    }`}>
                      {msg.sender === 'bot' ? 'OX' : 'أنت'}
                    </div>
                    <div className={`p-3 rounded-2xl shadow-sm max-w-[80%] ${
                      msg.sender === 'bot'
                        ? 'bg-white dark:bg-slate-800 border border-slate-100 dark:border-slate-700 rounded-tr-none text-slate-800 dark:text-slate-200'
                        : 'bg-amber-500 text-slate-950 rounded-tl-none font-medium'
                    }`}>
                      <p className="text-xs leading-relaxed">{msg.text}</p>
                    </div>
                  </div>
                ))}
              </div>

              <form onSubmit={handleSendBubbleMessage} className="p-4 bg-white dark:bg-slate-900 border-t border-slate-100 dark:border-slate-800 flex items-center gap-2">
                <input
                  type="text"
                  value={bubbleInput}
                  onChange={(e) => setBubbleInput(e.target.value)}
                  placeholder="اكتب رسالتك هنا..."
                  className="flex-1 bg-slate-100 dark:bg-slate-950 border-none rounded-xl px-4 py-2.5 text-xs focus:ring-2 focus:ring-amber-500"
                />
                <button
                  type="submit"
                  className="w-10 h-10 rounded-xl bg-amber-500 text-slate-950 flex items-center justify-center hover:scale-105 transition-transform shadow-md shadow-amber-500/20"
                >
                  <Send className="w-5 h-5" />
                </button>
              </form>
            </div>
          )}

          <button
            onClick={() => setShowChatBubble(!showChatBubble)}
            className={`w-16 h-16 rounded-full flex items-center justify-center shadow-2xl transition-all duration-300 hover:scale-110 active:scale-95 ${
              showChatBubble 
                ? 'bg-rose-500 text-white rotate-90' 
                : 'bg-amber-500 text-slate-950 rotate-0'
            }`}
          >
            {showChatBubble ? <X className="w-8 h-8" /> : <MessageCircle className="w-8 h-8" />}
          </button>
        </div>
      )}

      {/* NEW INVOICE MODAL */}
      {showNewInvoiceModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl relative space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-lg sm:text-xl font-bold text-white tracking-tight">إنشاء فاتورة إلكترونية جديدة</h3>
              <button 
                onClick={() => setShowNewInvoiceModal(false)}
                className="text-slate-400 hover:text-white text-xs sm:text-sm font-bold px-3 py-1.5 rounded-lg hover:bg-slate-800 transition-colors"
              >
                إغلاق
              </button>
            </div>

            <form onSubmit={handleCreateInvoice} className="space-y-4">
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">اسم العميل / المحل *</label>
                <Input
                  required
                  value={newClientName}
                  onChange={(e) => setNewClientName(e.target.value)}
                  placeholder="مثال: مجوهرات الأمانة"
                  className="bg-slate-950 border-slate-800 text-xs sm:text-sm py-2.5 text-white"
                />
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">رقم واتساب العميل *</label>
                <Input
                  required
                  value={newClientPhone}
                  onChange={(e) => setNewClientPhone(e.target.value)}
                  placeholder="+2010xxxxxxxx أو +9665xxxxxxxx"
                  className="bg-slate-950 border-slate-800 text-xs sm:text-sm py-2.5 text-white font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">بيان الصنف / المشغولات *</label>
                  <Input
                    required
                    value={newItemName}
                    onChange={(e) => setNewItemName(e.target.value)}
                    placeholder="مثال: إسورة ذهب عيار 21"
                    className="bg-slate-950 border-slate-800 text-xs sm:text-sm py-2.5 text-white"
                  />
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الإجمالي (ج.م) *</label>
                  <Input
                    required
                    type="number"
                    value={newItemPrice}
                    onChange={(e) => setNewItemPrice(e.target.value)}
                    placeholder="مثال: 35000"
                    className="bg-slate-950 border-slate-800 text-xs sm:text-sm py-2.5 text-white font-bold font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">المبلغ المسدد الآن (ج.م)</label>
                <Input
                  type="number"
                  value={newPaidAmount}
                  onChange={(e) => setNewPaidAmount(e.target.value)}
                  placeholder="اتركه فارغاً إذا تم السداد بالكامل"
                  className="bg-slate-950 border-slate-800 text-xs sm:text-sm py-2.5 text-white font-mono"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="vat-check"
                  checked={includeVat}
                  onChange={(e) => setIncludeVat(e.target.checked)}
                  className="rounded bg-slate-950 border-slate-800 text-amber-500 focus:ring-amber-400"
                />
                <label htmlFor="vat-check" className="text-xs sm:text-sm text-slate-300 cursor-pointer select-none">
                  تضمين ضريبة القيمة المضافة (14% VAT)
                </label>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setShowNewInvoiceModal(false)}
                  className="text-xs sm:text-sm text-slate-400 hover:text-white px-4 py-2"
                >
                  إلغاء
                </Button>
                <Button
                  type="submit"
                  className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs sm:text-sm px-6 py-2.5 rounded-xl shadow-lg shadow-amber-500/20 tracking-wide transition-all"
                >
                  تأكيد وإصدار الفاتورة
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* EDIT PROFILE / BUSINESS INFO MODAL */}
      {showProfileModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl relative space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
                  <User className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base sm:text-lg font-bold text-white tracking-tight">تحديث الملف التجاري</h3>
                  <p className="text-xs text-slate-400">بيانات المنشأة وترويسة الفواتير الضريبية</p>
                </div>
              </div>
              <button 
                onClick={() => setShowProfileModal(false)}
                className="text-slate-400 hover:text-white text-xs sm:text-sm font-bold px-3 py-1.5 rounded-lg hover:bg-slate-800 transition-colors"
              >
                إغلاق
              </button>
            </div>

            <form onSubmit={handleUpdateProfile} className="space-y-3.5">
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">اسم التاجر / المدير المسؤول *</label>
                <Input
                  required
                  value={profileForm.name}
                  onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                  placeholder="مثال: أحمد محمود"
                  className="bg-slate-950 border-slate-800 text-xs sm:text-sm text-white py-2.5"
                />
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الاسم التجاري للمنشأة *</label>
                <Input
                  required
                  value={profileForm.businessName}
                  onChange={(e) => setProfileForm({ ...profileForm, businessName: e.target.value })}
                  placeholder="مثال: مجوهرات العطار"
                  className="bg-slate-950 border-slate-800 text-xs sm:text-sm text-amber-400 font-bold py-2.5"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">رقم الهاتف *</label>
                  <Input
                    required
                    value={profileForm.phone}
                    onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                    placeholder="+2010xxxxxxxx"
                    className="bg-slate-950 border-slate-800 text-xs sm:text-sm text-white font-mono py-2.5"
                  />
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">المدينة / الفرع</label>
                  <Input
                    value={profileForm.city}
                    onChange={(e) => setProfileForm({ ...profileForm, city: e.target.value })}
                    placeholder="مثال: القاهرة"
                    className="bg-slate-950 border-slate-800 text-xs sm:text-sm text-white py-2.5"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الرقم الضريبي / السجل</label>
                  <Input
                    value={profileForm.taxNumber}
                    onChange={(e) => setProfileForm({ ...profileForm, taxNumber: e.target.value })}
                    placeholder="EG-000-000"
                    className="bg-slate-950 border-slate-800 text-xs sm:text-sm text-white font-mono py-2.5"
                  />
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">العملة الافتراضية</label>
                  <Input
                    value={profileForm.currency}
                    onChange={(e) => setProfileForm({ ...profileForm, currency: e.target.value })}
                    placeholder="ج.م (EGP)"
                    className="bg-slate-950 border-slate-800 text-xs sm:text-sm text-white py-2.5"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setShowProfileModal(false)}
                  className="text-xs sm:text-sm text-slate-400 hover:text-white px-4 py-2"
                >
                  إلغاء
                </Button>
                <Button
                  type="submit"
                  className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs sm:text-sm px-6 py-2.5 rounded-xl shadow-lg shadow-amber-500/20 tracking-wide transition-all"
                >
                  حفظ التعديلات
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* FLOATING TOAST NOTIFICATION CONTAINER */}
      <div 
        className="fixed bottom-5 left-5 right-5 sm:right-auto sm:w-96 z-50 flex flex-col gap-2 pointer-events-none"
        aria-live="polite"
      >
        {toasts.map((toast) => {
          const isSuccess = toast.type === 'success';
          const isError = toast.type === 'error';
          const isWarning = toast.type === 'warning';
          const isInfo = toast.type === 'info';

          return (
            <div
              key={toast.id}
              className={`pointer-events-auto p-4 rounded-2xl shadow-2xl border backdrop-blur-xl flex items-start gap-3 transition-all transform translate-y-0 opacity-100 ${
                isSuccess
                  ? 'bg-card/95 border-success/50 text-success shadow-success/10'
                  : isError
                  ? 'bg-card/95 border-error/50 text-error shadow-error/10'
                  : isWarning
                  ? 'bg-card/95 border-warning/50 text-warning shadow-warning/10'
                  : 'bg-card/95 border-info/50 text-info shadow-info/10'
              }`}
            >
              <div className="mt-0.5 shrink-0">
                {isSuccess && <CheckCircle className="w-5 h-5 text-success" />}
                {isError && <AlertCircle className="w-5 h-5 text-error" />}
                {isWarning && <AlertCircle className="w-5 h-5 text-warning" />}
                {isInfo && <Info className="w-5 h-5 text-info" />}
              </div>

              <div className="flex-1 min-w-0">
                <h4 className="text-xs font-bold text-foreground tracking-wide">{toast.title}</h4>
                {toast.description && (
                  <p className="text-[11px] text-muted-foreground mt-0.5 leading-relaxed break-words">{toast.description}</p>
                )}
              </div>

              <button
                onClick={() => removeToast(toast.id)}
                className="text-slate-400 hover:text-white text-xs font-bold px-2 py-1 rounded-lg hover:bg-slate-800 transition-colors shrink-0"
                title="إغلاق الإشعار"
              >
                إغلاق
              </button>
            </div>
          );
        })}
      </div>

      <WebAuthModal
        open={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onAuthenticated={(user) => {
          setAuthUser(user);
          setAuthToken(getWebAccessToken('user'));
          addToast('تم تسجيل الدخول وتوثيق الجلسة', `مرحباً ${user.name || user.phone}`, 'success');
        }}
      />

      {authToken && (
        <WebTrustedDevicesModal
          open={showTrustedDevicesModal}
          token={authToken}
          onClose={() => setShowTrustedDevicesModal(false)}
          onSessionInvalidated={() => {
            setAuthToken(null);
            setAuthUser(null);
          }}
        />
      )}

      {authToken && authUser && activeCallSession && (
        <WebRtcCallModal
          token={authToken}
          currentUserId={authUser.id}
          callSession={activeCallSession}
          wsRef={wsRef}
          incomingSignal={incomingCallSignal}
          onClose={() => setActiveCallSession(null)}
          onBudgetBlocked={(msg) => {
            addToast('تعذر بدء المكالمة', msg, 'warning');
          }}
        />
      )}
    </div>
  );
};
