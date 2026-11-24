export type WebOtpChannel = 'TELEGRAM' | 'WHATSAPP' | 'SMS';

export interface WebUserDto {
  id: string;
  name: string;
  email: string;
  phone: string;
  phoneE164?: string;
  role: string;
  isVerified?: boolean;
  preferredLanguage?: string;
}

export interface WebDeviceTrustDto {
  deviceId: string;
  deviceTrustToken: string;
  trustedUntil: string;
  trustDays?: number;
}

export interface WebTrustedDeviceInfo {
  deviceId: string;
  deviceName: string;
  platform: string;
  trustedAt?: string;
  trustedUntil: string;
  lastActiveAt?: string;
  lastIp?: string;
  isActive: boolean;
  isCurrent: boolean;
}

export interface WebOtpChallengeDto {
  challengeId: string;
  activeChannel: WebOtpChannel | string;
  attemptedChannels?: string[];
  expiresAt?: string;
  cooldownSeconds?: number;
  maxAttempts?: number;
  maskedPhone?: string;
}

export interface WebAuthResponsePayload {
  user?: WebUserDto;
  token?: string;
  refreshToken?: string;
  sessionId?: string;
  requiresOtp?: boolean;
  requiresStepUpOtp?: boolean;
  code?: string;
  purpose?: string;
  preAuthToken?: string;
  maskedPhone?: string;
  otpChallenge?: WebOtpChallengeDto;
  otpDeliveryError?: { code?: string; message?: string; retryAfterSeconds?: number };
  trustedDevice?: WebDeviceTrustDto;
  deviceTrust?: WebDeviceTrustDto;
}

const KEY_WEB_DEVICE_ID = 'oozox_web_device_id';
const KEY_WEB_TRUST = 'oozox_web_device_trust';
const KEY_WEB_USER_TOKEN = 'oozox_user_token';
const KEY_WEB_USER_REFRESH = 'oozox_user_refresh_token';
const KEY_WEB_ADMIN_TOKEN = 'oozox_admin_token';

export function getApiBase(): string {
  const envBase = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL;
  return envBase ? envBase.replace(/\/+$/, '') : '';
}

