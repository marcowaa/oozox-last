export type WebOtpChannel = 'TELEGRAM' | 'WHATSAPP' | 'SMS';
export type WebPortalScope = 'USER' | 'ADMIN';

export interface WebUserDto {
  id: string;
  name: string;
  email?: string;
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
  isExistingUser?: boolean;
  preAuthToken?: string;
}

export interface WebAuthResponsePayload {
  user?: WebUserDto;
  token?: string;
  refreshToken?: string;
  sessionId?: string;
  requiresOtp?: boolean;
  requiresStepUpOtp?: boolean;
  isExistingUser?: boolean;
  hasOtherActiveDevice?: boolean;
  code?: string;
  purpose?: string;
  preAuthToken?: string;
  maskedPhone?: string;
  otpChallenge?: WebOtpChallengeDto;
  otpDeliveryError?: { code?: string; message?: string; retryAfterSeconds?: number };
  trustedDevice?: WebDeviceTrustDto;
  deviceTrust?: WebDeviceTrustDto;
}

export interface WebCallAuthorizeData {
  allowed: boolean;
  callId: string;
  callType: 'AUDIO' | 'VIDEO';
  exp: number;
  callAuthSignature: string;
  usagePct: number;
}

export interface WebCallBudgetTelemetry {
  monthKey: string;
  monthlyBudgetUsd: number;
  estimatedCostUsd: number;
  usagePct: number;
  thresholds: { alert1Pct: number; alert2Pct: number; hardStopPct: number };
  alertsTriggered: number[];
  newCallsBlocked: boolean;
  blockedNewCallsAt?: string | null;
  totalCallsCount: number;
  answeredCallsCount: number;
  totalBilledMinutes: number;
  turnBytesTotal: number;
  coturnRestartCount: number;
  lastScrapedAt?: string | null;
  recentAlerts?: Array<{ _id?: string; title: string; message: string; createdAt: string }>;
}

export interface WebCallHistoryItem {
  _id: string;
  callId: string;
  callType: 'AUDIO' | 'VIDEO';
  status: string;
  billedDurationSeconds: number;
  billedMinutes: number;
  estimatedCostUsd: number;
  turnUsed: boolean;
  createdAt: string;
  callerId?: { _id?: string; name?: string; phone?: string };
  calleeId?: { _id?: string; name?: string; phone?: string };
}

export interface WebConversationItem {
  _id: string;
  conversationId: string;
  participantDetails?: Array<{ id: string; name?: string; phone?: string }>;
  lastMessagePreview?: string;
  lastMessageAt?: string;
  unreadCount?: number;
}

export interface WebChatMessageItem {
  _id: string;
  conversationId: string;
  senderId: string;
  content: string;
  messageType: string;
  createdAt: string;
}

