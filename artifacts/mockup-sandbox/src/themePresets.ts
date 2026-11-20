import { SiteThemeConfig } from './types';

export const defaultTheme: SiteThemeConfig = {
  id: 'gold',
  name: 'الذهب الملكي الأصيل',
  nameEn: 'Classic Royal Gold',
  description: 'الهوية الرسمية لمنصة أوزوكس المرتبطة بتجارة وتدقيق الذهب والمجوهرات.',
  primary: '#f59e0b',
  primaryLight: '#fbbf24',
  primaryHighlight: '#fcd34d',
  primaryDark: '#d97706',
  glowColor: 'rgba(245, 158, 11, 0.35)',
  lastUpdated: new Date().toISOString(),
};

export const themePresets: SiteThemeConfig[] = [
  defaultTheme,
  {
    id: 'emerald',
    name: 'الزمرد الاستثماري',
    nameEn: 'Digital Emerald',
    description: 'نبرة مالية تعزز الثقة والنمو والأمان والاستقرار المحاسبي للشركات.',
    primary: '#10b981',
    primaryLight: '#34d399',
    primaryHighlight: '#6ee7b7',
    primaryDark: '#059669',
    glowColor: 'rgba(16, 185, 129, 0.35)',
  },
  {
    id: 'sapphire',
    name: 'الياقوت التقني',
    nameEn: 'Cyber Sapphire',
    description: 'طابع تكنولوجي مصرفي معاصر يركز على الحوسبة السحابية وأمان البيانات.',
    primary: '#0ea5e9',
    primaryLight: '#38bdf8',
    primaryHighlight: '#7dd3fc',
    primaryDark: '#0284c7',
    glowColor: 'rgba(14, 165, 233, 0.35)',
  },
  {
    id: 'ruby',
    name: 'الياقوت القرمزي',
    nameEn: 'Prestige Ruby',
    description: 'نبرة حيوية فاخرة وجريئة تلفت الانتباه وتبرز العروض الحصرية والسرعة.',
    primary: '#f43f5e',
    primaryLight: '#fb7185',
    primaryHighlight: '#fda4af',
    primaryDark: '#e11d48',
    glowColor: 'rgba(244, 63, 94, 0.35)',
  },
  {
    id: 'amethyst',
    name: 'الأرجواني المخملي',
    nameEn: 'Royal Amethyst',
    description: 'إيحاء بالفخامة العالية والابتكار الرقمي المتقدم للشركات المتميزة.',
    primary: '#8b5cf6',
    primaryLight: '#a78bfa',
    primaryHighlight: '#c4b5fd',
    primaryDark: '#7c3aed',
    glowColor: 'rgba(139, 92, 246, 0.35)',
  },
  {
    id: 'bronze',
    name: 'البرونز الناري',
    nameEn: 'Sunset Bronze',
    description: 'مزيج حار من النحاس والذهب يعكس دفء الأسواق العريقة والتجارة النشطة.',
    primary: '#f97316',
    primaryLight: '#fb923c',
    primaryHighlight: '#fdba74',
    primaryDark: '#ea580c',
    glowColor: 'rgba(249, 115, 22, 0.35)',
  },
  {
    id: 'teal',
    name: 'الفيروز البحري',
    nameEn: 'Oceanic Teal',
    description: 'طابع حداثي منعش يجمع بين الهدوء ووضوح المؤشرات والبيانات الدقيقة.',
    primary: '#14b8a6',
    primaryLight: '#2dd4bf',
    primaryHighlight: '#5eead4',
    primaryDark: '#0d9488',
    glowColor: 'rgba(20, 184, 166, 0.35)',
  },
  {
    id: 'indigo',
    name: 'النيل الأزرق العميق',
    nameEn: 'Deep Indigo',
    description: 'مظهر مؤسسي موثوق يلائم البنوك والمؤسسات المالية الكبرى.',
    primary: '#6366f1',
    primaryLight: '#818cf8',
    primaryHighlight: '#a5b4fc',
    primaryDark: '#4f46e5',
    glowColor: 'rgba(99, 102, 241, 0.35)',
  }
];

// Helper: Convert HEX to RGB
function hexToRgb(hex: string): { r: number; g: number; b: number } | null {
  const cleanHex = hex.replace('#', '').trim();
  if (cleanHex.length === 3) {
    const r = parseInt(cleanHex[0] + cleanHex[0], 16);
    const g = parseInt(cleanHex[1] + cleanHex[1], 16);
    const b = parseInt(cleanHex[2] + cleanHex[2], 16);
    return { r, g, b };
  }
  if (cleanHex.length === 6) {
    const r = parseInt(cleanHex.substring(0, 2), 16);
    const g = parseInt(cleanHex.substring(2, 4), 16);
    const b = parseInt(cleanHex.substring(4, 6), 16);
    return { r, g, b };
  }
  return null;
}