export function getOrCreateWebDeviceId(): string {
  if (typeof window === 'undefined') return 'web_ssr';
  const existing = localStorage.getItem(KEY_WEB_DEVICE_ID);
  if (existing && existing.trim()) return existing;
  const generated =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? `web_${crypto.randomUUID().replace(/-/g, '')}`
      : `web_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  localStorage.setItem(KEY_WEB_DEVICE_ID, generated);
  return generated;
}

export function getWebDeviceName(): string {
  if (typeof navigator === 'undefined') return 'OOZOX Web Browser';
  const ua = navigator.userAgent || '';
  if (ua.includes('Edg/')) return 'Microsoft Edge Web';
  if (ua.includes('Chrome/')) return 'Google Chrome Web';
  if (ua.includes('Firefox/')) return 'Mozilla Firefox Web';
  if (ua.includes('Safari/')) return 'Apple Safari Web';
  return 'OOZOX Web Browser';
}

export async function getWebHardwareFingerprint(): Promise<string> {
  if (typeof window === 'undefined') return 'web_fp_ssr';
  const raw = [
    navigator.userAgent || '',
    navigator.language || '',
    navigator.platform || '',
    `${window.screen?.width || 0}x${window.screen?.height || 0}`,
  ].join('|');

  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const encoded = new TextEncoder().encode(raw);
    const digest = await crypto.subtle.digest('SHA-256', encoded);
    return Array.from(new Uint8Array(digest))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }
  return btoa(unescape(encodeURIComponent(raw))).slice(0, 48);
}

export interface StoredWebTrust {
  deviceId: string;
  deviceTrustToken: string;
  trustedUntil: string;
  phone: string;
  userId: string;
}

export function getStoredWebTrust(): StoredWebTrust | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(KEY_WEB_TRUST);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredWebTrust;
    if (!parsed?.deviceTrustToken || !parsed?.trustedUntil) return null;
    if (new Date(parsed.trustedUntil).getTime() <= Date.now()) {
      localStorage.removeItem(KEY_WEB_TRUST);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function saveStoredWebTrust(trust: WebDeviceTrustDto | undefined, user: WebUserDto | undefined) {
  if (typeof window === 'undefined' || !trust?.deviceTrustToken || !user) return;
  const payload: StoredWebTrust = {
    deviceId: trust.deviceId || getOrCreateWebDeviceId(),
    deviceTrustToken: trust.deviceTrustToken,
    trustedUntil: trust.trustedUntil,
    phone: user.phoneE164 || user.phone || '',
    userId: user.id || '',
  };
  localStorage.setItem(KEY_WEB_TRUST, JSON.stringify(payload));
}

export function clearStoredWebTrust() {
  if (typeof window !== 'undefined') {
    localStorage.removeItem(KEY_WEB_TRUST);
  }
}

export function saveWebAuthSession(
  payload: WebAuthResponsePayload,
  mode: 'user' | 'admin' = 'user'
) {
  if (typeof window === 'undefined') return;
  if (payload.token) {
    sessionStorage.setItem(mode === 'admin' ? KEY_WEB_ADMIN_TOKEN : KEY_WEB_USER_TOKEN, payload.token);
  }
  if (payload.refreshToken && mode === 'user') {
    sessionStorage.setItem(KEY_WEB_USER_REFRESH, payload.refreshToken);
  }
  const trust = payload.trustedDevice || payload.deviceTrust;
  if (trust && payload.user) {
    saveStoredWebTrust(trust, payload.user);
  }
}

export function getWebAccessToken(mode: 'user' | 'admin' = 'user'): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(mode === 'admin' ? KEY_WEB_ADMIN_TOKEN : KEY_WEB_USER_TOKEN);
}

export function clearWebAuthSession(mode: 'user' | 'admin' = 'user', revokeLocalTrust = false) {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(mode === 'admin' ? KEY_WEB_ADMIN_TOKEN : KEY_WEB_USER_TOKEN);
  if (mode === 'user') {
    sessionStorage.removeItem(KEY_WEB_USER_REFRESH);
  }
  if (revokeLocalTrust) {
    clearStoredWebTrust();
  }
}

async function webAuthHeaders(token?: string | null): Promise<Record<string, string>> {
  const fp = await getWebHardwareFingerprint();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Device-Id': getOrCreateWebDeviceId(),
    'X-Device-Name': getWebDeviceName(),
    'X-Platform': 'WEB',
    'X-Device-Fingerprint': fp,
    'X-App-Language': 'ar',
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export interface ApiResult<T> {
  ok: boolean;
  status: number;
  message: string;
  code?: string;
  retryAfterSeconds?: number;
  remainingAttempts?: number;
  requiresStepUpOtp?: boolean;
  maskedPhone?: string;
  data?: T;
}

async function callAuthApi<T>(
  path: string,
  method: string,
  body?: Record<string, unknown>,
  token?: string | null
): Promise<ApiResult<T>> {
  const headers = await webAuthHeaders(token);
  const res = await fetch(`${getApiBase()}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  let json: Record<string, unknown> = {};
  try {
    json = (await res.json()) as Record<string, unknown>;
  } catch {
    json = {};
  }
  return {
    ok: res.ok && json.success !== false,
    status: res.status,
    message: (json.message as string) || (res.ok ? 'OK' : `HTTP ${res.status}`),
    code: json.code as string | undefined,
    retryAfterSeconds: json.retryAfterSeconds as number | undefined,
    remainingAttempts: json.remainingAttempts as number | undefined,
    requiresStepUpOtp: json.requiresStepUpOtp as boolean | undefined,
    maskedPhone: json.maskedPhone as string | undefined,
    data: (json.data as T | undefined) ?? (json as unknown as T),
  };
}

export async function apiLoginPassword(emailOrPhone: string, password: string, preferredChannel: WebOtpChannel | 'AUTO' = 'AUTO') {
  const fp = await getWebHardwareFingerprint();
  const trust = getStoredWebTrust();
  return callAuthApi<WebAuthResponsePayload>('/api/auth/login', 'POST', {
    emailOrPhone: emailOrPhone.trim(),
    password,
    deviceId: getOrCreateWebDeviceId(),
    deviceTrustToken: trust?.deviceTrustToken,
    hardwareFingerprint: fp,
    preferredChannel,
    autoSendOtp: true,
  });
}

