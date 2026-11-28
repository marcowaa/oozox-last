import { useEffect, useState } from "react";
import { LandingPage } from "./LandingPage";
import { AdminLogin } from "./AdminLogin";
import { AdminDashboard } from "./AdminDashboard";
import { UserAppPortal } from "./UserAppPortal";
import { ContactUs } from "./components/ContactUs";
import { DeleteAccountPage } from "./DeleteAccountPage";
import {
  initialAnnouncements,
  initialTutorials,
  initialGoldPrices,
  initialProjectFeatures,
  initialHeroConfig,
} from "./dataStore";
import {
  Announcement,
  TutorialGuide,
  LiveGoldPrice,
  ProjectFeature,
  SiteHeroConfig,
  AdminUser,
  SiteThemeConfig,
} from "./types";
import { getSavedTheme, applyThemeToDocument, saveTheme } from "./themePresets";
import {
  apiGetPublicCmsContent,
  apiUpdateCmsContent,
  getApiBase,
  getWebAccessToken,
} from "./lib/webAuthClient";

type PortalRoute =
  | "landing"
  | "app"
  | "admin_login"
  | "admin_dashboard"
  | "contact"
  | "delete_account";

function safeReadStorage<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const saved = localStorage.getItem(key);
    return saved ? (JSON.parse(saved) as T) : fallback;
  } catch {
    return fallback;
  }
}