// Helper: Convert RGB to HSL
function rgbToHsl(r: number, g: number, b: number): { h: number; s: number; l: number } {
  r /= 255;
  g /= 255;
  b /= 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  let h = 0;
  let s = 0;
  const l = (max + min) / 2;

  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case r:
        h = (g - b) / d + (g < b ? 6 : 0);
        break;
      case g:
        h = (b - r) / d + 2;
        break;
      case b:
        h = (r - g) / d + 4;
        break;
    }
    h /= 6;
  }

  return { h: Math.round(h * 360), s: Math.round(s * 100), l: Math.round(l * 100) };
}

// Helper: Convert HSL to Hex
function hslToHex(h: number, s: number, l: number): string {
  s /= 100;
  l /= 100;
  const a = s * Math.min(l, 1 - l);
  const f = (n: number) => {
    const k = (n + h / 30) % 12;
    const color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
    return Math.round(255 * color).toString(16).padStart(2, '0');
  };
  return `#${f(0)}${f(8)}${f(4)}`;
}

// Automatically generate shades (400, 300, 600) from any arbitrary primary HEX
export function generateShadesFromHex(primaryHex: string): {
  primary: string;
  primaryLight: string;
  primaryHighlight: string;
  primaryDark: string;
  glowColor: string;
} {
  const rgb = hexToRgb(primaryHex);
  if (!rgb) {
    return {
      primary: primaryHex,
      primaryLight: primaryHex,
      primaryHighlight: primaryHex,
      primaryDark: primaryHex,
      glowColor: 'rgba(245, 158, 11, 0.35)',
    };
  }

  const { h, s, l } = rgbToHsl(rgb.r, rgb.g, rgb.b);

  const lightL = Math.min(92, l + 9);
  const highlightL = Math.min(96, l + 18);
  const darkL = Math.max(14, l - 10);

  return {
    primary: primaryHex.toLowerCase(),
    primaryLight: hslToHex(h, Math.min(95, s + 4), lightL),
    primaryHighlight: hslToHex(h, Math.min(95, s + 6), highlightL),
    primaryDark: hslToHex(h, s, darkL),
    glowColor: `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.35)`,
  };
}

// Apply theme CSS variables to Document
export function applyThemeToDocument(theme: SiteThemeConfig) {
  if (typeof document === 'undefined') return;

  const root = document.documentElement;
  root.style.setProperty('--oozox-accent-300', theme.primaryHighlight);
  root.style.setProperty('--oozox-accent-400', theme.primaryLight);
  root.style.setProperty('--oozox-accent-500', theme.primary);
  root.style.setProperty('--oozox-accent-600', theme.primaryDark);
  root.style.setProperty('--oozox-accent-glow', theme.glowColor);

  // Inject or update a targeted style tag for full reliability across dynamic components
  let styleEl = document.getElementById('oozox-theme-style-override') as HTMLStyleElement | null;
  if (!styleEl) {
    styleEl = document.createElement('style');
    styleEl.id = 'oozox-theme-style-override';
    document.head.appendChild(styleEl);
  }

  styleEl.innerHTML = `
    :root, [data-theme], body {
      --oozox-accent-300: ${theme.primaryHighlight} !important;
      --oozox-accent-400: ${theme.primaryLight} !important;
      --oozox-accent-500: ${theme.primary} !important;
      --oozox-accent-600: ${theme.primaryDark} !important;
      --oozox-accent-glow: ${theme.glowColor} !important;
      --color-amber-300: ${theme.primaryHighlight} !important;
      --color-amber-400: ${theme.primaryLight} !important;
      --color-amber-500: ${theme.primary} !important;
      --color-amber-600: ${theme.primaryDark} !important;
    }
  `;
}

// Retrieve saved theme from localStorage or fallback to default
export function getSavedTheme(): SiteThemeConfig {
  if (typeof window === 'undefined') return defaultTheme;

  try {
    const saved = localStorage.getItem('oozox_theme_config');
    if (saved) {
      const parsed = JSON.parse(saved);
      if (parsed && parsed.primary) {
        return parsed;
      }
    }
  } catch (e) {
    console.error('Error reading saved theme:', e);
  }

  return defaultTheme;
}

// Save theme to localStorage and broadcast event
export function saveTheme(theme: SiteThemeConfig) {
  if (typeof window === 'undefined') return;

  try {
    const themeToSave = {
      ...theme,
      lastUpdated: new Date().toISOString(),
    };
    localStorage.setItem('oozox_theme_config', JSON.stringify(themeToSave));
    applyThemeToDocument(themeToSave);
    window.dispatchEvent(new CustomEvent('oozox_theme_change', { detail: themeToSave }));
  } catch (e) {
    console.error('Error saving theme:', e);
  }
}
