import React, { useState, useEffect, useRef, useCallback } from 'react';
import { 
  Announcement, 
  TutorialGuide, 
  LiveGoldPrice, 
  ProjectFeature,
  SiteHeroConfig,
  AdminUser,
  JobOpportunity,
  BroadcastAnnouncement,
  SiteThemeConfig
} from './types';
import { themePresets, defaultTheme, applyThemeToDocument, generateShadesFromHex } from './themePresets';
import { AdminDashboardSkeleton } from './components/skeletons/AdminDashboardSkeleton';
import {
  WebCallBudgetTelemetry,
  WebCallHistoryItem,
  apiGetAdminCallBudget,
  apiGetCallHistory,
  getWebAccessToken,
} from './lib/webAuthClient';
import { 
  LayoutDashboard, 
  Megaphone, 
  BookOpen, 
  Coins, 
  Users, 
  ShieldCheck, 
  LogOut, 
  Plus, 
  Trash2, 
  Edit3, 
  Eye, 
  Check, 
  X, 
  ExternalLink,
  Save,
  TrendingUp,
  Building2,
  Lock,
  Sparkles,
  Zap,
  Layers,
  Settings,
  Type,
  FileText,
  HelpCircle,
  Smartphone,
  CloudCheck,
  RefreshCw,
  Radio,
  BellRing,
  Send,
  AlertTriangle,
  Palette,
  RotateCcw,
  Sliders,
  Paintbrush,
  Briefcase,
  Pipette,
  PhoneCall
} from 'lucide-react';

interface AdminDashboardProps {
  adminUser: AdminUser;
  heroConfig: SiteHeroConfig;
  announcements: Announcement[];
  tutorials: TutorialGuide[];
  features: ProjectFeature[];
  goldPrices: LiveGoldPrice[];
  onUpdateHeroConfig: (updated: SiteHeroConfig) => void;
  onUpdateAnnouncements: (updated: Announcement[]) => void;
  onUpdateTutorials: (updated: TutorialGuide[]) => void;
  onUpdateFeatures: (updated: ProjectFeature[]) => void;
  onUpdateGoldPrices: (updated: LiveGoldPrice[]) => void;
  currentTheme?: SiteThemeConfig;
  onUpdateTheme?: (updatedTheme: SiteThemeConfig) => void;
  onLogout: () => void;
  onViewLiveSite: () => void;
  isLoading?: boolean;
}