function App() {
  const [currentRoute, setCurrentRoute] = useState<PortalRoute>("landing");
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null);

  const [heroConfig, setHeroConfig] = useState<SiteHeroConfig>(() =>
    safeReadStorage("oozox_hero_config", initialHeroConfig)
  );
  const [features, setFeatures] = useState<ProjectFeature[]>(() =>
    safeReadStorage("oozox_features", initialProjectFeatures)
  );
  const [announcements, setAnnouncements] = useState<Announcement[]>(() =>
    safeReadStorage("oozox_announcements", initialAnnouncements)
  );
  const [tutorials, setTutorials] = useState<TutorialGuide[]>(() =>
    safeReadStorage("oozox_tutorials", initialTutorials)
  );
  const [goldPrices, setGoldPrices] = useState<LiveGoldPrice[]>(() =>
    safeReadStorage("oozox_gold_prices", initialGoldPrices)
  );
  const [currentTheme, setCurrentTheme] = useState<SiteThemeConfig>(getSavedTheme);
  const [isDataLoading, setIsDataLoading] = useState<boolean>(true);

  useEffect(() => {
    applyThemeToDocument(currentTheme);

    const handleThemeChange = (e: Event) => {
      const custom = e as CustomEvent<SiteThemeConfig>;
      if (custom.detail) {
        setCurrentTheme(custom.detail);
        applyThemeToDocument(custom.detail);
      }
    };

    const handleStorage = (e: StorageEvent) => {
      if (e.key === "oozox_theme_config" && e.newValue) {
        try {
          const parsed = JSON.parse(e.newValue) as SiteThemeConfig;
          setCurrentTheme(parsed);
          applyThemeToDocument(parsed);
        } catch {
          // Ignore malformed theme JSON
        }
      }
    };

    window.addEventListener("oozox_theme_change", handleThemeChange);
    window.addEventListener("storage", handleStorage);
    return () => {
      window.removeEventListener("oozox_theme_change", handleThemeChange);
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  const handleUpdateTheme = (updatedTheme: SiteThemeConfig) => {
    setCurrentTheme(updatedTheme);
    saveTheme(updatedTheme);
    const adminTok = getWebAccessToken("admin");
    if (adminTok) {
      void apiUpdateCmsContent(adminTok, { themeConfig: updatedTheme });
    }
  };

  useEffect(() => {
    let active = true;
    void apiGetPublicCmsContent()
      .then((res) => {
        if (!active || !res.ok || !res.data?.content) return;
        const c = res.data.content as Record<string, unknown>;
        if (c.hero && typeof c.hero === "object") {
          setHeroConfig((prev) => ({
            ...prev,
            ...(c.hero as Partial<SiteHeroConfig>),
          }));
        }
        if (Array.isArray(c.features) && c.features.length > 0) {
          setFeatures(c.features as ProjectFeature[]);
        }
        if (Array.isArray(c.announcements) && c.announcements.length > 0) {
          setAnnouncements(c.announcements as Announcement[]);
        }
        if (Array.isArray(c.tutorials) && c.tutorials.length > 0) {
          setTutorials(c.tutorials as TutorialGuide[]);
        }
        if (Array.isArray(c.goldRates) && c.goldRates.length > 0) {
          setGoldPrices(c.goldRates as LiveGoldPrice[]);
        }
        if (
          c.themeConfig &&
          typeof c.themeConfig === "object" &&
          (c.themeConfig as SiteThemeConfig).primary
        ) {
          const srvTheme = c.themeConfig as SiteThemeConfig;
          setCurrentTheme(srvTheme);
          applyThemeToDocument(srvTheme);
        }
      })
      .finally(() => {
        if (active) setIsDataLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const syncRouteFromLocation = () => {
      const path = window.location.pathname.toLowerCase();
      const token =
        typeof window !== "undefined"
          ? sessionStorage.getItem("oozox_admin_token")
          : null;

      if (path === "/app" || path.startsWith("/app/")) {
        setCurrentRoute("app");
        return;
      }
      if (path === "/contact" || path === "/contact-us") {
        setCurrentRoute("contact");
        return;
      }
      if (path === "/delete-account" || path.startsWith("/delete-account/")) {
        setCurrentRoute("delete_account");
        return;
      }

      const wantsAdminArea = path.startsWith("/admin");
      if (wantsAdminArea && token) {
        fetch(`${getApiBase()}/api/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
        })
          .then(async (res) => {
            if (!res.ok) throw new Error("Unauthorized");
            const payload = await res.json();
            const user = payload?.data?.user;
            if (!user || user.role !== "ADMIN") throw new Error("Forbidden");
            setAdminUser({
              username: user.name || user.phone || user.email,
              role: "SUPER_ADMIN",
              lastLogin: new Date().toLocaleString("ar-EG"),
            });
            setCurrentRoute("admin_dashboard");
            if (path !== "/admin/dashboard") {
              window.history.replaceState({}, "", "/admin/dashboard");
            }
          })
          .catch(() => {
            sessionStorage.removeItem("oozox_admin_token");
            setAdminUser(null);
            setCurrentRoute("admin_login");
            if (path !== "/admin/login") {
              window.history.replaceState({}, "", "/admin/login");
            }
          });
        return;
      }

      if (wantsAdminArea) {
        setCurrentRoute("admin_login");
        if (path !== "/admin/login") {
          window.history.replaceState({}, "", "/admin/login");
        }
        return;
      }

      setCurrentRoute("landing");
    };

    syncRouteFromLocation();
    window.addEventListener("popstate", syncRouteFromLocation);
    return () => window.removeEventListener("popstate", syncRouteFromLocation);
  }, []);

  const persistCms = (payload: Record<string, unknown>) => {
    const adminTok = getWebAccessToken("admin");
    if (adminTok) {
      void apiUpdateCmsContent(adminTok, payload);
    }
  };

  const handleUpdateHeroConfig = (updated: SiteHeroConfig) => {
    setHeroConfig(updated);
    localStorage.setItem("oozox_hero_config", JSON.stringify(updated));
    persistCms({ hero: updated, heroTitleAr: updated.headline });
  };

  const handleUpdateFeatures = (updated: ProjectFeature[]) => {
    setFeatures(updated);
    localStorage.setItem("oozox_features", JSON.stringify(updated));
    persistCms({ features: updated });
  };

  const handleUpdateAnnouncements = (updated: Announcement[]) => {
    setAnnouncements(updated);
    localStorage.setItem("oozox_announcements", JSON.stringify(updated));
    persistCms({ announcements: updated });
  };

  const handleUpdateTutorials = (updated: TutorialGuide[]) => {
    setTutorials(updated);
    localStorage.setItem("oozox_tutorials", JSON.stringify(updated));
    persistCms({ tutorials: updated });
  };

  const handleUpdateGoldPrices = (updated: LiveGoldPrice[]) => {
    setGoldPrices(updated);
    localStorage.setItem("oozox_gold_prices", JSON.stringify(updated));
    persistCms({ goldRates: updated });
  };

  const navigateTo = (route: PortalRoute, url: string) => {
    setCurrentRoute(route);
    window.history.pushState({}, "", url);
  };

  if (currentRoute === "delete_account") {
    return (
      <DeleteAccountPage
        onNavigateHome={() => navigateTo("landing", "/")}
      />
    );
  }

  if (currentRoute === "admin_login") {
    return (
      <AdminLogin
        onLoginSuccess={(user) => {
          setAdminUser(user);
          navigateTo("admin_dashboard", "/admin/dashboard");
        }}
        onNavigateHome={() => navigateTo("landing", "/")}
      />
    );
  }

  if (currentRoute === "app") {
    return (
      <UserAppPortal
        onNavigateHome={() => navigateTo("landing", "/")}
        onNavigateToAdmin={() => navigateTo("admin_login", "/admin/login")}
      />
    );
  }

  if (currentRoute === "admin_dashboard" && adminUser) {
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
          if (typeof window !== "undefined") {
            sessionStorage.removeItem("oozox_admin_token");
          }
          setAdminUser(null);
          navigateTo("admin_login", "/admin/login");
        }}
        onViewLiveSite={() => navigateTo("landing", "/")}
      />
    );
  }

  if (currentRoute === "contact") {
    return <ContactUs onNavigateHome={() => navigateTo("landing", "/")} />;
  }

  return (
    <LandingPage
      heroConfig={heroConfig}
      announcements={announcements}
      tutorials={tutorials}
      features={features}
      goldPrices={goldPrices}
      isLoading={isDataLoading}
      onNavigateToAdminLogin={() => navigateTo("admin_login", "/admin/login")}
      onLaunchAppPortal={() => navigateTo("app", "/app")}
      onNavigateToContact={() => navigateTo("contact", "/contact")}
      onNavigateToDeleteAccount={() =>
        navigateTo("delete_account", "/delete-account")
      }
    />
  );
}

export default App;
