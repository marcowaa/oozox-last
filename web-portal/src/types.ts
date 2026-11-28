export interface Announcement {
  id: string;
  title: string;
  category: 'promo' | 'system' | 'market' | 'event';
  content: string;
  badgeText: string;
  badgeColor: string;
  publishDate: string;
  isActive: boolean;
  actionUrl?: string;
  actionLabel?: string;
  viewCount: number;
  isBroadcast?: boolean;
}

export interface BroadcastAnnouncement {
  id: string;
  title: string;
  message: string;
  category: 'promo' | 'system' | 'market' | 'event' | 'urgent';
  badgeText: string;
  badgeColor: string;
  actionUrl?: string;
  actionLabel?: string;
  timestamp: number;
  durationSeconds?: number;
  author?: string;
}

export interface TutorialGuide {
  id: string;
  title: string;
  category: 'accounting' | 'vault' | 'ai_camera' | 'projects' | 'backup';
  summary: string;
  duration: string;
  steps: string[];
  icon: string;
  tags: string[];
  isFeatured: boolean;
}

export interface ProjectFeature {
  id: string;
  title: string;
  description: string;
  category: string;
  icon: string;
  highlights: string[];
}

export interface SiteHeroConfig {
  topBadge: string;
  headline: string;
  brandHighlight: string;
  subheadline: string;
  apkDownloadLabel: string;
  trialHighlight: string;
}

export interface LiveGoldPrice {
  karat: '18K' | '21K' | '24K' | 'Ounce' | 'Pound';
  buyPrice: number;
  sellPrice: number;
  change24h: number;
  lastUpdated: string;
}

export interface AdminUser {
  username: string;
  role: 'SUPER_ADMIN' | 'OPERATIONS' | 'FINANCE';
  lastLogin?: string;
}

export interface JobOpportunity {
  id: string;
  title: string;
  company: string;
  location: string;
  salary: string;
  postedDate: string;
  category: string;
  description: string;
}

export interface WorkAssignment {
  id: string;
  employeeName: string;
  jobTitle: string;
  location: {
    lat: number;
    lng: number;
    address: string;
    radius: number; // in meters
  };
  workingHours: {
    start: string; // "HH:mm"
    end: string;
  };
  status: 'active' | 'completed' | 'suspended';
  presenceScore: number; // 0-100
}

export interface AttendanceRecord {
  id: string;
  assignmentId: string;
  timestamp: string;
  type: 'check_in' | 'check_out' | 'violation';
  locationMatch: boolean;
  notes?: string;
}

export interface SiteThemeConfig {
  id: string;
  name: string;
  nameEn: string;
  description: string;
  primary: string;          // 500
  primaryLight: string;     // 400
  primaryHighlight: string; // 300
  primaryDark: string;      // 600
  glowColor: string;
  isCustom?: boolean;
  lastUpdated?: string;
}
