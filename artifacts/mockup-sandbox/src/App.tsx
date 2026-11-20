import { useEffect, useState, type ComponentType } from "react";
import { LandingPage } from "./LandingPage";
import { AdminLogin } from "./AdminLogin";
import { AdminDashboard } from "./AdminDashboard";
import { UserAppPortal } from "./UserAppPortal";
import { ContactUs } from "./components/mockups/ContactUs";
import { 
  initialAnnouncements, 
  initialTutorials, 
  initialGoldPrices, 
  initialProjectFeatures,
  initialHeroConfig
} from "./dataStore";
import { Announcement, TutorialGuide, LiveGoldPrice, ProjectFeature, SiteHeroConfig, AdminUser, SiteThemeConfig } from "./types";
import { getSavedTheme, applyThemeToDocument, saveTheme } from "./themePresets";
import { modules as discoveredModules } from "./.generated/mockup-components";

type ModuleMap = Record<string, () => Promise<Record<string, unknown>>>;

function _resolveComponent(
  mod: Record<string, unknown>,
  name: string,
): ComponentType | undefined {
  const fns = Object.values(mod).filter(
    (v) => typeof v === "function",
  ) as ComponentType[];
  return (
    (mod.default as ComponentType) ||
    (mod.Preview as ComponentType) ||
    (mod[name] as ComponentType) ||
    fns[fns.length - 1]
  );
}

function PreviewRenderer({
  componentPath,
  modules,
}: {
  componentPath: string;
  modules: ModuleMap;
}) {
  const [Component, setComponent] = useState<ComponentType | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    setComponent(null);
    setError(null);

    async function loadComponent(): Promise<void> {
      const key = `./components/mockups/${componentPath}.tsx`;
      const loader = modules[key];
      if (!loader) {
        setError(`No component found at ${componentPath}.tsx`);
        return;
      }

      try {
        const mod = await loader();
        if (cancelled) {
          return;
        }
        const name = componentPath.split("/").pop()!;
        const comp = _resolveComponent(mod, name);
        if (!comp) {
          setError(
            `No exported React component found in ${componentPath}.tsx\n\nMake sure the file has at least one exported function component.`,
          );
          return;
        }
        setComponent(() => comp);
      } catch (e) {
        if (cancelled) {
          return;
        }

        const message = e instanceof Error ? e.message : String(e);
        setError(`Failed to load preview.\n${message}`);
      }
    }

    void loadComponent();

    return () => {
      cancelled = true;
    };
  }, [componentPath, modules]);

  if (error) {
    return (
      <pre style={{ color: "red", padding: "2rem", fontFamily: "system-ui" }}>
        {error}
      </pre>
    );
  }

  if (!Component) return null;

  return <Component />;
}

function getBasePath(): string {
  return import.meta.env.BASE_URL.replace(/\/$/, "");
}

function getPreviewPath(): string | null {
  const basePath = getBasePath();
  const { pathname } = window.location;
  const local =
    basePath && pathname.startsWith(basePath)
      ? pathname.slice(basePath.length) || "/"
      : pathname;
  const match = local.match(/^\/preview\/(.+)$/);
  return match ? match[1] : null;
}

function safeReadStorage<T>(key: string, fallback: T): T {
  if (typeof window === 'undefined') return fallback;
  try {
    const saved = localStorage.getItem(key);
    return saved ? (JSON.parse(saved) as T) : fallback;
  } catch {
    return fallback;
  }
}

