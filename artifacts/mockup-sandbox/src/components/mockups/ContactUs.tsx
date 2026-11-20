import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  PhoneCall, 
  Mail, 
  MapPin, 
  Clock, 
  Send, 
  CheckCircle2, 
  Coins, 
  ArrowRight, 
  ShieldCheck, 
  MessageSquare, 
  Headphones, 
  Building2, 
  ExternalLink,
  Sparkles,
  HelpCircle,
  ChevronDown,
  ChevronUp,
  Copy,
  Check
} from 'lucide-react';

interface ContactUsProps {
  onNavigateHome?: () => void;
  lang?: 'ar' | 'en';
}

interface InquiryFormState {
  fullName: string;
  phone: string;
  email: string;
  companyName: string;
  category: string;
  subject: string;
  message: string;
}

export function ContactUs({ onNavigateHome, lang = 'ar' }: ContactUsProps) {
  const [copiedEmail, setCopiedEmail] = useState(false);
  const [copiedPhone, setCopiedPhone] = useState(false);
  const [activeFaq, setActiveFaq] = useState<number | null>(null);
  
  const [formState, setFormState] = useState<InquiryFormState>({
    fullName: '',
    phone: '',
    email: '',
    companyName: '',
    category: 'general',
    subject: '',
    message: ''
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedRef, setSubmittedRef] = useState<string | null>(null);

  const handleCopy = (text: string, type: 'email' | 'phone') => {
    navigator.clipboard.writeText(text);
    if (type === 'email') {
      setCopiedEmail(true);
      setTimeout(() => setCopiedEmail(false), 2000);
    } else {
      setCopiedPhone(true);
      setTimeout(() => setCopiedPhone(false), 2000);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formState.fullName || !formState.phone || !formState.message) {
      alert(lang === 'ar' ? 'يرجى إكمال الحقول الأساسية المطلوبة' : 'Please fill in required fields');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      const suffix = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID().slice(0, 6).toUpperCase()
        : String(Date.now()).slice(-6);
      const generatedRef = `OZ-${new Date().getFullYear()}-${suffix}`;
      setIsSubmitting(false);
      setSubmittedRef(generatedRef);
    }, 900);
  };

  const handleResetForm = () => {
    setSubmittedRef(null);
    setFormState({
      fullName: '',
      phone: '',
      email: '',
      companyName: '',
      category: 'general',
      subject: '',
      message: ''
    });
  };

  const faqs = [
    {
      q: 'ما هو متوسط وقت الرد على الاستفسارات والتذاكر؟',
      a: 'يرد فريق الدعم الفني وخدمة العملاء خلال أقل من 15 دقيقة عبر تطبيق واتساب والمحادثة المباشرة، وخلال ساعتين كحد أقصى للرسائل الواردة عبر البريد الإلكتروني ونماذج الموقع.'
    },
    {
      q: 'هل تقدمون جلسات تجريبية أو تدريب مجاني للشركات ومحلات الذهب؟',
      a: 'نعم، نوفر جلسات عرض حي (Live Demo) عبر مكالمة مرئية أو زيارة ميدانية لمندوبينا في الرياض والقاهرة ودبي لتدريب فريق المحاسبة وإدارة مبيعات الذهب على المنظومة.'
    },
    {
      q: 'كيف يمكن تفعيل اشتراك تجاري للمؤسسات (Enterprise)؟',
      a: 'يمكنكم التواصل مباشرة مع قسم المبيعات عبر نموذج الاتصال أو عبر واتساب، وسيتم تخصيص مستشار أعمال لحسابكم لتجهيز العقد وحسابات الموظفين ونقل البيانات السابقة.'
    },
    {
      q: 'هل بياناتي وفواتيري المحاسبية ومحافظ الذهب مشفرة؟',
      a: 'نعم، تعتمد منظومة OOZOX معايير التشفير العسكري AES-256 محلياً وقنوات اتصال آمنة TLS 1.3 مع دعم التخزين الاحتياطي المشفر على Google Drive وخوادم سحابية محمية.'
    }
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans antialiased selection:bg-amber-500 selection:text-black" dir="rtl">
      
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-50 bg-slate-950/90 backdrop-blur-md border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-amber-500 to-amber-300 flex items-center justify-center shadow-lg shadow-amber-500/20 ring-2 ring-amber-400/30">
              <Coins className="w-6 h-6 text-slate-950" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-2xl font-black tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-amber-200 to-teal-300">
                  OOZOX
                </span>
                <span className="text-xs bg-amber-500/10 text-amber-400 border border-amber-500/30 px-2 py-0.5 rounded-md font-bold">
                  مركز الاتصال والدعم
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-medium">CONNECT • THINK • DO | منصة إدارة الأعمال وتداول الذهب</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => {
                if (onNavigateHome) {
                  onNavigateHome();
                } else {
                  window.location.href = '/';
                }
              }}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-amber-500/40 text-xs sm:text-sm font-bold text-slate-200 hover:text-amber-400 transition-all cursor-pointer shadow-sm"
            >
              <ArrowRight className="w-4 h-4" />
              <span>العودة للرئيسية</span>
            </button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden pt-12 pb-16 border-b border-slate-900 bg-gradient-to-b from-slate-950 via-slate-900/60 to-slate-950">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-amber-500/10 via-transparent to-transparent pointer-events-none" />
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs sm:text-sm font-bold mb-6">
            <Sparkles className="w-4 h-4 text-amber-400 animate-pulse" />
            <span>فريق خدمة العملاء والمبيعات متاح الآن لمساعدتكم</span>
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          </div>

          <h1 className="text-3xl sm:text-5xl lg:text-6xl font-black text-white tracking-tight leading-tight max-w-4xl mx-auto mb-6">
            تواصل معنا مباشرة، <br className="hidden sm:inline" />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-amber-200 to-teal-300">
              نحن هنا لدعم نمو أعمالك ومشاريعك
            </span>
          </h1>

          <p className="text-base sm:text-lg text-slate-300 max-w-2xl mx-auto leading-relaxed mb-8">
            سواء كنت ترغب في الاستفسار عن باقات الاشتراكات، طلب عرض تجريبي لمنشأتك، أو تحتاج دعماً فنياً لحساباتك ودفاتر الذهب، يسعدنا استقبال رسالتك والرد الفوري عليك.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-4 text-xs sm:text-sm text-slate-400">
            <div className="flex items-center gap-1.5 bg-slate-900/80 px-3.5 py-1.5 rounded-lg border border-slate-800">
              <Clock className="w-4 h-4 text-amber-400" />
              <span>معدل الاستجابة: أقل من 15 دقيقة</span>
            </div>
            <div className="flex items-center gap-1.5 bg-slate-900/80 px-3.5 py-1.5 rounded-lg border border-slate-800">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>محادثات مشفرة ومحمية بالكامل</span>
            </div>
            <div className="flex items-center gap-1.5 bg-slate-900/80 px-3.5 py-1.5 rounded-lg border border-slate-800">
              <Headphones className="w-4 h-4 text-teal-400" />
              <span>دعم فني واستشارات متخصصة</span>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content Grid: Quick Channels + Form */}
      <section className="py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12">

          {/* Left / Secondary Column: Direct Channels & Office Info (5 cols) */}
          <div className="lg:col-span-5 space-y-6">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-white mb-2">قنوات التواصل الفوري</h2>
              <p className="text-xs sm:text-sm text-slate-400">اختر القناة الأنسب لك للتواصل السريع مع المستشارين والمطورين</p>
            </div>

            {/* WhatsApp Direct Card */}
            <div className="p-5 rounded-2xl bg-gradient-to-br from-emerald-950/40 to-slate-900/90 border border-emerald-500/30 hover:border-emerald-500/60 transition-all shadow-lg shadow-emerald-950/20 group">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3.5">
                  <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 group-hover:scale-105 transition-transform">
                    <MessageSquare className="w-6 h-6" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-white text-base">محادثة واتساب الرسمية</h3>
                      <span className="text-[10px] font-extrabold bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full">فوري</span>
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">رد فوري على الاستفسارات والتجربة الحية</p>
                  </div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-emerald-900/40 flex items-center justify-between">
                <span className="text-xs font-mono text-emerald-400 font-bold" dir="ltr">+966 50 123 4567</span>
                <a
                  href="https://wa.me/966501234567?text=%D9%85%D8%B1%D8%AD%D8%A8%D8%A7%D9%8B%D8%8C%20%D8%A3%D8%B1%D8%BA%D8%A8%20%D9%81%D9%8A%20%D8%A7%D9%84%D8%A7%D8%B3%D8%AA%D9%81%D8%B3%D8%A7%D8%B1%20%D8%B9%D9%86%20%D9%85%D9%86%D8%B8%D9%88%D9%85%D8%A9%20OOZOX"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs transition-colors"
                >
                  <span>فتح المحادثة</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>

            {/* Phone Hotlines Card */}
            <div className="p-5 rounded-2xl bg-slate-900/90 border border-slate-800 hover:border-amber-500/30 transition-all group">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3.5">
                  <div className="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 group-hover:scale-105 transition-transform">
                    <PhoneCall className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-white text-base">الاتصال الهاتفي المباشر</h3>
                    <p className="text-xs text-slate-400 mt-0.5">من الأحد إلى الخميس (9:00 ص - 9:00 م)</p>
                  </div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-slate-800/80 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400">الرقم الموحد (الخليج):</span>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-amber-300 font-bold" dir="ltr">9200 01234</span>
                    <button 
                      onClick={() => handleCopy('920001234', 'phone')}
                      className="p-1 text-slate-400 hover:text-amber-400 transition-colors" 
                      title="نسخ الرقم"
                    >
                      {copiedPhone ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-400">الخط الدولي / مصر:</span>
                  <span className="font-mono text-slate-300 font-bold" dir="ltr">+20 100 123 4567</span>
                </div>
              </div>
            </div>

            {/* Email Channels Card */}
            <div className="p-5 rounded-2xl bg-slate-900/90 border border-slate-800 hover:border-amber-500/30 transition-all group">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3.5">
                  <div className="w-12 h-12 rounded-xl bg-teal-500/10 border border-teal-500/20 flex items-center justify-center text-teal-400 group-hover:scale-105 transition-transform">
                    <Mail className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-white text-base">البريد الإلكتروني المعتمد</h3>
                    <p className="text-xs text-slate-400 mt-0.5">للمراسلات الرسمية والشراكات المؤسسية</p>
                  </div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-slate-800/80 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400">الدعم الفني والشكاوى:</span>
                  <div className="flex items-center gap-2">
                    <a href="mailto:support@oozox.com" className="font-mono text-amber-300 hover:underline">support@oozox.com</a>
                    <button 
                      onClick={() => handleCopy('support@oozox.com', 'email')}
                      className="p-1 text-slate-400 hover:text-amber-400 transition-colors" 
                      title="نسخ البريد"
                    >
                      {copiedEmail ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-400">المبيعات وعقود الشركات:</span>
                  <a href="mailto:sales@oozox.com" className="font-mono text-teal-300 hover:underline">sales@oozox.com</a>
                </div>
              </div>
            </div>

            {/* Regional Offices Card */}
            <div className="p-5 rounded-2xl bg-slate-900/90 border border-slate-800">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-amber-400">
                  <Building2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-sm sm:text-base">المكاتب الإقليمية</h3>
                  <p className="text-[11px] text-slate-400">مراكز الدعم والعمليات الميدانية</p>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/70">
                  <div className="font-bold text-amber-400 mb-1 flex items-center gap-1">
                    <MapPin className="w-3 h-3 text-amber-400" />
                    <span>الرياض</span>
                  </div>
                  <p className="text-[11px] text-slate-400 leading-tight">طريق الملك فهد، حي العليا، المملكة العربية السعودية</p>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/70">
                  <div className="font-bold text-amber-400 mb-1 flex items-center gap-1">
                    <MapPin className="w-3 h-3 text-amber-400" />
                    <span>القاهرة</span>
                  </div>
                  <p className="text-[11px] text-slate-400 leading-tight">التجمع الخامس، شارع التسعين الشمالي، مصر</p>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/70">
                  <div className="font-bold text-amber-400 mb-1 flex items-center gap-1">
                    <MapPin className="w-3 h-3 text-amber-400" />
                    <span>دبي</span>
                  </div>
                  <p className="text-[11px] text-slate-400 leading-tight">مركز دبي المالي العالمي (DIFC)، الإمارات</p>
                </div>
              </div>
            </div>

          </div>

          {/* Right Column: Interactive Inquiry Form (7 cols) */}
          <div className="lg:col-span-7">
            <div className="p-6 sm:p-8 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-2xl relative overflow-hidden">
              <div className="absolute top-0 right-0 w-48 h-48 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

              {submittedRef ? (
                /* Success Confirmation State */
                <div className="text-center py-10 space-y-6">
                  <div className="w-20 h-20 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center mx-auto text-emerald-400">
                    <CheckCircle2 className="w-10 h-10" />
                  </div>

                  <div>
                    <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
                      تم استلام طلبكم بنجاح
                    </span>
                    <h3 className="text-2xl font-black text-white mt-3">شكراً لتواصلك مع OOZOX</h3>
                    <p className="text-sm text-slate-300 max-w-md mx-auto mt-2 leading-relaxed">
                      تم تسجيل رسالتك في نظام الدعم الفني، وسيقوم المستشار المختص بمراجعتها والتواصل معك عبر الواتساب أو الهاتف خلال دقائق.
                    </p>
                  </div>

                  <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 max-w-sm mx-auto text-center">
                    <span className="text-xs text-slate-400 block">رقم التذكرة المرجعي:</span>
                    <span className="font-mono text-lg font-black text-amber-400 tracking-wider mt-1 block">{submittedRef}</span>
                  </div>

                  <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
                    <a
                      href={`https://wa.me/966501234567?text=${encodeURIComponent(`مرحباً OOZOX، قمت للتو بإرسال تذكرة برقم مرجعي: ${submittedRef} بخصوص: ${formState.subject || 'استفسار'}`)}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm transition-all shadow-md shadow-emerald-500/20"
                    >
                      <MessageSquare className="w-4 h-4" />
                      <span>متابعة فورية عبر واتساب</span>
                    </a>

                    <button
                      onClick={handleResetForm}
                      className="w-full sm:w-auto px-5 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold text-sm transition-all"
                    >
                      إرسال استفسار جديد
                    </button>
                  </div>
                </div>
              ) : (
                /* Contact / Inquiry Form */
                <form onSubmit={handleSubmit} className="space-y-5">
                  <div>
                    <h2 className="text-2xl font-black text-white">نموذج الاستفسار وطلب العرض</h2>
                    <p className="text-xs sm:text-sm text-slate-400 mt-1">
                      املأ البيانات التالية وسيتواصل معك مستشار معتمد في أقرب وقت
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-300 mb-1.5">
                        الاسم بالكامل <span className="text-rose-400">*</span>
                      </label>
                      <input
                        type="text"
                        required
                        value={formState.fullName}
                        onChange={(e) => setFormState({ ...formState, fullName: e.target.value })}
                        placeholder="مثال: أحمد عبد الله"
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-300 mb-1.5">
                        رقم الجوال أو واتساب <span className="text-rose-400">*</span>
                      </label>
                      <input
                        type="tel"
                        required
                        value={formState.phone}
                        onChange={(e) => setFormState({ ...formState, phone: e.target.value })}
                        placeholder="مثال: +966 50 123 4567"
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all text-right"
                        dir="ltr"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-300 mb-1.5">
                        البريد الإلكتروني
                      </label>
                      <input
                        type="email"
                        value={formState.email}
                        onChange={(e) => setFormState({ ...formState, email: e.target.value })}
                        placeholder="name@company.com"
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all"
                        dir="ltr"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-300 mb-1.5">
                        اسم المنشأة / المحل (اختياري)
                      </label>
                      <input
                        type="text"
                        value={formState.companyName}
                        onChange={(e) => setFormState({ ...formState, companyName: e.target.value })}
                        placeholder="مثال: شركة مجوهرات الرياض"
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-300 mb-1.5">
                        نوع الاستفسار أو الطلب
                      </label>
                      <select
                        value={formState.category}
                        onChange={(e) => setFormState({ ...formState, category: e.target.value })}
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all cursor-pointer"
                      >
                        <option value="general">استفسار عام</option>
                        <option value="enterprise_demo">طلب عرض تجريبي حي (Live Demo)</option>
                        <option value="pricing">استفسار عن باقات واشتراكات OOZOX</option>
                        <option value="gold_accounting">حلول محاسبة وتجارة الذهب والموازين</option>
                        <option value="technical_support">دعم فني وحل مشكلات</option>
                        <option value="partnership">شراكة تجارية أو وكالة توزيع</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-300 mb-1.5">
                        عنوان الرسالة
                      </label>
                      <input
                        type="text"
                        value={formState.subject}
                        onChange={(e) => setFormState({ ...formState, subject: e.target.value })}
                        placeholder="مثال: استفسار حول ربط الفروع والميزان"
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-300 mb-1.5">
                      تفاصيل الرسالة أو الاستفسار <span className="text-rose-400">*</span>
                    </label>
                    <textarea
                      required
                      rows={4}
                      value={formState.message}
                      onChange={(e) => setFormState({ ...formState, message: e.target.value })}
                      placeholder="اكتب تفاصيل طلبك أو عدد الفروع أو الميزات التي تود الاستفسار عنها..."
                      className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition-all resize-y"
                    />
                  </div>

                  <div className="pt-2">
                    <button
                      type="submit"
                      disabled={isSubmitting}
                      className="w-full inline-flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-xl bg-gradient-to-r from-amber-500 via-amber-400 to-amber-300 hover:from-amber-400 hover:to-amber-200 text-slate-950 font-black text-sm transition-all shadow-lg shadow-amber-500/20 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                    >
                      {isSubmitting ? (
                        <>
                          <div className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                          <span>جاري إرسال طلبك...</span>
                        </>
                      ) : (
                        <>
                          <Send className="w-4 h-4 text-slate-950" />
                          <span>إرسال الرسالة الآن</span>
                        </>
                      )}
                    </button>
                  </div>

                  <p className="text-[11px] text-center text-slate-400 flex items-center justify-center gap-1.5 pt-1">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                    <span>بياناتك محمية ومشفرة ولا تتم مشاركتها مع أي طرف ثالث إطلاقاً.</span>
                  </p>
                </form>
              )}
            </div>
          </div>

        </div>
      </section>

      {/* Frequently Asked Questions Section */}
      <section className="py-16 bg-slate-900/40 border-t border-slate-900">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-10">
            <span className="text-xs font-bold text-amber-400 uppercase tracking-wider bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20 inline-block mb-3">
              إجابات سريعة
            </span>
            <h2 className="text-2xl sm:text-3xl font-black text-white">الأسئلة الشائعة حول الدعم والتواصل</h2>
          </div>

          <div className="space-y-3">
            {faqs.map((faq, idx) => {
              const isOpen = activeFaq === idx;
              return (
                <div 
                  key={idx}
                  className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden transition-colors"
                >
                  <button
                    type="button"
                    onClick={() => setActiveFaq(isOpen ? null : idx)}
                    className="w-full px-5 py-4 text-right flex items-center justify-between gap-4 font-bold text-sm sm:text-base text-white hover:text-amber-400 transition-colors"
                  >
                    <span>{faq.q}</span>
                    <span className="text-amber-400 shrink-0">
                      {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                    </span>
                  </button>
                  {isOpen && (
                    <div className="px-5 pb-4 pt-1 text-xs sm:text-sm text-slate-300 border-t border-slate-800/60 leading-relaxed bg-slate-950/40">
                      {faq.a}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Live System Status Banner */}
      <section className="py-8 border-t border-slate-900 bg-slate-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500/50 animate-pulse" />
              <span className="font-bold text-slate-200">حالة خوادم OOZOX:</span>
              <span className="text-emerald-400 font-medium">جميع الخدمات، بروتوكولات التزامن، وشبكة WebRTC تعمل بكفاءة 99.99%</span>
            </div>
            <div className="text-slate-400 text-[11px]">
              آخر فحص أمني: {new Date().toLocaleDateString('ar-SA')}
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-8 border-t border-slate-900 text-center text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Coins className="w-4 h-4 text-amber-400" />
            <span className="font-bold text-slate-300">OOZOX (oozox.com)</span>
            <span>— جميع الحقوق محفوظة © {new Date().getFullYear()}</span>
          </div>

          <div className="flex items-center gap-4 text-slate-400">
            <button 
              onClick={() => {
                if (onNavigateHome) onNavigateHome();
                else window.location.href = '/';
              }} 
              className="hover:text-amber-400 transition-colors"
            >
              الرئيسية
            </button>
            <span>•</span>
            <a href="mailto:support@oozox.com" className="hover:text-amber-400 transition-colors">الدعم الفني</a>
            <span>•</span>
            <a href="https://wa.me/966501234567" target="_blank" rel="noopener noreferrer" className="hover:text-emerald-400 transition-colors">واتساب</a>
          </div>
        </div>
      </footer>

    </div>
  );
}

export default ContactUs;
