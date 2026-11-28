import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Lock,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Trash2,
} from 'lucide-react';
import { WebChannelSelectorGrid } from './components/WebAuthPortal';
import {
  WebOtpChallengeDto,
  WebOtpChannel,
  WebUserDto,
  apiCancelOtp,
  apiDeleteMyAccount,
  apiFetchMe,
  apiRequestOtp,
  apiVerifyOtp,
  clearWebAuthSession,
  getWebAccessToken,
  saveWebAuthSession,
} from './lib/webAuthClient';

interface DeleteAccountPageProps {
  onNavigateHome: () => void;
}

type DeletionStage =
  | 'phone_input'
  | 'login_otp'
  | 'confirm_delete'
  | 'step_up_otp'
  | 'deleted';

export const DeleteAccountPage: React.FC<DeleteAccountPageProps> = ({ onNavigateHome }) => {
  const [stage, setStage] = useState<DeletionStage>('phone_input');
  const [phone, setPhone] = useState('');
  const [channel, setChannel] = useState<WebOtpChannel>('TELEGRAM');
  const [challenge, setChallenge] = useState<WebOtpChallengeDto | null>(null);
  const [otpCode, setOtpCode] = useState('');
  const [cooldown, setCooldown] = useState(0);

  const [sessionToken, setSessionToken] = useState<string | null>(null);
  const [verifiedUser, setVerifiedUser] = useState<WebUserDto | null>(null);

  const [ackChecked, setAckChecked] = useState(false);
  const [confirmWord, setConfirmWord] = useState('');
  const [stepUpChallenge, setStepUpChallenge] = useState<WebOtpChallengeDto | null>(null);
  const [stepUpCode, setStepUpCode] = useState('');

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.title = 'حذف الحساب نهائياً — OOZOX | Account Deletion';
    }
    const existingToken = getWebAccessToken('user');
    if (existingToken) {
      void apiFetchMe(existingToken).then((res) => {
        if (res.ok && res.data?.user) {
          setSessionToken(existingToken);
          setVerifiedUser(res.data.user);
        }
      });
    }
  }, []);

  useEffect(() => {
    if (cooldown <= 0) return;
    const id = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [cooldown]);

  const handleRequestLoginOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = phone.trim();
    if (!cleaned || cleaned.length < 8) {
      setError('أدخل رقم الهاتف بصيغته الدولية (مثل +2010... أو +9665...).');
      return;
    }

    setBusy(true);
    setError(null);
    setInfo(null);
    const res = await apiRequestOtp({
      purpose: 'LOGIN',
      channel,
      phone: cleaned,
      portalScope: 'USER',
      allowFallback: true,
    });
    setBusy(false);

    if (!res.ok || !res.data?.challengeId) {
      if (res.retryAfterSeconds) setCooldown(res.retryAfterSeconds);
      setError(res.message);
      return;
    }

    setChallenge(res.data);
    setOtpCode('');
    setCooldown(res.data.cooldownSeconds || 60);
    setStage('login_otp');
    setInfo(`تم إرسال رمز التحقق عبر ${res.data.activeChannel}.`);
  };

  const handleVerifyLoginOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!challenge?.challengeId) return;
    const cleanedCode = otpCode.replace(/\D/g, '');
    if (cleanedCode.length !== 6) {
      setError('أدخل رمز التحقق المكوّن من 6 أرقام.');
      return;
    }

    setBusy(true);
    setError(null);
    setInfo(null);
    const res = await apiVerifyOtp({
      challengeId: challenge.challengeId,
      code: cleanedCode,
      phone: phone.trim(),
      trustDevice: false,
      portalScope: 'USER',
    });
    setBusy(false);

    if (!res.ok || !res.data?.token || !res.data?.user) {
      setError(res.message);
      return;
    }

    saveWebAuthSession(res.data, 'user');
    setSessionToken(res.data.token);
    setVerifiedUser(res.data.user);
    setStage('confirm_delete');
  };

  const handleSwitchLoginChannel = async (newCh: WebOtpChannel) => {
    setChannel(newCh);
    if (!challenge?.challengeId || cooldown > 50) return;

    setBusy(true);
    setError(null);
    await apiCancelOtp({
      challengeId: challenge.challengeId,
      phone: phone.trim(),
    });
    const res = await apiRequestOtp({
      purpose: 'LOGIN',
      channel: newCh,
      phone: phone.trim(),
      portalScope: 'USER',
      allowFallback: true,
    });
    setBusy(false);

    if (!res.ok || !res.data?.challengeId) {
      if (res.retryAfterSeconds) setCooldown(res.retryAfterSeconds);
      setError(res.message);
      return;
    }

    setChallenge(res.data);
    setOtpCode('');
    setCooldown(res.data.cooldownSeconds || 60);
    setInfo(`تم إرسال رمز جديد عبر ${res.data.activeChannel}.`);
  };

  const requestStepUpOtp = async () => {
    const targetPhone = verifiedUser?.phoneE164 || verifiedUser?.phone || phone.trim();
    if (!targetPhone) {
      setError('رقم الهاتف غير متوفر للتحقق الإضافي. سجّل الدخول مجدداً.');
      setStage('phone_input');
      return;
    }
    setBusy(true);
    setError(null);
    setInfo(null);
    const res = await apiRequestOtp({
      purpose: 'SENSITIVE_ACTION',
      channel,
      phone: targetPhone,
      portalScope: 'USER',
      allowFallback: true,
    });
    setBusy(false);

    if (!res.ok || !res.data?.challengeId) {
      if (res.retryAfterSeconds) setCooldown(res.retryAfterSeconds);
      setError(res.message);
      return;
    }

    setStepUpChallenge(res.data);
    setStepUpCode('');
    setCooldown(res.data.cooldownSeconds || 60);
    setStage('step_up_otp');
    setInfo(`تم إرسال رمز تأكيد الحذف النهائي عبر ${res.data.activeChannel}.`);
  };

  const executeAccountDeletion = async (stepUpPayload?: { stepUpChallengeId: string; stepUpCode: string }) => {
    if (!sessionToken) {
      setError('انتهت جلسة التحقق. أعد تأكيد رقم الهاتف أولاً.');
      setStage('phone_input');
      return;
    }

    setBusy(true);
    setError(null);
    setInfo(null);

    const res = await apiDeleteMyAccount(sessionToken, stepUpPayload);
    setBusy(false);

    if (res.ok) {
      clearWebAuthSession('user', true);
      setSessionToken(null);
      setVerifiedUser(null);
      setStage('deleted');
      return;
    }

    if (res.status === 409 || res.code === 'LAST_ADMIN_CANNOT_DELETE') {
      setError(
        'تعذر حذف الحساب: لا يمكن حذف آخر حساب مدير نظام (ADMIN) نشط في المنصة. قم بترقية مدير آخر أولاً قبل حذف هذا الحساب.'
      );
      return;
    }

    if (res.status === 403 && res.requiresStepUpOtp) {
      setInfo('انتهت نافذة التحقق الفوري — يرجى تأكيد رمز OTP إضافي لإتمام الحذف النهائي.');
      await requestStepUpOtp();
      return;
    }

    setError(res.message);
  };

  const handleConfirmDeleteSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedWord = confirmWord.trim().toUpperCase();
    if (!ackChecked || (normalizedWord !== 'DELETE' && confirmWord.trim() !== 'حذف')) {
      setError('يرجى الموافقة على الإقرار وكتابة كلمة DELETE أو حذف للمتابعة.');
      return;
    }
    await executeAccountDeletion();
  };

  const handleStepUpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stepUpChallenge?.challengeId) return;
    const cleaned = stepUpCode.replace(/\D/g, '');
    if (cleaned.length !== 6) {
      setError('أدخل رمز التحقق المكوّن من 6 أرقام.');
      return;
    }
    await executeAccountDeletion({
      stepUpChallengeId: stepUpChallenge.challengeId,
      stepUpCode: cleaned,
    });
  };

  return (
    <div
      className="min-h-screen bg-slate-950 text-slate-100 font-sans antialiased flex flex-col"
      dir="rtl"
    >
      <header className="border-b border-slate-800/80 bg-slate-950/90 backdrop-blur-md">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <button
            type="button"
            onClick={onNavigateHome}
            className="inline-flex items-center gap-2 text-sm font-bold text-slate-300 hover:text-amber-400 transition-colors cursor-pointer"
          >
            <ArrowRight className="w-4 h-4" />
            <span>العودة إلى الرئيسية (OOZOX)</span>
          </button>
          <span className="text-xs font-mono text-slate-400">Account Deletion Portal</span>
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center p-4 sm:p-8">
        <div className="w-full max-w-2xl rounded-3xl border border-slate-800 bg-slate-900/75 shadow-2xl overflow-hidden">
          <div className="px-6 py-6 border-b border-slate-800 bg-gradient-to-l from-red-950/40 via-slate-900 to-slate-900 flex items-start gap-4">
            <div className="w-12 h-12 rounded-2xl bg-red-500/15 border border-red-500/30 flex items-center justify-center shrink-0">
              <ShieldAlert className="w-6 h-6 text-red-400" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-black text-white">
                طلب حذف الحساب والبيانات نهائياً (OOZOX)
              </h1>
              <p className="text-xs sm:text-sm text-slate-400 mt-1">
                بوابة الحذف الذاتي المعتمدة لسياسة Google Play Data Safety — محمية بالتحقق الثنائي (OTP Step-Up).
              </p>
            </div>
          </div>

          <div className="p-6 space-y-6">
            <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-4 space-y-2 text-xs sm:text-sm text-slate-300">
              <div className="font-bold text-amber-400 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4" />
                <span>ما الذي يحدث عند حذف حسابك في OOZOX؟</span>
              </div>
              <ul className="list-disc pr-5 space-y-1 text-slate-300 leading-relaxed">
                <li>
                  <strong>يُحذف نهائياً فوراً:</strong> النسخ الاحتياطية السحابية المشفرة، جميع الأجهزة الموثوقة (Trusted Devices)، الجلسات النشطة، رموز الإشعارات (FCM)، وسجلات OTP.
                </li>
                <li>
                  <strong>تُزال الهوية الشخصية (Anonymized):</strong> يُمحى اسمك ورقم هاتفك وبريدك الإلكتروني وصورة الملف الشخصي مع الإبقاء على سلامة الفواتير والمحادثات المشتركة لدى الأطراف المقابلة دون أي بيانات تعريفية لك.
                </li>
                <li>
                  <strong>تنبيه مدير النظام:</strong> لا يسمح النظام بحذف آخر حساب مدير (`ADMIN`) نشط لحماية استمرارية المنشأة.
                </li>
              </ul>
            </div>

            {error && (
              <div className="rounded-2xl border border-red-500/40 bg-red-500/10 p-4 flex items-start gap-3 text-sm text-red-200">
                <AlertTriangle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            {info && (
              <div className="rounded-2xl border border-sky-500/40 bg-sky-500/10 p-4 text-sm text-sky-200">
                {info}
              </div>
            )}

            {stage === 'phone_input' && (
              <div className="space-y-5">
                {verifiedUser && sessionToken && (
                  <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <div className="text-xs sm:text-sm">
                      <p className="font-bold text-amber-300">توجد جلسة مسجّلة حالياً على هذا المتصفح</p>
                      <p className="text-slate-300 font-mono mt-0.5">
                        {verifiedUser.name} ({verifiedUser.phoneE164 || verifiedUser.phone})
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setStage('confirm_delete')}
                      className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-black transition-colors cursor-pointer"
                    >
                      المتابعة بهذا الحساب
                    </button>
                  </div>
                )}

                <form onSubmit={handleRequestLoginOtp} className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-300 mb-2">
                      1. أدخل رقم الهاتف المسجّل في التطبيق (بالصيغة الدولية)
                    </label>
                    <input
                      type="tel"
                      dir="ltr"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      placeholder="+201000000000"
                      required
                      className="w-full bg-slate-950 border border-slate-700 focus:border-amber-400 rounded-xl px-4 py-3 text-sm font-mono text-white placeholder-slate-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-300 mb-2">
                      2. اختر قناة استلام رمز التحقق (OTP)
                    </label>
                    <WebChannelSelectorGrid selected={channel} onSelect={setChannel} />
                  </div>

                  <button
                    type="submit"
                    disabled={busy}
                    className="w-full py-3.5 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:opacity-50 text-slate-950 font-black text-sm transition-colors cursor-pointer"
                  >
                    {busy ? 'جاري إرسال رمز التحقق...' : 'إرسال رمز التحقق وتأكيد الملكية'}
                  </button>
                </form>
              </div>
            )}

            {stage === 'login_otp' && (
              <form onSubmit={handleVerifyLoginOtp} className="space-y-4">
                <div className="text-xs sm:text-sm text-slate-300">
                  تم إرسال رمز مكوّن من 6 أرقام إلى{' '}
                  <span className="font-mono font-bold text-amber-400" dir="ltr">
                    {challenge?.maskedPhone || phone}
                  </span>{' '}
                  عبر <span className="font-bold">{challenge?.activeChannel || channel}</span>.
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-2">رمز التحقق (OTP)</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    dir="ltr"
                    value={otpCode}
                    onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                    placeholder="000000"
                    required
                    className="w-full bg-slate-950 border border-slate-700 focus:border-amber-400 rounded-xl px-4 py-3.5 text-center text-xl tracking-[0.4em] font-mono text-white focus:outline-none"
                  />
                </div>

                <div className="space-y-2">
                  <div className="text-[11px] text-slate-400">تبديل قناة الإرسال أو إعادة المحاولة:</div>
                  <WebChannelSelectorGrid selected={channel} onSelect={handleSwitchLoginChannel} />
                  {cooldown > 0 && (
                    <p className="text-[11px] text-slate-400 font-mono">
                      الانتظار المتبقي قبل الإرسال التالي: {cooldown}s
                    </p>
                  )}
                </div>

                <div className="flex flex-col sm:flex-row gap-3">
                  <button
                    type="submit"
                    disabled={busy}
                    className="flex-1 py-3 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:opacity-50 text-slate-950 font-black text-sm cursor-pointer"
                  >
                    {busy ? 'جاري التحقق...' : 'تأكيد الرمز والمتابعة'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setStage('phone_input')}
                    className="px-4 py-3 rounded-xl border border-slate-700 text-slate-300 hover:text-white text-xs font-bold cursor-pointer"
                  >
                    تغيير الرقم
                  </button>
                </div>
              </form>
            )}

            {stage === 'confirm_delete' && verifiedUser && (
              <form onSubmit={handleConfirmDeleteSubmit} className="space-y-5">
                <div className="rounded-2xl border border-slate-700 bg-slate-950 p-4 flex items-center justify-between">
                  <div>
                    <div className="text-xs text-slate-400">الحساب المحدد للحذف</div>
                    <div className="text-base font-black text-white mt-0.5">{verifiedUser.name}</div>
                    <div className="text-xs font-mono text-amber-400 mt-0.5" dir="ltr">
                      {verifiedUser.phoneE164 || verifiedUser.phone} • {verifiedUser.role}
                    </div>
                  </div>
                  <Lock className="w-6 h-6 text-red-400" />
                </div>

                <label className="flex items-start gap-3 p-3 rounded-xl border border-red-500/25 bg-red-950/20 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={ackChecked}
                    onChange={(e) => setAckChecked(e.target.checked)}
                    className="mt-1 w-4 h-4 accent-red-500"
                  />
                  <span className="text-xs sm:text-sm text-slate-200 leading-relaxed">
                    أدرك أن هذا الإجراء نهائي ولا يمكن التراجع عنه، وسيؤدي إلى حذف نسخ السحابة والأجهزة الموثوقة وإنهاء الوصول للحساب فوراً.
                  </span>
                </label>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-2">
                    اكتب كلمة <code className="text-red-400 font-mono">DELETE</code> أو <code className="text-red-400">حذف</code> للتأكيد:
                  </label>
                  <input
                    type="text"
                    value={confirmWord}
                    onChange={(e) => setConfirmWord(e.target.value)}
                    placeholder="DELETE"
                    required
                    className="w-full bg-slate-950 border border-red-500/40 focus:border-red-400 rounded-xl px-4 py-3 text-sm font-mono text-white focus:outline-none"
                  />
                </div>

                <div className="flex flex-col sm:flex-row gap-3">
                  <button
                    type="submit"
                    disabled={busy}
                    className="flex-1 inline-flex items-center justify-center gap-2 py-3.5 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-black text-sm transition-colors cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" />
                    <span>{busy ? 'جاري حذف الحساب...' : 'حذف الحساب نهائياً الآن'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={onNavigateHome}
                    className="px-5 py-3.5 rounded-xl border border-slate-700 text-slate-300 hover:text-white text-xs font-bold cursor-pointer"
                  >
                    إلغاء والعودة
                  </button>
                </div>
              </form>
            )}

            {stage === 'step_up_otp' && (
              <form onSubmit={handleStepUpSubmit} className="space-y-4">
                <div className="text-xs sm:text-sm text-slate-300">
                  أدخل رمز التأكيد الأمني (`SENSITIVE_ACTION`) المرسل إلى{' '}
                  <span className="font-mono font-bold text-amber-400" dir="ltr">
                    {stepUpChallenge?.maskedPhone || verifiedUser?.phone}
                  </span>{' '}
                  لإتمام الحذف النهائي:
                </div>

                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  dir="ltr"
                  value={stepUpCode}
                  onChange={(e) => setStepUpCode(e.target.value.replace(/\D/g, ''))}
                  placeholder="000000"
                  required
                  className="w-full bg-slate-950 border border-red-500/40 focus:border-red-400 rounded-xl px-4 py-3.5 text-center text-xl tracking-[0.4em] font-mono text-white focus:outline-none"
                />

                <div className="flex flex-col sm:flex-row gap-3">
                  <button
                    type="submit"
                    disabled={busy}
                    className="flex-1 py-3 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-black text-sm cursor-pointer"
                  >
                    {busy ? 'جاري الحذف...' : 'تأكيد الحذف النهائي'}
                  </button>
                  <button
                    type="button"
                    disabled={busy || cooldown > 0}
                    onClick={() => void requestStepUpOtp()}
                    className="inline-flex items-center justify-center gap-1.5 px-4 py-3 rounded-xl border border-slate-700 text-slate-300 disabled:opacity-50 text-xs font-bold cursor-pointer"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    <span>{cooldown > 0 ? `إعادة الإرسال (${cooldown}s)` : 'إعادة إرسال الرمز'}</span>
                  </button>
                </div>
              </form>
            )}

            {stage === 'deleted' && (
              <div className="rounded-2xl border border-emerald-500/40 bg-emerald-500/10 p-6 text-center space-y-4">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
                <h2 className="text-xl font-black text-white">تم حذف حسابك وإزالة بياناتك الشخصية بنجاح</h2>
                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                  تم إبطال جميع الجلسات والأجهزة الموثوقة والنسخ الاحتياطية السحابية المرتبطة بحسابك وفق سياسة الخصوصية.
                </p>
                <button
                  type="button"
                  onClick={onNavigateHome}
                  className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-sm cursor-pointer"
                >
                  العودة إلى الصفحة الرئيسية
                </button>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};
