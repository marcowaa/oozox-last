import React, { useState } from 'react';
import { Shield, User, Key, AlertTriangle } from 'lucide-react';
import { AdminUser } from './types';
import {
  WebOtpChannel,
  apiLoginPassword,
  apiTrustedDeviceLogin,
  getStoredWebTrust,
  saveWebAuthSession,
} from './lib/webAuthClient';
import { WebOtpVerificationCard } from './components/WebAuthPortal';

interface AdminLoginProps {
  onLoginSuccess: (admin: AdminUser) => void;
  onNavigateHome: () => void;
}

export const AdminLogin: React.FC<AdminLoginProps> = ({ onLoginSuccess, onNavigateHome }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [otpChallenge, setOtpChallenge] = useState<{
    challengeId: string;
    activeChannel: WebOtpChannel;
    phoneDisplay: string;
    preAuthToken?: string;
    cooldownSeconds?: number;
    notice?: string | null;
  } | null>(null);

  const storedTrust = getStoredWebTrust();

  const finalizeAdmin = (user?: { name?: string; phone?: string; email?: string; role?: string }) => {
    if (!user || user.role !== 'ADMIN') {
      setError('هذا الحساب لا يملك صلاحية الإدارة المركزية (ADMIN).');
      return;
    }
    onLoginSuccess({
      username: user.name || user.phone || user.email || username.trim(),
      role: 'SUPER_ADMIN',
      lastLogin: new Date().toLocaleString('ar-EG'),
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const res = await apiLoginPassword(username.trim(), password.trim(), 'TELEGRAM');
      setIsLoading(false);
      if (!res.ok || !res.data) {
        setError(res.message || 'بيانات الاعتماد غير صحيحة أو الحساب لا يملك صلاحية الإدارة.');
        return;
      }
      if (res.data.requiresOtp) {
        const ch = res.data.otpChallenge;
        setOtpChallenge({
          challengeId: ch?.challengeId || '',
          activeChannel: (String(ch?.activeChannel || 'TELEGRAM').toUpperCase() as WebOtpChannel) || 'TELEGRAM',
          phoneDisplay: res.data.maskedPhone || username.trim(),
          preAuthToken: res.data.preAuthToken,
          cooldownSeconds: ch?.cooldownSeconds ?? 60,
          notice: res.data.otpDeliveryError?.message || null,
        });
        return;
      }
      if (!res.data.token || res.data.user?.role !== 'ADMIN') {
        setError(res.message || 'بيانات الاعتماد غير صحيحة أو الحساب لا يملك صلاحية الإدارة.');
        return;
      }
      saveWebAuthSession(res.data, 'admin');
      finalizeAdmin(res.data.user);
    } catch {
      setIsLoading(false);
      setError('تعذر الاتصال بخادم المصادقة. تأكد من تشغيل الـ Backend API.');
    }
  };

  const handleTrustedAdminLogin = async () => {
    setError(null);
    setIsLoading(true);
    try {
      const res = await apiTrustedDeviceLogin();
      setIsLoading(false);
      if (res.ok && res.data?.token && res.data.user?.role === 'ADMIN') {
        saveWebAuthSession(res.data, 'admin');
        finalizeAdmin(res.data.user);
        return;
      }
      setError(res.message || 'يلزم التحقق بكلمة المرور ورمز OTP على هذا المتصفح.');
    } catch {
      setIsLoading(false);
      setError('تعذر الاتصال بخادم المصادقة.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between font-sans selection:bg-amber-500 selection:text-black" dir="rtl">
      
      {/* Top Header */}
      <header className="border-b border-slate-900 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 to-amber-300 flex items-center justify-center font-black text-slate-950 text-sm shadow-md">
            OX
          </div>
          <div>
            <span className="text-base sm:text-lg font-black text-white tracking-tight">OOZOX ADMIN GATE</span>
            <span className="text-xs block text-slate-400 font-mono tracking-wide">https://oozox.com/admin/login</span>
          </div>
        </div>

        <button
          onClick={onNavigateHome}
          className="text-xs sm:text-sm font-bold text-slate-300 hover:text-white transition-colors bg-slate-900 px-4 py-2.5 rounded-xl border border-slate-800 hover:border-slate-700 tracking-wide"
        >
          العودة للموقع الرئيسي
        </button>
      </header>

      {/* Center Box */}
      <main className="flex-1 flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-slate-900/90 border border-slate-800 rounded-3xl p-8 sm:p-10 shadow-2xl backdrop-blur-sm relative overflow-hidden">
          
          {/* Top subtle glow */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-1 bg-gradient-to-r from-transparent via-amber-500 to-transparent" />

          <div className="text-center mb-8">
            <div className="w-16 h-16 rounded-2xl bg-slate-950 border border-amber-500/30 flex items-center justify-center mx-auto mb-4 text-amber-400 shadow-inner">
              <Shield className="w-8 h-8 text-amber-400" />
            </div>

            <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight leading-snug">تسجيل دخول مدير النظام</h1>
            <p className="text-xs sm:text-sm text-slate-400 mt-1.5 leading-relaxed">
              بوابة الإدارة المركزية لمنصة أوزوكس وموقع oozox.com
            </p>
            <div className="mt-2.5 inline-block bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs font-mono font-bold px-3 py-1 rounded-full">
              /admin/login
            </div>
          </div>

          {error && (
            <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-start gap-3 text-rose-400 text-xs sm:text-sm leading-relaxed">
              <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {otpChallenge ? (
            <WebOtpVerificationCard
              phoneDisplay={otpChallenge.phoneDisplay}
              challengeId={otpChallenge.challengeId}
              initialChannel={otpChallenge.activeChannel}
              preAuthToken={otpChallenge.preAuthToken}
              purpose="LOGIN"
              fallbackNotice={otpChallenge.notice}
              cooldownSeconds={otpChallenge.cooldownSeconds}
              onBack={() => setOtpChallenge(null)}
              onVerified={(data) => finalizeAdmin(data.user)}
            />
          ) : (
            <>
              {storedTrust && (
                <div className="mb-4 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-between gap-2">
                  <span className="text-xs text-emerald-300 font-bold">متصفح إداري موثوق (30 يوماً)</span>
                  <button
                    type="button"
                    disabled={isLoading}
                    onClick={handleTrustedAdminLogin}
                    className="px-3 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs"
                  >
                    دخول سريع
                  </button>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    البريد الإلكتروني أو رقم الهاتف (Admin Login)
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-500">
                      <User className="w-4 h-4" />
                    </div>
                    <input
                      type="text"
                      required
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="أدخل البريد الإلكتروني أو رقم الهاتف لحساب الإدارة"
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-10 pl-4 py-3 text-xs sm:text-sm text-white placeholder-slate-600 focus:outline-none focus:border-amber-500 transition-colors"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs sm:text-sm font-bold text-slate-300 mb-1.5 tracking-wide">
                    كلمة المرور المشفرة (Admin Password)
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-500">
                      <Key className="w-4 h-4" />
                    </div>
                    <input
                      type="password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="أدخل كلمة مرور الإدارة"
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-10 pl-4 py-3 text-xs sm:text-sm text-white placeholder-slate-600 focus:outline-none focus:border-amber-500 transition-colors"
                    />
                  </div>
                </div>

                {/* Note: In strict compliance with user request: NO forgot password button or recovery link */}
                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full bg-gradient-to-r from-amber-500 to-amber-400 hover:from-amber-400 hover:to-amber-300 text-slate-950 font-black py-3.5 px-6 rounded-xl text-sm sm:text-base shadow-lg shadow-amber-500/20 transition-all hover:scale-[1.01] text-center tracking-wide disabled:opacity-50"
                  >
                    {isLoading ? 'جاري التحقق والدخول...' : 'دخول لوحة التحكم'}
                  </button>
                </div>
              </form>
            </>
          )}

          {/* Security Notice */}
          <div className="mt-8 pt-4 border-t border-slate-800/80 text-center">
            <p className="text-xs text-slate-400 leading-relaxed font-medium">
              اتصال مشفر بنظام SSL/TLS ومحمي بالتحقق متعدد القنوات والأجهزة الموثوقة
            </p>
          </div>

        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 py-4 px-6 text-center text-xs text-slate-500 font-mono tracking-wider">
        OOZOX PLATFORM ADMINISTRATION CORE • SECURE GATEWAY
      </footer>

    </div>
  );
};
