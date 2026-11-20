import React, { useState, useEffect, useMemo } from 'react';
import { motion, type Variants } from 'framer-motion';
import { Announcement, TutorialGuide, LiveGoldPrice, ProjectFeature, SiteHeroConfig, BroadcastAnnouncement } from './types';
import { SEOProvider } from './components/SEOProvider';
import { LandingPageSkeleton } from './components/skeletons/LandingPageSkeleton';
import { Badge } from './components/ui/badge';
import { Button } from './components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './components/ui/card';
import { 
  Sparkles, 
  ShieldCheck, 
  TrendingUp, 
  Download, 
  BookOpen, 
  Bell, 
  Calculator, 
  Lock, 
  Camera, 
  Briefcase, 
  ChevronDown, 
  ChevronUp, 
  ArrowLeft, 
  CheckCircle2, 
  Smartphone, 
  ExternalLink,
  Coins,
  FileSpreadsheet,
  Layers,
  HelpCircle,
  PhoneCall,
  Zap,
  Users,
  Shield,
  Receipt,
  Check,
  Menu,
  X,
  ArrowUp,
  RotateCcw,
  Trophy,
  Radio
} from 'lucide-react';

interface LandingPageProps {
  heroConfig: SiteHeroConfig;
  announcements: Announcement[];
  tutorials: TutorialGuide[];
  goldPrices: LiveGoldPrice[];
  features: ProjectFeature[];
  onNavigateToAdminLogin: () => void;
  onLaunchAppPortal?: () => void;
  onNavigateToContact?: () => void;
  isLoading?: boolean;
}

// Framer Motion entrance animation variants for Hero Section
const heroContainerVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.14,
      delayChildren: 0.08,
    },
  },
};

const heroBadgeVariants: Variants = {
  hidden: { opacity: 0, scale: 0.85, y: -20 },
  visible: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: {
      duration: 0.6,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

const heroTitleVariants: Variants = {
  hidden: { opacity: 0, y: 36, scale: 0.96 },
  visible: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      duration: 0.85,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

const heroSubtitleVariants: Variants = {
  hidden: { opacity: 0, y: 24 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.7,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

const heroCtaContainerVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.12,
      delayChildren: 0.15,
    },
  },
};

const heroCtaItemVariants: Variants = {
  hidden: { opacity: 0, y: 28, scale: 0.94 },
  visible: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      duration: 0.65,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

const metricsContainerVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.35,
    },
  },
};