export const AdminDashboard: React.FC<AdminDashboardProps> = ({
  adminUser,
  heroConfig,
  announcements,
  tutorials,
  features,
  goldPrices,
  onUpdateHeroConfig,
  onUpdateAnnouncements,
  onUpdateTutorials,
  onUpdateFeatures,
  onUpdateGoldPrices,
  currentTheme,
  onUpdateTheme,
  onLogout,
  onViewLiveSite,
  isLoading = false
}) => {
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const [activeTab, setActiveTab] = useState<'overview' | 'cms_features' | 'cms_announcements' | 'cms_tutorials' | 'cms_hero' | 'gold_prices' | 'companies' | 'jobs' | 'calls_budget' | 'theme_customizer'>('overview');

  // --- Calls & Monthly RTC Budget Telemetry State ---
  const [rtcBudget, setRtcBudget] = useState<WebCallBudgetTelemetry | null>(null);
  const [rtcCalls, setRtcCalls] = useState<WebCallHistoryItem[]>([]);
  const [rtcLoading, setRtcLoading] = useState(false);

  const loadRtcTelemetry = useCallback(async () => {
    const adminToken = getWebAccessToken('admin');
    if (!adminToken) return;
    setRtcLoading(true);
    const [bRes, hRes] = await Promise.all([
      apiGetAdminCallBudget(adminToken),
      apiGetCallHistory(adminToken, 1, 20),
    ]);
    setRtcLoading(false);
    if (bRes.ok && bRes.data) setRtcBudget(bRes.data);
    if (hRes.ok && hRes.data?.calls) setRtcCalls(hRes.data.calls);
  }, []);

  useEffect(() => {
    if (activeTab === 'calls_budget' || activeTab === 'overview') {
      void loadRtcTelemetry();
    }
  }, [activeTab, loadRtcTelemetry]);
  
  // --- Jobs CMS State ---
  const [jobs, setJobs] = useState<JobOpportunity[]>(() => {
    if (typeof window === 'undefined') return [];
    try {
      const saved = localStorage.getItem('oozox_jobs');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [newJobForm, setNewJobForm] = useState({
    title: '',
    company: '',
    location: '',
    salary: '',
    category: '',
    description: ''
  });

  const handlePostJob = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newJobForm.title || !newJobForm.company) return;

    const newJob: JobOpportunity = {
      id: `job-${Date.now()}`,
      title: newJobForm.title,
      company: newJobForm.company,
      location: newJobForm.location,
      salary: newJobForm.salary,
      category: newJobForm.category,
      description: newJobForm.description,
      postedDate: new Date().toLocaleDateString('ar-EG', { year: 'numeric', month: 'long', day: 'numeric' }),
    };

    const updatedJobs = [newJob, ...jobs];
    setJobs(updatedJobs);
    localStorage.setItem('oozox_jobs', JSON.stringify(updatedJobs));
    
    // Dispatch custom event for User Portal to catch
    window.dispatchEvent(new CustomEvent('oozox_new_job', { detail: newJob }));
    
    triggerToast('تم نشر الوظيفة الجديدة بنجاح وإرسال تنبيهات للمطابقين');
    setNewJobForm({ title: '', company: '', location: '', salary: '', category: '', description: '' });
  };

  const handleDeleteJob = (id: string) => {
    const updated = jobs.filter(j => j.id !== id);
    setJobs(updated);
    localStorage.setItem('oozox_jobs', JSON.stringify(updated));
    triggerToast('تم حذف الوظيفة بنجاح');
  };

  const [saveToast, setSaveToast] = useState<string | null>(null);
  const [autoSaveStatus, setAutoSaveStatus] = useState<'idle' | 'saving' | 'saved'>('idle');
  const [lastSavedTime, setLastSavedTime] = useState<string>('الآن');

  const triggerToast = (msg: string) => {
    setSaveToast(msg);
    setTimeout(() => setSaveToast(null), 3000);
  };

  // --- Hero Section CMS State ---
  const [tempHeroConfig, setTempHeroConfig] = useState<SiteHeroConfig>(heroConfig);
  const isHeroConfigMounted = useRef(false);

  // Sync if prop changes externally
  useEffect(() => {
    setTempHeroConfig(heroConfig);
  }, [heroConfig]);

  // Debounced auto-save for Hero Config
  useEffect(() => {
    if (!isHeroConfigMounted.current) {
      isHeroConfigMounted.current = true;
      return;
    }

    setAutoSaveStatus('saving');
    const timer = setTimeout(() => {
      onUpdateHeroConfig(tempHeroConfig);
      setAutoSaveStatus('saved');
      setLastSavedTime(new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    }, 700);

    return () => clearTimeout(timer);
  }, [tempHeroConfig]);

  const handleSaveHeroConfig = () => {
    onUpdateHeroConfig(tempHeroConfig);
    setAutoSaveStatus('saved');
    setLastSavedTime(new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    triggerToast('تم حفظ وتحديث نصوص واجهة الموقع العام بنجاح!');
  };

  // --- Features CMS State ---
  const [tempFeatures, setTempFeatures] = useState<ProjectFeature[]>(features);
  const isFeaturesMounted = useRef(false);

  useEffect(() => {
    setTempFeatures(features);
  }, [features]);

  // Debounced auto-save for Features
  useEffect(() => {
    if (!isFeaturesMounted.current) {
      isFeaturesMounted.current = true;
      return;
    }

    setAutoSaveStatus('saving');
    const timer = setTimeout(() => {
      onUpdateFeatures(tempFeatures);
      setAutoSaveStatus('saved');
      setLastSavedTime(new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    }, 700);

    return () => clearTimeout(timer);
  }, [tempFeatures]);

  // Quick inline update feature field with instant debounced save
  const handleUpdateFeatureField = (id: string, field: keyof ProjectFeature, value: any) => {
    setTempFeatures(prev => prev.map(f => f.id === id ? { ...f, [field]: value } : f));
  };

  const [showAddFeatureModal, setShowAddFeatureModal] = useState(false);
  const [editingFeature, setEditingFeature] = useState<ProjectFeature | null>(null);
  const [featTitle, setFeatTitle] = useState('');
  const [featDesc, setFeatDesc] = useState('');
  const [featCategory, setFeatCategory] = useState('محاسبة ومالية');
  const [featIcon, setFeatIcon] = useState('Receipt');
  const [featHighlightsText, setFeatHighlightsText] = useState('');

  const openNewFeatureModal = () => {
    setEditingFeature(null);
    setFeatTitle('');
    setFeatDesc('');
    setFeatCategory('محاسبة ومالية');
    setFeatIcon('Receipt');
    setFeatHighlightsText('');
    setShowAddFeatureModal(true);
  };

  const openEditFeatureModal = (feat: ProjectFeature) => {
    setEditingFeature(feat);
    setFeatTitle(feat.title);
    setFeatDesc(feat.description);
    setFeatCategory(feat.category);
    setFeatIcon(feat.icon);
    setFeatHighlightsText(feat.highlights.join('\n'));
    setShowAddFeatureModal(true);
  };

  const handleSaveFeature = () => {
    if (!featTitle.trim()) return;
    const highlights = featHighlightsText
      .split('\n')
      .map(h => h.trim())
      .filter(h => h.length > 0);

    if (editingFeature) {
      // Update existing
      const updated = tempFeatures.map(f => 
        f.id === editingFeature.id 
          ? { ...f, title: featTitle.trim(), description: featDesc.trim(), category: featCategory, icon: featIcon, highlights }
          : f
      );
      setTempFeatures(updated);
      onUpdateFeatures(updated);
      triggerToast('تم تحديث بيانات الميزة وحفظها تلقائياً!');
    } else {
      // Add new
      const newFeat: ProjectFeature = {
        id: `feat-${Date.now()}`,
        title: featTitle.trim(),
        description: featDesc.trim(),
        category: featCategory,
        icon: featIcon,
        highlights: highlights.length > 0 ? highlights : ['ميزة حصرية وسريعة']
      };
      const updated = [...tempFeatures, newFeat];
      setTempFeatures(updated);
      onUpdateFeatures(updated);
      triggerToast('تمت إضافة الميزة الجديدة وحفظها تلقائياً!');
    }
    setShowAddFeatureModal(false);
  };

  const handleDeleteFeature = (id: string) => {
    if (window.confirm('هل أنت متأكد من حذف هذه الميزة من الصفحة العامة؟')) {
      const updated = tempFeatures.filter(f => f.id !== id);
      setTempFeatures(updated);
      onUpdateFeatures(updated);
      triggerToast('تم حذف الميزة من الصفحة العامة.');
    }
  };

  // --- Announcements CMS State ---
  const [showAddAnnModal, setShowAddAnnModal] = useState(false);
  const [editingAnn, setEditingAnn] = useState<Announcement | null>(null);
  const [newAnnTitle, setNewAnnTitle] = useState('');
  const [newAnnContent, setNewAnnContent] = useState('');
  const [newAnnCategory, setNewAnnCategory] = useState<'promo' | 'system' | 'market' | 'event'>('promo');
  const [newAnnBadge, setNewAnnBadge] = useState('عرض جديد ✨');
  const [newAnnActionLabel, setNewAnnActionLabel] = useState('اكتشف الآن');
  const [newAnnActionUrl, setNewAnnActionUrl] = useState('#download');

  // --- System-Wide Broadcast Toast Modal State ---
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [broadcastCategory, setBroadcastCategory] = useState<'urgent' | 'promo' | 'system' | 'market' | 'event'>('urgent');
  const [broadcastBadge, setBroadcastBadge] = useState('بث مباشر 🔴');
  const [broadcastBadgeColor, setBroadcastBadgeColor] = useState('#EF4444');
  const [broadcastActionLabel, setBroadcastActionLabel] = useState('تحميل التطبيق الآن');
  const [broadcastActionUrl, setBroadcastActionUrl] = useState('#download');
  const [broadcastDurationSeconds, setBroadcastDurationSeconds] = useState(12);
  const [saveAsPersistentAnn, setSaveAsPersistentAnn] = useState(true);

  const openBroadcastModal = (preset?: Partial<Announcement>) => {
    if (preset) {
      setBroadcastTitle(preset.title || '');
      setBroadcastMessage(preset.content || '');
      setBroadcastCategory((preset.category as any) || 'urgent');
      setBroadcastBadge(preset.badgeText || 'بث عاجل 🔴');
      setBroadcastBadgeColor(preset.badgeColor || '#EF4444');
      setBroadcastActionLabel(preset.actionLabel || 'تحميل التطبيق الآن');
      setBroadcastActionUrl(preset.actionUrl || '#download');
      setBroadcastDurationSeconds(12);
      setSaveAsPersistentAnn(false);
    } else {
      setBroadcastTitle('');
      setBroadcastMessage('');
      setBroadcastCategory('urgent');
      setBroadcastBadge('بث مباشر 🔴');
      setBroadcastBadgeColor('#EF4444');
      setBroadcastActionLabel('تحميل التطبيق الآن');
      setBroadcastActionUrl('#download');
      setBroadcastDurationSeconds(12);
      setSaveAsPersistentAnn(true);
    }
    setShowBroadcastModal(true);
  };

  const handleBroadcastAnnouncement = () => {
    if (!broadcastTitle.trim() || !broadcastMessage.trim()) {
      alert('يرجى كتابة عنوان ورسالة الإشعار قبل بدء البث');
      return;
    }

    const payload: BroadcastAnnouncement = {
      id: `broadcast-${Date.now()}`,
      title: broadcastTitle.trim(),
      message: broadcastMessage.trim(),
      category: broadcastCategory,
      badgeText: broadcastBadge.trim() || 'بث مباشر 🔴',
      badgeColor: broadcastBadgeColor,
      actionLabel: broadcastActionLabel.trim() || undefined,
      actionUrl: broadcastActionUrl.trim() || undefined,
      timestamp: Date.now(),
      durationSeconds: broadcastDurationSeconds,
      author: adminUser?.username || 'الإدارة'
    };

    // Save to localStorage for instant cross-tab / Landing Page delivery
    try {
      localStorage.setItem('oozox_broadcast_announcement', JSON.stringify(payload));
      localStorage.setItem('oozox_broadcast_timestamp', String(Date.now()));
    } catch (e) {
      console.error(e);
    }

    // Dispatch DOM custom event for same-tab immediate reaction
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('oozox_broadcast', { detail: payload }));
    }

    // Optionally also record as a permanent announcement in CMS
    if (saveAsPersistentAnn) {
      const newAnn: Announcement = {
        id: `ann-${Date.now()}`,
        title: broadcastTitle.trim(),
        category: broadcastCategory === 'urgent' ? 'system' : broadcastCategory,
        content: broadcastMessage.trim(),
        badgeText: broadcastBadge.trim(),
        badgeColor: broadcastBadgeColor,
        publishDate: new Date().toISOString().split('T')[0],
        isActive: true,
        actionLabel: broadcastActionLabel.trim() || undefined,
        actionUrl: broadcastActionUrl.trim() || undefined,
        viewCount: 1,
        isBroadcast: true
      };
      onUpdateAnnouncements([newAnn, ...announcements]);
    }

    setShowBroadcastModal(false);
    triggerToast('🚀 تم بث الإشعار بنجاح! سيظهر الآن كتنبيه Toast فوري لجميع زوار الصفحة الرئيسية.');
  };

  const openNewAnnModal = () => {
    setEditingAnn(null);
    setNewAnnTitle('');
    setNewAnnContent('');
    setNewAnnCategory('promo');
    setNewAnnBadge('عرض خاص 🎁');
    setNewAnnActionLabel('اكتشف الآن');
    setNewAnnActionUrl('#download');
    setShowAddAnnModal(true);
  };

  const openEditAnnModal = (ann: Announcement) => {
    setEditingAnn(ann);
    setNewAnnTitle(ann.title);
    setNewAnnContent(ann.content);
    setNewAnnCategory(ann.category);
    setNewAnnBadge(ann.badgeText);
    setNewAnnActionLabel(ann.actionLabel || 'اكتشف الآن');
    setNewAnnActionUrl(ann.actionUrl || '#download');
    setShowAddAnnModal(true);
  };

  const handleSaveAnnouncement = () => {
    if (!newAnnTitle.trim()) return;

    if (editingAnn) {
      const updated = announcements.map(a => 
        a.id === editingAnn.id 
          ? {
              ...a,
              title: newAnnTitle.trim(),
              content: newAnnContent.trim(),
              category: newAnnCategory,
              badgeText: newAnnBadge.trim(),
              badgeColor: newAnnCategory === 'promo' ? '#10B981' : newAnnCategory === 'system' ? '#0EA5E9' : '#F59E0B',
              actionLabel: newAnnActionLabel,
              actionUrl: newAnnActionUrl
            }
          : a
      );
      onUpdateAnnouncements(updated);
      triggerToast('تم تعديل الإعلان بنجاح!');
    } else {
      const newAnn: Announcement = {
        id: `ann-${Date.now()}`,
        title: newAnnTitle.trim(),
        category: newAnnCategory,
        content: newAnnContent.trim(),
        badgeText: newAnnBadge.trim(),
        badgeColor: newAnnCategory === 'promo' ? '#10B981' : newAnnCategory === 'system' ? '#0EA5E9' : '#F59E0B',
        publishDate: new Date().toISOString().split('T')[0],
        isActive: true,
        actionLabel: newAnnActionLabel,
        actionUrl: newAnnActionUrl,
        viewCount: 0
      };
      onUpdateAnnouncements([newAnn, ...announcements]);
      triggerToast('تم نشر الإعلان الجديد على الموقع العام!');
    }
    setShowAddAnnModal(false);
  };

  const handleDeleteAnnouncement = (id: string) => {
    if (window.confirm('هل أنت متأكد من حذف هذا الإعلان من موقع oozox.com؟')) {
      onUpdateAnnouncements(announcements.filter(a => a.id !== id));
      triggerToast('تم حذف الإعلان بنجاح.');
    }
  };

  const handleToggleAnnouncement = (id: string) => {
    onUpdateAnnouncements(
      announcements.map(a => a.id === id ? { ...a, isActive: !a.isActive } : a)
    );
  };

  // --- Tutorials CMS State ---
  const [showAddTutModal, setShowAddTutModal] = useState(false);
  const [editingTut, setEditingTut] = useState<TutorialGuide | null>(null);
  const [newTutTitle, setNewTutTitle] = useState('');
  const [newTutSummary, setNewTutSummary] = useState('');
  const [newTutCategory, setNewTutCategory] = useState<'accounting' | 'vault' | 'ai_camera' | 'projects' | 'backup'>('accounting');
  const [newTutDuration, setNewTutDuration] = useState('3 دقائق قراءة');
  const [newTutStepsText, setNewTutStepsText] = useState('');
  const [newTutTagsText, setNewTutTagsText] = useState('');

  const openNewTutModal = () => {
    setEditingTut(null);
    setNewTutTitle('');
    setNewTutSummary('');
    setNewTutCategory('accounting');
    setNewTutDuration('3 دقائق قراءة');
    setNewTutStepsText('');
    setNewTutTagsText('');
    setShowAddTutModal(true);
  };

  const openEditTutModal = (tut: TutorialGuide) => {
    setEditingTut(tut);
    setNewTutTitle(tut.title);
    setNewTutSummary(tut.summary);
    setNewTutCategory(tut.category);
    setNewTutDuration(tut.duration);
    setNewTutStepsText(tut.steps.join('\n'));
    setNewTutTagsText(tut.tags.join(', '));
    setShowAddTutModal(true);
  };

  const handleSaveTutorial = () => {
    if (!newTutTitle.trim()) return;
    const steps = newTutStepsText.split('\n').filter(s => s.trim().length > 0);
    const tags = newTutTagsText.split(',').map(t => t.trim()).filter(t => t.length > 0);

    if (editingTut) {
      const updated = tutorials.map(t => 
        t.id === editingTut.id 
          ? {
              ...t,
              title: newTutTitle.trim(),
              category: newTutCategory,
              summary: newTutSummary.trim(),
              duration: newTutDuration,
              steps: steps.length > 0 ? steps : ['اتبع الإرشادات داخل التطبيق'],
              tags: tags.length > 0 ? tags : ['شرح']
            }
          : t
      );
      onUpdateTutorials(updated);
      triggerToast('تم تحديث الشرح بنجاح!');
    } else {
      const newTut: TutorialGuide = {
        id: `tut-${Date.now()}`,
        title: newTutTitle.trim(),
        category: newTutCategory,
        summary: newTutSummary.trim(),
        duration: newTutDuration,
        steps: steps.length > 0 ? steps : ['اتبع الإرشادات داخل التطبيق'],
        icon: 'BookOpen',
        tags: tags.length > 0 ? tags : ['شرح'],
        isFeatured: false
      };
      onUpdateTutorials([newTut, ...tutorials]);
      triggerToast('تمت إضافة الشرح الجديد إلى المركز التعليمي!');
    }
    setShowAddTutModal(false);
  };

  const handleDeleteTutorial = (id: string) => {
    if (window.confirm('هل أنت متأكد من حذف هذا الشرح من الموقع؟')) {
      onUpdateTutorials(tutorials.filter(t => t.id !== id));
      triggerToast('تم حذف الشرح من المركز التعليمي.');
    }
  };

  const handleToggleFeaturedTutorial = (id: string) => {
    onUpdateTutorials(
      tutorials.map(t => t.id === id ? { ...t, isFeatured: !t.isFeatured } : t)
    );
  };

  // --- Gold Price CMS State ---
  const handleUpdatePrice = (karat: string, field: 'buyPrice' | 'sellPrice', value: number) => {
    onUpdateGoldPrices(
      goldPrices.map(g => g.karat === karat ? { ...g, [field]: value, lastUpdated: 'الآن' } : g)
    );
    triggerToast(`تم تحديث سعر ${karat} لحظياً على الصفحة العامة!`);
  };

  // --- Live Theme Previewer & Brand Palette Studio State ---
  const activeCurrentTheme = currentTheme || defaultTheme;
  const [selectedPreviewTheme, setSelectedPreviewTheme] = useState<SiteThemeConfig>(activeCurrentTheme);
  const [customColorHex, setCustomColorHex] = useState<string>(activeCurrentTheme.primary);
  const [isLivePreviewActive, setIsLivePreviewActive] = useState<boolean>(true);
  const [interactiveTestSteps, setInteractiveTestSteps] = useState<number[]>([0, 1]);

  // Sync selectedPreviewTheme with currentTheme whenever currentTheme changes
  useEffect(() => {
    if (currentTheme) {
      setSelectedPreviewTheme(currentTheme);
      setCustomColorHex(currentTheme.primary);
    }
  }, [currentTheme]);

  const handleSelectPreset = (preset: SiteThemeConfig) => {
    setSelectedPreviewTheme(preset);
    setCustomColorHex(preset.primary);
    if (isLivePreviewActive) {
      applyThemeToDocument(preset);
    }
  };

  const handleCustomHexChange = (hex: string) => {
    setCustomColorHex(hex);
    if (/^#[0-9A-Fa-f]{6}$/.test(hex)) {
      const shades = generateShadesFromHex(hex);
      const customTheme: SiteThemeConfig = {
        id: `custom-${hex.replace('#', '').toLowerCase()}`,
        name: `تخصيص حر (${hex.toUpperCase()})`,
        nameEn: `Custom Color (${hex.toUpperCase()})`,
        description: 'لون مخصص تم اختياره وإعداده مباشرة من لوحة الإدارة.',
        ...shades,
        isCustom: true,
      };
      setSelectedPreviewTheme(customTheme);
      if (isLivePreviewActive) {
        applyThemeToDocument(customTheme);
      }
    }
  };

  const handleApplyAndSaveTheme = (themeToSave: SiteThemeConfig) => {
    if (onUpdateTheme) {
      onUpdateTheme(themeToSave);
    } else {
      applyThemeToDocument(themeToSave);
      try {
        localStorage.setItem('oozox_theme_config', JSON.stringify(themeToSave));
      } catch (e) {
        console.error(e);
      }
    }
    triggerToast(`🎨 تم اعتماد ونشر هوية "${themeToSave.name}" على كامل الموقع العام بنجاح!`);
  };

  const handleResetToDefaultTheme = () => {
    setSelectedPreviewTheme(defaultTheme);
    setCustomColorHex(defaultTheme.primary);
    if (onUpdateTheme) {
      onUpdateTheme(defaultTheme);
    } else {
      applyThemeToDocument(defaultTheme);
      try {
        localStorage.setItem('oozox_theme_config', JSON.stringify(defaultTheme));
      } catch (e) {
        console.error(e);
      }
    }
    triggerToast('تمت استعادة اللون الافتراضي للمنصة (الذهب الملكي الأصيل).');
  };

  const toggleTestStep = (stepIdx: number) => {
    setInteractiveTestSteps(prev => 
      prev.includes(stepIdx) ? prev.filter(i => i !== stepIdx) : [...prev, stepIdx]
    );
  };

  if (isLoading || isRefreshing) {
    return <AdminDashboardSkeleton />;
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col font-sans selection:bg-amber-500 selection:text-black transition-colors duration-300" dir="rtl">
      
      {/* Toast Notification */}
      {saveToast && (
        <div className="fixed bottom-5 left-5 z-50 bg-emerald-500 text-slate-950 font-bold px-4 py-3 rounded-2xl shadow-2xl flex items-center gap-2 animate-bounce">
          <Check className="w-4 h-4" />
          <span>{saveToast}</span>
        </div>
      )}

      {/* Admin Top Header */}
      <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 py-3.5 flex items-center justify-between sticky top-0 z-30 transition-colors duration-300">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-amber-500 text-slate-950 flex items-center justify-center font-black shadow-md">
              OX
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-slate-900 dark:text-white text-base sm:text-lg tracking-tight">لوحة تحكم إدارة OOZOX.COM</span>
                <span className="text-[11px] bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded font-bold tracking-wide">
                  {adminUser.role}
                </span>
              </div>
              <span className="text-xs text-slate-400 font-mono">المدير: {adminUser.username}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Real-time Debounced Auto-Save Status Indicator */}
          <div className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-950/80 border border-slate-200 dark:border-slate-800 text-xs transition-colors duration-300">
            {autoSaveStatus === 'saving' ? (
              <div className="flex items-center gap-1.5 text-amber-400">
                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                <span className="text-xs font-semibold tracking-wide">جارٍ الحفظ التلقائي...</span>
              </div>
            ) : autoSaveStatus === 'saved' ? (
              <div className="flex items-center gap-1.5 text-emerald-400">
                <CloudCheck className="w-3.5 h-3.5" />
                <span className="text-xs font-semibold tracking-wide">تم الحفظ تلقائياً ({lastSavedTime})</span>
              </div>
            ) : (
              <div className="flex items-center gap-1.5 text-slate-400">
                <CloudCheck className="w-3.5 h-3.5 text-slate-500" />
                <span className="text-xs font-medium tracking-wide">الحفظ التلقائي مفعل</span>
              </div>
            )}
          </div>

          <button
            onClick={() => {
              setIsRefreshing(true);
              setTimeout(() => {
                setIsRefreshing(false);
                triggerToast('تمت مزامنة البيانات وتحديث لوحة التحكم بنجاح!');
              }, 550);
            }}
            className="flex items-center gap-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 px-3.5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide"
            title="تحديث البيانات وإعادة المزامنة"
            aria-label="تحديث البيانات"
          >
            <RefreshCw className="w-3.5 h-3.5 text-amber-500" />
            <span className="hidden sm:inline">تحديث</span>
          </button>

          <button
            onClick={onViewLiveSite}
            className="bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-amber-600 dark:text-amber-400 border border-slate-200 dark:border-slate-700 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide"
          >
            معاينة الموقع الحي (oozox.com)
          </button>

          <button
            onClick={onLogout}
            className="bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide"
          >
            خروج آمن
          </button>
        </div>
      </header>

      {/* Main Admin Workspace */}
      <div className="flex-1 flex flex-col md:flex-row">
        
        {/* Sidebar Navigation */}
        <aside className="w-full md:w-64 bg-slate-900/90 border-b md:border-b-0 md:border-l border-slate-800 p-4 space-y-1.5 flex-shrink-0">
          
          <div className="text-xs font-bold text-slate-400 px-3 py-1.5 uppercase tracking-wider">
            نظام الإدارة الشامل
          </div>

          <button
            onClick={() => setActiveTab('overview')}
            className={`w-full text-right px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'overview'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            نظرة عامة والإحصائيات
          </button>

          <div className="pt-4 text-xs font-bold text-amber-400 px-3 py-1.5 uppercase tracking-wider">
            إدارة محتوى الموقع العام (CMS)
          </div>

          <button
            onClick={() => setActiveTab('cms_features')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'cms_features'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <span>مميزات التطبيق والمشروع</span>
            <span className="text-xs font-mono font-bold bg-slate-950 px-2 py-0.5 rounded text-amber-400">
              {features.length}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('cms_announcements')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'cms_announcements'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <span>الإعلانات والبانرات</span>
            <span className="text-xs font-mono font-bold bg-slate-950 px-2 py-0.5 rounded text-amber-400">
              {announcements.length}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('cms_tutorials')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'cms_tutorials'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <span>الشروحات والأدلة</span>
            <span className="text-xs font-mono font-bold bg-slate-950 px-2 py-0.5 rounded text-amber-400">
              {tutorials.length}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('jobs')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'jobs'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <span>إدارة فرص التوظيف</span>
            <span className="text-xs font-mono font-bold bg-slate-950 px-2 py-0.5 rounded text-amber-400">
              {jobs.length}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('cms_hero')}
            className={`w-full text-right px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'cms_hero'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            نصوص الواجهة والترويج
          </button>

          <div className="pt-4 text-xs font-bold text-slate-400 px-3 py-1.5 uppercase tracking-wider">
            السوق والشركات
          </div>

          <button
            onClick={() => setActiveTab('gold_prices')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'gold_prices'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <span>تحديث أسعار الذهب</span>
            <span className="text-xs font-bold bg-emerald-500/20 text-emerald-400 px-2 py-0.5 rounded">
              مباشر
            </span>
          </button>

          <button
            onClick={() => setActiveTab('companies')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'companies'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <span>الشركات والمشتركين</span>
            <span className="text-xs font-mono font-bold bg-slate-950 px-2 py-0.5 rounded text-slate-400">
              1,248
            </span>
          </button>

          <button
            onClick={() => setActiveTab('calls_budget')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'calls_budget'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <div className="flex items-center gap-2">
              <PhoneCall className="w-4 h-4 text-emerald-400" />
              <span>المكالمات وميزانية RTC</span>
            </div>
            <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
              rtcBudget?.newCallsBlocked
                ? 'bg-rose-500/20 text-rose-300'
                : 'bg-slate-950 text-emerald-400'
            }`}>
              {rtcBudget ? `${rtcBudget.usagePct.toFixed(0)}%` : 'LIVE'}
            </span>
          </button>

          <div className="pt-4 text-xs font-bold text-slate-400 px-3 py-1.5 uppercase tracking-wider">
            الهوية والتصميم الحي
          </div>

          <button
            onClick={() => setActiveTab('theme_customizer')}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-bold transition-all tracking-wide leading-normal ${
              activeTab === 'theme_customizer'
                ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                : 'text-slate-300 hover:bg-slate-800'
            }`}
          >
            <div className="flex items-center gap-2">
              <Palette className="w-4 h-4 text-amber-400 group-hover:rotate-12 transition-transform" />
              <span>ألوان وهوية المنصة</span>
            </div>
            <span className="text-[10px] font-black bg-amber-500/20 text-amber-400 px-2 py-0.5 rounded-md border border-amber-500/30">
              معاينة حية
            </span>
          </button>
        </aside>

        {/* Content Area */}
        <main className="flex-1 p-6 lg:p-8 overflow-y-auto">
          
          {/* TAB 1: OVERVIEW */}
          {activeTab === 'overview' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug">مركز التحكم والإحصائيات</h1>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">مرحباً بك في إدارة منصة وموقع OOZOX.COM</p>
                </div>
              </div>

              {/* Metrics Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                  <div className="flex items-center justify-between">
                    <span className="text-xs sm:text-sm text-slate-400 font-bold tracking-wide">فرص العمل المنشورة</span>
                    <Users className="w-4 h-4 text-amber-400" />
                  </div>
                  <p className="text-3xl sm:text-4xl font-black text-white font-mono tabular-nums tracking-tight mt-3">{jobs.length}</p>
                  <p className="text-xs text-emerald-400 font-medium mt-1.5">مسجلة في بوابة التوظيف</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                  <div className="flex items-center justify-between">
                    <span className="text-xs sm:text-sm text-slate-400 font-bold tracking-wide">مميزات المشروع المعروضة</span>
                    <Zap className="w-4 h-4 text-teal-400" />
                  </div>
                  <p className="text-3xl sm:text-4xl font-black text-white font-mono tabular-nums tracking-tight mt-3">{features.length}</p>
                  <p className="text-xs text-slate-400 font-medium mt-1.5">مفعلة وتظهر على الصفحة العامة</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                  <div className="flex items-center justify-between">
                    <span className="text-xs sm:text-sm text-slate-400 font-bold tracking-wide">الشروحات والأدلة النشطة</span>
                    <BookOpen className="w-4 h-4 text-amber-400" />
                  </div>
                  <p className="text-3xl sm:text-4xl font-black text-white font-mono tabular-nums tracking-tight mt-3">{tutorials.length}</p>
                  <p className="text-xs text-slate-400 font-medium mt-1.5">{tutorials.filter(t => t.isFeatured).length} شروحات مميزة في الصفحة الأولى</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                  <div className="flex items-center justify-between">
                    <span className="text-xs sm:text-sm text-slate-400 font-bold tracking-wide">الإعلانات النشطة</span>
                    <Megaphone className="w-4 h-4 text-emerald-400" />
                  </div>
                  <p className="text-3xl sm:text-4xl font-black text-white font-mono tabular-nums tracking-tight mt-3">{announcements.filter(a => a.isActive).length}</p>
                  <p className="text-xs text-slate-400 font-medium mt-1.5">من أصل {announcements.length} إعلان مسجل</p>
                </div>
              </div>

              {/* Quick Shortcuts */}
              <div className="p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-amber-500/10 via-slate-900 to-teal-500/10 border border-amber-500/20">
                <h3 className="text-base sm:text-lg font-bold text-white tracking-tight leading-snug mb-2">إدارة محتوى الموقع العام وتحديثه المباشر</h3>
                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed max-w-2xl mb-5">
                  يمكنك من خلال هذه اللوحة إضافة وحذف وتعديل كافة مميزات المشروع، الشروحات التفاعلية، الإعلانات والبانرات، وأسعار الذهب اللحظية التي تظهر لزوار موقع oozox.com.
                </p>
                <div className="flex flex-wrap gap-2.5">
                  <button 
                    onClick={() => openBroadcastModal()}
                    className="bg-gradient-to-r from-rose-500 to-amber-500 hover:from-rose-600 hover:to-amber-600 text-slate-950 font-black px-4 py-2.5 rounded-xl text-xs sm:text-sm transition-all tracking-wide flex items-center gap-2 shadow-lg shadow-rose-500/20 hover:scale-[1.02]"
                  >
                    <Radio className="w-4 h-4 animate-pulse text-slate-950" />
                    بث إشعار عاجل للزوار (Toast)
                  </button>
                  <button 
                    onClick={() => setActiveTab('theme_customizer')}
                    className="bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 font-black px-4 py-2.5 rounded-xl text-xs sm:text-sm transition-all tracking-wide flex items-center gap-2 shadow-md shadow-amber-500/20 hover:scale-[1.02]"
                  >
                    <Palette className="w-4 h-4 text-slate-950" />
                    تخصيص ألوان وهوية المنصة
                  </button>
                  <button 
                    onClick={() => setActiveTab('cms_features')}
                    className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold px-4 py-2.5 rounded-xl text-xs sm:text-sm transition-colors tracking-wide"
                  >
                    تعديل مميزات المشروع ({features.length})
                  </button>
                  <button 
                    onClick={() => setActiveTab('cms_announcements')}
                    className="bg-slate-800 hover:bg-slate-700 text-white font-bold px-4 py-2.5 rounded-xl text-xs sm:text-sm transition-colors tracking-wide"
                  >
                    نشر إعلان جديد
                  </button>
                  <button 
                    onClick={() => setActiveTab('cms_tutorials')}
                    className="bg-slate-800 hover:bg-slate-700 text-white font-bold px-4 py-2.5 rounded-xl text-xs sm:text-sm transition-colors tracking-wide"
                  >
                    إضافة دليل أو شرح
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: PROJECT FEATURES CMS (مميزات التطبيق) */}
          {activeTab === 'cms_features' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                      <Zap className="w-6 h-6 text-amber-400" />
                      إدارة مميزات التطبيق والمشروع (الصفحة العامة)
                    </h1>
                    <span className="hidden sm:inline-flex items-center gap-1 text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2.5 py-0.5 rounded-full font-bold">
                      <CloudCheck className="w-3.5 h-3.5" />
                      حفظ تلقائي دوري
                    </span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                    أضف أو عدل مميزات المنصة المعروضة على الصفحة الرئيسية لـ oozox.com. يتم حفظ أي تعديل تلقائياً في التخزين المحلي.
                  </p>
                </div>

                <button
                  onClick={openNewFeatureModal}
                  className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold px-5 py-2.5 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 transition-transform hover:scale-105 tracking-wide"
                >
                  إضافة ميزة جديدة
                </button>
              </div>

              {/* Features List */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {tempFeatures.map((feat) => (
                  <div key={feat.id} className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all flex flex-col justify-between">
                    <div>
                      <div className="flex items-start justify-between gap-3 mb-3">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold bg-teal-500/20 text-teal-400 px-2.5 py-0.5 rounded-full border border-teal-500/30 tracking-wide">
                            {feat.category}
                          </span>
                          <span className="text-xs text-slate-400 font-mono">أيقونة: {feat.icon}</span>
                        </div>

                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => openEditFeatureModal(feat)}
                            className="px-3 py-1 text-xs sm:text-sm font-bold text-slate-300 hover:text-amber-400 hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            تعديل
                          </button>
                          <button
                            onClick={() => handleDeleteFeature(feat.id)}
                            className="px-3 py-1 text-xs sm:text-sm font-bold text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            حذف
                          </button>
                        </div>
                      </div>

                      {/* Quick inline title edit with auto-save */}
                      <input 
                        type="text"
                        value={feat.title}
                        onChange={(e) => handleUpdateFeatureField(feat.id, 'title', e.target.value)}
                        className="text-base sm:text-lg font-bold text-white mb-2 bg-transparent border-b border-transparent hover:border-slate-700 focus:border-amber-400 focus:bg-slate-950/60 rounded px-2 py-1 w-full focus:outline-none transition-colors tracking-tight leading-snug"
                        title="انقر للتعديل السريع مع الحفظ التلقائي"
                      />

                      {/* Quick inline description edit with auto-save */}
                      <textarea
                        rows={2}
                        value={feat.description}
                        onChange={(e) => handleUpdateFeatureField(feat.id, 'description', e.target.value)}
                        className="text-xs sm:text-sm text-slate-300 leading-relaxed mb-4 bg-transparent border border-transparent hover:border-slate-800 focus:border-amber-400/50 focus:bg-slate-950/60 rounded-lg p-2 w-full focus:outline-none transition-colors"
                        title="انقر لتعديل الوصف السريع مع الحفظ التلقائي"
                      />

                      {feat.highlights && feat.highlights.length > 0 && (
                        <div className="bg-slate-950/60 p-3.5 rounded-xl border border-slate-800/80 space-y-1.5">
                          <span className="text-xs font-bold text-amber-400 block mb-1 tracking-wide">النقاط البارزة:</span>
                          {feat.highlights.map((hl, i) => (
                            <div key={i} className="text-xs sm:text-sm text-slate-300 flex items-center gap-2 leading-relaxed">
                              <Check className="w-3.5 h-3.5 text-teal-400 flex-shrink-0" />
                              <span>{hl}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400 font-medium">
                      <span className="flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                        مباشرة على oozox.com (محفوظة)
                      </span>
                      <button 
                        onClick={() => openEditFeatureModal(feat)}
                        className="text-amber-400 font-bold hover:underline tracking-wide"
                      >
                        تعديل تفصيلي
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 3: ANNOUNCEMENTS CMS (الإعلانات) */}
          {activeTab === 'cms_announcements' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                  <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                    <Megaphone className="w-6 h-6 text-amber-400" />
                    إدارة إعلانات وبانرات الموقع العام
                  </h1>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                    التحكم في البانر العلوي المتحرك وقسم أخبار المنصة في oozox.com.
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2.5">
                  <button
                    onClick={() => openBroadcastModal()}
                    className="bg-gradient-to-r from-rose-500 via-amber-500 to-amber-400 hover:from-rose-600 hover:to-amber-500 text-slate-950 font-black px-5 py-2.5 rounded-xl text-xs sm:text-sm shadow-lg shadow-rose-500/25 tracking-wide flex items-center gap-2 transition-all hover:scale-[1.02]"
                  >
                    <Radio className="w-4 h-4 animate-pulse text-slate-950" />
                    <span>بث إشعار عام فوري (Toast)</span>
                  </button>
                  <button
                    onClick={openNewAnnModal}
                    className="bg-slate-800 hover:bg-slate-700 text-white font-bold px-4 py-2.5 rounded-xl text-xs sm:text-sm border border-slate-700 tracking-wide transition-colors"
                  >
                    إنشاء إعلان جديد
                  </button>
                </div>
              </div>

              <div className="space-y-3">
                {announcements.map((ann) => (
                  <div 
                    key={ann.id} 
                    className={`p-5 rounded-2xl border transition-all ${
                      ann.isActive ? 'bg-slate-900 border-slate-800' : 'bg-slate-900/40 border-slate-800/60 opacity-60'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="space-y-2 flex-1">
                        <div className="flex items-center gap-2.5">
                          <span 
                            className="text-xs font-bold px-2.5 py-1 rounded-md tracking-wide"
                            style={{ backgroundColor: `${ann.badgeColor}20`, color: ann.badgeColor }}
                          >
                            {ann.badgeText}
                          </span>
                          <span className="text-xs text-slate-400 font-mono">{ann.publishDate}</span>
                          <span className={`text-xs font-bold px-2 py-0.5 rounded tracking-wide ${ann.isActive ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'}`}>
                            {ann.isActive ? 'منشور ونشط' : 'معطل (مسودة)'}
                          </span>
                          {ann.isBroadcast && (
                            <span className="text-xs font-bold px-2 py-0.5 rounded bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center gap-1">
                              <Radio className="w-3 h-3 animate-pulse" />
                              تم بثه
                            </span>
                          )}
                        </div>

                        <h3 className="text-base sm:text-lg font-bold text-white tracking-tight leading-snug">{ann.title}</h3>
                        <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">{ann.content}</p>

                        {ann.actionUrl && (
                          <div className="text-xs text-amber-400 font-mono pt-1">
                            الرابط التفاعلي: {ann.actionLabel} → {ann.actionUrl}
                          </div>
                        )}
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => openBroadcastModal(ann)}
                          title="بث هذا الإعلان كإشعار Toast فوري على الموقع"
                          className="px-3 py-2 text-xs sm:text-sm font-bold text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-xl transition-colors tracking-wide flex items-center gap-1.5 border border-rose-500/20"
                        >
                          <Radio className="w-3.5 h-3.5" />
                          <span>بث الآن</span>
                        </button>
                        <button
                          onClick={() => handleToggleAnnouncement(ann.id)}
                          className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-bold tracking-wide transition-colors ${
                            ann.isActive 
                              ? 'bg-amber-500/10 text-amber-400 hover:bg-amber-500/20' 
                              : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
                          }`}
                        >
                          {ann.isActive ? 'إيقاف' : 'تفعيل'}
                        </button>
                        <button
                          onClick={() => openEditAnnModal(ann)}
                          className="px-3.5 py-2 text-xs sm:text-sm font-bold text-slate-300 hover:text-amber-400 hover:bg-slate-800 rounded-xl transition-colors tracking-wide"
                        >
                          تعديل
                        </button>
                        <button
                          onClick={() => handleDeleteAnnouncement(ann.id)}
                          className="px-3.5 py-2 text-xs sm:text-sm font-bold text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-xl transition-colors tracking-wide"
                        >
                          حذف
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 4: TUTORIALS CMS (الشروحات والأدلة) */}
          {activeTab === 'cms_tutorials' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                  <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                    <BookOpen className="w-6 h-6 text-amber-400" />
                    إدارة مركز الشروحات والأدلة التفاعلية
                  </h1>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                    أضف شروحات عملية توضح كيفية استخدام الهاشتاج، كاميرا الذهب، والخزائن المشفرة.
                  </p>
                </div>

                <button
                  onClick={openNewTutModal}
                  className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold px-5 py-2.5 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 tracking-wide"
                >
                  إضافة شرح جديد
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {tutorials.map((tut) => (
                  <div key={tut.id} className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
                    <div>
                      <div className="flex items-start justify-between gap-3 mb-2">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-mono font-bold bg-slate-800 text-amber-400 px-2.5 py-1 rounded">
                            {tut.duration}
                          </span>
                          <span className="text-xs font-bold bg-slate-800 text-slate-400 px-2.5 py-1 rounded">
                            {tut.category}
                          </span>
                        </div>

                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => handleToggleFeaturedTutorial(tut.id)}
                            className={`px-3 py-1.5 rounded-lg text-xs sm:text-sm font-bold tracking-wide transition-colors ${
                              tut.isFeatured ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'text-slate-400 hover:text-slate-200 bg-slate-800'
                            }`}
                          >
                            {tut.isFeatured ? 'مميز' : 'عادي'}
                          </button>
                          <button
                            onClick={() => openEditTutModal(tut)}
                            className="px-3 py-1.5 text-xs sm:text-sm font-bold text-slate-300 hover:text-amber-400 hover:bg-slate-800 rounded-lg transition-colors tracking-wide"
                          >
                            تعديل
                          </button>
                          <button
                            onClick={() => handleDeleteTutorial(tut.id)}
                            className="px-3 py-1.5 text-xs sm:text-sm font-bold text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors tracking-wide"
                          >
                            حذف
                          </button>
                        </div>
                      </div>

                      <h3 className="text-base sm:text-lg font-bold text-white mb-2 tracking-tight leading-snug">{tut.title}</h3>
                      <p className="text-xs sm:text-sm text-slate-400 mb-4 leading-relaxed">{tut.summary}</p>

                      <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-1.5 text-xs sm:text-sm text-slate-300">
                        <span className="text-xs font-bold text-teal-400 block tracking-wide">خطوات التنفيذ ({tut.steps.length}):</span>
                        {tut.steps.slice(0, 3).map((step, idx) => (
                          <div key={idx} className="flex items-start gap-2 text-xs sm:text-sm leading-relaxed">
                            <span className="text-amber-400 font-bold">{idx + 1}.</span>
                            <span className="line-clamp-1">{step}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between">
                      <div className="flex flex-wrap gap-1">
                        {tut.tags.map((t, i) => (
                          <span key={i} className="text-[11px] font-mono bg-slate-800 text-slate-400 px-2 py-0.5 rounded">
                            #{t}
                          </span>
                        ))}
                      </div>
                      <button 
                        onClick={() => openEditTutModal(tut)}
                        className="text-xs sm:text-sm text-amber-400 font-bold hover:underline tracking-wide"
                      >
                        تعديل
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 5: HERO & PROMO TEXTS CMS */}
          {activeTab === 'cms_hero' && (
            <div className="space-y-6 max-w-4xl">
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                    <Type className="w-6 h-6 text-amber-400" />
                    تعديل نصوص الواجهة والبانر الرئيسي (Hero & Promo)
                  </h1>
                  <span className="hidden sm:inline-flex items-center gap-1 text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2.5 py-0.5 rounded-full font-bold tracking-wide">
                    <CloudCheck className="w-3.5 h-3.5" />
                    حفظ تلقائي دوري مفعّل
                  </span>
                </div>
                <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                  تعديل العنوان الرئيسي، الرسالة الترويجية، الشعار، وأزرار التحميل. يتم الحفظ والتحديث تلقائياً في التخزين المحلي فور الكتابة.
                </p>
              </div>

              <div className="p-6 sm:p-8 rounded-3xl bg-slate-900 border border-slate-800 space-y-5">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                    شارة العرض الترويجي العلوية (Top Badge)
                  </label>
                  <input
                    type="text"
                    value={tempHeroConfig.topBadge}
                    onChange={(e) => setTempHeroConfig({ ...tempHeroConfig, topBadge: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                      العنوان الرئيسي (Headline)
                    </label>
                    <input
                      type="text"
                      value={tempHeroConfig.headline}
                      onChange={(e) => setTempHeroConfig({ ...tempHeroConfig, headline: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors font-bold"
                    />
                  </div>
                  <div>
                    <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                      اسم البراند المميز (Brand Highlight)
                    </label>
                    <input
                      type="text"
                      value={tempHeroConfig.brandHighlight}
                      onChange={(e) => setTempHeroConfig({ ...tempHeroConfig, brandHighlight: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors font-bold"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                    النص التعريفي التفصيلي (Subheadline)
                  </label>
                  <textarea
                    rows={3}
                    value={tempHeroConfig.subheadline}
                    onChange={(e) => setTempHeroConfig({ ...tempHeroConfig, subheadline: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                      نص زر تحميل الـ APK
                    </label>
                    <input
                      type="text"
                      value={tempHeroConfig.apkDownloadLabel}
                      onChange={(e) => setTempHeroConfig({ ...tempHeroConfig, apkDownloadLabel: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                    />
                  </div>
                  <div>
                    <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                      نص ميزة التجربة في العدادات
                    </label>
                    <input
                      type="text"
                      value={tempHeroConfig.trialHighlight}
                      onChange={(e) => setTempHeroConfig({ ...tempHeroConfig, trialHighlight: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                    />
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3">
                  <div className="text-xs sm:text-sm text-slate-400 flex items-center gap-2">
                    <CloudCheck className="w-4 h-4 text-emerald-400" />
                    <span>يتم الحفظ تلقائياً في التخزين المحلي دورياً عند التوقف عن الكتابة</span>
                  </div>
                  <button
                    onClick={handleSaveHeroConfig}
                    className="w-full sm:w-auto bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold px-6 py-3 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 tracking-wide"
                  >
                    حفظ يدوي فوري
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* TAB 6: GOLD PRICES */}
          {activeTab === 'gold_prices' && (
            <div className="space-y-6 max-w-4xl">
              <div>
                <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                  <Coins className="w-6 h-6 text-amber-400" />
                  تحديث أسعار الذهب اللحظية المباشرة
                </h1>
                <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                  الأسعار المدخلة هنا يتم نشرها فورياً على الصفحة العامة وفي محرك حسابات تطبيق OOZOX.
                </p>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {goldPrices.map((g) => (
                    <div key={g.karat} className="bg-slate-950 border border-slate-800 p-5 rounded-2xl space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="font-black text-amber-400 text-sm sm:text-base tracking-tight">
                          {g.karat === 'Pound' ? 'الجنيه الذهب' : g.karat === 'Ounce' ? 'الأوقية' : `عيار ${g.karat}`}
                        </span>
                        <span className="text-xs text-slate-500 font-mono">{g.lastUpdated}</span>
                      </div>

                      <div className="space-y-2.5">
                        <div>
                          <label className="text-xs font-bold text-slate-400 block mb-1.5 tracking-wide">سعر الشراء (ج.م):</label>
                          <input
                            type="number"
                            value={g.buyPrice}
                            onChange={(e) => handleUpdatePrice(g.karat, 'buyPrice', Number(e.target.value))}
                            className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-sm text-white font-mono tabular-nums font-bold focus:border-amber-400 focus:outline-none"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-bold text-slate-400 block mb-1.5 tracking-wide">سعر البيع (ج.م):</label>
                          <input
                            type="number"
                            value={g.sellPrice}
                            onChange={(e) => handleUpdatePrice(g.karat, 'sellPrice', Number(e.target.value))}
                            className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-sm text-white font-mono tabular-nums font-bold focus:border-amber-400 focus:outline-none"
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 7: COMPANIES / SUBSCRIBERS */}
          {activeTab === 'companies' && (
            <div className="space-y-6">
              <div>
                <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                  <Building2 className="w-6 h-6 text-amber-400" />
                  قائمة الشركات والمشتركين والاشتراكات
                </h1>
                <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                  عرض الشركات ومحلات الذهب والمقاولين المسجلين في منصة أوزوكس.
                </p>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-3xl overflow-hidden shadow-xl">
                <table className="w-full text-right text-xs sm:text-sm">
                  <thead className="bg-slate-950 text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="p-4 sm:p-5 font-bold tracking-wide">اسم المنشأة / العميل</th>
                      <th className="p-4 sm:p-5 font-bold tracking-wide">النشاط</th>
                      <th className="p-4 sm:p-5 font-bold tracking-wide">الباقة الحالية</th>
                      <th className="p-4 sm:p-5 font-bold tracking-wide">حالة الاشتراك</th>
                      <th className="p-4 sm:p-5 font-bold tracking-wide">رقم الهاتف المقترن</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    <tr>
                      <td colSpan={5} className="p-8 text-center text-xs sm:text-sm text-slate-400">
                        لا توجد منشآت مسجلة معروضة حالياً. يتم جلب الحسابات المعتمدة مباشرة عبر واجهة <span className="font-mono text-amber-400">/api/auth/search</span> عند الربط بالخادم.
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 8: JOB OPPORTUNITIES */}
          {activeTab === 'jobs' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                    <Briefcase className="w-6 h-6 text-amber-400" />
                    إدارة فرص التوظيف والتنبيهات
                  </h1>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
                    قم بنشر وظائف جديدة ليتم إخطار المستخدمين الذين تتطابق تخصصاتهم مع المسمى الوظيفي.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Add Job Form */}
                <div className="lg:col-span-1 bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-4 h-fit">
                  <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
                    <Plus className="w-4 h-4 text-amber-400" />
                    نشر وظيفة جديدة
                  </h3>
                  <form onSubmit={handlePostJob} className="space-y-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider">المسمى الوظيفي</label>
                      <input
                        type="text"
                        value={newJobForm.title}
                        onChange={(e) => setNewJobForm({ ...newJobForm, title: e.target.value })}
                        placeholder="مثال: محاسب قانوني"
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:border-amber-400 focus:outline-none"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider">الشركة / صاحب العمل</label>
                      <input
                        type="text"
                        value={newJobForm.company}
                        onChange={(e) => setNewJobForm({ ...newJobForm, company: e.target.value })}
                        placeholder="مثال: مكتب العطار للمحاسبة"
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:border-amber-400 focus:outline-none"
                        required
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider">الموقع</label>
                        <input
                          type="text"
                          value={newJobForm.location}
                          onChange={(e) => setNewJobForm({ ...newJobForm, location: e.target.value })}
                          placeholder="القاهرة"
                          className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:border-amber-400 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider">الراتب المتوقع</label>
                        <input
                          type="text"
                          value={newJobForm.salary}
                          onChange={(e) => setNewJobForm({ ...newJobForm, salary: e.target.value })}
                          placeholder="قابل للتفاوض"
                          className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:border-amber-400 focus:outline-none"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider">التصنيف</label>
                      <input
                        type="text"
                        value={newJobForm.category}
                        onChange={(e) => setNewJobForm({ ...newJobForm, category: e.target.value })}
                        placeholder="مالية / محاسبة"
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:border-amber-400 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider">وصف مختصر</label>
                      <textarea
                        rows={3}
                        value={newJobForm.description}
                        onChange={(e) => setNewJobForm({ ...newJobForm, description: e.target.value })}
                        placeholder="اكتب تفاصيل الوظيفة والمتطلبات..."
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:border-amber-400 focus:outline-none"
                      />
                    </div>
                    <button
                      type="submit"
                      className="w-full bg-amber-500 hover:bg-amber-400 text-slate-950 font-black py-3 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 transition-all flex items-center justify-center gap-2"
                    >
                      <Megaphone className="w-4 h-4" />
                      نشر وإرسال إشعارات فورية
                    </button>
                  </form>
                </div>

                {/* Jobs List */}
                <div className="lg:col-span-2 space-y-4">
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="text-sm font-bold text-slate-300">الوظائف المنشورة مؤخراً</h3>
                    <span className="text-[10px] text-slate-500 font-mono">العدد: {jobs.length}</span>
                  </div>
                  
                  {jobs.length === 0 ? (
                    <div className="bg-slate-900 border border-slate-800 border-dashed rounded-3xl p-10 text-center text-slate-500">
                      لا يوجد وظائف منشورة حالياً. ابدأ بنشر أول وظيفة.
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {jobs.map(job => (
                        <div key={job.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex items-center justify-between group hover:border-amber-500/30 transition-all">
                          <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-xl bg-slate-950 flex items-center justify-center text-amber-500 border border-slate-800 group-hover:scale-105 transition-transform">
                              <Briefcase className="w-6 h-6" />
                            </div>
                            <div>
                              <h4 className="text-sm font-bold text-white">{job.title}</h4>
                              <p className="text-[11px] text-slate-400 flex items-center gap-1.5 mt-0.5">
                                <Building2 className="w-3 h-3" /> {job.company} • {job.location}
                              </p>
                              <p className="text-[10px] text-slate-500 mt-1 italic">تم النشر في: {job.postedDate}</p>
                            </div>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                              {job.salary}
                            </span>
                            <button 
                              onClick={() => handleDeleteJob(job.id)}
                              className="p-2 rounded-lg text-slate-500 hover:bg-rose-500/10 hover:text-rose-500 transition-all"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 8: LIVE THEME PREVIEWER & BRAND PALETTE STUDIO */}
          {activeTab === 'theme_customizer' && (
            <div className="space-y-8">
              {/* Header & Status Ribbon */}
              <div className="bg-gradient-to-r from-slate-900 via-slate-900 to-slate-800 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden">
                <div 
                  className="absolute top-0 left-0 w-80 h-80 blur-3xl opacity-20 pointer-events-none rounded-full"
                  style={{ backgroundColor: selectedPreviewTheme.primary }}
                />

                <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6 relative z-10">
                  <div className="space-y-2">
                    <div className="flex items-center gap-2.5">
                      <div 
                        className="w-10 h-10 rounded-2xl flex items-center justify-center border shadow-inner transition-colors duration-300"
                        style={{ 
                          backgroundColor: `${selectedPreviewTheme.primary}20`,
                          borderColor: `${selectedPreviewTheme.primary}40`,
                          color: selectedPreviewTheme.primary 
                        }}
                      >
                        <Palette className="w-5 h-5" />
                      </div>
                      <h1 className="text-xl sm:text-2xl lg:text-3xl font-black text-white tracking-tight leading-snug">
                        معاين ومخصص هوية وألوان المنصة المباشرة
                      </h1>
                    </div>
                    <p className="text-xs sm:text-sm text-slate-400 max-w-2xl leading-relaxed">
                      اختبر ألوان التمييز الأساسية (Primary Accent Colors) بدقة وشاهد فوراً كيف تنعكس في الوقت الفعلي على جميع عناصر الموقع (الأزرار، البانرات، أسعار الذهب، وأشرطة التقدم) قبل اعتمادها للمستخدمين.
                    </p>
                  </div>

                  {/* Top Action Buttons */}
                  <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
                    <button
                      onClick={() => handleApplyAndSaveTheme(selectedPreviewTheme)}
                      className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl text-xs sm:text-sm font-black text-slate-950 flex items-center justify-center gap-2 shadow-xl hover:scale-105 active:scale-95 transition-all duration-200"
                      style={{ backgroundColor: selectedPreviewTheme.primary }}
                    >
                      <Check className="w-4 h-4 text-slate-950 stroke-[3]" />
                      <span>حفظ واعتماد هذا اللون على الموقع الحي</span>
                    </button>

                    <button
                      onClick={handleResetToDefaultTheme}
                      className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 flex items-center gap-2 transition-colors"
                      title="استعادة اللون الافتراضي (الذهب الملكي الأصيل)"
                    >
                      <RotateCcw className="w-4 h-4" />
                      <span>استعادة الافتراضي</span>
                    </button>

                    <button
                      onClick={onViewLiveSite}
                      className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-slate-800 hover:bg-slate-700 text-amber-400 border border-slate-700 flex items-center gap-2 transition-colors"
                    >
                      <Eye className="w-4 h-4" />
                      <span>معاينة الموقع العام</span>
                    </button>
                  </div>
                </div>

                {/* Status Bar */}
                <div className="mt-6 pt-5 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-4 text-xs">
                  <div className="flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-2 bg-slate-950/80 border border-slate-800 px-3.5 py-1.5 rounded-xl">
                      <span className="text-slate-400">اللون المعتمد حالياً:</span>
                      <div className="flex items-center gap-1.5">
                        <span 
                          className="w-3 h-3 rounded-full border border-white/20"
                          style={{ backgroundColor: activeCurrentTheme.primary }} 
                        />
                        <span className="font-bold text-white">{activeCurrentTheme.name}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 bg-slate-950/80 border border-slate-800 px-3.5 py-1.5 rounded-xl">
                      <span className="text-slate-400">قيد المعاينة الآن:</span>
                      <div className="flex items-center gap-1.5">
                        <span 
                          className="w-3 h-3 rounded-full animate-pulse border border-white/20"
                          style={{ backgroundColor: selectedPreviewTheme.primary }} 
                        />
                        <span className="font-bold text-white">{selectedPreviewTheme.name}</span>
                        <span className="text-[10px] font-mono text-slate-400">({selectedPreviewTheme.primary.toUpperCase()})</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2.5">
                    <label className="flex items-center gap-2 cursor-pointer select-none text-slate-300">
                      <input
                        type="checkbox"
                        checked={isLivePreviewActive}
                        onChange={(e) => {
                          setIsLivePreviewActive(e.target.checked);
                          if (e.target.checked) {
                            applyThemeToDocument(selectedPreviewTheme);
                          } else {
                            applyThemeToDocument(activeCurrentTheme);
                          }
                        }}
                        className="rounded bg-slate-950 border-slate-700 text-amber-500 focus:ring-0 w-4 h-4 cursor-pointer"
                      />
                      <span>تطبيق المعاينة الفورية المباشرة على كامل الواجهة (Live DOM Sync)</span>
                    </label>
                  </div>
                </div>
              </div>

              {/* Main Two-Column Layout: Presets + Custom Studio */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Column 1 & 2: Curated Presets Grid */}
                <div className="lg:col-span-2 space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
                      <Sparkles className="w-4 h-4 text-amber-400" />
                      <span>باقات الألوان الجاهزة والمعتمدة (Curated Accent Palettes)</span>
                    </h3>
                    <span className="text-xs text-slate-400 font-mono">8 باقات احترافية</span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {themePresets.map((preset) => {
                      const isSelected = selectedPreviewTheme.id === preset.id;
                      const isActiveLive = activeCurrentTheme.id === preset.id;

                      return (
                        <div
                          key={preset.id}
                          onClick={() => handleSelectPreset(preset)}
                          className={`group cursor-pointer rounded-2xl p-4 sm:p-5 border transition-all duration-200 relative overflow-hidden ${
                            isSelected
                              ? 'bg-slate-900 shadow-2xl scale-[1.01]'
                              : 'bg-slate-900/60 hover:bg-slate-900 border-slate-800/80 hover:border-slate-700'
                          }`}
                          style={{
                            borderColor: isSelected ? preset.primary : undefined,
                            boxShadow: isSelected ? `0 10px 30px -10px ${preset.primary}40` : undefined,
                          }}
                        >
                          {/* Ambient Glow */}
                          <div 
                            className="absolute -top-10 -right-10 w-24 h-24 rounded-full blur-2xl opacity-15 pointer-events-none transition-opacity group-hover:opacity-30"
                            style={{ backgroundColor: preset.primary }}
                          />

                          {/* Top Row: Name, Swatch, and Badges */}
                          <div className="flex items-start justify-between gap-2 relative z-10 mb-3">
                            <div className="flex items-center gap-3">
                              <div 
                                className="w-9 h-9 rounded-xl flex items-center justify-center shadow-lg border border-white/20 group-hover:scale-110 transition-transform"
                                style={{ backgroundColor: preset.primary }}
                              >
                                {isSelected && <Check className="w-4 h-4 text-slate-950 stroke-[3]" />}
                              </div>
                              <div>
                                <h4 className="text-sm font-black text-white leading-tight">
                                  {preset.name}
                                </h4>
                                <span className="text-[11px] text-slate-400 font-mono tracking-wide">
                                  {preset.nameEn}
                                </span>
                              </div>
                            </div>

                            <div className="flex flex-col items-end gap-1">
                              {isActiveLive && (
                                <span className="text-[10px] font-black bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded-md">
                                  النشط على الموقع
                                </span>
                              )}
                              {isSelected && !isActiveLive && (
                                <span 
                                  className="text-[10px] font-black px-2 py-0.5 rounded-md border"
                                  style={{
                                    backgroundColor: `${preset.primary}20`,
                                    borderColor: `${preset.primary}40`,
                                    color: preset.primary
                                  }}
                                >
                                  قيد المعاينة
                                </span>
                              )}
                            </div>
                          </div>

                          {/* Description */}
                          <p className="text-xs text-slate-300 leading-relaxed mb-4 line-clamp-2">
                            {preset.description}
                          </p>

                          {/* Color Gradient Spectrum Bar */}
                          <div className="space-y-1.5">
                            <div className="h-2 rounded-full flex overflow-hidden shadow-inner">
                              <div className="flex-1" style={{ backgroundColor: preset.primaryHighlight }} title="300 Light" />
                              <div className="flex-1" style={{ backgroundColor: preset.primaryLight }} title="400 Medium Light" />
                              <div className="flex-1" style={{ backgroundColor: preset.primary }} title="500 Primary" />
                              <div className="flex-1" style={{ backgroundColor: preset.primaryDark }} title="600 Dark" />
                            </div>
                            <div className="flex justify-between text-[10px] font-mono text-slate-400">
                              <span>300: {preset.primaryHighlight}</span>
                              <span className="font-bold text-white">500: {preset.primary}</span>
                              <span>600: {preset.primaryDark}</span>
                            </div>
                          </div>

                          {/* Quick Actions Footer */}
                          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between gap-2">
                            <span className="text-[11px] text-slate-400 group-hover:text-slate-200 transition-colors">
                              انقر للمعاينة الفورية
                            </span>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleApplyAndSaveTheme(preset);
                              }}
                              className="text-[11px] font-black px-3 py-1 rounded-lg bg-slate-800 hover:bg-white hover:text-slate-950 text-slate-200 transition-all border border-slate-700"
                            >
                              اعتماد ونشر
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Column 3: Custom Color Studio & Shading Engine */}
                <div className="space-y-6">
                  <div className="bg-slate-900 border border-slate-800 rounded-3xl p-5 sm:p-6 space-y-5 shadow-xl">
                    <div className="flex items-center gap-2.5 pb-3 border-b border-slate-800">
                      <div className="w-8 h-8 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center justify-center">
                        <Paintbrush className="w-4 h-4" />
                      </div>
                      <div>
                        <h3 className="text-sm sm:text-base font-bold text-white">أداة التخصيص الحر (Custom Hex)</h3>
                        <p className="text-[11px] text-slate-400">اختر أي لون تريده للمنصة وسنولد درجاته آلياً</p>
                      </div>
                    </div>

                    {/* Color Input Controls */}
                    <div className="space-y-3">
                      <label className="block text-xs font-bold text-slate-300">اللون الأساسي (Primary HEX)</label>
                      <div className="flex items-center gap-3">
                        {/* Native Color Picker Circle */}
                        <div className="relative w-12 h-12 rounded-2xl overflow-hidden border border-slate-700 flex-shrink-0 cursor-pointer shadow-inner">
                          <input
                            type="color"
                            value={customColorHex}
                            onChange={(e) => handleCustomHexChange(e.target.value)}
                            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                          />
                          <div 
                            className="w-full h-full flex items-center justify-center"
                            style={{ backgroundColor: customColorHex }}
                          >
                            <Pipette className="w-4 h-4 text-slate-950 mix-blend-difference" />
                          </div>
                        </div>

                        {/* Hex Text Field */}
                        <div className="flex-1 relative">
                          <input
                            type="text"
                            value={customColorHex}
                            onChange={(e) => handleCustomHexChange(e.target.value)}
                            placeholder="#F59E0B"
                            maxLength={7}
                            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-sm font-mono text-white focus:outline-none uppercase tracking-wider"
                            style={{ borderColor: selectedPreviewTheme.isCustom ? selectedPreviewTheme.primary : undefined }}
                          />
                        </div>
                      </div>
                    </div>

                    {/* Auto-Generated Shades Swatches */}
                    <div className="space-y-2 pt-2 border-t border-slate-800/80">
                      <span className="text-xs font-bold text-slate-400">تدرجات اللون المولدة آلياً:</span>
                      <div className="grid grid-cols-4 gap-2 text-center">
                        <div className="bg-slate-950 border border-slate-800 p-2 rounded-xl">
                          <div 
                            className="w-full h-7 rounded-lg mb-1 shadow-sm"
                            style={{ backgroundColor: selectedPreviewTheme.primaryHighlight }}
                          />
                          <span className="text-[10px] text-slate-400 font-mono">300</span>
                        </div>
                        <div className="bg-slate-950 border border-slate-800 p-2 rounded-xl">
                          <div 
                            className="w-full h-7 rounded-lg mb-1 shadow-sm"
                            style={{ backgroundColor: selectedPreviewTheme.primaryLight }}
                          />
                          <span className="text-[10px] text-slate-400 font-mono">400</span>
                        </div>
                        <div className="bg-slate-950 border border-slate-800 p-2 rounded-xl">
                          <div 
                            className="w-full h-7 rounded-lg mb-1 shadow-sm"
                            style={{ backgroundColor: selectedPreviewTheme.primary }}
                          />
                          <span className="text-[10px] font-bold text-white font-mono">500</span>
                        </div>
                        <div className="bg-slate-950 border border-slate-800 p-2 rounded-xl">
                          <div 
                            className="w-full h-7 rounded-lg mb-1 shadow-sm"
                            style={{ backgroundColor: selectedPreviewTheme.primaryDark }}
                          />
                          <span className="text-[10px] text-slate-400 font-mono">600</span>
                        </div>
                      </div>
                    </div>

                    {/* WCAG Accessibility Rating */}
                    <div className="bg-slate-950/80 border border-slate-800 p-3 rounded-2xl flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <ShieldCheck className="w-4 h-4 text-emerald-400" />
                        <span className="text-slate-300">توافق التباين البصري</span>
                      </div>
                      <span className="font-bold text-emerald-400 font-mono">WCAG AAA متوافق</span>
                    </div>

                    {/* Apply Custom Button */}
                    <button
                      onClick={() => handleApplyAndSaveTheme(selectedPreviewTheme)}
                      className="w-full py-3 rounded-xl text-xs sm:text-sm font-black text-slate-950 flex items-center justify-center gap-2 shadow-lg transition-all hover:scale-[1.02] active:scale-95"
                      style={{ backgroundColor: selectedPreviewTheme.primary }}
                    >
                      <Check className="w-4 h-4 text-slate-950 stroke-[3]" />
                      <span>اعتماد هذا التخصيص للموقع الحي</span>
                    </button>
                  </div>
                </div>

              </div>

              {/* SECTION 3: INTERACTIVE LIVE COMPONENT PREVIEW SANDBOX */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Sliders className="w-4 h-4 text-amber-400" />
                    <h3 className="text-base sm:text-lg font-black text-white tracking-tight">
                      مختبر معاينة العناصر الحية (Live Components Sandbox)
                    </h3>
                  </div>
                  <span className="text-xs text-slate-400">
                    هكذا تظهر الأزرار، التنبيهات، وأسعار الذهب للمستخدمين باللون الجديد
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  
                  {/* Sandbox Card 1: Hero Action CTA Buttons */}
                  <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-4 shadow-xl">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                      <span className="text-xs font-bold text-slate-300">أزرار الإجراءات الرئيسية (Hero CTA)</span>
                      <span className="text-[10px] text-slate-400 font-mono">زر التحميل والطلب</span>
                    </div>

                    <div className="space-y-3 pt-1">
                      {/* Hero Badge */}
                      <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold border"
                        style={{
                          backgroundColor: `${selectedPreviewTheme.primary}15`,
                          borderColor: `${selectedPreviewTheme.primary}40`,
                          color: selectedPreviewTheme.primary
                        }}
                      >
                        <span 
                          className="w-2 h-2 rounded-full animate-ping"
                          style={{ backgroundColor: selectedPreviewTheme.primary }} 
                        />
                        <span>الإصدار المعتمد 2026 • تحديثات لحظية</span>
                      </div>

                      {/* Primary CTA Button */}
                      <button
                        className="w-full py-3.5 px-6 rounded-xl font-black text-xs sm:text-sm text-slate-950 flex items-center justify-center gap-2.5 shadow-xl transition-transform hover:scale-[1.02] active:scale-95"
                        style={{
                          backgroundColor: selectedPreviewTheme.primary,
                          boxShadow: `0 10px 25px -5px ${selectedPreviewTheme.primary}50`
                        }}
                      >
                        <Smartphone className="w-4 h-4" />
                        <span>تحميل تطبيق أوزوكس APK مجاناً</span>
                      </button>

                      {/* Secondary Outline Button */}
                      <button
                        className="w-full py-3 px-6 rounded-xl font-bold text-xs sm:text-sm bg-slate-950 border hover:bg-slate-800 transition-all flex items-center justify-center gap-2"
                        style={{
                          borderColor: `${selectedPreviewTheme.primary}60`,
                          color: selectedPreviewTheme.primaryLight
                        }}
                      >
                        <Eye className="w-4 h-4" />
                        <span>مشاهدة العرض التفاعلي الحي</span>
                      </button>
                    </div>
                  </div>

                  {/* Sandbox Card 2: Live Gold Rates Simulator */}
                  <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-4 shadow-xl">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                      <span className="text-xs font-bold text-slate-300">شريط أسعار الذهب اللحظي (Gold Tickers)</span>
                      <span 
                        className="text-[10px] font-bold px-2 py-0.5 rounded"
                        style={{
                          backgroundColor: `${selectedPreviewTheme.primary}20`,
                          color: selectedPreviewTheme.primary
                        }}
                      >
                        مباشر
                      </span>
                    </div>

                    <div className="space-y-2.5">
                      <div 
                        className="bg-slate-950 p-3.5 rounded-2xl border flex items-center justify-between transition-colors"
                        style={{ borderColor: `${selectedPreviewTheme.primary}40` }}
                      >
                        <div className="flex items-center gap-2.5">
                          <div 
                            className="w-8 h-8 rounded-xl flex items-center justify-center font-black text-xs text-slate-950"
                            style={{ backgroundColor: selectedPreviewTheme.primary }}
                          >
                            24
                          </div>
                          <div>
                            <p className="text-xs font-bold text-white">عيار 24 (الذهب الخالص)</p>
                            <span className="text-[10px] text-slate-400">سعر الشراء الحالي</span>
                          </div>
                        </div>
                        <div className="text-left">
                          <p 
                            className="text-sm font-black font-mono"
                            style={{ color: selectedPreviewTheme.primaryLight }}
                          >
                            3,450 ج.م
                          </p>
                          <span className="text-[10px] text-emerald-400 font-bold">↑ +15 ج.م</span>
                        </div>
                      </div>

                      <div 
                        className="bg-slate-950 p-3.5 rounded-2xl border flex items-center justify-between transition-colors"
                        style={{ borderColor: `${selectedPreviewTheme.primary}30` }}
                      >
                        <div className="flex items-center gap-2.5">
                          <div 
                            className="w-8 h-8 rounded-xl flex items-center justify-center font-black text-xs text-slate-950"
                            style={{ backgroundColor: selectedPreviewTheme.primaryLight }}
                          >
                            21
                          </div>
                          <div>
                            <p className="text-xs font-bold text-white">عيار 21 (الأكثر تداولاً)</p>
                            <span className="text-[10px] text-slate-400">سعر الشراء الحالي</span>
                          </div>
                        </div>
                        <div className="text-left">
                          <p 
                            className="text-sm font-black font-mono"
                            style={{ color: selectedPreviewTheme.primaryLight }}
                          >
                            3,018 ج.م
                          </p>
                          <span className="text-[10px] text-emerald-400 font-bold">↑ +12 ج.م</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Sandbox Card 3: Tutorial Progress Bar */}
                  <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-4 shadow-xl">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                      <span className="text-xs font-bold text-slate-300">أشرطة تقدم الشروحات (Tutorial Progress)</span>
                      <span 
                        className="text-[11px] font-black font-mono"
                        style={{ color: selectedPreviewTheme.primary }}
                      >
                        {Math.round((interactiveTestSteps.length / 3) * 100)}% منجز
                      </span>
                    </div>

                    <div className="space-y-3">
                      {/* Animated Progress Bar */}
                      <div className="h-2 bg-slate-950 rounded-full overflow-hidden p-0.5 border border-slate-800">
                        <div 
                          className="h-full rounded-full transition-all duration-500 shadow-sm"
                          style={{ 
                            width: `${(interactiveTestSteps.length / 3) * 100}%`,
                            backgroundColor: selectedPreviewTheme.primary 
                          }}
                        />
                      </div>

                      {/* Interactive Test Steps */}
                      <div className="space-y-2 pt-1">
                        {['تثبيت التطبيق وإنشاء الحساب', 'توصيل ميزان الذهب بالبلوتوث', 'طباعة أول باركود للمصوغات'].map((step, idx) => {
                          const isDone = interactiveTestSteps.includes(idx);
                          return (
                            <div
                              key={idx}
                              onClick={() => toggleTestStep(idx)}
                              className={`flex items-center justify-between p-2.5 rounded-xl text-xs cursor-pointer border transition-all ${
                                isDone 
                                  ? 'bg-slate-950 text-white' 
                                  : 'bg-slate-950/50 text-slate-400 border-slate-800/80 hover:border-slate-700'
                              }`}
                              style={{ borderColor: isDone ? `${selectedPreviewTheme.primary}50` : undefined }}
                            >
                              <div className="flex items-center gap-2">
                                <div 
                                  className={`w-4 h-4 rounded-md flex items-center justify-center border text-[10px] ${
                                    isDone ? 'text-slate-950' : 'border-slate-700'
                                  }`}
                                  style={{
                                    backgroundColor: isDone ? selectedPreviewTheme.primary : 'transparent',
                                    borderColor: isDone ? selectedPreviewTheme.primary : undefined
                                  }}
                                >
                                  {isDone && <Check className="w-3 h-3 stroke-[3]" />}
                                </div>
                                <span className={isDone ? 'font-bold' : ''}>{step}</span>
                              </div>
                              <span className="text-[10px] text-slate-400 font-mono">
                                {isDone ? 'مكتمل' : 'انقر للتجربة'}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>

                  {/* Sandbox Card 4: Broadcast Alert Banner Toast Simulation */}
                  <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-4 shadow-xl md:col-span-2 lg:col-span-3">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                      <div className="flex items-center gap-2">
                        <Radio className="w-4 h-4 text-rose-400 animate-pulse" />
                        <span className="text-xs font-bold text-slate-300">معاينة إشعار البث المباشر (Broadcast Toast Preview)</span>
                      </div>
                      <span className="text-[10px] text-slate-400">محاكاة فورية كما تظهر لزوار الصفحة الرئيسية</span>
                    </div>

                    <div 
                      className="bg-slate-950/90 border rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xl relative overflow-hidden"
                      style={{ 
                        borderColor: `${selectedPreviewTheme.primary}40`,
                        boxShadow: `0 10px 30px -10px ${selectedPreviewTheme.primary}20` 
                      }}
                    >
                      <div 
                        className="absolute -top-10 -right-10 w-32 h-32 rounded-full blur-3xl opacity-20 pointer-events-none"
                        style={{ backgroundColor: selectedPreviewTheme.primary }}
                      />

                      <div className="flex items-center gap-3.5 relative z-10">
                        <div 
                          className="w-10 h-10 rounded-xl flex items-center justify-center font-bold text-slate-950 shadow-md flex-shrink-0"
                          style={{ backgroundColor: selectedPreviewTheme.primary }}
                        >
                          <Radio className="w-5 h-5 animate-pulse" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span 
                              className="text-[10px] font-black px-2 py-0.5 rounded"
                              style={{ 
                                backgroundColor: `${selectedPreviewTheme.primary}20`,
                                color: selectedPreviewTheme.primary 
                              }}
                            >
                              بث عاجل 🔴
                            </span>
                            <span className="text-[10px] text-slate-400">إشعار الموقع العام</span>
                          </div>
                          <h4 className="text-sm font-black text-white mt-1">تحديث شامل في أسعار الفائدة والذهب اللحظي</h4>
                          <p className="text-xs text-slate-300 mt-0.5">تم تحديث منظومة التدقيق المالي وربطها بالأسعار الرسمية للذهب.</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 w-full sm:w-auto relative z-10">
                        <button
                          className="flex-1 sm:flex-none px-4 py-2 rounded-xl text-xs font-black text-slate-950 shadow-md"
                          style={{ backgroundColor: selectedPreviewTheme.primary }}
                        >
                          عرض التفاصيل
                        </button>
                      </div>
                    </div>
                  </div>

                </div>
              </div>
            </div>
          )}

          {/* TAB: CALLS & MONTHLY RTC BUDGET TELEMETRY */}
          {activeTab === 'calls_budget' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-800 pb-4">
                <div>
                  <h1 className="text-2xl font-black text-white flex items-center gap-2">
                    <PhoneCall className="w-6 h-6 text-emerald-400" />
                    مراقبة المكالمات (WebRTC / TURN) وميزانية الإنفاق الشهرية
                  </h1>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1">
                    تنبيهات تلقائية عند 50% و80%، وإيقاف بدء المكالمات الجديدة فقط عند 100% دون قطع المكالمات النشطة.
                  </p>
                </div>
                <button
                  onClick={() => void loadRtcTelemetry()}
                  className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs flex items-center gap-2"
                >
                  <RefreshCw className={`w-4 h-4 ${rtcLoading ? 'animate-spin' : ''}`} />
                  تحديث القياسات
                </button>
              </div>

              {rtcBudget ? (
                <>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                      <span className="text-xs text-slate-400 font-bold">نسبة الاستهلاك الشهري ({rtcBudget.monthKey})</span>
                      <p className={`text-3xl font-black font-mono mt-2 ${
                        rtcBudget.usagePct >= 100 ? 'text-rose-400' : rtcBudget.usagePct >= 80 ? 'text-amber-400' : 'text-emerald-400'
                      }`}>
                        {rtcBudget.usagePct.toFixed(1)}%
                      </p>
                      <p className="text-xs text-slate-400 mt-1 font-mono">
                        ${rtcBudget.estimatedCostUsd.toFixed(2)} / ${rtcBudget.monthlyBudgetUsd.toFixed(2)}
                      </p>
                    </div>
                    <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                      <span className="text-xs text-slate-400 font-bold">حالة المكالمات الجديدة</span>
                      <p className={`text-xl font-black mt-2 ${rtcBudget.newCallsBlocked ? 'text-rose-400' : 'text-emerald-400'}`}>
                        {rtcBudget.newCallsBlocked ? 'موقوفة عند 100%' : 'متاحة وتعمل بشكل طبيعي'}
                      </p>
                      <p className="text-xs text-slate-500 mt-1">
                        عتبات التنبيه: {rtcBudget.thresholds.alert1Pct}% • {rtcBudget.thresholds.alert2Pct}% • 100%
                      </p>
                    </div>
                    <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                      <span className="text-xs text-slate-400 font-bold">إجمالي الدقائق المفوترة</span>
                      <p className="text-3xl font-black font-mono text-white mt-2">{rtcBudget.totalBilledMinutes}</p>
                      <p className="text-xs text-slate-400 mt-1">
                        مكالمات مجاب عليها: {rtcBudget.answeredCallsCount} من {rtcBudget.totalCallsCount}
                      </p>
                    </div>
                    <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
                      <span className="text-xs text-slate-400 font-bold">ترافيك Coturn TURN</span>
                      <p className="text-3xl font-black font-mono text-cyan-400 mt-2">
                        {(rtcBudget.turnBytesTotal / (1024 * 1024)).toFixed(2)} MB
                      </p>
                      <p className="text-xs text-slate-500 mt-1">
                        إعادة تشغيل الخادم: {rtcBudget.coturnRestartCount}
                      </p>
                    </div>
                  </div>

                  {rtcBudget.alertsTriggered.length > 0 && (
                    <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center gap-3 text-xs text-amber-200">
                      <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />
                      <span>
                        التنبيهات المُفعّلة هذا الشهر: {rtcBudget.alertsTriggered.map((t) => `${t}%`).join(' ، ')}
                      </span>
                    </div>
                  )}

                  <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
                    <div className="px-5 py-4 border-b border-slate-800 flex items-center justify-between">
                      <h3 className="text-sm font-black text-white">أحدث سجلات المكالمات</h3>
                      <span className="text-xs text-slate-400">{rtcCalls.length} سجل</span>
                    </div>
                    {rtcCalls.length === 0 ? (
                      <p className="p-6 text-center text-xs text-slate-500">لا توجد مكالمات مسجلة حتى الآن.</p>
                    ) : (
                      <div className="divide-y divide-slate-800">
                        {rtcCalls.map((call) => (
                          <div key={call._id} className="px-5 py-3 flex items-center justify-between gap-4 text-xs">
                            <div>
                              <p className="font-bold text-white">
                                {call.callerId?.name || call.callerId?.phone || 'متصل'} ← {call.calleeId?.name || call.calleeId?.phone || 'مستقبل'}
                              </p>
                              <p className="text-[11px] text-slate-400 font-mono">
                                {call.callType} • {call.status} • {call.billedDurationSeconds}s
                              </p>
                            </div>
                            <span className="font-mono font-bold text-amber-400">
                              ${Number(call.estimatedCostUsd || 0).toFixed(4)}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </>
              ) : (
                <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 text-center text-xs text-slate-400">
                  {rtcLoading ? 'جاري جلب بيانات الميزانية والمكالمات من الخادم...' : 'لا تتوفر بيانات حالياً — تأكد من تسجيل الدخول كـ ADMIN.'}
                </div>
              )}
            </div>
          )}

        </main>
      </div>

      {/* MODAL: ADD / EDIT FEATURE */}
      {showAddFeatureModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-xl p-6 sm:p-8 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-lg sm:text-xl font-bold text-white tracking-tight flex items-center gap-2">
                <Zap className="w-5 h-5 text-amber-400" />
                {editingFeature ? 'تعديل ميزة في الصفحة العامة' : 'إضافة ميزة جديدة للمشروع'}
              </h3>
              <button 
                onClick={() => setShowAddFeatureModal(false)}
                className="text-xs sm:text-sm font-bold text-slate-400 hover:text-white px-3 py-1.5 rounded-lg hover:bg-slate-800 transition-colors"
              >
                إغلاق
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">عنوان الميزة</label>
                <input
                  type="text"
                  placeholder="مثال: المحاسب الذكي والهاشتاج المالي"
                  value={featTitle}
                  onChange={(e) => setFeatTitle(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">التصنيف</label>
                  <select
                    value={featCategory}
                    onChange={(e) => setFeatCategory(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  >
                    <option value="محاسبة ومالية">محاسبة ومالية</option>
                    <option value="أمان وخصوصية">أمان وخصوصية</option>
                    <option value="ذهب وتجارة">ذهب وتجارة</option>
                    <option value="إدارة ومشاريع">إدارة ومشاريع</option>
                    <option value="ذكاء اصطناعي">ذكاء اصطناعي</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الأيقونة</label>
                  <select
                    value={featIcon}
                    onChange={(e) => setFeatIcon(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  >
                    <option value="Receipt">Receipt (فاتورة ومحاسبة)</option>
                    <option value="Lock">Lock (قفل وخزنة)</option>
                    <option value="Sparkles">Sparkles (ذهب وذكاء)</option>
                    <option value="Users">Users (فريق ومشاريع)</option>
                    <option value="Zap">Zap (سرعة وطاقة)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الوصف التفصيلي للميزة</label>
                <textarea
                  rows={2}
                  placeholder="وصف مبسط لما تقدمه هذه الميزة للمستخدمين..."
                  value={featDesc}
                  onChange={(e) => setFeatDesc(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                  النقاط البارزة (كل نقطة في سطر منفصل)
                </label>
                <textarea
                  rows={3}
                  placeholder="سجلات دائمة غير قابلة للتعديل&#10;تصدير فواتير PDF فوراً&#10;ربط مباشر مع الذهب"
                  value={featHighlightsText}
                  onChange={(e) => setFeatHighlightsText(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                />
              </div>
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
              <button
                onClick={() => setShowAddFeatureModal(false)}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-slate-400 hover:bg-slate-800 transition-colors"
              >
                إلغاء
              </button>
              <button
                onClick={handleSaveFeature}
                className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-black px-6 py-2.5 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 tracking-wide transition-all"
              >
                {editingFeature ? 'حفظ التعديلات' : 'نشر الميزة'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: ADD / EDIT ANNOUNCEMENT */}
      {showAddAnnModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-xl p-6 sm:p-8 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-lg sm:text-xl font-bold text-white tracking-tight flex items-center gap-2">
                <Megaphone className="w-5 h-5 text-amber-400" />
                {editingAnn ? 'تعديل الإعلان' : 'إنشاء إعلان أو بانر جديد'}
              </h3>
              <button 
                onClick={() => setShowAddAnnModal(false)}
                className="text-xs sm:text-sm font-bold text-slate-400 hover:text-white px-3 py-1.5 rounded-lg hover:bg-slate-800 transition-colors"
              >
                إغلاق
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">عنوان الإعلان</label>
                <input
                  type="text"
                  placeholder="مثال: عرض الإطلاق الحصري..."
                  value={newAnnTitle}
                  onChange={(e) => setNewAnnTitle(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">القسم</label>
                  <select
                    value={newAnnCategory}
                    onChange={(e: any) => setNewAnnCategory(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  >
                    <option value="promo">عرض ترويجي (Promo)</option>
                    <option value="system">تحديث نظام وأمان (System)</option>
                    <option value="market">سوق الذهب (Market)</option>
                    <option value="event">فعالية وإطلاق (Event)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">نص الشارة (Badge)</label>
                  <input
                    type="text"
                    value={newAnnBadge}
                    onChange={(e) => setNewAnnBadge(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">محتوى وتفاصيل الإعلان</label>
                <textarea
                  rows={3}
                  value={newAnnContent}
                  onChange={(e) => setNewAnnContent(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">نص الزر التفاعلي</label>
                  <input
                    type="text"
                    value={newAnnActionLabel}
                    onChange={(e) => setNewAnnActionLabel(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">رابط التوجيه (URL)</label>
                  <input
                    type="text"
                    value={newAnnActionUrl}
                    onChange={(e) => setNewAnnActionUrl(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none font-mono transition-colors"
                  />
                </div>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
              <button
                onClick={() => setShowAddAnnModal(false)}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-slate-400 hover:bg-slate-800 transition-colors"
              >
                إلغاء
              </button>
              <button
                onClick={handleSaveAnnouncement}
                className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-black px-6 py-2.5 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 tracking-wide transition-all"
              >
                {editingAnn ? 'حفظ التعديلات' : 'نشر الإعلان'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: BROADCAST SYSTEM-WIDE ANNOUNCEMENT (LIVE TOAST) */}
      {showBroadcastModal && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-2xl p-6 sm:p-8 space-y-6 shadow-2xl my-8 relative">
            {/* Header */}
            <div className="flex items-start justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-rose-500/20 to-amber-500/20 border border-rose-500/40 flex items-center justify-center text-rose-400 shadow-inner">
                  <Radio className="w-6 h-6 animate-pulse" />
                </div>
                <div>
                  <h3 className="text-lg sm:text-xl font-black text-white tracking-tight flex items-center gap-2">
                    بث إشعار عام عاجل للموقع (Live Toast Broadcast)
                  </h3>
                  <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                    يظهر فوراً كإشعار Toast تفاعلي منبثق لجميع زوار موقع oozox.com الحاليين والجدد.
                  </p>
                </div>
              </div>
              <button 
                onClick={() => setShowBroadcastModal(false)}
                className="text-slate-400 hover:text-white p-2 rounded-xl hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Form Fields */}
            <div className="space-y-4">
              {/* Alert Category Type */}
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-2 tracking-wide">
                  نوع ونبرة الإشعار (Alert Type)
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                  <button
                    type="button"
                    onClick={() => {
                      setBroadcastCategory('urgent');
                      setBroadcastBadge('بث عاجل 🔴');
                      setBroadcastBadgeColor('#EF4444');
                    }}
                    className={`p-3 rounded-xl border text-right transition-all ${
                      broadcastCategory === 'urgent'
                        ? 'bg-rose-500/15 border-rose-500 text-rose-300 shadow-md shadow-rose-500/10'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span>عاجل وهام</span>
                      <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
                    </div>
                    <p className="text-[10px] text-slate-400 mt-1">تنبيهات حرجة أو صيانة</p>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setBroadcastCategory('promo');
                      setBroadcastBadge('عرض حصري 🎁');
                      setBroadcastBadgeColor('#10B981');
                    }}
                    className={`p-3 rounded-xl border text-right transition-all ${
                      broadcastCategory === 'promo'
                        ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 shadow-md shadow-emerald-500/10'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span>عرض ترويجي</span>
                      <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
                    </div>
                    <p className="text-[10px] text-slate-400 mt-1">عروض خاصة وجديد</p>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setBroadcastCategory('system');
                      setBroadcastBadge('تحديث منصة ⚡');
                      setBroadcastBadgeColor('#0EA5E9');
                    }}
                    className={`p-3 rounded-xl border text-right transition-all ${
                      broadcastCategory === 'system'
                        ? 'bg-sky-500/15 border-sky-500 text-sky-300 shadow-md shadow-sky-500/10'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span>تحديث نظام</span>
                      <Zap className="w-3.5 h-3.5 text-sky-400" />
                    </div>
                    <p className="text-[10px] text-slate-400 mt-1">إصدارات وميزات</p>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setBroadcastCategory('market');
                      setBroadcastBadge('سوق الذهب 💰');
                      setBroadcastBadgeColor('#F59E0B');
                    }}
                    className={`p-3 rounded-xl border text-right transition-all ${
                      broadcastCategory === 'market'
                        ? 'bg-amber-500/15 border-amber-500 text-amber-300 shadow-md shadow-amber-500/10'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span>تنبيه سوق</span>
                      <TrendingUp className="w-3.5 h-3.5 text-amber-400" />
                    </div>
                    <p className="text-[10px] text-slate-400 mt-1">أسعار وتغيرات اللحظية</p>
                  </button>
                </div>
              </div>

              {/* Title & Badge */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-2">
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    عنوان الإشعار العام *
                  </label>
                  <input
                    type="text"
                    placeholder="مثال: إطلاق تطبيق أوزوكس رسمياً بنسخة APK كاملة المميزات!"
                    value={broadcastTitle}
                    onChange={(e) => setBroadcastTitle(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    نص الشارة (Badge)
                  </label>
                  <input
                    type="text"
                    value={broadcastBadge}
                    onChange={(e) => setBroadcastBadge(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>
              </div>

              {/* Message Content */}
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                  نص وتفاصيل الإشعار الموجه للمستخدمين *
                </label>
                <textarea
                  rows={3}
                  placeholder="أدخل رسالة الإشعار الواضحة التي ستظهر للمستخدم في الـ Toast..."
                  value={broadcastMessage}
                  onChange={(e) => setBroadcastMessage(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                />
              </div>

              {/* Action Button Label & URL */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    نص الزر التفاعلي (اختياري)
                  </label>
                  <input
                    type="text"
                    placeholder="مثال: تحميل التطبيق الآن"
                    value={broadcastActionLabel}
                    onChange={(e) => setBroadcastActionLabel(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    رابط أو قسم التوجيه (URL / Anchor)
                  </label>
                  <input
                    type="text"
                    placeholder="مثال: #download أو #gold-rates"
                    value={broadcastActionUrl}
                    onChange={(e) => setBroadcastActionUrl(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none font-mono transition-colors"
                  />
                </div>
              </div>

              {/* Duration and Settings */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    مدة ظهور الإشعار التلقائي
                  </label>
                  <select
                    value={broadcastDurationSeconds}
                    onChange={(e) => setBroadcastDurationSeconds(Number(e.target.value))}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  >
                    <option value={8}>8 ثوانٍ (سريع)</option>
                    <option value={12}>12 ثانية (موصى به)</option>
                    <option value={20}>20 ثانية (تنبيه طويل)</option>
                    <option value={0}>يبقى ظاهراً حتى يغلقه المستخدم يدوياً</option>
                  </select>
                </div>

                <div className="flex items-center gap-3 pt-6">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={saveAsPersistentAnn}
                      onChange={(e) => setSaveAsPersistentAnn(e.target.checked)}
                      className="w-4 h-4 rounded text-amber-500 bg-slate-950 border-slate-700 focus:ring-amber-400 focus:ring-offset-slate-900"
                    />
                    <span className="text-xs sm:text-sm text-slate-300 font-bold">
                      حفظ ونشر أيضاً في قسم الإعلانات بالصفحة الرئيسية
                    </span>
                  </label>
                </div>
              </div>

              {/* Live Toast Preview */}
              <div className="pt-2">
                <label className="block text-xs font-bold text-slate-400 mb-2 tracking-wide flex items-center gap-1.5">
                  <Eye className="w-3.5 h-3.5 text-amber-400" />
                  معاينة حية للـ Toast كما سيظهر لزوار موقع oozox.com:
                </label>
                <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 shadow-2xl relative overflow-hidden">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3 flex-1">
                      <div 
                        className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5"
                        style={{ backgroundColor: `${broadcastBadgeColor}20`, color: broadcastBadgeColor }}
                      >
                        <Radio className="w-4 h-4 animate-pulse" />
                      </div>
                      <div className="space-y-1 flex-1">
                        <div className="flex items-center gap-2">
                          <span 
                            className="text-[11px] font-black px-2 py-0.5 rounded tracking-wide inline-flex items-center gap-1"
                            style={{ backgroundColor: `${broadcastBadgeColor}20`, color: broadcastBadgeColor }}
                          >
                            <span className="w-1.5 h-1.5 rounded-full animate-ping" style={{ backgroundColor: broadcastBadgeColor }} />
                            {broadcastBadge || 'إشعار بث مباشر'}
                          </span>
                          <span className="text-[10px] text-slate-400 font-mono">الآن</span>
                        </div>
                        <h4 className="text-sm font-black text-white leading-snug">
                          {broadcastTitle || 'عنوان الإشعار سيظهر هنا...'}
                        </h4>
                        <p className="text-xs text-slate-300 leading-relaxed">
                          {broadcastMessage || 'تفاصيل ورسالة البث العام ستظهر في هذا الجزء بدقة ووضوح...'}
                        </p>
                        {broadcastActionLabel && (
                          <div className="pt-1.5">
                            <span className="inline-flex items-center gap-1 text-xs font-bold text-amber-400 hover:text-amber-300 underline underline-offset-4 cursor-pointer">
                              {broadcastActionLabel}
                              <ExternalLink className="w-3 h-3" />
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                    <button className="text-slate-500 hover:text-slate-300 p-1 rounded-lg">
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Simulated countdown bar */}
                  <div className="absolute bottom-0 left-0 right-0 h-1 bg-slate-800">
                    <div 
                      className="h-full w-2/3 rounded-full transition-all"
                      style={{ backgroundColor: broadcastBadgeColor }}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="text-xs text-slate-400 flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>يتم توجيه الإشعار لجميع الأجهزة النشطة والزوار الجدد</span>
              </div>

              <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
                <button
                  onClick={() => setShowBroadcastModal(false)}
                  className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-slate-400 hover:bg-slate-800 transition-colors"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleBroadcastAnnouncement}
                  className="bg-gradient-to-r from-rose-500 via-amber-500 to-amber-400 hover:from-rose-600 hover:to-amber-500 text-slate-950 font-black px-6 py-2.5 rounded-xl text-xs sm:text-sm shadow-xl shadow-rose-500/25 tracking-wide flex items-center gap-2 transition-all hover:scale-[1.02]"
                >
                  <Send className="w-4 h-4" />
                  <span>بث الإشعار الآن لجميع المستخدمين</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: ADD / EDIT TUTORIAL */}
      {showAddTutModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-xl p-6 sm:p-8 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-lg sm:text-xl font-bold text-white tracking-tight flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-amber-400" />
                {editingTut ? 'تعديل الشرح' : 'إضافة شرح أو دليل جديد'}
              </h3>
              <button 
                onClick={() => setShowAddTutModal(false)}
                className="text-xs sm:text-sm font-bold text-slate-400 hover:text-white px-3 py-1.5 rounded-lg hover:bg-slate-800 transition-colors"
              >
                إغلاق
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">عنوان الشرح</label>
                <input
                  type="text"
                  placeholder="مثال: كيف تدير حساباتك بالهاشتاج؟"
                  value={newTutTitle}
                  onChange={(e) => setNewTutTitle(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">التصنيف</label>
                  <select
                    value={newTutCategory}
                    onChange={(e: any) => setNewTutCategory(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  >
                    <option value="accounting">المحاسب والهاشتاج</option>
                    <option value="vault">الخزائن المشفرة</option>
                    <option value="ai_camera">كاميرا الميزان والذهب</option>
                    <option value="projects">إدارة المشاريع ورأس المال</option>
                    <option value="backup">النسخ الاحتياطي المشفر</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">مدة القراءة</label>
                  <input
                    type="text"
                    value={newTutDuration}
                    onChange={(e) => setNewTutDuration(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">ملخص الشرح</label>
                <textarea
                  rows={2}
                  value={newTutSummary}
                  onChange={(e) => setNewTutSummary(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                  خطوات التنفيذ (كل خطوة في سطر منفصل)
                </label>
                <textarea
                  rows={4}
                  placeholder="افتح المحادثة واكتب #رمل&#10;يفتح جدول المحاسبة المباشر&#10;حدد المبلغ وطريقة السداد"
                  value={newTutStepsText}
                  onChange={(e) => setNewTutStepsText(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none leading-relaxed transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">الوسوم (مفصولة بفاصلة)</label>
                <input
                  type="text"
                  placeholder="محاسبة, هاشتاج, فواتير"
                  value={newTutTagsText}
                  onChange={(e) => setNewTutTagsText(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:border-amber-400 focus:outline-none transition-colors"
                />
              </div>
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
              <button
                onClick={() => setShowAddTutModal(false)}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-slate-400 hover:bg-slate-800 transition-colors"
              >
                إلغاء
              </button>
              <button
                onClick={handleSaveTutorial}
                className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-black px-6 py-2.5 rounded-xl text-xs sm:text-sm shadow-lg shadow-amber-500/20 tracking-wide transition-all"
              >
                {editingTut ? 'حفظ التعديلات' : 'نشر الشرح'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export { AdminDashboardSkeleton };
