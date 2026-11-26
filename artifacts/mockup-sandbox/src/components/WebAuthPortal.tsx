import React, { useEffect, useState } from 'react';
import {
  ShieldCheck,
  Smartphone,
  RefreshCw,
  AlertTriangle,
  X,
  Laptop,
  ArrowRight,
} from 'lucide-react';
import {
  WebOtpChannel,
  WebPortalScope,
  WebTrustedDeviceInfo,
  WebUserDto,
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

const CHANNELS: Array<{ id: WebOtpChannel; label: string; desc: string }> = [
  { id: 'TELEGRAM', label: 'تليجرام', desc: 'Telegram Gateway — تحقق رسمي فوري' },
  { id: 'WHATSAPP', label: 'واتساب', desc: 'Twilio Verify WhatsApp' },
  { id: 'SMS', label: 'SMS', desc: 'رسالة نصية مباشرة عبر الجوال' },
];

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

  const handleSwitchOrResend = async (targetChannel: WebOtpChannel) => {
    if (isBusy) return;
    if (targetChannel === channel && cooldown > 0) return;
    setIsBusy(true);
    setError(null);
    const res = await apiRequestOtp({
      purpose,
      channel: targetChannel,
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
    const nextChannel = (String(res.data.activeChannel || targetChannel).toUpperCase() as WebOtpChannel) || targetChannel;
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
          onClick={onBack}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-400 hover:text-amber-300"
        >
          <ArrowRight className="w-4 h-4" />
          تغيير رقم الهاتف
        </button>
        <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-amber-500/15 border border-amber-500/30 text-amber-300">
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

      <div className="grid grid-cols-3 gap-2">
        {CHANNELS.map((ch) => {
          const active = ch.id === channel;
          const disabled = isBusy || (active && cooldown > 0);
          return (
            <button
              key={ch.id}
              type="button"
              disabled={disabled}
              onClick={() => handleSwitchOrResend(ch.id)}
              className={`py-2 px-2 rounded-xl border text-xs font-bold transition-all ${
                active
                  ? 'bg-amber-500/20 border-amber-500 text-amber-300'
                  : 'bg-slate-950 border-slate-800 text-slate-300 hover:border-slate-600'
              } disabled:opacity-45`}
              title={ch.desc}
            >
              {ch.label}
            </button>
          );
        })}
      </div>

      {cooldown > 0 && (
        <p className="text-center text-[11px] text-slate-400">
          إعادة الإرسال على نفس القناة متاحة بعد {cooldown} ثانية (أو بدّل القناة الآن)
        </p>
      )}

      <button
        type="submit"
        disabled={isBusy || code.length !== 6}
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
              <label className="block text-[11px] font-bold text-slate-400 mb-1.5">
                قناة التحقق المفضلة (Fallback: Telegram ← WhatsApp ← SMS)
              </label>
              <div className="grid grid-cols-3 gap-2">
                {CHANNELS.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setChannel(c.id)}
                    className={`py-2 rounded-xl border text-xs font-bold transition-all ${
                      channel === c.id
                        ? 'bg-amber-500/20 border-amber-500 text-amber-300'
                        : 'bg-slate-950 border-slate-800 text-slate-400'
                    }`}
                  >
                    {c.label}
                  </button>
                ))}
              </div>
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