const metricCardVariants: Variants = {
  hidden: { opacity: 0, y: 28, scale: 0.92 },
  visible: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      duration: 0.6,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

export const LandingPage: React.FC<LandingPageProps> = ({
  heroConfig,
  announcements,
  tutorials,
  goldPrices,
  features,
  onNavigateToAdminLogin,
  onLaunchAppPortal,
  onNavigateToContact,
  isLoading = false
}) => {
  const [selectedTutorial, setSelectedTutorial] = useState<TutorialGuide | null>(tutorials[0] || null);
  const [activeCategory, setActiveCategory] = useState<string>('all');
  const [expandedFaq, setExpandedFaq] = useState<number | null>(null);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState<boolean>(false);
  const [activeSection, setActiveSection] = useState<string>('');
  const [showScrollTop, setShowScrollTop] = useState<boolean>(false);
  const [lang, setLang] = useState<'ar' | 'en'>('ar');

  // Tutorial Progress Tracking State: maps tutorialId -> array of completed step indices
  const [tutorialProgress, setTutorialProgress] = useState<Record<string, number[]>>(() => {
    if (typeof window !== 'undefined') {
      try {
        const saved = localStorage.getItem('oozox_tutorial_progress');
        if (saved) return JSON.parse(saved);
      } catch {
        // Fallback to empty progress on malformed storage
      }
    }
    return {};
  });

  const toggleStepCompletion = (tutorialId: string, stepIdx: number) => {
    setTutorialProgress(prev => {
      const current = prev[tutorialId] || [];
      const updated = current.includes(stepIdx)
        ? current.filter(i => i !== stepIdx)
        : [...current, stepIdx];
      const next = { ...prev, [tutorialId]: updated };
      try {
        localStorage.setItem('oozox_tutorial_progress', JSON.stringify(next));
      } catch (err) {
        console.error(err);
      }
      return next;
    });
  };

  const markAllStepsCompleted = (tutorial: TutorialGuide) => {
    setTutorialProgress(prev => {
      const allSteps = tutorial.steps.map((_, i) => i);
      const next = { ...prev, [tutorial.id]: allSteps };
      try {
        localStorage.setItem('oozox_tutorial_progress', JSON.stringify(next));
      } catch (err) {
        console.error(err);
      }
      return next;
    });
  };

  const resetTutorialProgress = (tutorialId: string) => {
    setTutorialProgress(prev => {
      const next = { ...prev, [tutorialId]: [] };
      try {
        localStorage.setItem('oozox_tutorial_progress', JSON.stringify(next));
      } catch (err) {
        console.error(err);
      }
      return next;
    });
  };

  const getTutorialProgressData = (tut: TutorialGuide) => {
    const completed = tutorialProgress[tut.id] || [];
    const total = tut.steps.length;
    const count = completed.length;
    const percent = total > 0 ? Math.round((count / total) * 100) : 0;
    const isDone = count === total && total > 0;
    return { completed, total, count, percent, isDone };
  };

  const overallProgress = useMemo(() => {
    let totalSteps = 0;
    let completedSteps = 0;
    tutorials.forEach(tut => {
      totalSteps += tut.steps.length;
      const completed = tutorialProgress[tut.id] || [];
      completedSteps += completed.length;
    });
    const percent = totalSteps > 0 ? Math.round((completedSteps / totalSteps) * 100) : 0;
    return { totalSteps, completedSteps, percent };
  }, [tutorials, tutorialProgress]);

  // --- System-Wide Live Broadcast Toast Notification State ---
  const [broadcastToast, setBroadcastToast] = useState<BroadcastAnnouncement | null>(null);
  const [isBroadcastVisible, setIsBroadcastVisible] = useState<boolean>(false);
  const [broadcastTimeRemaining, setBroadcastTimeRemaining] = useState<number>(12);
  const [isBroadcastHovered, setIsBroadcastHovered] = useState<boolean>(false);

  // Check on mount for any recent active broadcast that wasn't dismissed
  useEffect(() => {
    if (typeof window === 'undefined') return;

    try {
      const saved = localStorage.getItem('oozox_broadcast_announcement');
      const timestamp = localStorage.getItem('oozox_broadcast_timestamp');
      if (saved) {
        const parsed: BroadcastAnnouncement = JSON.parse(saved);
        const dismissedStr = localStorage.getItem('oozox_dismissed_broadcasts');
        const dismissed: string[] = dismissedStr ? JSON.parse(dismissedStr) : [];
        
        // Show if not dismissed and within the last 48 hours
        const isRecent = timestamp ? (Date.now() - Number(timestamp)) < 48 * 3600 * 1000 : true;
        if (!dismissed.includes(parsed.id) && isRecent) {
          const timer = setTimeout(() => {
            setBroadcastToast(parsed);
            setIsBroadcastVisible(true);
            setBroadcastTimeRemaining(parsed.durationSeconds ?? 12);
          }, 800);
          return () => clearTimeout(timer);
        }
      }
    } catch (e) {
      console.error(e);
    }
  }, []);

  // Listen for live broadcast events dispatched from Admin or storage changes across tabs
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const handleLiveBroadcast = (e: any) => {
      const payload: BroadcastAnnouncement = e.detail;
      if (payload) {
        setBroadcastToast(payload);
        setIsBroadcastVisible(true);
        setBroadcastTimeRemaining(payload.durationSeconds ?? 12);
      }
    };

    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'oozox_broadcast_announcement' && e.newValue) {
        try {
          const payload: BroadcastAnnouncement = JSON.parse(e.newValue);
          setBroadcastToast(payload);
          setIsBroadcastVisible(true);
          setBroadcastTimeRemaining(payload.durationSeconds ?? 12);
        } catch {}
      }
    };

    window.addEventListener('oozox_broadcast', handleLiveBroadcast);
    window.addEventListener('storage', handleStorageChange);
    return () => {
      window.removeEventListener('oozox_broadcast', handleLiveBroadcast);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  // Auto-dismiss countdown timer
  useEffect(() => {
    if (!isBroadcastVisible || !broadcastToast || broadcastTimeRemaining <= 0 || isBroadcastHovered) return;
    if (broadcastToast.durationSeconds === 0) return; // 0 means persistent until closed

    const interval = setInterval(() => {
      setBroadcastTimeRemaining(prev => {
        if (prev <= 1) {
          setIsBroadcastVisible(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isBroadcastVisible, broadcastToast, broadcastTimeRemaining, isBroadcastHovered]);

  const dismissBroadcast = () => {
    setIsBroadcastVisible(false);
    if (broadcastToast && typeof window !== 'undefined') {
      try {
        const dismissedStr = localStorage.getItem('oozox_dismissed_broadcasts');
        const dismissed: string[] = dismissedStr ? JSON.parse(dismissedStr) : [];
        if (!dismissed.includes(broadcastToast.id)) {
          dismissed.push(broadcastToast.id);
          localStorage.setItem('oozox_dismissed_broadcasts', JSON.stringify(dismissed));
        }
      } catch (e) {
        console.error(e);
      }
    }
  };

  // Lead Generation & Contact Form State
  const [contactName, setContactName] = useState('');
  const [contactPhoneOrEmail, setContactPhoneOrEmail] = useState('');
  const [contactBusinessType, setContactBusinessType] = useState('تجارة جملة / تجزئة');
  const [contactMessage, setContactMessage] = useState('');
  const [contactStatus, setContactStatus] = useState<'idle' | 'submitting' | 'success'>('idle');

  const handleContactSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!contactName || !contactPhoneOrEmail) {
      alert('يرجى كتابة الاسم ورقم الهاتف أو البريد للتواصل');
      return;
    }
    setContactStatus('submitting');
    setTimeout(() => {
      setContactStatus('success');
      setContactName('');
      setContactPhoneOrEmail('');
      setContactMessage('');
    }, 700);
  };

  const activeAnnouncements = announcements.filter(a => a.isActive);

  const filteredTutorials = activeCategory === 'all' 
    ? tutorials 
    : tutorials.filter(t => t.category === activeCategory);

  // Smooth scroll handler with header offset calculation or page navigation
  const scrollToSection = (e: React.MouseEvent<HTMLAnchorElement>, targetId: string, isContact?: boolean) => {
    if (isContact || targetId === '/contact' || targetId.includes('ContactUs')) {
      e.preventDefault();
      if (onNavigateToContact) {
        onNavigateToContact();
      } else {
        window.location.href = '/preview/ContactUs';
      }
      setIsMobileMenuOpen(false);
      return;
    }

    if (targetId && targetId.startsWith('#')) {
      e.preventDefault();
      const id = targetId.replace('#', '');
      const element = document.getElementById(id);
      if (element) {
        const headerOffset = 80;
        const elementPosition = element.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.pageYOffset - headerOffset;

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        });

        // Update URL hash cleanly
        window.history.pushState(null, '', targetId);
        setIsMobileMenuOpen(false);
      }
    }
  };

  const scrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  };

  // Scroll listener for active section indicator and back-to-top visibility
  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 400) {
        setShowScrollTop(true);
      } else {
        setShowScrollTop(false);
      }

      const sections = ['features', 'gold-prices', 'tutorials', 'announcements', 'pricing', 'faq', 'download'];
      const scrollPos = window.scrollY + 120;

      for (let i = sections.length - 1; i >= 0; i--) {
        const sectionEl = document.getElementById(sections[i]);
        if (sectionEl && sectionEl.offsetTop <= scrollPos) {
          setActiveSection(sections[i]);
          break;
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();

    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  // Set Meta Tags for SEO (description, keywords, og:image) to improve search engine visibility
  useEffect(() => {
    if (typeof document === 'undefined') return;

    const pageTitle = `${heroConfig.brandHighlight || 'OOZOX'} — ${heroConfig.headline || 'نظام الفواتير والمحاسبة الذكي وتداول الذهب'}`;
    document.title = pageTitle;

    const pageDescription = heroConfig.subheadline || 
      'المنظومة المحاسبية الذكية لإصدار الفواتير الإلكترونية المعتمدة، إدارة ديون العملاء، وحسابات الذهب والدمغة لحظياً مع دعم العمل دون إنترنت والتصدير إلى Excel.';

    const pageKeywords = [
      'OOZOX',
      'فواتير الكترونية',
      'برنامج محاسبة',
      'دفترة ديون العملاء',
      'حسابات الذهب',
      'تصدير كشف حساب اكسل',
      'حاسبة المصنعية والدمغة',
      'فواتير PDF واتساب',
      'نظام نقاط البيع POS',
      'Smart Invoicing System',
      'Gold Accounting App',
      'سعر الذهب اليوم',
      'الفوترة الإلكترونية مصر والخليج'
    ].join(', ');

    const ogImageUrl = 'https://oozox.com/assets/og-banner.png';
    const canonicalUrl = 'https://oozox.com';

    // Helper to dynamically upsert meta tags in <head>
    const upsertMetaTag = (attribute: 'name' | 'property', key: string, value: string) => {
      let meta = document.querySelector(`meta[${attribute}="${key}"]`) as HTMLMetaElement | null;
      if (!meta) {
        meta = document.createElement('meta');
        meta.setAttribute(attribute, key);
        document.head.appendChild(meta);
      }
      meta.setAttribute('content', value);
    };

    // Standard SEO Meta Tags
    upsertMetaTag('name', 'description', pageDescription);
    upsertMetaTag('name', 'keywords', pageKeywords);
    upsertMetaTag('name', 'author', 'OOZOX Technologies');
    upsertMetaTag('name', 'robots', 'index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1');

    // OpenGraph Meta Tags (Social Media & Search Preview)
    upsertMetaTag('property', 'og:title', pageTitle);
    upsertMetaTag('property', 'og:description', pageDescription);
    upsertMetaTag('property', 'og:image', ogImageUrl);
    upsertMetaTag('property', 'og:image:alt', 'OOZOX - منظومة الفواتير الذكية وحسابات الذهب');
    upsertMetaTag('property', 'og:url', canonicalUrl);
    upsertMetaTag('property', 'og:type', 'website');
    upsertMetaTag('property', 'og:site_name', 'OOZOX');
    upsertMetaTag('property', 'og:locale', lang === 'ar' ? 'ar_AR' : 'en_US');

    // Twitter Card Meta Tags
    upsertMetaTag('name', 'twitter:card', 'summary_large_image');
    upsertMetaTag('name', 'twitter:title', pageTitle);
    upsertMetaTag('name', 'twitter:description', pageDescription);
    upsertMetaTag('name', 'twitter:image', ogImageUrl);

    // Canonical link tag
    let canonicalLink = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    if (!canonicalLink) {
      canonicalLink = document.createElement('link');
      canonicalLink.setAttribute('rel', 'canonical');
      document.head.appendChild(canonicalLink);
    }
    canonicalLink.setAttribute('href', canonicalUrl);
  }, [heroConfig, lang]);

  const getFeatureIcon = (iconName: string) => {
    switch (iconName.toLowerCase()) {
      case 'receipt':
      case 'calculator':
        return <Receipt className="w-6 h-6 text-amber-400" />;
      case 'lock':
      case 'shield':
        return <Lock className="w-6 h-6 text-teal-400" />;
      case 'sparkles':
      case 'camera':
        return <Sparkles className="w-6 h-6 text-amber-400" />;
      case 'users':
      case 'briefcase':
        return <Users className="w-6 h-6 text-teal-400" />;
      default:
        return <Zap className="w-6 h-6 text-amber-400" />;
    }
  };

  const [faqCategory, setFaqCategory] = useState<string>('all');
  const [faqSearch, setFaqSearch] = useState<string>('');

  const rawFaqs = [
    {
      id: 'trial-60-days',
      category: 'general',
      qAr: 'كيف أستفيد من فترة التجربة المجانية الشاملة لمدة 60 يوماً في أوزوكس؟',
      qEn: 'How do I activate the 60-day full access free trial in OOZOX?',
      aAr: 'بمجرد تنزيل تطبيق OOZOX والتسجيل برقم هاتفك، يتم تفعيل باقة التجربة المجانية الكاملة تلقائياً لمدة شهرين كاملين (60 يوماً) مع فتح كافة الميزات المتقدمة دون الحاجة لإدخال أي بطاقة بنكية أو التزامات مسبقة.',
      aEn: 'Upon downloading the OOZOX app and signing up with your phone number, a 60-day full-featured free trial is automatically activated with no credit card required.'
    },
    {
      id: 'whatsapp-invoicing',
      category: 'invoices',
      qAr: 'كيف يعمل نظام إصدار فواتير PDF الإلكترونية ومشاركتها عبر واتساب؟',
      qEn: 'How does instant PDF invoice generation and WhatsApp sharing work?',
      aAr: 'يتيح لك OOZOX إنشاء فواتير بيع وشراء ضريبية وتجارية معتمدة تدعم رمز الاستجابة السريعة (QR Code)، حساب ضريبة القيمة المضافة (VAT)، الخصومات، والأرقام التسلسلية، ثم إرسالها بصيغة PDF مباشرة إلى محادثة العميل عبر WhatsApp بنقرة واحدة.',
      aEn: 'OOZOX generates compliant commercial and tax e-invoices with QR codes, VAT, item discounts, and serial numbers, allowing one-click PDF sharing directly to clients via WhatsApp.'
    },
    {
      id: 'hashtag-accounting',
      category: 'accounting',
      qAr: 'كيف يحول نظام الهاشتاج المحاسبي الذكي (#) نصوص المحادثة إلى قيود مالية؟',
      qEn: 'How does the in-chat hashtag accounting (#) convert text to ledger entries?',
      aAr: 'بمجرد كتابة رمز الشباك متبوعاً باسم الصنف مثل #ذهب أو #حديد أو #بضاعة داخل المحادثة، يقوم المحرك المحاسبي في أوزوكس بتحويل النص تلقائياً إلى جدول قيود محاسبي تفاعلي لحساب إجمالي المبيعات، الدائن والمدين، والمتبقي بدقة متناهية.',
      aEn: 'Typing a hashtag with an item name (e.g. #gold or #materials) in chat instantly triggers OOZOX accounting engine to generate an interactive ledger table computing debits, credits, and balances.'
    },
    {
      id: 'excel-sumifs-export',
      category: 'accounting',
      qAr: 'هل يدعم تطبيق OOZOX تصدير كشوف الحسابات إلى Excel بصيغ SUMIFS ومصفوفات ديناميكية؟',
      qEn: 'Does OOZOX support exporting Excel workbooks with dynamic SUMIFS formulas?',
      aAr: 'نعم، يولد محرك تصدير إكسل في OOZOX ملفات جداول بيانات متطورة مدمجة بصيغ SUMIFS التلقائية، مع لوحة مؤشرات السيولة النقدية لـ 90 يوماً وتنسيق شرطي ينبهك للتركزات الائتمانية والديون المتأخرة.',
      aEn: 'Yes, OOZOX exports formula-rich Excel workbooks with dynamic SUMIFS, 90-day cash flow projections, and conditional formatting alerting to credit concentrations over 40%.'
    },
    {
      id: 'gold-and-jewelry',
      category: 'gold',
      qAr: 'ما هي الحلول المتخصصة التي يقدمها OOZOX لتجار وصاغة الذهب والمجوهرات؟',
      qEn: 'What dedicated solutions does OOZOX provide for gold and jewelry traders?',
      aAr: 'يقدم OOZOX متابعة لحظية لأسعار الذهب بكافة العيارات (24، 21، 18، والأونصة)، حاسبة دقيقة للدمغة والمصنعية والتقاص، وميزة قراءة شاشة ميزان الذهب الحساس آلياً عبر كاميرا الذكاء الاصطناعي (AI Scale OCR).',
      aEn: 'OOZOX offers live multi-karat gold rates (24K, 21K, 18K, oz), craftsmanship & stamp fee settlement calculators, and AI camera OCR to read sensitive digital scale displays.'
    },
    {
      id: 'stealth-vaults',
      category: 'security',
      qAr: 'كيف تحمي الخزائن المشفرة بنظام التخفي (Zero-Trace Stealth) سرية المعاملات المالية؟',
      qEn: 'How do Zero-Trace Stealth Vaults safeguard sensitive transactions?',
      aAr: 'تتيح الميزة إنشاء خزائن وحسابات فرعية مستقلة مقفلة برمز PIN مخصص؛ وعند إغلاق الخزنة، تُخفى تماماً ولا تترك أي إشعارات أو سجلات مرئية على شاشة الهاتف، مع تشفير محلي كامل للبيانات بتقنية AES-256-GCM العسكرية.',
      aEn: 'Users can create PIN-protected sub-vaults that leave zero notification footprint when locked, shielded locally by military-grade AES-256-GCM encryption.'
    },
    {
      id: 'offline-first-room',
      category: 'security',
      qAr: 'هل يعمل نظام OOZOX في الميدان والمحلات عند انقطاع الإنترنت (Offline-First)؟',
      qEn: 'Does OOZOX work 100% offline in stores and field operations?',
      aAr: 'نعم بنسبة 100%! تم بناء التطبيق بهندسة Offline-First وقاعدة بيانات Room المحلية المشفرة، مما يسمح لك بإصدار الفواتير، تسجيل المدفوعات، وإدارة حركة المخزن دون إنترنت، مع مزامنة سحابية ذكية وفورية فور استعادة الاتصال.',
      aEn: 'Yes, 100%. Built on an offline-first Room database, allowing seamless invoicing, payments, and debt records without an internet connection, auto-syncing securely when reconnected.'
    },
    {
      id: 'debt-collection-reminders',
      category: 'invoices',
      qAr: 'كيف يساعد OOZOX في إدارة ديون العملاء وإرسال تذكيرات السداد الآلية؟',
      qEn: 'How does OOZOX manage customer receivables and payment reminders?',
      aAr: 'يوفر التطبيق دفتر أستاذ تفاعلي لكل عميل ومورد يرصد كل معاملة وتاريخ استحقاقها بدقة، مع إمكانية توليد وإرسال مطالبات وتذكيرات دفع دورية مهذبة وموثقة بكشف حساب رسمي عبر واتساب ورسائل SMS.',
      aEn: 'OOZOX provides interactive customer ledgers tracking due dates and aging, with automated, polite WhatsApp statement reminders to accelerate collections.'
    },
    {
      id: 'vat-compliance',
      category: 'invoices',
      qAr: 'هل يتوافق OOZOX مع متطلبات الفوترة الإلكترونية وضريبة القيمة المضافة (VAT) في مصر والخليج؟',
      qEn: 'Is OOZOX compliant with VAT and electronic invoicing standards in MENA & GCC?',
      aAr: 'نعم، صُممت فواتير OOZOX لتتوافق مع معايير الفوترة الإلكترونية الحديثة، بما في ذلك حساب الضرائب المتعددة، تفقيط المبالغ، وتضمين رموز QR التشفيرية المعتمدة للتحقق من صحة الفاتورة.',
      aEn: 'Yes, invoices adhere to modern electronic invoicing standards, supporting multi-tier taxes, currency wording, and verifiable cryptographic QR codes.'
    }
  ];

  const faqs = rawFaqs.map(f => ({
    id: f.id,
    category: f.category,
    q: lang === 'ar' ? f.qAr : f.qEn,
    a: lang === 'ar' ? f.aAr : f.aEn
  }));

  const filteredFaqs = faqs.filter(faq => {
    const matchesCategory = faqCategory === 'all' || faq.category === faqCategory;
    const matchesSearch = !faqSearch.trim() || 
      faq.q.toLowerCase().includes(faqSearch.toLowerCase()) || 
      faq.a.toLowerCase().includes(faqSearch.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  // Comprehensive Schema.org Structured Metadata for AI Agents & Search Engines
  const structuredSchemaData = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "Organization",
        "@id": "https://oozox.com/#organization",
        "name": "OOZOX Inc.",
        "url": "https://oozox.com",
        "logo": "https://oozox.com/assets/icon.png",
        "description": "المنظومة المحاسبية المتكاملة لإدارة الفواتير والديون وتداول الذهب وتصدير تقارير إكسل الذكية.",
        "foundingDate": "2026",
        "contactPoint": {
          "@type": "ContactPoint",
          "contactType": "Customer Support",
          "email": "support@oozox.com",
          "availableLanguage": ["Arabic", "English"]
        },
        "sameAs": [
          "https://play.google.com/store/apps/details?id=com.oozox.app",
          "https://apps.apple.com/app/oozox/id123456789"
        ]
      },
      {
        "@type": "Product",
        "@id": "https://oozox.com/#product",
        "name": "OOZOX - المنظومة المحاسبية المتكاملة",
        "brand": {
          "@type": "Brand",
          "name": "OOZOX"
        },
        "category": "Accounting & Invoicing Software",
        "image": "https://oozox.com/assets/banner.png",
        "description": "برنامج فواتير الكترونية، إدارة ديون، حسابات الذهب وتصدير تقارير إكسل الذكية.",
        "offers": {
          "@type": "AggregateOffer",
          "priceCurrency": "EGP",
          "lowPrice": "0",
          "highPrice": "999",
          "offerCount": "3"
        },
        "aggregateRating": {
          "@type": "AggregateRating",
          "ratingValue": "4.9",
          "reviewCount": "1250"
        }
      },
      {
        "@type": "SoftwareApplication",
        "@id": "https://oozox.com/#software",
        "name": "OOZOX - منظومة الفواتير والمحاسبة والذهب",
        "alternateName": ["أوزوكس", "Oozox Accounting", "تطبيق فواتير اوزوكس"],
        "operatingSystem": "Android, iOS",
        "applicationCategory": "BusinessApplication, FinanceApplication",
        "downloadUrl": "https://oozox.com/#download",
        "offers": [
          {
            "@type": "Offer",
            "name": "تجربة مجانية شاملة 60 يوماً",
            "price": "0",
            "priceCurrency": "EGP",
            "availability": "https://schema.org/InStock"
          },
          {
            "@type": "Offer",
            "name": "الباقة الفضية",
            "price": "499",
            "priceCurrency": "EGP",
            "billingDuration": "P1M"
          },
          {
            "@type": "Offer",
            "name": "الباقة الذهبية",
            "price": "999",
            "priceCurrency": "EGP",
            "billingDuration": "P1M"
          }
        ],
        "aggregateRating": {
          "@type": "AggregateRating",
          "ratingValue": "4.9",
          "ratingCount": "1250",
          "bestRating": "5",
          "worstRating": "1"
        },
        "description": "تطبيق محاسبة وفواتير إلكترونية متكامل، يدعم تصدير إكسل بصيغ SUMIFS، دفتر ديون، حسابات أسعار الذهب اللحظية، وخزائن مشفرة بـ AES-256.",
        "featureList": [
          "إصدار فواتير PDF ومشاركتها عبر واتساب في ثوانٍ",
          "نظام الهاشتاج المحاسبي الذكي مع مصفوفة SUMIFS",
          "تصدير كشوف الحسابات إلى Excel و CSV",
          "مؤشر حي وحاسبة لأسعار الذهب والعيارات",
          "خزائن سرية مشفرة بـ Zero-Trace PIN",
          "قراءة ميزان الذهب بكاميرا الذكاء الاصطناعي AI OCR"
        ]
      },
      {
        "@type": "FAQPage",
        "@id": "https://oozox.com/#faq",
        "mainEntity": faqs.map(faq => ({
          "@type": "Question",
          "name": faq.q,
          "acceptedAnswer": {
            "@type": "Answer",
            "text": faq.a
          }
        }))
      }
    ]
  };

  const navLinks = [
    { href: '#features', id: 'features', label: lang === 'ar' ? 'المميزات' : 'Features' },
    { href: '#updates', id: 'updates', label: lang === 'ar' ? 'جديد 2026' : "What's New" },
    { href: '#gold-prices', id: 'gold-prices', label: lang === 'ar' ? 'أسعار الذهب' : 'Gold Rates' },
    { href: '#tutorials', id: 'tutorials', label: lang === 'ar' ? 'الشروحات' : 'Guides' },
    { href: '#pricing', id: 'pricing', label: lang === 'ar' ? 'الباقات' : 'Pricing' },
    { href: '/contact', id: 'contact-page', label: lang === 'ar' ? 'اتصل بنا' : 'Contact Us', isContact: true },
    { href: '#contact', id: 'contact', label: lang === 'ar' ? 'طلب استشارة' : 'Consultation' },
    { href: '#faq', id: 'faq', label: lang === 'ar' ? 'الأسئلة' : 'FAQ' },
  ];

  if (isLoading) {
    return <LandingPageSkeleton />;
  }

  return (
    <SEOProvider 
      heroConfig={heroConfig} 
      faqs={faqs} 
      lang={lang}
      ogImage="https://oozox.com/assets/og-banner.png"
      keywords={[
        'OOZOX',
        'فواتير الكترونية',
        'تطبيق محاسبة',
        'دفترة ديون',
        'حسابات الذهب',
        'تصدير اكسل',
        'كشف حساب عملاء',
        'حاسبة المصنعية',
        'Smart Invoicing',
        'Gold Accounting App'
      ]}
    >
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans antialiased selection:bg-amber-500 selection:text-black scroll-smooth transition-colors duration-300" dir="rtl">
      {/* Schema.org Structured Metadata for AI Crawlers (Gemini, ChatGPT, Google) */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredSchemaData) }}
      />
      
      {/* Top Notification Announcement Bar */}
      {activeAnnouncements.length > 0 && (
        <aside aria-label="إعلانات هامة" className="bg-gradient-to-r from-amber-600 via-amber-500 to-teal-600 text-slate-950 font-bold px-4 py-2.5 shadow-md">
          <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2 text-xs sm:text-sm">
            <div className="flex items-center gap-2">
              <span className="bg-slate-950 text-amber-400 px-2 py-0.5 rounded-full text-xs uppercase tracking-wider font-extrabold">
                {activeAnnouncements[0].badgeText}
              </span>
              <span>{activeAnnouncements[0].title}</span>
            </div>
            <a 
              href={activeAnnouncements[0].actionUrl || '#download'} 
              onClick={(e) => scrollToSection(e, activeAnnouncements[0].actionUrl || '#download')}
              className="inline-flex items-center gap-1 bg-slate-950 hover:bg-slate-900 text-amber-400 px-3 py-1 rounded-lg text-xs font-bold transition-transform hover:scale-105 cursor-pointer"
            >
              <span>{activeAnnouncements[0].actionLabel || 'اكتشف المزيد'}</span>
              <ArrowLeft className="w-3 h-3" />
            </a>
          </div>
        </aside>
      )}

      {/* Main Header / Navigation */}
      <header className="sticky top-0 z-40 bg-slate-950/90 backdrop-blur-md border-b border-slate-800 transition-all duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-amber-500 to-amber-300 flex items-center justify-center shadow-lg shadow-amber-500/20 ring-2 ring-amber-400/30">
              <Coins className="w-6 h-6 text-slate-950" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-2xl font-black tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-amber-200 to-teal-300">
                  {heroConfig.brandHighlight || 'OOZOX'}
                </span>
                <span className="text-xs bg-amber-500/10 text-amber-400 border border-amber-500/30 px-2 py-0.5 rounded-md font-bold">
                  منصة ذهبي 3.0
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-medium">المنصة الشاملة لإدارة الأعمال والمحاسبة وتداول الذهب</p>
            </div>
          </div>

          <nav aria-label="التنقل الرئيسي" className="hidden md:flex items-center gap-6 lg:gap-8 text-sm font-medium">
            {navLinks.map((link) => {
              const isActive = activeSection === link.id;
              return (
                <a 
                  key={link.id}
                  href={link.href} 
                  onClick={(e) => scrollToSection(e, link.href, link.isContact)}
                  className={`py-2 px-1 border-b-2 transition-all cursor-pointer ${
                    isActive 
                      ? 'text-amber-400 border-amber-400 font-bold' 
                      : 'text-slate-300 border-transparent hover:text-amber-400 hover:border-amber-400/50'
                  }`}
                >
                  {link.label}
                </a>
              );
            })}
          </nav>

          <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
            {/* Live App Portal Launch Button */}
            {onLaunchAppPortal && (
              <button
                onClick={onLaunchAppPortal}
                className="hidden sm:inline-flex items-center px-4 py-2 rounded-xl bg-slate-900/90 border border-amber-500/40 text-xs font-bold text-amber-400 hover:bg-amber-500 hover:text-slate-950 transition-all duration-200 shadow-sm shrink-0 hover:shadow-amber-500/20 tracking-wide"
                title="تشغيل بوابة المنظومة الحية"
              >
                <span>تشغيل المنظومة</span>
              </button>
            )}

            {/* Language Toggle */}
            <button
              onClick={() => setLang(lang === 'ar' ? 'en' : 'ar')}
              className="px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs font-bold text-slate-300 hover:text-amber-400 hover:border-amber-500/40 transition-all duration-200 shrink-0 shadow-sm"
              title="تبديل لغة العرض / Switch Language"
            >
              <span>{lang === 'ar' ? 'English' : 'عربي'}</span>
            </button>

            {/* Main Free Download CTA */}
            <a 
              href="#download"
              onClick={(e) => scrollToSection(e, '#download')}
              className="inline-flex items-center bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 px-5 py-2 rounded-xl font-bold text-xs sm:text-sm shadow-md shadow-amber-500/20 transition-all hover:scale-102 shrink-0 cursor-pointer tracking-wide"
            >
              <span>{lang === 'ar' ? 'تحميل مجاناً' : 'Get App Free'}</span>
            </a>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs font-bold text-slate-300 hover:text-amber-400 shrink-0"
              aria-label="القائمة"
            >
              {isMobileMenuOpen ? 'إغلاق' : 'القائمة'}
            </button>
          </div>
        </div>

        {/* Mobile Dropdown Navigation Menu */}
        {isMobileMenuOpen && (
          <div className="md:hidden bg-slate-950/95 backdrop-blur-xl border-b border-slate-800 px-4 pt-2 pb-6 space-y-2 animate-in fade-in slide-in-from-top-2 duration-200">
            {navLinks.map((link) => {
              const isActive = activeSection === link.id;
              return (
                <a
                  key={link.id}
                  href={link.href}
                  onClick={(e) => scrollToSection(e, link.href, link.isContact)}
                  className={`block px-4 py-3 rounded-xl text-sm font-bold transition-all cursor-pointer ${
                    isActive 
                      ? 'bg-amber-500/15 text-amber-400 border border-amber-500/30' 
                      : 'text-slate-300 hover:bg-slate-900 hover:text-amber-400'
                  }`}
                >
                  {link.label}
                </a>
              );
            })}
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main id="main-content" itemScope itemType="https://schema.org/SoftwareApplication">
        <meta itemProp="name" content="OOZOX - منظومة الفواتير والمحاسبة الذكية" />
        <meta itemProp="applicationCategory" content="BusinessApplication" />
        <meta itemProp="operatingSystem" content="Android, iOS" />

        {/* Hero Section with Semantic HTML5 & ASO Optimization */}
        <section 
          id="hero"
          aria-labelledby="hero-heading" 
          className="relative overflow-hidden pt-16 pb-20 lg:pt-24 lg:pb-32"
          itemScope 
          itemType="https://schema.org/WebPageElement"
        >
          <motion.div 
            initial={{ opacity: 0, scale: 0.7 }}
            animate={{ 
              opacity: [0.3, 0.6, 0.3], 
              scale: [0.95, 1.05, 0.95],
              y: [0, -15, 0]
            }}
            transition={{ 
              duration: 7, 
              repeat: Infinity, 
              ease: "easeInOut" 
            }}
            className="absolute top-1/4 right-10 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" 
          />
          <motion.div 
            initial={{ opacity: 0, scale: 0.7 }}
            animate={{ 
              opacity: [0.3, 0.6, 0.3], 
              scale: [1.05, 0.95, 1.05],
              y: [0, 15, 0]
            }}
            transition={{ 
              duration: 8, 
              delay: 0.5, 
              repeat: Infinity, 
              ease: "easeInOut" 
            }}
            className="absolute bottom-10 left-10 w-96 h-96 bg-teal-500/10 rounded-full blur-3xl pointer-events-none" 
          />

          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
            <motion.article 
              variants={heroContainerVariants}
              initial="hidden"
              animate="visible"
              className="text-center max-w-3xl mx-auto"
            >
              <header>
                <motion.div 
                  variants={heroBadgeVariants}
                  className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-slate-900 border border-slate-700/80 text-amber-400 text-xs font-bold uppercase tracking-wider mb-6 shadow-inner"
                >
                  <Sparkles className="w-4 h-4 animate-pulse text-amber-400" aria-hidden="true" />
                  <span itemProp="headline">{heroConfig.topBadge}</span>
                </motion.div>

                <motion.h1 
                  variants={heroTitleVariants}
                  id="hero-heading" 
                  className="text-4xl sm:text-5xl lg:text-6xl font-black text-white tracking-tight leading-[1.2] sm:leading-[1.18] mb-6"
                  itemProp="name"
                >
                  {heroConfig.headline}{' '}
                  <span className="text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-amber-300 to-teal-400">
                    {heroConfig.brandHighlight}
                  </span>
                </motion.h1>

                <motion.p 
                  variants={heroSubtitleVariants}
                  className="text-base sm:text-lg lg:text-xl text-slate-300 font-normal leading-relaxed mb-10 max-w-2xl mx-auto"
                  itemProp="description"
                >
                  {heroConfig.subheadline}
                </motion.p>
              </header>

              <motion.nav 
                variants={heroCtaContainerVariants}
                aria-label="روابط الإجراءات السريعة" 
                className="flex flex-col sm:flex-row items-center justify-center gap-4"
              >
                {onLaunchAppPortal && (
                  <motion.button
                    variants={heroCtaItemVariants}
                    whileHover={{ scale: 1.04, y: -3, boxShadow: "0 20px 25px -5px rgba(245, 158, 11, 0.3)" }}
                    whileTap={{ scale: 0.97 }}
                    onClick={onLaunchAppPortal}
                    className="w-full sm:w-auto inline-flex items-center justify-center bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 font-extrabold px-8 py-4 rounded-2xl shadow-xl shadow-amber-500/25 transition-all text-sm sm:text-base cursor-pointer tracking-wide"
                    title="تشغيل بوابة الفواتير والمحاسبة مباشرة"
                  >
                    <span>تشغيل المنظومة الحية (Live App)</span>
                  </motion.button>
                )}

                <motion.a 
                  variants={heroCtaItemVariants}
                  whileHover={{ scale: 1.03, y: -3, borderColor: "rgba(251, 191, 36, 0.8)", boxShadow: "0 10px 20px -5px rgba(245, 158, 11, 0.15)" }}
                  whileTap={{ scale: 0.97 }}
                  href="#download"
                  onClick={(e) => scrollToSection(e, '#download')}
                  className="w-full sm:w-auto inline-flex items-center justify-center bg-slate-900/90 hover:bg-slate-800 text-amber-400 font-bold px-8 py-4 rounded-2xl border border-amber-500/40 transition-all text-sm sm:text-base cursor-pointer tracking-wide"
                  title="تحميل تطبيق أوزوكس مجاناً"
                >
                  <span>{heroConfig.apkDownloadLabel}</span>
                </motion.a>

                <motion.a 
                  variants={heroCtaItemVariants}
                  whileHover={{ scale: 1.03, y: -3, borderColor: "rgba(148, 163, 184, 0.5)", backgroundColor: "rgba(15, 23, 42, 1)" }}
                  whileTap={{ scale: 0.97 }}
                  href="#tutorials"
                  onClick={(e) => scrollToSection(e, '#tutorials')}
                  className="w-full sm:w-auto inline-flex items-center justify-center bg-slate-950/80 hover:bg-slate-900 text-slate-300 font-bold px-7 py-4 rounded-2xl border border-slate-800 transition-all text-xs sm:text-sm cursor-pointer tracking-wide"
                  title="شروحات ودليل استخدام برنامج الفواتير"
                >
                  <span>الشروحات</span>
                </motion.a>
              </motion.nav>

              {/* Quick Metrics & Trust Badges */}
              <motion.aside 
                variants={metricsContainerVariants}
                initial="hidden"
                animate="visible"
                aria-label="أبرز ميزات الأمان والضمان" 
                className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-16 pt-12 border-t border-slate-800/80"
              >
                <motion.div 
                  variants={metricCardVariants}
                  whileHover={{ y: -4, borderColor: "rgba(245, 158, 11, 0.4)" }}
                  className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 transition-colors"
                >
                  <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-amber-400 tabular-nums tracking-tight">60 يوم</h2>
                  <p className="text-xs text-slate-400 font-medium mt-1.5 leading-snug">{heroConfig.trialHighlight || 'تجربة مجانية شاملة'}</p>
                </motion.div>
                <motion.div 
                  variants={metricCardVariants}
                  whileHover={{ y: -4, borderColor: "rgba(45, 212, 191, 0.4)" }}
                  className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 transition-colors"
                >
                  <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-teal-400 font-mono tracking-tight">AES-256</h2>
                  <p className="text-xs text-slate-400 font-medium mt-1.5 leading-snug">تشفير عسكري مربوط بالهاتف</p>
                </motion.div>
                <motion.div 
                  variants={metricCardVariants}
                  whileHover={{ y: -4, borderColor: "rgba(245, 158, 11, 0.4)" }}
                  className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 transition-colors"
                >
                  <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-amber-400 tabular-nums tracking-tight">100%</h2>
                  <p className="text-xs text-slate-400 font-medium mt-1.5 leading-snug">سجلات دائمة غير قابلة للتلاعب</p>
                </motion.div>
                <motion.div 
                  variants={metricCardVariants}
                  whileHover={{ y: -4, borderColor: "rgba(45, 212, 191, 0.4)" }}
                  className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 transition-colors"
                >
                  <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-teal-400 font-mono tracking-tight">AI OCR</h2>
                  <p className="text-xs text-slate-400 font-medium mt-1.5 leading-snug">قراءة الميزان والوزن آلياً</p>
                </motion.div>
              </motion.aside>
            </motion.article>
          </div>
        </section>

      {/* Dynamic Features & Capabilities Section (المميزات) */}
      <section 
        id="features" 
        aria-labelledby="features-heading"
        className="py-20 bg-slate-900/40 border-t border-slate-800 scroll-mt-24"
        itemScope
        itemType="https://schema.org/ItemList"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <header className="text-center max-w-2xl mx-auto mb-14">
            <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 inline-block">
              {lang === 'ar' ? 'المميزات والقدرات الحصرية' : 'Exclusive Capabilities'}
            </span>
            <h2 id="features-heading" className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-tight mt-3">
              {lang === 'ar' ? 'كل ما تحتاجه لإدارة الفواتير والديون والمحاسبة والذهب' : 'Everything You Need for Invoicing, Debts & Gold Accounting'}
            </h2>
            <p className="text-xs sm:text-sm text-slate-400 mt-2.5 leading-relaxed max-w-xl mx-auto">
              {lang === 'ar' ? 'منظومة متكاملة مبنية بأحدث معايير الأمان والتشفير والذكاء الاصطناعي.' : 'An integrated ecosystem engineered with advanced encryption & AI standards.'}
            </p>
          </header>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feat, index) => (
              <article 
                key={feat.id}
                itemProp="itemListElement"
                itemScope
                itemType="https://schema.org/ListItem"
                className="bg-slate-950 border border-slate-800 hover:border-amber-500/50 rounded-3xl p-6 transition-all duration-300 hover:shadow-xl hover:shadow-amber-500/10 flex flex-col justify-between group"
              >
                <meta itemProp="position" content={String(index + 1)} />
                <div itemProp="item" itemScope itemType="https://schema.org/Service">
                  <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                    {getFeatureIcon(feat.icon)}
                  </div>
                  <span className="text-[11px] font-bold text-teal-400 bg-teal-500/10 px-2.5 py-1 rounded-md tracking-wide">
                    {feat.category}
                  </span>
                  <h3 itemProp="name" className="text-base sm:text-lg font-bold text-white mt-3 mb-2 tracking-tight leading-snug">{feat.title}</h3>
                  <p itemProp="description" className="text-xs sm:text-sm text-slate-400 leading-relaxed mb-4">{feat.description}</p>
                </div>

                {feat.highlights && feat.highlights.length > 0 && (
                  <footer className="border-t border-slate-800/80 pt-3 space-y-1.5">
                    {feat.highlights.map((item, idx) => (
                      <div key={idx} className="flex items-center gap-2 text-xs text-slate-300 font-medium">
                        <Check className="w-3.5 h-3.5 text-amber-400 flex-shrink-0" aria-hidden="true" />
                        <span>{item}</span>
                      </div>
                    ))}
                  </footer>
                )}
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* Live Gold Ticker & Market Section */}
      <section id="gold-prices" className="py-12 bg-slate-900/80 border-y border-slate-800 scroll-mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4 mb-8">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-amber-500/10 rounded-xl border border-amber-500/20 text-amber-400">
                <TrendingUp className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight leading-snug flex items-center gap-2">
                  أسعار الذهب اللحظية المباشرة
                  <span className="flex h-2.5 w-2.5 relative">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                  </span>
                </h2>
                <p className="text-xs text-slate-400 font-medium mt-0.5">محدثة لحظياً ومتصلة تلقائياً بمحاسب الذهب في التطبيق</p>
              </div>
            </div>

            <span className="text-xs bg-slate-800 text-slate-300 px-3.5 py-1.5 rounded-lg border border-slate-700 font-mono font-semibold tracking-wide">
              الأسعار بالجنيه المصري (EGP)
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
            {goldPrices.map((item, idx) => (
              <div key={idx} className="p-4 rounded-2xl bg-slate-950 border border-slate-800 hover:border-amber-500/40 transition-all shadow-md">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm sm:text-base font-bold text-amber-400 tracking-tight">
                    {item.karat === 'Pound' ? 'الجنيه الذهب' : item.karat === 'Ounce' ? 'الأوقية' : `عيار ${item.karat}`}
                  </span>
                  <span className={`text-xs font-bold font-mono px-2 py-0.5 rounded tabular-nums ${item.change24h >= 0 ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'}`}>
                    {item.change24h >= 0 ? `+${item.change24h}%` : `${item.change24h}%`}
                  </span>
                </div>
                <div className="space-y-1.5">
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-400 font-medium">شراء:</span>
                    <span className="font-black font-mono text-white tabular-nums text-sm">{item.buyPrice.toLocaleString()} ج.م</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-400 font-medium">بيع:</span>
                    <span className="font-bold font-mono text-slate-300 tabular-nums text-xs sm:text-sm">{item.sellPrice.toLocaleString()} ج.م</span>
                  </div>
                </div>
                <div className="mt-3 pt-2 border-t border-slate-800/80 text-[11px] text-slate-500 flex justify-between font-mono">
                  <span>التحديث:</span>
                  <span>{item.lastUpdated}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Interactive Tutorials & Guides Section */}
      <section id="tutorials" className="py-20 bg-slate-950 scroll-mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 inline-block">
              مركز التعلم والشروحات
            </span>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight leading-snug mt-3">
              شروحات تفاعلية لكل ما يخص مشروع أوزوكس
            </h2>
            <p className="text-xs sm:text-sm text-slate-400 mt-2.5 leading-relaxed max-w-xl mx-auto">
              دليل عملي خطوة بخطوة لاستخدام المحاسب الذكي والخزائن المشفرة وكاميرا الميزان.
            </p>
          </div>

          {/* Category Filter Tabs */}
          <div className="flex flex-wrap items-center justify-center gap-2 mb-8">
            {[
              { id: 'all', label: 'جميع الشروحات' },
              { id: 'accounting', label: 'المحاسب والهاشتاج' },
              { id: 'vault', label: 'الخزائن المشفرة' },
              { id: 'ai_camera', label: 'كاميرا الميزان والذهب' },
              { id: 'projects', label: 'إدارة المشاريع ورأس المال' },
              { id: 'backup', label: 'النسخ الاحتياطي المشفر' }
            ].map(cat => (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id)}
                className={`px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold tracking-wide transition-all ${
                  activeCategory === cat.id
                    ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                    : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>

          {/* Overall Learning Progress Tracker Banner */}
          <div className="max-w-4xl mx-auto mb-10 p-5 rounded-2xl bg-gradient-to-r from-amber-500/10 via-slate-900/90 to-teal-500/10 border border-amber-500/30 backdrop-blur-sm shadow-xl">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center border border-amber-500/30 shrink-0">
                  <Trophy className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm sm:text-base font-bold text-white tracking-tight">
                      إجمالي مسار التعلم في المنظومة
                    </h3>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-800 text-amber-400 border border-slate-700">
                      {overallProgress.percent >= 100 
                        ? '🏆 خبير أوزوكس' 
                        : overallProgress.percent >= 50 
                        ? '⭐ متقدم' 
                        : '🌱 مبتدئ'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-0.5">
                    أنجزت {overallProgress.completedSteps} من أصل {overallProgress.totalSteps} خطوة تعليمية عبر جميع الأدلة
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-center font-mono">
                <span className="text-xs text-slate-400 font-sans">الإنجاز الكلي:</span>
                <span className="text-2xl font-black text-amber-400 tabular-nums">
                  {overallProgress.percent}%
                </span>
              </div>
            </div>

            {/* Overall Visual Progress Bar */}
            <div className="relative w-full bg-slate-950 rounded-full h-2.5 overflow-hidden p-0.5 border border-slate-800">
              <div 
                className="h-full rounded-full bg-gradient-to-r from-amber-500 via-amber-400 to-teal-400 transition-all duration-700 ease-out shadow-sm shadow-amber-500/30"
                style={{ width: `${overallProgress.percent}%` }}
              />
            </div>
          </div>

          {/* Tutorial Grid & Detail Drawer */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            {/* List of Tutorials with Visual Progress Bars */}
            <div className="lg:col-span-5 space-y-3">
              {filteredTutorials.map((tut) => {
                const { count, total, percent, isDone } = getTutorialProgressData(tut);
                const isSelected = selectedTutorial?.id === tut.id;
                return (
                  <div
                    key={tut.id}
                    onClick={() => setSelectedTutorial(tut)}
                    className={`p-5 rounded-2xl border cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-slate-900 border-amber-500 shadow-lg shadow-amber-500/10 ring-1 ring-amber-500/30'
                        : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="space-y-2 w-full">
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2">
                            <span className="text-[11px] bg-slate-800 text-amber-400 px-2 py-0.5 rounded font-mono font-bold">
                              {tut.duration}
                            </span>
                            {tut.isFeatured && (
                              <span className="text-[11px] bg-emerald-500/20 text-emerald-400 px-2 py-0.5 rounded font-bold">
                                مميز
                              </span>
                            )}
                          </div>
                          {isDone ? (
                            <span className="text-[11px] font-bold text-emerald-400 bg-emerald-500/15 border border-emerald-500/30 px-2.5 py-0.5 rounded-full flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3" />
                              مكتمل
                            </span>
                          ) : count > 0 ? (
                            <span className="text-[11px] font-bold text-amber-400 bg-amber-500/15 border border-amber-500/30 px-2.5 py-0.5 rounded-full font-mono">
                              {percent}%
                            </span>
                          ) : (
                            <span className="text-[11px] text-slate-500 bg-slate-800/60 px-2 py-0.5 rounded">
                              لم يبدأ
                            </span>
                          )}
                        </div>

                        <h3 className="text-sm sm:text-base font-bold text-white tracking-tight leading-snug">{tut.title}</h3>
                        <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">{tut.summary}</p>

                        {/* Individual Tutorial Visual Progress Bar */}
                        <div className="pt-2 border-t border-slate-800/60 mt-1">
                          <div className="flex items-center justify-between text-[11px] mb-1.5 font-mono">
                            <span className="text-[11px] text-slate-400 font-sans">التقدم في الدليل:</span>
                            <span className={`font-bold ${isDone ? 'text-emerald-400' : count > 0 ? 'text-amber-400' : 'text-slate-500'}`}>
                              {count} / {total} خطوات ({percent}%)
                            </span>
                          </div>
                          <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden p-0.5 border border-slate-800">
                            <div 
                              className={`h-full rounded-full transition-all duration-500 ease-out ${
                                isDone 
                                  ? 'bg-gradient-to-r from-emerald-500 to-teal-400 shadow-xs shadow-emerald-500/30' 
                                  : 'bg-gradient-to-r from-amber-500 to-teal-400'
                              }`}
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Selected Tutorial Preview & Interactive Step Completion */}
            <div className="lg:col-span-7">
              {selectedTutorial ? (() => {
                const { completed, total, count, percent, isDone } = getTutorialProgressData(selectedTutorial);
                return (
                  <div className="bg-slate-900 border border-amber-500/30 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4 border-b border-slate-800 pb-4">
                      <div>
                        <span className="text-xs font-bold font-mono text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-md border border-amber-500/20">
                          {selectedTutorial.duration}
                        </span>
                        <h3 className="text-xl sm:text-2xl font-black text-white tracking-tight leading-snug mt-2">{selectedTutorial.title}</h3>
                      </div>

                      {isDone ? (
                        <span className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-300 bg-emerald-500/20 border border-emerald-500/40 px-3 py-1.5 rounded-xl self-start sm:self-center">
                          <Trophy className="w-4 h-4 text-emerald-400" />
                          دليل مكتمل بالكامل!
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs font-mono font-bold text-amber-400 bg-amber-500/10 border border-amber-500/20 px-3 py-1.5 rounded-xl self-start sm:self-center">
                          {count} من {total} خطوات منجزة
                        </span>
                      )}
                    </div>

                    <p className="text-xs sm:text-sm text-slate-300 mb-6 leading-relaxed bg-slate-950/60 p-4 rounded-xl border border-slate-800">
                      {selectedTutorial.summary}
                    </p>

                    {/* Interactive Progress Tracking Card with Visual Progress Bar */}
                    <div className="mb-6 p-4 rounded-2xl bg-slate-950 border border-slate-800/90 space-y-3 shadow-inner">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-white">نسبة إنجاز الدليل الحالي</span>
                          <span className="text-xs font-mono font-black text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">
                            {percent}%
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          {count > 0 && (
                            <button
                              onClick={() => resetTutorialProgress(selectedTutorial.id)}
                              className="text-[11px] text-slate-400 hover:text-rose-400 flex items-center gap-1 transition-colors px-2.5 py-1 rounded-lg hover:bg-slate-800"
                              title="إعادة ضبط تقدم هذا الدليل"
                            >
                              <RotateCcw className="w-3 h-3" />
                              <span>إعادة ضبط</span>
                            </button>
                          )}
                          {!isDone && (
                            <button
                              onClick={() => markAllStepsCompleted(selectedTutorial)}
                              className="text-[11px] text-amber-400 hover:text-amber-300 font-bold flex items-center gap-1.5 transition-colors px-2.5 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>إكمال جميع الخطوات</span>
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Visual Progress Bar Track & Gradient Fill */}
                      <div className="relative w-full bg-slate-900 rounded-full h-3 overflow-hidden p-0.5 border border-slate-800">
                        <div 
                          className={`h-full rounded-full transition-all duration-500 ease-out ${
                            isDone 
                              ? 'bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-400 shadow-md shadow-emerald-500/40' 
                              : 'bg-gradient-to-r from-amber-500 via-amber-400 to-teal-400 shadow-sm shadow-amber-500/20'
                          }`}
                          style={{ width: `${percent}%` }}
                        />
                      </div>

                      <div className="flex items-center justify-between text-[11px] text-slate-400 pt-0.5">
                        <span>انقر على الخطوة بالأسفل لتحديد إنجازها وتحديث الشريط تلقائياً</span>
                        <span className="font-mono text-slate-300 font-bold">
                          {count} من أصل {total} خطوات منجزة
                        </span>
                      </div>
                    </div>

                    <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 mb-4 flex items-center justify-between">
                      <span className="flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-teal-400" />
                        خطوات التنفيذ التفاعلية:
                      </span>
                      <span className="text-[11px] text-slate-400 normal-case font-normal">
                        (اضغط على أي خطوة لإتمامها)
                      </span>
                    </h4>

                    {/* Interactive Step Items with Completion Checkboxes */}
                    <div className="space-y-3">
                      {selectedTutorial.steps.map((step, idx) => {
                        const isStepCompleted = completed.includes(idx);
                        return (
                          <div 
                            key={idx} 
                            onClick={() => toggleStepCompletion(selectedTutorial.id, idx)}
                            className={`flex items-start gap-3.5 p-3.5 rounded-xl border cursor-pointer select-none transition-all ${
                              isStepCompleted 
                                ? 'bg-emerald-950/20 border-emerald-500/40 shadow-sm shadow-emerald-500/5 hover:bg-emerald-950/30' 
                                : 'bg-slate-950 border-slate-800 hover:border-amber-500/30 hover:bg-slate-950/80'
                            }`}
                          >
                            <button
                              type="button"
                              aria-label={`تبديل حالة إنجاز الخطوة ${idx + 1}`}
                              className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-black flex-shrink-0 transition-all ${
                                isStepCompleted
                                  ? 'bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/30'
                                  : 'bg-slate-800 text-slate-300 border border-slate-700 hover:border-amber-400'
                              }`}
                            >
                              {isStepCompleted ? (
                                <Check className="w-3.5 h-3.5 stroke-[3]" />
                              ) : (
                                idx + 1
                              )}
                            </button>
                            <div className="flex-1 flex items-center justify-between gap-2">
                              <p className={`text-xs sm:text-sm leading-relaxed transition-all ${
                                isStepCompleted 
                                  ? 'text-slate-300 line-through decoration-emerald-500/50 font-medium' 
                                  : 'text-slate-200 font-normal'
                              }`}>
                                {step}
                              </p>
                              {isStepCompleted && (
                                <span className="text-[10px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded shrink-0 border border-emerald-500/20">
                                  تم الإنجاز ✓
                                </span>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>

                    <div className="mt-8 pt-4 border-t border-slate-800 flex items-center justify-between">
                      <div className="flex flex-wrap gap-1.5">
                        {selectedTutorial.tags.map((tag, i) => (
                          <span key={i} className="text-[11px] bg-slate-800 text-slate-400 px-2.5 py-0.5 rounded font-mono">
                            #{tag}
                          </span>
                        ))}
                      </div>

                      <a 
                        href="#download"
                        onClick={(e) => scrollToSection(e, '#download')}
                        className="text-xs sm:text-sm font-bold text-amber-400 hover:text-amber-300 cursor-pointer tracking-wide"
                      >
                        جرب الميزة في التطبيق
                      </a>
                    </div>
                  </div>
                );
              })() : (
                <div className="h-full flex items-center justify-center p-12 bg-slate-900/40 rounded-3xl border border-dashed border-slate-800 text-slate-500 text-xs sm:text-sm font-medium">
                  اختر شرحاً من القائمة لعرض تفاصيله
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Announcements & News Section */}
      <section id="announcements" className="py-16 bg-slate-900/60 border-t border-slate-800 scroll-mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-10">
            <div>
              <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 inline-block">
                إعلانات وأخبار المنصة
              </span>
              <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight leading-snug mt-2">
                آخر التحديثات والإعلانات الرسمية
              </h2>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {announcements.map((ann) => (
              <div 
                key={ann.id}
                className="bg-slate-950 border border-slate-800 hover:border-amber-500/40 rounded-2xl p-6 transition-all shadow-md flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span 
                      className="text-[11px] font-bold px-2.5 py-1 rounded-md tracking-wide"
                      style={{ backgroundColor: `${ann.badgeColor}20`, color: ann.badgeColor }}
                    >
                      {ann.badgeText}
                    </span>
                    <span className="text-xs font-mono text-slate-500">{ann.publishDate}</span>
                  </div>

                  <h3 className="text-base sm:text-lg font-bold text-white mb-2 leading-snug tracking-tight">{ann.title}</h3>
                  <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">{ann.content}</p>
                </div>

                {ann.actionUrl && (
                  <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between">
                    <span className="text-xs font-mono text-slate-500">{ann.viewCount} مشاهدة</span>
                    <a
                      href={ann.actionUrl}
                      onClick={(e) => scrollToSection(e, ann.actionUrl)}
                      className="text-xs font-bold text-amber-400 hover:text-amber-300 cursor-pointer tracking-wide"
                    >
                      {ann.actionLabel || 'تفاصيل'}
                    </a>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing Matrix & 60-Day Trial Offer */}
      <section id="pricing" className="py-20 bg-slate-950 relative overflow-hidden scroll-mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-14">
            <span className="text-teal-400 text-xs font-bold uppercase tracking-wider bg-teal-500/10 px-3.5 py-1 rounded-full border border-teal-500/20 inline-block">
              خطط الاشتراك والأسعار
            </span>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-tight mt-3">
              باقات مرنة تناسب الأفراد والشركات
            </h2>
            <p className="text-xs sm:text-sm text-slate-400 mt-2.5 leading-relaxed max-w-xl mx-auto">
              جميع الباقات تشمل فترة تجريبية مجانية لمدة 60 يوماً بكامل الصلاحيات وبدون أي التزامات.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {/* Free Trial Plan */}
            <div className="bg-slate-900/80 border border-teal-500/40 rounded-3xl p-6 relative flex flex-col justify-between">
              <div className="absolute -top-3 right-6 bg-teal-500 text-slate-950 font-black text-[10px] px-3 py-0.5 rounded-full uppercase tracking-wider">
                عرض ترويجي مجاني
              </div>
              <div>
                <h3 className="text-lg font-bold text-white tracking-tight">النسخة الأساسية التجريبية</h3>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">للمستخدمين الجدد واستكشاف المنصة</p>
                <div className="my-6">
                  <span className="text-3xl sm:text-4xl font-black text-teal-400 tracking-tight">مجاناً</span>
                  <span className="text-xs text-slate-400 mr-2 font-medium">/ لمدة 60 يوماً كاملة</span>
                </div>
                <ul className="space-y-2.5 text-xs text-slate-300 font-medium border-t border-slate-800 pt-4">
                  <li className="flex items-center gap-2">✓ استخدام كامل لكافة ميزات المنصة</li>
                  <li className="flex items-center gap-2">✓ قيود محاسبية وهاشتاج غير محدود</li>
                  <li className="flex items-center gap-2">✓ حساب أرباح الذهب وكاميرا OCR</li>
                  <li className="flex items-center gap-2">✓ نسخ احتياطي محلي مشفر</li>
                </ul>
              </div>
              <a 
                href="#download" 
                onClick={(e) => scrollToSection(e, '#download')}
                className="mt-8 w-full block text-center bg-slate-800 hover:bg-slate-700 text-teal-400 py-3.5 rounded-xl font-bold text-xs tracking-wide cursor-pointer transition-colors"
              >
                ابدأ التجربة المجانية الآن
              </a>
            </div>

            {/* Silver Plan */}
            <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 flex flex-col justify-between">
              <div>
                <h3 className="text-lg font-bold text-white tracking-tight">الباقة الفضية</h3>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">المحاسب وإدارة المحلات الفردية</p>
                <div className="my-6">
                  <span className="text-3xl sm:text-4xl font-black text-white font-mono tabular-nums tracking-tight">499</span>
                  <span className="text-xs text-slate-400 mr-2 font-medium">ج.م / شهرياً</span>
                </div>
                <ul className="space-y-2.5 text-xs text-slate-300 font-medium border-t border-slate-800 pt-4">
                  <li className="flex items-center gap-2">✓ قيود ومحادثات غير محدودة</li>
                  <li className="flex items-center gap-2">✓ تصدير كشوف حسابات Excel وPDF</li>
                  <li className="flex items-center gap-2">✓ حساب فرعي مشفر واحد</li>
                  <li className="flex items-center gap-2">✓ تقاص وتصفية الصنعيات</li>
                </ul>
              </div>
              <a 
                href="#download" 
                onClick={(e) => scrollToSection(e, '#download')}
                className="mt-8 w-full block text-center bg-slate-800 hover:bg-slate-700 text-white py-3.5 rounded-xl font-bold text-xs tracking-wide cursor-pointer transition-colors"
              >
                اشتراك شهري
              </a>
            </div>

            {/* Gold Plan (Most Popular) */}
            <div className="bg-gradient-to-b from-amber-500/10 via-slate-900 to-slate-900 border-2 border-amber-500 rounded-3xl p-6 relative flex flex-col justify-between shadow-xl shadow-amber-500/10">
              <div className="absolute -top-3 right-6 bg-amber-500 text-slate-950 font-black text-[10px] px-3 py-0.5 rounded-full uppercase tracking-wider">
                الأكثر طلباً
              </div>
              <div>
                <h3 className="text-lg font-bold text-amber-400 tracking-tight">الباقة الذهبية</h3>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">كبار الصاغة والشركات والمقاولين</p>
                <div className="my-6">
                  <span className="text-3xl sm:text-4xl font-black text-amber-400 font-mono tabular-nums tracking-tight">999</span>
                  <span className="text-xs text-slate-400 mr-2 font-medium">ج.م / شهرياً</span>
                </div>
                <ul className="space-y-2.5 text-xs text-slate-300 font-medium border-t border-slate-800 pt-4">
                  <li className="flex items-center gap-2">✓ جميع مميزات الفضية</li>
                  <li className="flex items-center gap-2">✓ حسابات فرعية وخزائن مشفرة بلا حدود</li>
                  <li className="flex items-center gap-2">✓ إدارة المشاريع بالصلاحيات (RBAC)</li>
                  <li className="flex items-center gap-2">✓ نظام الشكاوى وتدقيق عجز الصندوق</li>
                </ul>
              </div>
              <a 
                href="#download" 
                onClick={(e) => scrollToSection(e, '#download')}
                className="mt-8 w-full block text-center bg-amber-500 hover:bg-amber-400 text-slate-950 py-3.5 rounded-xl font-black text-xs tracking-wide cursor-pointer transition-colors shadow-lg shadow-amber-500/20"
              >
                اشترك الآن
              </a>
            </div>

            {/* Diamond Plan */}
            <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 flex flex-col justify-between">
              <div>
                <h3 className="text-lg font-bold text-white tracking-tight">الباقة الماسية</h3>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">المؤسسات والمصانع والشركات الكبرى VIP</p>
                <div className="my-6">
                  <span className="text-3xl sm:text-4xl font-black text-white font-mono tabular-nums tracking-tight">1,999</span>
                  <span className="text-xs text-slate-400 mr-2 font-medium">ج.م / شهرياً</span>
                </div>
                <ul className="space-y-2.5 text-xs text-slate-300 font-medium border-t border-slate-800 pt-4">
                  <li className="flex items-center gap-2">✓ وصول غير محدود لكافة ميزات النظام</li>
                  <li className="flex items-center gap-2">✓ قراءة الفواتير والميزان بـ Gemini AI</li>
                  <li className="flex items-center gap-2">✓ نسخ احتياطي تلقائي سحابي مجدول</li>
                  <li className="flex items-center gap-2">✓ مدير حساب ودعم VIP فوري</li>
                </ul>
              </div>
              <a 
                href="#download" 
                onClick={(e) => scrollToSection(e, '#download')}
                className="mt-8 w-full block text-center bg-slate-800 hover:bg-slate-700 text-white py-3.5 rounded-xl font-bold text-xs tracking-wide cursor-pointer transition-colors"
              >
                تواصل للاشتراك VIP
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* What's New & 2026 Engine Updates Section */}
      <section id="updates" className="py-16 bg-slate-900/30 border-t border-slate-800 scroll-mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <span className="text-teal-400 text-xs font-bold uppercase tracking-wider bg-teal-500/10 px-3.5 py-1 rounded-full border border-teal-500/20 inline-block">
              {lang === 'ar' ? 'تحديثات الجيل الجديد 2026' : '2026 Next-Gen Updates'}
            </span>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight leading-snug mt-3">
              {lang === 'ar' ? 'أقوى الميزات المحاسبية المطورة حديثاً' : 'Latest Accounting & AI Advancements'}
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 relative overflow-hidden">
              <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 font-black text-xs font-mono mb-4">
                01
              </div>
              <h3 className="text-base sm:text-lg font-bold text-white mb-2 tracking-tight leading-snug">
                {lang === 'ar' ? 'محرك إكسل ومصفوفات SUMIFS' : 'SUMIFS Excel Formula Engine'}
              </h3>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                {lang === 'ar' 
                  ? 'تصدير كشوف حسابات محاسبية حقيقية بصيغ إكسل تفاعلية، مع التنبؤ بالتدفق النقدي لـ 90 يوماً وتنسيق شرطي آلي.' 
                  : 'Export native Excel sheets with dynamic SUMIFS formulas, 90-day cash forecasting, and auto conditional formatting.'}
              </p>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 relative overflow-hidden">
              <div className="w-10 h-10 rounded-xl bg-teal-500/10 border border-teal-500/30 flex items-center justify-center text-teal-400 font-black text-xs font-mono mb-4">
                02
              </div>
              <h3 className="text-base sm:text-lg font-bold text-white mb-2 tracking-tight leading-snug">
                {lang === 'ar' ? 'فواتير واتساب بكود QR فوري' : 'WhatsApp Invoices with QR'}
              </h3>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                {lang === 'ar'
                  ? 'إصدار ومشاركة فواتير PDF المعتمدة مباشرة إلى محادثات العملاء برابط وتشفير غير قابل للتعديل.'
                  : 'Generate and send certified digital invoices to customer WhatsApp with permanent checksum verification.'}
              </p>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 relative overflow-hidden">
              <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 font-black text-xs font-mono mb-4">
                03
              </div>
              <h3 className="text-base sm:text-lg font-bold text-white mb-2 tracking-tight leading-snug">
                {lang === 'ar' ? 'الخزائن المخفية (Zero-Trace)' : 'Zero-Trace Hardware Vaults'}
              </h3>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                {lang === 'ar'
                  ? 'حسابات فرعية مستقلة ومشفرة برمز PIN لا تترك أي أثر على الشاشة الرئيسية عند قفلها.'
                  : 'Hardware-tied encrypted sub-accounts with zero notification trace for maximum financial privacy.'}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Dedicated FAQ Section mapped to JSON-LD Schema */}
      <section 
        id="faq" 
        aria-labelledby="faq-heading"
        className="py-16 bg-slate-900/60 border-t border-slate-800 scroll-mt-24"
        itemScope 
        itemType="https://schema.org/FAQPage"
      >
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <header className="text-center mb-8">
            <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 inline-block">
              {lang === 'ar' ? 'الأسئلة المتكررة ودليل الأعمال' : 'Business FAQ & Knowledge Base'}
            </span>
            <h2 id="faq-heading" className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight leading-snug mt-2">
              {lang === 'ar' ? 'إجابات على أهم استفسارات الفوترة والمحاسبة والأمان' : 'Answers to Key Invoicing, Accounting & Security Questions'}
            </h2>
            <p className="text-xs sm:text-sm text-slate-400 mt-2.5 max-w-xl mx-auto leading-relaxed">
              {lang === 'ar' 
                ? 'إجابات شاملة وموثقة مصممة لمساعدة التجار والمحاسبين وفهرستها مباشرة بواسطة محركات البحث والذكاء الاصطناعي.' 
                : 'Comprehensive answers optimized for merchants, accountants, and direct AI agent retrieval.'}
            </p>
          </header>

          {/* Search & Category Filter Controls */}
          <div className="mb-8 space-y-4">
            {/* Search Input */}
            <div className="relative max-w-md mx-auto">
              <input
                type="text"
                value={faqSearch}
                onChange={(e) => setFaqSearch(e.target.value)}
                placeholder={lang === 'ar' ? 'ابحث في الأسئلة (مثال: فواتير، ذهب، إكسل، أمان)...' : 'Search questions (e.g. invoices, gold, Excel, security)...'}
                className="w-full bg-slate-950 border border-slate-800 rounded-2xl px-4 py-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 transition-all shadow-inner"
              />
              {faqSearch && (
                <button
                  onClick={() => setFaqSearch('')}
                  className="absolute left-3 top-2.5 px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-bold rounded-lg transition-colors"
                  aria-label="مسح البحث"
                >
                  مسح
                </button>
              )}
            </div>

            {/* Category Filter Pills */}
            <div className="flex flex-wrap items-center justify-center gap-2">
              {[
                { id: 'all', labelAr: 'الكل (9)', labelEn: 'All (9)' },
                { id: 'invoices', labelAr: 'الفواتير والواتساب', labelEn: 'Invoicing & WhatsApp' },
                { id: 'accounting', labelAr: 'المحاسبة وإكسل', labelEn: 'Accounting & Excel' },
                { id: 'gold', labelAr: 'الصاغة والذهب', labelEn: 'Gold & Jewelry' },
                { id: 'security', labelAr: 'الأمان والتشفير', labelEn: 'Security & Privacy' },
                { id: 'general', labelAr: 'عام وتجربة', labelEn: 'General & Trial' },
              ].map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setFaqCategory(cat.id)}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
                    faqCategory === cat.id
                      ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-500/20'
                      : 'bg-slate-950 text-slate-400 border border-slate-800 hover:border-slate-700 hover:text-slate-200'
                  }`}
                >
                  {lang === 'ar' ? cat.labelAr : cat.labelEn}
                </button>
              ))}
            </div>
          </div>

          {/* FAQ Accordion List */}
          <div className="space-y-3">
            {filteredFaqs.length > 0 ? (
              filteredFaqs.map((faq, idx) => (
                <article 
                  key={faq.id || idx}
                  itemScope
                  itemProp="mainEntity"
                  itemType="https://schema.org/Question"
                  className="bg-slate-950 border border-slate-800 hover:border-slate-700 rounded-2xl overflow-hidden transition-all shadow-sm"
                >
                  <button
                    onClick={() => setExpandedFaq(expandedFaq === idx ? null : idx)}
                    aria-expanded={expandedFaq === idx}
                    className="w-full p-5 text-right flex items-center justify-between gap-4 font-bold text-sm text-slate-200 hover:text-amber-400 transition-colors"
                  >
                    <span itemProp="name" className="leading-snug">{faq.q}</span>
                    <span className="text-xs font-bold text-slate-500 flex-shrink-0">
                      {expandedFaq === idx ? 'إغلاق' : 'عرض'}
                    </span>
                  </button>
                  {expandedFaq === idx && (
                    <div 
                      itemScope 
                      itemProp="acceptedAnswer" 
                      itemType="https://schema.org/Answer"
                      className="px-5 pb-5 pt-1 text-xs sm:text-sm text-slate-400 leading-relaxed border-t border-slate-800/80 bg-slate-900/30"
                    >
                      <p itemProp="text">{faq.a}</p>
                    </div>
                  )}
                </article>
              ))
            ) : (
              <div className="text-center py-10 bg-slate-950 border border-slate-800 rounded-2xl p-6">
                <p className="text-xs text-slate-400 mb-3">
                  {lang === 'ar' ? 'لم يتم العثور على أسئلة تطابق بحثك.' : 'No matching questions found.'}
                </p>
                <button
                  onClick={() => { setFaqCategory('all'); setFaqSearch(''); }}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-amber-400 text-xs font-bold rounded-xl"
                >
                  {lang === 'ar' ? 'إعادة ضبط عوامل التصفية' : 'Reset Filters'}
                </button>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Lead Generation & Consultation Request Section */}
      <section id="contact" className="py-20 bg-slate-900/50 border-t border-slate-800 scroll-mt-24">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-10">
            <span className="text-amber-400 text-xs font-bold uppercase tracking-wider bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 inline-block">
              {lang === 'ar' ? 'استشارة مجانية وعرض حي' : 'Free Demo & Inquiries'}
            </span>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight leading-snug mt-2">
              {lang === 'ar' ? 'تواصل مع مستشاري التحول الرقمي المحاسبي' : 'Request a Tailored Demo for Your Business'}
            </h2>
            <p className="text-xs sm:text-sm text-slate-400 mt-2.5 max-w-xl mx-auto leading-relaxed">
              {lang === 'ar' ? 'احصل على إعداد مخصص لحسابات نشاطك التجاري والذهب والمقاولات خلال أقل من ساعتين.' : 'Get customized onboarding and workflow setup in under 2 hours.'}
            </p>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl relative">
            {contactStatus === 'success' ? (
              <div className="text-center py-8">
                <div className="w-16 h-16 bg-teal-500/20 text-teal-400 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl font-black">
                  ✓
                </div>
                <h3 className="text-xl sm:text-2xl font-black text-white mb-2 tracking-tight">
                  {lang === 'ar' ? 'تم استلام طلبك بنجاح!' : 'Request Received Successfully!'}
                </h3>
                <p className="text-xs sm:text-sm text-slate-400 max-w-md mx-auto leading-relaxed">
                  {lang === 'ar' 
                    ? 'سيقوم فريق الدعم الفني والمحاسبي في OOZOX بالتواصل معك خلال دقائق لتفعيل وتخصيص نسختك.' 
                    : 'Our technical onboarding team will contact you shortly to configure your system.'}
                </p>
                <button 
                  onClick={() => setContactStatus('idle')}
                  className="mt-6 px-6 py-2.5 bg-slate-800 hover:bg-slate-700 text-amber-400 rounded-xl text-xs font-bold tracking-wide transition-all"
                >
                  {lang === 'ar' ? 'إرسال استفسار آخر' : 'Send Another Request'}
                </button>
              </div>
            ) : (
              <form onSubmit={handleContactSubmit} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-300 tracking-wide mb-1.5">
                      {lang === 'ar' ? 'الاسم أو اسم المؤسسة / المحل *' : 'Your Name / Business Name *'}
                    </label>
                    <input 
                      type="text" 
                      required
                      value={contactName}
                      onChange={(e) => setContactName(e.target.value)}
                      placeholder={lang === 'ar' ? 'مثال: محلات النور للذهب' : 'e.g. Al Noor Jewelry'}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 transition-colors"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-300 tracking-wide mb-1.5">
                      {lang === 'ar' ? 'رقم الهاتف أو واتساب *' : 'Phone / WhatsApp *'}
                    </label>
                    <input 
                      type="text" 
                      required
                      value={contactPhoneOrEmail}
                      onChange={(e) => setContactPhoneOrEmail(e.target.value)}
                      placeholder={lang === 'ar' ? '+2010xxxxxxxx أو +9665xxxxxxxx' : '+1234567890'}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 font-mono transition-colors"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-300 tracking-wide mb-1.5">
                      {lang === 'ar' ? 'نوع النشاط التجاري' : 'Business Sector'}
                    </label>
                    <select 
                      value={contactBusinessType}
                      onChange={(e) => setContactBusinessType(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white focus:outline-none focus:border-amber-400 transition-colors"
                    >
                      <option value="تجارة الذهب والمجوهرات">{lang === 'ar' ? 'تجارة الذهب والمجوهرات' : 'Gold & Jewelry Trading'}</option>
                      <option value="تجارة جملة / تجزئة">{lang === 'ar' ? 'تجارة جملة وتجزئة' : 'Retail / Wholesale'}</option>
                      <option value="مقاولات ومصانع">{lang === 'ar' ? 'مقاولات وورش ومصانع' : 'Contracting & Manufacturing'}</option>
                      <option value="مكتب محاسبة ومراجعة">{lang === 'ar' ? 'مكتب محاسبة ومراجعة' : 'Accounting Firm'}</option>
                      <option value="أخرى">{lang === 'ar' ? 'نشاط آخر' : 'Other'}</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-300 tracking-wide mb-1.5">
                      {lang === 'ar' ? 'طبيعة الاستفسار أو الاحتياج' : 'Message or Inquiries'}
                    </label>
                    <input 
                      type="text" 
                      value={contactMessage}
                      onChange={(e) => setContactMessage(e.target.value)}
                      placeholder={lang === 'ar' ? 'مثال: ربط كشوفات Excel وتدريب المحاسبين' : 'e.g. Need help migrating Excel sheets'}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 transition-colors"
                    />
                  </div>
                </div>

                <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3">
                  <button 
                    type="button"
                    onClick={() => onNavigateToContact ? onNavigateToContact() : window.location.href = '/preview/ContactUs'}
                    className="w-full sm:w-auto inline-flex items-center justify-center gap-2 bg-slate-900 hover:bg-slate-800 border border-slate-700 hover:border-amber-400/50 text-slate-300 hover:text-amber-400 font-bold px-5 py-3 rounded-xl transition-all text-xs cursor-pointer"
                  >
                    <span>{lang === 'ar' ? 'أو انتقل لصفحة اتصل بنا الشاملة ←' : 'Or visit Full Contact Center →'}</span>
                  </button>

                  <button 
                    type="submit" 
                    disabled={contactStatus === 'submitting'}
                    className="w-full sm:w-auto inline-flex items-center justify-center bg-gradient-to-r from-amber-500 to-amber-400 text-slate-950 font-black px-8 py-3.5 rounded-xl shadow-lg hover:scale-105 transition-all text-xs sm:text-sm cursor-pointer tracking-wide"
                  >
                    <span>{contactStatus === 'submitting' ? (lang === 'ar' ? 'جاري الإرسال...' : 'Submitting...') : (lang === 'ar' ? 'طلب التواصل المباشر' : 'Request Direct Contact')}</span>
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </section>

      {/* App Download Ecosystem Call To Action */}
      <section id="download" className="py-20 bg-gradient-to-t from-slate-950 via-slate-900 to-slate-950 border-t border-slate-800 scroll-mt-24">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="p-8 sm:p-12 rounded-3xl bg-gradient-to-r from-amber-500/10 via-slate-900 to-teal-500/10 border border-amber-500/30 shadow-2xl relative">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-snug mb-4">
              {lang === 'ar' ? 'ابدأ الآن مع أوزوكس واحصل على 60 يوماً مجاناً' : 'Get Started with OOZOX & Enjoy 60 Days Free'}
            </h2>
            <p className="text-slate-300 max-w-xl mx-auto text-sm sm:text-base leading-relaxed mb-8">
              {lang === 'ar' 
                ? 'حمل التطبيق الرسمي على هاتفك واستمتع بنظام إدارة حسابات الذهب والفواتير المشفر الأكثر أماناً وسرعة.' 
                : 'Download the official application on your mobile device for ultra-fast, encrypted accounting & invoicing.'}
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-3xl mx-auto">
              {/* Direct APK Card */}
              <button 
                onClick={() => alert('جاري تحميل حزمة APK الرسمية لتطبيق OOZOX...')}
                className="bg-slate-950 hover:bg-slate-900 border border-amber-500/50 hover:border-amber-400 p-6 rounded-2xl flex flex-col items-center justify-center gap-2.5 group transition-all"
              >
                <span className="font-black text-white text-sm sm:text-base tracking-tight">حزمة APK المباشرة</span>
                <span className="text-xs text-amber-400 font-bold font-mono bg-amber-500/10 px-3 py-1 rounded-lg">تنزيل فوري v3.0</span>
              </button>

              {/* Google Play */}
              <button 
                onClick={() => alert('رابط Google Play Store سيتوفر قريباً عبر النطاق الرسمي!')}
                className="bg-slate-950 hover:bg-slate-900 border border-slate-800 hover:border-teal-400 p-6 rounded-2xl flex flex-col items-center justify-center gap-2.5 group transition-all"
              >
                <span className="font-black text-white text-sm sm:text-base tracking-tight">Google Play Store</span>
                <span className="text-xs text-slate-400 font-semibold">نظام أندرويد الرسمي</span>
              </button>

              {/* Apple App Store */}
              <button 
                onClick={() => alert('نسخة Apple iOS قيد المراجعة في متجر التطبيقات وستتاح قريباً!')}
                className="bg-slate-950 hover:bg-slate-900 border border-slate-800 hover:border-slate-600 p-6 rounded-2xl flex flex-col items-center justify-center gap-2.5 group transition-all"
              >
                <span className="font-black text-white text-sm sm:text-base tracking-tight">Apple App Store</span>
                <span className="text-xs text-slate-400 font-semibold">لأجهزة iPhone و iPad</span>
              </button>
            </div>
          </div>
        </div>
      </section>
      </main>

      {/* Footer */}
      <footer className="bg-slate-950 border-t border-slate-800 py-12 text-slate-400 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-amber-500 flex items-center justify-center text-slate-950 font-black text-sm">
              OX
            </div>
            <div>
              <span className="font-bold text-slate-200">منصة أوزوكس (OOZOX.COM)</span>
              <p className="text-[10px] text-slate-500">جميع الحقوق محفوظة © 2026</p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-6">
            {navLinks.map((link) => (
              <a 
                key={link.id}
                href={link.href} 
                onClick={(e) => scrollToSection(e, link.href, link.isContact)}
                className="hover:text-amber-400 cursor-pointer transition-colors"
              >
                {link.label}
              </a>
            ))}
            <button 
              onClick={onNavigateToAdminLogin} 
              className="text-slate-500 hover:text-slate-300 text-xs font-semibold underline transition-colors"
            >
              بوابة النظام
            </button>
          </div>
        </div>
      </footer>

      {/* Live System Broadcast Toast Notification */}
      {isBroadcastVisible && broadcastToast && (
        <motion.div
          initial={{ opacity: 0, y: 50, scale: 0.95 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 30, scale: 0.95 }}
          transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          onMouseEnter={() => setIsBroadcastHovered(true)}
          onMouseLeave={() => setIsBroadcastHovered(false)}
          className="fixed bottom-6 right-4 sm:right-6 z-50 max-w-md w-[calc(100vw-2rem)] sm:w-full bg-slate-900/95 border border-amber-500/40 rounded-2xl shadow-2xl backdrop-blur-xl p-4 sm:p-5 text-right overflow-hidden shadow-amber-500/10"
          dir="rtl"
          role="alert"
          aria-live="assertive"
        >
          {/* Ambient Glow Accent */}
          <div 
            className="absolute top-0 right-0 w-32 h-32 blur-3xl opacity-20 pointer-events-none rounded-full"
            style={{ backgroundColor: broadcastToast.badgeColor }}
          />

          <div className="flex items-start justify-between gap-3 relative z-10">
            <div className="flex items-start gap-3 flex-1">
              {/* Pulsing beacon icon */}
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5 border shadow-inner"
                style={{ 
                  backgroundColor: `${broadcastToast.badgeColor}20`, 
                  borderColor: `${broadcastToast.badgeColor}40`,
                  color: broadcastToast.badgeColor 
                }}
              >
                <Radio className="w-5 h-5 animate-pulse" />
              </div>

              <div className="space-y-1.5 flex-1">
                <div className="flex items-center gap-2">
                  <span 
                    className="text-[11px] font-black px-2.5 py-0.5 rounded-md tracking-wide inline-flex items-center gap-1.5 shadow-sm"
                    style={{ backgroundColor: `${broadcastToast.badgeColor}20`, color: broadcastToast.badgeColor }}
                  >
                    <span 
                      className="w-2 h-2 rounded-full animate-ping" 
                      style={{ backgroundColor: broadcastToast.badgeColor }} 
                    />
                    {broadcastToast.badgeText}
                  </span>
                  <span className="text-[10px] text-slate-400 font-mono">بث مباشر من الإدارة</span>
                </div>

                <h4 className="text-sm sm:text-base font-black text-white leading-snug tracking-tight">
                  {broadcastToast.title}
                </h4>

                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                  {broadcastToast.message}
                </p>

                {broadcastToast.actionLabel && (
                  <div className="pt-2">
                    <a
                      href={broadcastToast.actionUrl || '#download'}
                      onClick={(e) => {
                        if (broadcastToast.actionUrl?.startsWith('#')) {
                          scrollToSection(e as any, broadcastToast.actionUrl);
                        }
                      }}
                      className="inline-flex items-center gap-1.5 text-xs font-black px-3.5 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 shadow-md shadow-amber-500/20 transition-all hover:scale-105 active:scale-95"
                    >
                      <span>{broadcastToast.actionLabel}</span>
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  </div>
                )}
              </div>
            </div>

            {/* Dismiss button */}
            <button
              onClick={dismissBroadcast}
              className="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800 transition-colors flex-shrink-0"
              aria-label="إغلاق الإشعار"
              title="إغلاق الإشعار"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Countdown Progress Bar (if duration > 0) */}
          {broadcastToast.durationSeconds && broadcastToast.durationSeconds > 0 ? (
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-slate-800/80">
              <div 
                className="h-full transition-all duration-1000 ease-linear"
                style={{ 
                  width: `${Math.max(0, (broadcastTimeRemaining / broadcastToast.durationSeconds) * 100)}%`,
                  backgroundColor: broadcastToast.badgeColor 
                }}
              />
            </div>
          ) : null}
        </motion.div>
      )}

      {/* Smooth Back-to-Top Floating Button */}
      {showScrollTop && (
        <button
          onClick={scrollToTop}
          className="fixed bottom-6 left-6 z-50 px-4 py-2.5 rounded-xl bg-gradient-to-tr from-amber-500 to-amber-400 text-slate-950 text-xs font-black shadow-2xl shadow-amber-500/30 hover:scale-105 active:scale-95 transition-all duration-300 tracking-wide"
          title="العودة للأعلى"
          aria-label="العودة لأعلى الصفحة"
        >
          للأعلى
        </button>
      )}

      </div>
    </SEOProvider>
  );
};

export { LandingPageSkeleton };
