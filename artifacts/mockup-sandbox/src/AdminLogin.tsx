import React, { useState } from 'react';
import { Lock, Shield, User, Key, ArrowRight, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { AdminUser } from './types';

interface AdminLoginProps {
  onLoginSuccess: (admin: AdminUser) => void;
  onNavigateHome: () => void;
}

export const AdminLogin: React.FC<AdminLoginProps> = ({ onLoginSuccess, onNavigateHome }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const apiBase = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL || '';
      const response = await fetch(`${apiBase}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          emailOrPhone: username.trim(),
          password: password.trim()
        })
      });
      const payload = await response.json();
      const authData = payload?.data || payload;
      if (!response.ok || !authData?.token || authData?.user?.role !== 'ADMIN') {
        setIsLoading(false);
        setError(payload?.message || 'بيانات الاعتماد غير صحيحة أو الحساب لا يملك صلاحية الإدارة.');
        return;
      }

      sessionStorage.setItem('oozox_admin_token', authData.token);
      setIsLoading(false);
      onLoginSuccess({
        username: authData.user.name || authData.user.phone || username.trim(),
        role: 'SUPER_ADMIN',
        lastLogin: new Date().toLocaleString('ar-EG')
      });
    } catch {
      setIsLoading(false);
      setError('تعذر الاتصال بخادم المصادقة. تأكد من تشغيل الـ Backend API.');
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

          {/* Security Notice */}
          <div className="mt-8 pt-4 border-t border-slate-800/80 text-center">
            <p className="text-xs text-slate-400 leading-relaxed font-medium">
              اتصال مشفر بنظام SSL ومحمي ضد هجمات التخمين Brute-Force
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
