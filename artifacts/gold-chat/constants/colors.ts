// ============================================================================
// ذهبي — Premium Dark Theme (2026 Design System)
// Ensured high contrast: dark text on gold buttons, light text on dark cards
// ============================================================================

export const Colors = {
  // ===== Dark Base =====
  bg: '#0B0F14',               // deepest background (app bg)
  background: '#0D1117',        // main background
  surface: '#161B22',           // cards, panels
  surfaceAlt: '#1C2128',        // slight elevation
  surfaceHover: '#22262D',      // pressed/hover
  glassOverlay: 'rgba(22, 27, 34, 0.85)',

  // ===== Brand =====
  primary: '#0D1117',
  accent: '#D4AF37',            // premium gold (buttons, highlights)
  accentLight: '#F0C75E',       // lighter gold for icons on dark
  accentDark: '#A67C00',        // deep gold for borders
  accentGlow: 'rgba(212, 175, 55, 0.12)',  // soft gold bg for badges
  accentTextOnGold: '#0B0F14',  // dark text ON gold buttons (high contrast)
  accentTextOnGlow: '#F0C75E',  // gold text on dark-glow badges

  // ===== Chat bubbles =====
  sentBubble: '#D4AF37',
  sentBubbleText: '#0B0F14',     // dark text on gold bubble
  receivedBubble: '#1C2128',
  receivedBubbleText: '#E6EDF3',  // light text on dark bubble
  systemMessage: 'rgba(212, 175, 55, 0.08)',

  // ===== Text hierarchy =====
  textPrimary: '#E6EDF3',        // main text on dark bg — bright white
  textSecondary: '#B0B8C4',      // secondary text — bright grey (was #9DA5B0)
  textMuted: '#8B95A5',           // muted text — brighter for readability (was #6E7681)
  textInverse: '#E6EDF3',        // text on dark = light

  // ===== Status colors =====
  success: '#10B981',
  successLight: 'rgba(16, 185, 129, 0.12)',
  successText: '#34D399',         // bright green text on dark
  warning: '#F59E0B',
  warningLight: 'rgba(245, 158, 11, 0.12)',
  warningText: '#FBBF24',          // bright amber text on dark
  error: '#F43F5E',
  errorLight: 'rgba(244, 63, 94, 0.12)',
  errorText: '#FB7185',           // bright red text on dark
  info: '#0EA5E9',
  infoLight: 'rgba(14, 165, 233, 0.12)',
  infoText: '#38BDF8',             // bright blue text on dark

  // ===== Transaction status =====
  purchased: '#0EA5E9',
  purchasedBg: 'rgba(14, 165, 233, 0.12)',
  purchasedText: '#38BDF8',
  returned: '#F43F5E',
  returnedBg: 'rgba(244, 63, 94, 0.12)',
  returnedText: '#FB7185',
  resold: '#10B981',
  resoldBg: 'rgba(16, 185, 129, 0.12)',
  resoldText: '#34D399',

  // ===== UI elements =====
  border: '#30363D',               // brighter borders (was #21262D)
  borderLight: '#3D444D',
  divider: 'rgba(255, 255, 255, 0.1)',
  shadow: '#000000',
  glow: 'rgba(212, 175, 55, 0.25)',

  // ===== Tab bar =====
  tabBar: '#0D1117',
  tabBarActive: '#D4AF37',
  tabBarInactive: '#7D8590',       // brighter inactive tabs (was #484F58)

  // ===== Input =====
  inputBg: '#0D1117',
  inputBorder: '#30363D',
  inputText: '#E6EDF3',
  placeholder: '#6B7280',         // brighter placeholder (was #484F58)

  // ===== Avatar palette =====
  avatarPalette: [
    '#D4AF37', '#A67C00', '#F0C75E', '#B8860B',
    '#10B981', '#0EA5E9', '#8B5CF6', '#EC4899',
    '#F59E0B', '#EF4444', '#06B6D4', '#84CC16',
  ],

  // ===== Profit/Loss =====
  profit: '#10B981',
  profitBg: 'rgba(16, 185, 129, 0.12)',
  profitText: '#34D399',
  loss: '#F43F5E',
  lossBg: 'rgba(244, 63, 94, 0.12)',
  lossText: '#FB7185',
  neutral: '#8B949E',
  neutralBg: 'rgba(139, 148, 158, 0.12)',

  // ===== Gradients =====
  gradient: ['#0B0F14', '#0D1117', '#161B22'],
  goldGradient: ['#D4AF37', '#F0C75E', '#D4AF37'],
  onboardingGradient: ['#050709', '#0D1117', '#1A1410'],

  // ===== Aliases for legacy code =====
  header: '#0D1117',
  headerText: '#E6EDF3',
  foreground: '#E6EDF3',
  secondary: '#1C2128',
  card: '#161B22',
  muted: '#8B95A5',                // was #6E7681 — brighter for readability
  mutedForeground: '#B0B8C4',     // was #9DA5B0 — brighter
};
