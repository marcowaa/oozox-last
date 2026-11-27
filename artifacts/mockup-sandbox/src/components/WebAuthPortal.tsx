import React, { useEffect, useState } from 'react';
import {
  ShieldCheck,
  Smartphone,
  RefreshCw,
  AlertTriangle,
  X,
  Laptop,
  ArrowRight,
  Lock,
  CheckCircle2,
} from 'lucide-react';
import {
  WebOtpChannel,
  WebPortalScope,
  WebTrustedDeviceInfo,
  WebUserDto,
  apiCancelOtp,
  apiListTrustedDevices,
  apiRequestOtp,
  apiRevokeTrustedDevice,
  apiTrustedDeviceLogin,
  apiVerifyOtp,
  clearWebAuthSession,
  getOrCreateWebDeviceId,
  getStoredWebTrust,
  saveWebAuthSession,
} from '../lib/webAuthClient';

const CHANNELS: Array<{
  id: WebOtpChannel;
  label: string;
  desc: string;
  accentClass: string;
  activeBorderClass: string;
}> = [
  {
    id: 'TELEGRAM',
    label: 'تليجرام',
    desc: 'Telegram Gateway — تحقق رسمي فوري',
    accentClass: 'from-sky-500/20 to-sky-600/5 text-sky-300',
    activeBorderClass: 'border-sky-400 ring-2 ring-sky-400/25 shadow-lg shadow-sky-500/15',
  },
  {
    id: 'WHATSAPP',
    label: 'واتساب',
    desc: 'Twilio Verify WhatsApp',
    accentClass: 'from-emerald-500/20 to-emerald-600/5 text-emerald-300',
    activeBorderClass: 'border-emerald-400 ring-2 ring-emerald-400/25 shadow-lg shadow-emerald-500/15',
  },
  {
    id: 'SMS',
    label: 'SMS',
    desc: 'رسالة نصية مباشرة عبر الجوال',
    accentClass: 'from-amber-500/20 to-amber-600/5 text-amber-300',
    activeBorderClass: 'border-amber-400 ring-2 ring-amber-400/25 shadow-lg shadow-amber-500/15',
  },
];