export interface WebIceServerConfig {
  urls: string[];
  username?: string;
  credential?: string;
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

export function getSignalingHttpBase(): string {
  const envSig = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_SIGNALING_HTTP_URL;
  return (envSig || 'https://signaling.oozox.com').replace(/\/+$/, '');
}

export function getSignalingWsUrl(token: string): string {
  const envWs = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_SIGNALING_WS_URL;
  const base = (envWs || 'wss://signaling.oozox.com/ws').replace(/\/+$/, '');
  const sep = base.includes('?') ? '&' : '?';
  return `${base}${sep}token=${encodeURIComponent(token)}&deviceId=${encodeURIComponent(getOrCreateWebDeviceId())}`;
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

function toFriendlyWebError(
  status: number,
  code?: string,
  rawMessage?: string,
  retryAfterSeconds?: number,
  remainingAttempts?: number
): string {
  const c = (code || '').toUpperCase();
  if (c === 'OTP_COOLDOWN') {
    return `انتظر ${Math.max(1, retryAfterSeconds || 60)} ثانية قبل طلب رمز جديد`;
  }
  if (c === 'OTP_LOCKED') {
    return `محاولات كثيرة، انتظر ${Math.max(1, retryAfterSeconds || 60)} ثانية`;
  }
  if (c === 'OTP_DAILY_LIMIT') return 'وصلت للحد اليومي للرموز، حاول غداً';
  if (c === 'OTP_DELIVERY_UNAVAILABLE' || c === 'OTP_BUDGET_EXCEEDED' || c === 'OTP_ADAPTER_UNAVAILABLE') {
    return 'تعذر الإرسال عبر هذه الوسيلة، جرب وسيلة أخرى';
  }
  if (c === 'OTP_CANCELLED') return 'تم إلغاء الرمز، اطلب رمزاً جديداً';
  if (c === 'OTP_EXPIRED' || c === 'OTP_CHALLENGE_NOT_FOUND' || c === 'OTP_ALREADY_USED') {
    return 'انتهت صلاحية الرمز، اطلب رمزاً جديداً';
  }
  if (c === 'OTP_INVALID_CODE' || c === 'INVALID_OTP_FORMAT') {
    return typeof remainingAttempts === 'number'
      ? `الرمز غير صحيح (متبقي ${remainingAttempts} محاولات)`
      : 'رمز التحقق غير صحيح';
  }
  if (c === 'INVALID_PHONE') return 'تأكد من رقم الهاتف وأعد المحاولة';
  if (c === 'COUNTRY_NOT_ALLOWED') return 'التحقق غير متاح لهذا البلد حالياً';

  const trimmed = (rawMessage || '').trim();
  const isTechnical =
    !trimmed ||
    /failed to fetch|networkerror|enotfound|econnrefused|etimedout|socket|syntaxerror|http \d{3}/i.test(trimmed);
  if (!isTechnical) return trimmed;

  if (status === 400) return 'تحقق من البيانات المدخلة';
  if (status === 401) return 'انتهت الجلسة، سجل الدخول مجدداً';
  if (status === 403) return 'غير مصرح لك بهذا الإجراء';
  if (status === 404) return 'البيانات المطلوبة غير موجودة';
  if (status === 429) return 'محاولات كثيرة، انتظر قليلاً ثم حاول';
  if (status >= 500) return 'الخدمة غير متاحة مؤقتاً، حاول لاحقاً';
  return 'تعذر الاتصال بالخادم، تحقق من الإنترنت';
}

async function callApi<T>(
  url: string,
  method: string,
  body?: Record<string, unknown>,
  token?: string | null
): Promise<ApiResult<T>> {
  try {
    const headers = await webAuthHeaders(token);
    const res = await fetch(url, {
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
    const ok = res.ok && json.success !== false;
    const code = json.code as string | undefined;
    const retryAfterSeconds = json.retryAfterSeconds as number | undefined;
    const remainingAttempts = json.remainingAttempts as number | undefined;
    return {
      ok,
      status: res.status,
      message: ok
        ? (json.message as string) || 'OK'
        : toFriendlyWebError(
            res.status,
            code,
            json.message as string | undefined,
            retryAfterSeconds,
            remainingAttempts
          ),
      code,
      retryAfterSeconds,
      remainingAttempts,
      requiresStepUpOtp: json.requiresStepUpOtp as boolean | undefined,
      maskedPhone: json.maskedPhone as string | undefined,
      data: (json.data as T | undefined) ?? (json as unknown as T),
    };
  } catch {
    return {
      ok: false,
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'تعذر الاتصال بالخادم، تحقق من الإنترنت',
    };
  }
}

export async function apiRequestOtp(params: {
  purpose?: 'LOGIN' | 'CHANGE_PHONE' | 'DEVICE_TRUST' | 'SENSITIVE_ACTION';
  channel: WebOtpChannel | 'AUTO';
  phone?: string;
  preAuthToken?: string;
  allowFallback?: boolean;
  portalScope?: WebPortalScope;
}) {
  const fp = await getWebHardwareFingerprint();
  return callApi<WebOtpChallengeDto>(`${getApiBase()}/api/auth/otp/request`, 'POST', {
    purpose: params.purpose || 'LOGIN',
    channel: params.channel,
    phone: params.phone?.trim(),
    preAuthToken: params.preAuthToken,
    portalScope: params.portalScope || 'USER',
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
  portalScope?: WebPortalScope;
}) {
  const fp = await getWebHardwareFingerprint();
  return callApi<WebAuthResponsePayload>(`${getApiBase()}/api/auth/otp/verify`, 'POST', {
    ...params,
    portalScope: params.portalScope || 'USER',
    trustDevice: params.trustDevice ?? true,
    deviceId: getOrCreateWebDeviceId(),
    deviceName: getWebDeviceName(),
    platform: 'WEB',
    hardwareFingerprint: fp,
  });
}

export async function apiCancelOtp(params: {
  challengeId: string;
  phone?: string;
  preAuthToken?: string;
}) {
  if (!params.challengeId) {
    return { ok: false, status: 400, message: 'Missing challengeId' } as ApiResult<{ cancelled: boolean }>;
  }
  return callApi<{ cancelled: boolean; challengeId?: string; status?: string }>(
    `${getApiBase()}/api/auth/otp/cancel`,
    'POST',
    {
      challengeId: params.challengeId,
      phone: params.phone?.trim(),
      preAuthToken: params.preAuthToken,
    }
  );
}

export async function apiTrustedDeviceLogin(portalScope: WebPortalScope = 'USER') {
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
  return callApi<WebAuthResponsePayload>(`${getApiBase()}/api/auth/trusted-login`, 'POST', {
    userId: trust.userId,
    phone: trust.phone,
    portalScope,
    deviceId: getOrCreateWebDeviceId(),
    deviceTrustToken: trust.deviceTrustToken,
    hardwareFingerprint: fp,
  });
}

export async function apiFetchMe(token: string) {
  return callApi<{ user: WebUserDto }>(`${getApiBase()}/api/auth/me`, 'GET', undefined, token);
}

export async function apiListTrustedDevices(token: string) {
  return callApi<{ devices: WebTrustedDeviceInfo[] }>(`${getApiBase()}/api/auth/devices`, 'GET', undefined, token);
}

export async function apiRevokeTrustedDevice(token: string, deviceId: string) {
  return callApi<{ revokedDeviceId?: string }>(
    `${getApiBase()}/api/auth/devices/${encodeURIComponent(deviceId)}`,
    'DELETE',
    undefined,
    token
  );
}

export async function apiDeleteMyAccount(
  token: string,
  payload?: { stepUpChallengeId?: string; stepUpCode?: string }
) {
  return callApi<{ deleted?: boolean; anonymizedSharedRecords?: boolean }>(
    `${getApiBase()}/api/auth/me`,
    'DELETE',
    payload || {},
    token
  );
}

// --- 1-on-1 WebRTC Calls & Budget Gate ---
export async function apiAuthorizeCall(token: string, calleeId: string, callType: 'AUDIO' | 'VIDEO' = 'AUDIO') {
  return callApi<WebCallAuthorizeData>(
    `${getApiBase()}/api/calls/authorize`,
    'POST',
    { calleeId, callType },
    token
  );
}

export async function apiGetCallStatus(token: string) {
  return callApi<{ canStartNewCalls: boolean; blockedReason?: string | null; dailyUserLimitMinutes?: number }>(
    `${getApiBase()}/api/calls/status`,
    'GET',
    undefined,
    token
  );
}

export async function apiGetCallHistory(token: string, page = 1, limit = 20) {
  return callApi<{ calls: WebCallHistoryItem[] }>(
    `${getApiBase()}/api/calls/history?page=${page}&limit=${limit}`,
    'GET',
    undefined,
    token
  );
}

export async function apiGetAdminCallBudget(token: string, month?: string) {
  const qs = month ? `?month=${encodeURIComponent(month)}` : '';
  return callApi<WebCallBudgetTelemetry>(`${getApiBase()}/api/calls/budget${qs}`, 'GET', undefined, token);
}

export async function apiFetchTurnCredentials(token: string, ttl = 3600): Promise<WebIceServerConfig[]> {
  try {
    const res = await fetch(`${getSignalingHttpBase()}/turn-credentials`, {
      method: 'POST',
      headers: await webAuthHeaders(token),
      body: JSON.stringify({ ttl }),
    });
    if (!res.ok) return [{ urls: ['stun:stun.l.google.com:19302'] }];
    const json = (await res.json()) as { iceServers?: WebIceServerConfig[]; data?: { iceServers?: WebIceServerConfig[] } };
    const servers = json.iceServers || json.data?.iceServers || [];
    return servers.length > 0 ? servers : [{ urls: ['stun:stun.l.google.com:19302'] }];
  } catch {
    return [{ urls: ['stun:stun.l.google.com:19302'] }];
  }
}

// --- Workspace Real-Time Chats ---
export async function apiListConversations(token: string) {
  return callApi<{ conversations: WebConversationItem[] }>(`${getApiBase()}/api/chats`, 'GET', undefined, token);
}

export async function apiCreateOrGetConversation(token: string, participantId: string) {
  return callApi<{ conversation: WebConversationItem }>(
    `${getApiBase()}/api/chats`,
    'POST',
    { participantId },
    token
  );
}

export async function apiListMessages(token: string, conversationId: string) {
  return callApi<{ messages: WebChatMessageItem[] }>(
    `${getApiBase()}/api/chats/${encodeURIComponent(conversationId)}/messages`,
    'GET',
    undefined,
    token
  );
}

export async function apiSendMessage(token: string, conversationId: string, content: string) {
  return callApi<{ message: WebChatMessageItem }>(
    `${getApiBase()}/api/chats/${encodeURIComponent(conversationId)}/messages`,
    'POST',
    { content, messageType: 'TEXT' },
    token
  );
}

// --- Public & Admin CMS Persistence (/api/content) ---
export async function apiGetPublicCmsContent() {
  return callApi<{ content: Record<string, unknown> }>(`${getApiBase()}/api/content`, 'GET');
}

export async function apiUpdateCmsContent(token: string, payload: Record<string, unknown>) {
  return callApi<{ content: Record<string, unknown> }>(`${getApiBase()}/api/content`, 'PUT', payload, token);
}