export async function apiRegisterUser(params: {
  name: string;
  email: string;
  phone: string;
  password: string;
  preferredChannel?: WebOtpChannel | 'AUTO';
}) {
  return callAuthApi<WebAuthResponsePayload>('/api/auth/register', 'POST', {
    ...params,
    preferredLanguage: 'ar',
    preferredChannel: params.preferredChannel || 'AUTO',
    autoSendOtp: true,
    deviceId: getOrCreateWebDeviceId(),
  });
}

export async function apiRequestOtp(params: {
  purpose: 'LOGIN' | 'REGISTER' | 'RECOVER' | 'CHANGE_PHONE' | 'DEVICE_TRUST' | 'SENSITIVE_ACTION';
  channel: WebOtpChannel | 'AUTO';
  phone?: string;
  preAuthToken?: string;
  allowFallback?: boolean;
}) {
  const fp = await getWebHardwareFingerprint();
  return callAuthApi<WebOtpChallengeDto>('/api/auth/otp/request', 'POST', {
    ...params,
    allowFallback: params.allowFallback ?? true,
    deviceId: getOrCreateWebDeviceId(),
    deviceName: getWebDeviceName(),
    platform: 'WEB',
    hardwareFingerprint: fp,
  });
}

export async function apiVerifyOtp(params: {
  challengeId: string;
  code: string;
  phone?: string;
  preAuthToken?: string;
  trustDevice?: boolean;
}) {
  const fp = await getWebHardwareFingerprint();
  return callAuthApi<WebAuthResponsePayload>('/api/auth/otp/verify', 'POST', {
    ...params,
    trustDevice: params.trustDevice ?? true,
    deviceId: getOrCreateWebDeviceId(),
    deviceName: getWebDeviceName(),
    platform: 'WEB',
    hardwareFingerprint: fp,
  });
}

export async function apiTrustedDeviceLogin() {
  const trust = getStoredWebTrust();
  if (!trust) {
    return {
      ok: false,
      status: 401,
      code: 'OTP_REQUIRED',
      message: 'لا توجد ثقة سارية لهذا المتصفح — يلزم التحقق عبر OTP.',
    } as ApiResult<WebAuthResponsePayload>;
  }
  const fp = await getWebHardwareFingerprint();
  return callAuthApi<WebAuthResponsePayload>('/api/auth/trusted-login', 'POST', {
    userId: trust.userId,
    emailOrPhone: trust.phone,
    deviceId: getOrCreateWebDeviceId(),
    deviceTrustToken: trust.deviceTrustToken,
    hardwareFingerprint: fp,
  });
}

export async function apiRecoverRequest(phone: string, channel: WebOtpChannel | 'AUTO' = 'AUTO') {
  const fp = await getWebHardwareFingerprint();
  return callAuthApi<WebOtpChallengeDto>('/api/auth/recover', 'POST', {
    phone: phone.trim(),
    channel,
    deviceId: getOrCreateWebDeviceId(),
    deviceName: getWebDeviceName(),
    platform: 'WEB',
    hardwareFingerprint: fp,
  });
}

export async function apiRecoverConfirm(challengeId: string, code: string, newPassword: string) {
  const fp = await getWebHardwareFingerprint();
  return callAuthApi<WebAuthResponsePayload>('/api/auth/recover', 'POST', {
    challengeId,
    code: code.trim(),
    newPassword,
    deviceId: getOrCreateWebDeviceId(),
    deviceName: getWebDeviceName(),
    platform: 'WEB',
    hardwareFingerprint: fp,
  });
}

export async function apiFetchMe(token: string) {
  return callAuthApi<{ user: WebUserDto }>('/api/auth/me', 'GET', undefined, token);
}

export async function apiListTrustedDevices(token: string) {
  return callAuthApi<{ devices: WebTrustedDeviceInfo[] }>('/api/auth/devices', 'GET', undefined, token);
}

export async function apiRevokeTrustedDevice(token: string, deviceId: string) {
  return callAuthApi<{ revokedDeviceId?: string }>(
    `/api/auth/devices/${encodeURIComponent(deviceId)}`,
    'DELETE',
    undefined,
    token
  );
}

export async function apiChangePassword(
  token: string,
  currentPassword: string,
  newPassword: string,
  stepUpChallengeId?: string,
  stepUpCode?: string
) {
  return callAuthApi<WebAuthResponsePayload>(
    '/api/auth/change-password',
    'POST',
    {
      currentPassword,
      newPassword,
      stepUpChallengeId,
      stepUpCode,
      deviceId: getOrCreateWebDeviceId(),
    },
    token
  );
}