export const WebChannelBrandIcon: React.FC<{ channel: WebOtpChannel; className?: string }> = ({
  channel,
  className = 'w-7 h-7',
}) => {
  if (channel === 'TELEGRAM') {
    return (
      <svg viewBox="0 0 96 96" className={className} aria-hidden="true">
        <defs>
          <linearGradient id="oozoxTgGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#37AEE2" />
            <stop offset="100%" stopColor="#1E96C8" />
          </linearGradient>
        </defs>
        <circle cx="48" cy="48" r="44" fill="url(#oozoxTgGrad)" />
        <path
          fill="#FFFFFF"
          d="M21.4 47.2C35.3 41.1 44.6 37.1 49.2 35.2C62.5 29.7 65.2 28.7 67 28.7C67.4 28.7 68.3 28.8 68.9 29.3C69.4 29.7 69.5 30.2 69.6 30.6C69.7 31 69.7 31.9 69.6 32.6C68.9 40 65.8 58.1 64.2 66.4C63.5 69.9 62.2 71.1 60.9 71.2C58.1 71.5 56 69.4 53.3 67.6C49.1 64.8 46.7 63.1 42.6 60.4C37.9 57.3 40.9 55.6 43.6 52.8C44.3 52.1 56.7 40.8 56.9 39.8C56.9 39.7 57 39.2 56.7 39C56.4 38.7 56 38.8 55.6 38.9C55.2 39 48.3 43.6 34.9 52.6C32.9 53.9 31.1 54.6 29.6 54.6C27.8 54.5 24.4 53.6 21.9 52.7C18.8 51.7 16.4 51.2 16.6 49.5C16.7 48.6 18 47.7 21.4 47.2Z"
        />
        <path fill="#C8DAEA" d="M39.9 66.5C38.5 66.5 38.7 66 38.2 64.7L34 51L56.8 38.9Z" />
        <path fill="#A9C9DD" d="M39.9 66.5C41 66.5 41.5 66 42.1 65.4L47.9 59.7L40.6 55.3Z" />
      </svg>
    );
  }
  if (channel === 'WHATSAPP') {
    return (
      <svg viewBox="0 0 96 96" className={className} aria-hidden="true">
        <defs>
          <linearGradient id="oozoxWaGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#25D366" />
            <stop offset="100%" stopColor="#128C7E" />
          </linearGradient>
        </defs>
        <circle cx="48" cy="48" r="44" fill="url(#oozoxWaGrad)" />
        <path
          fill="#FFFFFF"
          fillRule="evenodd"
          clipRule="evenodd"
          d="M48.5 20C33.3 20 21 32.2 21 47.3C21 52.5 22.5 57.4 25 61.6L21.4 74.8L35 71.2C39 73.4 43.6 74.7 48.5 74.7C63.7 74.7 76 62.4 76 47.3C76 32.2 63.7 20 48.5 20ZM48.5 24.8C61 24.8 71.1 34.9 71.1 47.3C71.1 59.7 61 69.8 48.5 69.8C44.1 69.8 39.9 68.6 36.4 66.4L35.5 65.9L27.5 68L29.6 60.2L29 59.3C26.7 55.7 25.8 51.6 25.8 47.3C25.8 34.9 36 24.8 48.5 24.8Z"
        />
        <path
          fill="#FFFFFF"
          d="M41.7 35.4C41.2 34.2 40.7 34.2 40.2 34.2C39.8 34.2 39.3 34.2 38.8 34.2C38.3 34.2 37.5 34.4 36.9 35.1C36.2 35.8 34.4 37.5 34.4 41C34.4 44.5 36.9 47.9 37.3 48.4C37.6 48.9 42.2 56.3 49.4 59.1C55.4 61.5 56.6 61 57.9 60.9C59.2 60.8 62.1 59.2 62.7 57.5C63.3 55.8 63.3 54.4 63.1 54.1C62.9 53.8 62.5 53.6 61.7 53.2C61 52.8 57.5 51.1 56.8 50.9C56.2 50.6 55.7 50.5 55.2 51.2C54.7 51.9 53.4 53.5 53 54C52.6 54.5 52.1 54.5 51.4 54.2C50.7 53.8 48.4 53.1 45.7 50.6C43.6 48.7 42.1 46.4 41.7 45.7C41.3 45 41.7 44.6 42 44.2C42.4 43.9 42.8 43.4 43.1 42.9C43.5 42.5 43.6 42.2 43.9 41.7C44.1 41.2 44 40.8 43.8 40.4C43.6 40.1 42.2 36.5 41.7 35.4Z"
        />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 96 96" className={className} aria-hidden="true">
      <defs>
        <linearGradient id="oozoxSmsGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#FBBF24" />
          <stop offset="100%" stopColor="#D97706" />
        </linearGradient>
      </defs>
      <circle cx="48" cy="48" r="44" fill="url(#oozoxSmsGrad)" />
      <path
        fill="#FFFFFF"
        d="M29 27H67C71.97 27 76 31.03 76 36V56.5C76 61.47 71.97 65.5 67 65.5H42.5L29.2 74.8C28.1 75.5 26.6 74.7 26.7 73.4L27.6 65.5C23.3 64.8 20 61 20 56.5V36C20 31.03 24.03 27 29 27Z"
      />
      <circle cx="35.5" cy="46.25" r="4" fill="#D97706" />
      <circle cx="48" cy="46.25" r="4" fill="#D97706" />
      <circle cx="60.5" cy="46.25" r="4" fill="#D97706" />
    </svg>
  );
};

export const WebChannelSelectorGrid: React.FC<{
  selected: WebOtpChannel;
  onSelect: (ch: WebOtpChannel) => void;
}> = ({ selected, onSelect }) => (
  <div className="grid grid-cols-3 gap-2.5">
    {CHANNELS.map((c) => {
      const active = selected === c.id;
      return (
        <button
          key={c.id}
          type="button"
          onClick={() => onSelect(c.id)}
          title={c.desc}
          className={`relative group flex flex-col items-center justify-center gap-2 py-3.5 px-2 rounded-2xl border transition-all duration-200 active:scale-[0.96] ${
            active
              ? `bg-gradient-to-b ${c.accentClass} ${c.activeBorderClass}`
              : 'bg-slate-900/90 border-slate-800 text-slate-300 hover:border-slate-600 hover:bg-slate-900'
          }`}
        >
          {active && (
            <CheckCircle2 className="w-4 h-4 absolute top-2 right-2 opacity-95" />
          )}
          <WebChannelBrandIcon
            channel={c.id}
            className={`w-10 h-10 transition-transform duration-200 ${active ? 'scale-105' : 'group-hover:scale-105'}`}
          />
          <span className={`text-xs font-extrabold tracking-wide ${active ? 'text-white' : 'text-slate-200'}`}>
            {c.label}
          </span>
        </button>
      );
    })}
  </div>
);

const COUNTRY_CODES = [
  { code: '+20', label: '🇪🇬 مصر (+20)' },
  { code: '+966', label: '🇸🇦 السعودية (+966)' },
  { code: '+971', label: '🇦🇪 الإمارات (+971)' },
  { code: '+965', label: '🇰🇼 الكويت (+965)' },
  { code: '+974', label: '🇶🇦 قطر (+974)' },
  { code: '+962', label: '🇯🇴 الأردن (+962)' },
  { code: '+1', label: '🇺🇸 أمريكا/كندا (+1)' },
];

export function formatPhoneToE164(countryCode: string, rawPhone: string): string {
  const cleaned = rawPhone.replace(/[^\d+]/g, '').trim();
  if (cleaned.startsWith('+')) return cleaned;
  const local = cleaned.replace(/^0+/, '');
  return `${countryCode}${local}`;
}

export const WebOtpVerificationCard: React.FC<{
  phoneDisplay: string;
  challengeId: string;
  initialChannel: WebOtpChannel;
  preAuthToken?: string;
  portalScope?: WebPortalScope;
  purpose?: 'LOGIN' | 'CHANGE_PHONE' | 'DEVICE_TRUST' | 'SENSITIVE_ACTION';
  fallbackNotice?: string | null;
  cooldownSeconds?: number;
  onBack: () => void;
  onVerified: (data: {
    user?: WebUserDto;
    token?: string;
    refreshToken?: string;
    trustedDevice?: unknown;
    isExistingUser?: boolean;
    hasOtherActiveDevice?: boolean;
  }) => void;
  onChallengeUpdated?: (newChallengeId: string, newChannel: WebOtpChannel) => void;
}> = ({
  phoneDisplay,
  challengeId: initialChallengeId,
  initialChannel,
  preAuthToken,
  portalScope = 'USER',
  purpose = 'LOGIN',
  fallbackNotice: initialNotice,
  cooldownSeconds: initialCooldown = 60,
  onBack,
  onVerified,
  onChallengeUpdated,
}) => {
  const [challengeId, setChallengeId] = useState(initialChallengeId);
  const [channel, setChannel] = useState<WebOtpChannel>(initialChannel);
  const [code, setCode] = useState('');
  const [trustDevice, setTrustDevice] = useState(true);
  const [cooldown, setCooldown] = useState(initialCooldown);
  const [notice, setNotice] = useState<string | null>(initialNotice || null);
  const [error, setError] = useState<string | null>(null);
  const [isBusy, setIsBusy] = useState(false);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const t = setTimeout(() => setCooldown((c) => Math.max(0, c - 1)), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    const clean = code.replace(/\D/g, '').slice(0, 6);
    if (clean.length !== 6) {
      setError('أدخل رمز التحقق المكوّن من 6 أرقام.');
      return;
    }
    setIsBusy(true);
    setError(null);
    const res = await apiVerifyOtp({
      challengeId,
      code: clean,
      phone: preAuthToken ? undefined : phoneDisplay,
      preAuthToken,
      trustDevice,
      portalScope,
    });
    setIsBusy(false);
    if (!res.ok || !res.data?.token) {
      if (res.retryAfterSeconds) setCooldown(res.retryAfterSeconds);
      setError(res.message || 'تعذر التحقق من الرمز.');
      return;
    }
    const sessionMode = portalScope === 'ADMIN' || res.data.user?.role === 'ADMIN' ? 'admin' : 'user';
    saveWebAuthSession(res.data, sessionMode);
    onVerified(res.data);
  };

  const [isCancelling, setIsCancelling] = useState(false);

  const handleBackAndCancel = async () => {
    if (isBusy || isCancelling) return;
    setIsCancelling(true);
    try {
      await apiCancelOtp({
        challengeId,
        phone: preAuthToken ? undefined : phoneDisplay,
        preAuthToken,
      });
    } finally {
      setIsCancelling(false);
      onBack();
    }
  };

  const handleResendSameChannel = async () => {
    if (isBusy || isCancelling || cooldown > 0) return;
    setIsBusy(true);
    setError(null);
    const res = await apiRequestOtp({
      purpose,
      channel,
      phone: preAuthToken ? undefined : phoneDisplay,
      preAuthToken,
      portalScope,
      allowFallback: true,
    });
    setIsBusy(false);
    if (!res.ok || !res.data?.challengeId) {
      if (res.retryAfterSeconds) setCooldown(res.retryAfterSeconds);
      setError(res.message || 'تعذر إعادة إرسال رمز التحقق.');
      return;
    }
    const nextChannel = (String(res.data.activeChannel || channel).toUpperCase() as WebOtpChannel) || channel;
    setChallengeId(res.data.challengeId);
    setChannel(nextChannel);
    setCode('');
    setCooldown(res.data.cooldownSeconds ?? 60);
    setNotice(
      res.data.attemptedChannels && res.data.attemptedChannels.length > 1
        ? `تم تحويل الإرسال تلقائياً إلى ${CHANNELS.find((c) => c.id === nextChannel)?.label || nextChannel} وإلغاء الرمز السابق.`
        : `تم إرسال رمز جديد عبر ${CHANNELS.find((c) => c.id === nextChannel)?.label || nextChannel}.`
    );
    onChallengeUpdated?.(res.data.challengeId, nextChannel);
  };

  return (
    <form onSubmit={handleVerify} className="space-y-4" dir="rtl">
      <div className="flex items-center justify-between">
        <button
          type="button"
          disabled={isBusy || isCancelling}
          onClick={handleBackAndCancel}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-400 hover:text-amber-300 disabled:opacity-50"
        >
          <ArrowRight className="w-4 h-4" />
          {isCancelling ? 'جاري إلغاء الرمز...' : 'رجوع وإلغاء الرمز'}
        </button>
        <span className="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-amber-500/15 border border-amber-500/30 text-amber-300">
          <WebChannelBrandIcon channel={channel} className="w-4 h-4" />
          عبر {CHANNELS.find((c) => c.id === channel)?.label || channel}
        </span>
      </div>

      <div className="text-center space-y-1">
        <p className="text-sm font-bold text-white">أدخل رمز التحقق الأمني (6 أرقام)</p>
        <p className="text-xs font-mono text-amber-400" dir="ltr">
          {phoneDisplay}
        </p>
      </div>

      {notice && (
        <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs font-medium">
          {notice}
        </div>
      )}

      {error && (
        <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-start gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      <input
        type="text"
        inputMode="numeric"
        maxLength={6}
        value={code}
        onChange={(e) => {
          setCode(e.target.value.replace(/\D/g, '').slice(0, 6));
          setError(null);
        }}
        placeholder="000000"
        className="w-full text-center tracking-[0.5em] text-2xl font-mono font-black bg-slate-950 border border-slate-700 focus:border-amber-500 rounded-2xl py-3.5 text-white outline-none"
      />

      <label className="flex items-center gap-2 text-xs text-slate-300 cursor-pointer select-none">
        <input
          type="checkbox"
          checked={trustDevice}
          onChange={(e) => setTrustDevice(e.target.checked)}
          className="rounded border-slate-700 bg-slate-950 text-amber-500"
        />
        <span>الوثوق بهذا المتصفح لمدة 30 يوماً</span>
      </label>

      <div className="p-3 rounded-xl bg-slate-950/90 border border-slate-800 flex items-center gap-2 text-[11px] text-slate-300">
        <Lock className="w-4 h-4 text-amber-400 shrink-0" />
        <span>لتغيير وسيلة الاستلام، اضغط "رجوع وإلغاء الرمز" لإبطال صلاحية الكود الحالي أولاً.</span>
      </div>

      {cooldown > 0 ? (
        <p className="text-center text-[11px] text-slate-400">
          إعادة الإرسال على نفس الوسيلة متاحة بعد {cooldown} ثانية
        </p>
      ) : (
        <button
          type="button"
          disabled={isBusy || isCancelling}
          onClick={handleResendSameChannel}
          className="w-full py-2 rounded-xl border border-amber-500/30 text-amber-300 hover:bg-amber-500/10 text-xs font-bold transition-all disabled:opacity-50"
        >
          إعادة إرسال الرمز عبر {CHANNELS.find((c) => c.id === channel)?.label || channel}
        </button>
      )}

      <button
        type="submit"
        disabled={isBusy || isCancelling || code.length !== 6}
        className="w-full py-3.5 rounded-xl bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-sm transition-all disabled:opacity-50"
      >
        {isBusy ? 'جاري التحقق من الخادم...' : 'تأكيد الرمز والدخول'}
      </button>
    </form>
  );
};

export const WebAuthModal: React.FC<{
  open: boolean;
  onClose: () => void;
  onAuthenticated: (user: WebUserDto) => void;
}> = ({ open, onClose, onAuthenticated }) => {
  const [channel, setChannel] = useState<WebOtpChannel>('TELEGRAM');
  const [countryCode, setCountryCode] = useState('+20');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isBusy, setIsBusy] = useState(false);

  const [otpStep, setOtpStep] = useState<{
    challengeId: string;
    activeChannel: WebOtpChannel;
    phoneDisplay: string;
    preAuthToken?: string;
    notice?: string | null;
    cooldown?: number;
  } | null>(null);

  const storedTrust = open ? getStoredWebTrust() : null;

  if (!open) return null;

  const handlePhoneOtpStart = async (e: React.FormEvent) => {
    e.preventDefault();
    const e164 = formatPhoneToE164(countryCode, phone);
    if (e164.length < 8) {
      setError('أدخل رقم هاتف صحيح لتسجيل الدخول.');
      return;
    }
    setIsBusy(true);
    setError(null);
    const res = await apiRequestOtp({
      purpose: 'LOGIN',
      channel,
      phone: e164,
      portalScope: 'USER',
      allowFallback: true,
    });
    setIsBusy(false);
    if (!res.ok || !res.data?.challengeId) {
      setError(res.message || 'تعذر إرسال رمز التحقق.');
      return;
    }
    const activeCh = (String(res.data.activeChannel || channel).toUpperCase() as WebOtpChannel) || channel;
    setOtpStep({
      challengeId: res.data.challengeId,
      activeChannel: activeCh,
      phoneDisplay: e164,
      cooldown: res.data.cooldownSeconds ?? 60,
      notice:
        res.data.attemptedChannels && res.data.attemptedChannels.length > 1
          ? `تم التحويل تلقائياً إلى ${activeCh} لضمان وصول الرمز.`
          : null,
    });
  };

  const handleTrustedLogin = async () => {
    setIsBusy(true);
    setError(null);
    const res = await apiTrustedDeviceLogin('USER');
    setIsBusy(false);
    if (res.ok && res.data?.token && res.data.user) {
      saveWebAuthSession(res.data, 'user');
      onAuthenticated(res.data.user);
      onClose();
      return;
    }
    setError(res.message || 'انتهت الثقة المحلية لهذا المتصفح — يلزم التحقق عبر OTP.');
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4" dir="rtl">
      <div className="w-full max-w-md bg-slate-900 border border-amber-500/30 rounded-3xl p-6 shadow-2xl space-y-5 text-slate-100">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-white">تسجيل الدخول برقم الهاتف — OOZOX</h3>
              <p className="text-[11px] text-slate-400">تحقق فوري (Telegram Gateway / WhatsApp / SMS)</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800">
            <X className="w-5 h-5" />
          </button>
        </div>

        {otpStep ? (
          <WebOtpVerificationCard
            phoneDisplay={otpStep.phoneDisplay}
            challengeId={otpStep.challengeId}
            initialChannel={otpStep.activeChannel}
            preAuthToken={otpStep.preAuthToken}
            portalScope="USER"
            purpose="LOGIN"
            fallbackNotice={otpStep.notice}
            cooldownSeconds={otpStep.cooldown}
            onBack={() => setOtpStep(null)}
            onVerified={(data) => {
              if (data.user) onAuthenticated(data.user);
              onClose();
            }}
          />
        ) : (
          <>
            {storedTrust && (
              <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold text-emerald-300">متصفح موثوق حتى {storedTrust.trustedUntil.slice(0, 10)}</p>
                  <p className="text-[11px] text-slate-400 font-mono" dir="ltr">{storedTrust.phone}</p>
                </div>
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={handleTrustedLogin}
                  className="px-3.5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shrink-0"
                >
                  دخول سريع
                </button>
              </div>
            )}

            {error && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label className="block text-[11px] font-bold text-slate-400 mb-2">
                قناة التحقق المفضلة (Fallback: Telegram ← WhatsApp ← SMS)
              </label>
              <WebChannelSelectorGrid selected={channel} onSelect={setChannel} />
            </div>

            <form onSubmit={handlePhoneOtpStart} className="space-y-3">
              <label className="block text-xs font-bold text-slate-300">رقم الهاتف المحمول</label>
              <div className="flex gap-2" dir="ltr">
                <select
                  value={countryCode}
                  onChange={(e) => setCountryCode(e.target.value)}
                  className="bg-slate-950 border border-slate-800 focus:border-amber-500 rounded-xl px-2.5 py-3 text-xs text-white outline-none"
                >
                  {COUNTRY_CODES.map((cc) => (
                    <option key={cc.code} value={cc.code}>
                      {cc.label}
                    </option>
                  ))}
                </select>
                <div className="relative flex-1">
                  <Smartphone className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="1012345678"
                    className="w-full bg-slate-950 border border-slate-800 focus:border-amber-500 rounded-xl pl-9 pr-3 py-3 text-sm text-white outline-none font-mono"
                  />
                </div>
              </div>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                يتم تسجيل الدخول أو فتح حسابك تلقائياً عبر رقم هاتفك ورمز OTP فقط دون الحاجة إلى كلمة مرور أو بريد إلكتروني.
              </p>
              <button
                type="submit"
                disabled={isBusy}
                className="w-full py-3 rounded-xl bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-sm disabled:opacity-50"
              >
                {isBusy ? 'جاري الإرسال...' : 'إرسال رمز التحقق (OTP)'}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
};

export const WebTrustedDevicesModal: React.FC<{
  open: boolean;
  token: string;
  onClose: () => void;
  onSessionInvalidated: () => void;
}> = ({ open, token, onClose, onSessionInvalidated }) => {
  const [devices, setDevices] = useState<WebTrustedDeviceInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const myDeviceId = getOrCreateWebDeviceId();

  const refreshDevices = async () => {
    setLoading(true);
    const res = await apiListTrustedDevices(token);
    setLoading(false);
    if (res.ok && res.data?.devices) {
      setDevices(res.data.devices);
    } else if (res.status === 401) {
      onSessionInvalidated();
    }
  };

  useEffect(() => {
    if (open && token) void refreshDevices();
  }, [open, token]);

  if (!open) return null;

  const handleRevoke = async (deviceId: string) => {
    setErrorMsg(null);
    const res = await apiRevokeTrustedDevice(token, deviceId);
    if (!res.ok) {
      setErrorMsg(res.message);
      return;
    }
    if (deviceId === myDeviceId) {
      clearWebAuthSession('user', true);
      onSessionInvalidated();
      onClose();
      return;
    }
    setStatusMsg('تم إلغاء وثوق الجهاز بنجاح.');
    void refreshDevices();
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4" dir="rtl">
      <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl space-y-5 text-slate-100 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <Laptop className="w-5 h-5 text-amber-400" />
            <h3 className="text-base font-black">الأجهزة والمتصفحات الموثوقة (30 يوماً)</h3>
          </div>
          <button onClick={onClose} className="p-2 rounded-xl text-slate-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        {statusMsg && <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs">{statusMsg}</div>}
        {errorMsg && <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">{errorMsg}</div>}

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-300">الأجهزة النشطة ({devices.length})</span>
            <button onClick={refreshDevices} className="text-xs text-amber-400 hover:text-amber-300 flex items-center gap-1">
              <RefreshCw className="w-3.5 h-3.5" />
              تحديث
            </button>
          </div>
          {loading ? (
            <p className="text-xs text-slate-400 py-4 text-center">جاري تحميل الأجهزة...</p>
          ) : devices.length === 0 ? (
            <p className="text-xs text-slate-500 py-4 text-center">لا توجد أجهزة موثوقة حالياً.</p>
          ) : (
            devices.map((d) => (
              <div key={d.deviceId} className="p-3 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold text-white">
                    {d.deviceName || d.deviceId}{' '}
                    {(d.isCurrent || d.deviceId === myDeviceId) && (
                      <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 mr-1">هذا المتصفح</span>
                    )}
                  </p>
                  <p className="text-[11px] text-slate-400 font-mono">
                    {d.platform} • صالح حتى {String(d.trustedUntil).slice(0, 10)}
                  </p>
                </div>
                <button
                  onClick={() => handleRevoke(d.deviceId)}
                  className="px-3 py-1.5 rounded-xl bg-rose-500/15 hover:bg-rose-500/25 border border-rose-500/30 text-rose-300 text-xs font-bold"
                >
                  إلغاء
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