function App() {
  const previewPath = getPreviewPath();

  // Web Portal State
  const [currentRoute, setCurrentRoute] = useState<'landing' | 'app' | 'admin_login' | 'admin_dashboard' | 'contact'>('landing');
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null);

  // Dynamic Content Managed by Admin
  const [heroConfig, setHeroConfig] = useState<SiteHeroConfig>(() =>
    safeReadStorage('oozox_hero_config', initialHeroConfig)
  );

  const [features, setFeatures] = useState<ProjectFeature[]>(() =>
    safeReadStorage('oozox_features', initialProjectFeatures)
  );

  const [announcements, setAnnouncements] = useState<Announcement[]>(() =>
    safeReadStorage('oozox_announcements', initialAnnouncements)
  );

  const [tutorials, setTutorials] = useState<TutorialGuide[]>(() =>
    safeReadStorage('oozox_tutorials', initialTutorials)
  );

  const [goldPrices, setGoldPrices] = useState<LiveGoldPrice[]>(() =>
    safeReadStorage('oozox_gold_prices', initialGoldPrices)
  );

  // Site Theme Configuration State
  const [currentTheme, setCurrentTheme] = useState<SiteThemeConfig>(getSavedTheme);

  // Apply theme to document on mount and listen to real-time changes
  useEffect(() => {
    applyThemeToDocument(currentTheme);

    const handleThemeChange = (e: any) => {
      if (e.detail) {
        setCurrentTheme(e.detail);
        applyThemeToDocument(e.detail);
      }
    };

    const handleStorage = (e: StorageEvent) => {
      if (e.key === 'oozox_theme_config' && e.newValue) {
        try {
          const parsed = JSON.parse(e.newValue);
          setCurrentTheme(parsed);
          applyThemeToDocument(parsed);
        } catch {}
      }
    };

    window.addEventListener('oozox_theme_change', handleThemeChange);
    window.addEventListener('storage', handleStorage);
    return () => {
      window.removeEventListener('oozox_theme_change', handleThemeChange);
      window.removeEventListener('storage', handleStorage);
    };
  }, []);

  const handleUpdateTheme = (updatedTheme: SiteThemeConfig) => {
    setCurrentTheme(updatedTheme);
    saveTheme(updatedTheme);
  };

  // Simulated Smooth Data Hydration / Loading state
  const [isDataLoading, setIsDataLoading] = useState<boolean>(true);

  useEffect(() => {
    // Initial fetch / hydration simulation
    const timer = setTimeout(() => {
      setIsDataLoading(false);
    }, 600);
    return () => clearTimeout(timer);
  }, []);

  // Check URL on load & restore verified ADMIN session if token is still valid
  useEffect(() => {
    const path = window.location.pathname.toLowerCase();
    const token = typeof window !== 'undefined' ? sessionStorage.getItem('oozox_admin_token') : null;

    if (path === '/app' || path.startsWith('/app/')) {
      setCurrentRoute('app');
      return;
    }
    if (path === '/contact' || path === '/contact-us') {
      setCurrentRoute('contact');
      return;
    }

    const wantsAdminArea = path.startsWith('/admin');
    if (wantsAdminArea && token) {
      const apiBase = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL || '';
      fetch(`${apiBase}/api/auth/me`, {
        headers: { Authorization: `Bearer ${token}` },
      })
        .then(async (res) => {
          if (!res.ok) throw new Error('Unauthorized');
          const payload = await res.json();
          const user = payload?.data?.user;
          if (!user || user.role !== 'ADMIN') throw new Error('Forbidden');
          setAdminUser({
            username: user.name || user.phone || user.email,
            role: 'SUPER_ADMIN',
            lastLogin: new Date().toLocaleString('ar-EG'),
          });
          setCurrentRoute('admin_dashboard');
          if (path !== '/admin/dashboard') {
            window.history.replaceState({}, '', '/admin/dashboard');
          }
        })
        .catch(() => {
          sessionStorage.removeItem('oozox_admin_token');
          setAdminUser(null);
          setCurrentRoute('admin_login');
          if (path !== '/admin/login') {
            window.history.replaceState({}, '', '/admin/login');
          }
        });
      return;
    }

    if (wantsAdminArea) {
      setCurrentRoute('admin_login');
      if (path !== '/admin/login') {
        window.history.replaceState({}, '', '/admin/login');
      }
    }
  }, []);

  // Save changes to localStorage for persistence
  const handleUpdateHeroConfig = (updated: SiteHeroConfig) => {
    setHeroConfig(updated);
    localStorage.setItem('oozox_hero_config', JSON.stringify(updated));
  };

  const handleUpdateFeatures = (updated: ProjectFeature[]) => {
    setFeatures(updated);
    localStorage.setItem('oozox_features', JSON.stringify(updated));
  };

  const handleUpdateAnnouncements = (updated: Announcement[]) => {
    setAnnouncements(updated);
    localStorage.setItem('oozox_announcements', JSON.stringify(updated));
  };

  const handleUpdateTutorials = (updated: TutorialGuide[]) => {
    setTutorials(updated);
    localStorage.setItem('oozox_tutorials', JSON.stringify(updated));
  };

  const handleUpdateGoldPrices = (updated: LiveGoldPrice[]) => {
    setGoldPrices(updated);
    localStorage.setItem('oozox_gold_prices', JSON.stringify(updated));
  };

  if (previewPath) {
    return (
      <PreviewRenderer
        componentPath={previewPath}
        modules={discoveredModules}
      />
    );
  }

  // Route 1: Admin Login Gate (/admin/login)
  if (currentRoute === 'admin_login') {
    return (
      <AdminLogin
        onLoginSuccess={(user) => {
          setAdminUser(user);
          setCurrentRoute('admin_dashboard');
          window.history.pushState({}, '', '/admin/dashboard');
        }}
        onNavigateHome={() => {
          setCurrentRoute('landing');
          window.history.pushState({}, '', '/');
        }}
      />
    );
  }

  // Route 2: Live Interactive Accounting & Invoicing App Portal
  if (currentRoute === 'app') {
    return (
      <UserAppPortal
        onNavigateHome={() => {
          setCurrentRoute('landing');
          window.history.pushState({}, '', '/');
        }}
        onNavigateToAdmin={() => {
          setCurrentRoute('admin_login');
          window.history.pushState({}, '', '/admin/login');
        }}
      />
    );
  }

  // Route 3: Admin Dashboard & Content Manager
  if (currentRoute === 'admin_dashboard' && adminUser) {
    return (
      <AdminDashboard
        adminUser={adminUser}
        heroConfig={heroConfig}
        announcements={announcements}
        tutorials={tutorials}
        features={features}
        goldPrices={goldPrices}
        onUpdateHeroConfig={handleUpdateHeroConfig}
        onUpdateAnnouncements={handleUpdateAnnouncements}
        onUpdateTutorials={handleUpdateTutorials}
        onUpdateFeatures={handleUpdateFeatures}
        onUpdateGoldPrices={handleUpdateGoldPrices}
        currentTheme={currentTheme}
        onUpdateTheme={handleUpdateTheme}
        isLoading={isDataLoading}
        onLogout={() => {
          if (typeof window !== 'undefined') {
            sessionStorage.removeItem('oozox_admin_token');
          }
          setAdminUser(null);
          setCurrentRoute('admin_login');
          window.history.pushState({}, '', '/admin/login');
        }}
        onViewLiveSite={() => {
          setCurrentRoute('landing');
          window.history.pushState({}, '', '/');
        }}
      />
    );
  }

  // Route 4: Contact Us Page
  if (currentRoute === 'contact') {
    return (
      <ContactUs
        onNavigateHome={() => {
          setCurrentRoute('landing');
          window.history.pushState({}, '', '/');
        }}
      />
    );
  }

  // Route 5: Public Website Portal (oozox.com)
  return (
    <LandingPage
      heroConfig={heroConfig}
      announcements={announcements}
      tutorials={tutorials}
      features={features}
      goldPrices={goldPrices}
      isLoading={isDataLoading}
      onNavigateToAdminLogin={() => {
        setCurrentRoute('admin_login');
        window.history.pushState({}, '', '/admin/login');
      }}
      onLaunchAppPortal={() => {
        setCurrentRoute('app');
        window.history.pushState({}, '', '/app');
      }}
      onNavigateToContact={() => {
        setCurrentRoute('contact');
        window.history.pushState({}, '', '/contact');
      }}
    />
  );
}

export default App;
