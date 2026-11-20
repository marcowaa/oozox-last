import React from 'react';
import { Skeleton } from '../ui/skeleton';

export const LandingPageSkeleton: React.FC = () => {
  return (
    <div 
      className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans antialiased selection:bg-amber-500 selection:text-black transition-colors duration-300 overflow-hidden" 
      dir="rtl"
      aria-busy="true"
      aria-label="جاري تحميل الصفحة الرئيسية..."
    >
      {/* 1. Top Announcement Bar Skeleton */}
      <div className="bg-gradient-to-r from-amber-500/20 via-amber-500/10 to-teal-500/20 border-b border-amber-500/20 px-4 py-2.5">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <Skeleton className="h-5 w-20 rounded-full bg-amber-500/30" />
            <Skeleton className="h-4 w-64 sm:w-80" />
          </div>
          <Skeleton className="h-6 w-28 rounded-lg" />
        </div>
      </div>

      {/* 2. Main Header / Navigation Skeleton */}
      <header className="sticky top-0 z-40 bg-white/80 dark:bg-slate-950/80 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 px-4 sm:px-6 py-3.5">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          {/* Brand Logo & Name */}
          <div className="flex items-center gap-3">
            <Skeleton className="w-10 h-10 rounded-2xl bg-amber-500/25" />
            <div className="space-y-1.5">
              <Skeleton className="h-5 w-28 rounded-md" />
              <Skeleton className="h-3 w-36 rounded-md hidden sm:block" />
            </div>
          </div>

          {/* Desktop Nav Links */}
          <nav className="hidden lg:flex items-center gap-2">
            <Skeleton className="h-8 w-20 rounded-xl" />
            <Skeleton className="h-8 w-20 rounded-xl" />
            <Skeleton className="h-8 w-24 rounded-xl" />
            <Skeleton className="h-8 w-20 rounded-xl" />
            <Skeleton className="h-8 w-18 rounded-xl" />
            <Skeleton className="h-8 w-20 rounded-xl" />
          </nav>

          {/* Action CTAs */}
          <div className="flex items-center gap-2 sm:gap-3">
            <Skeleton className="h-9 w-9 rounded-xl hidden sm:block" />
            <Skeleton className="h-9 w-24 rounded-xl" />
            <Skeleton className="h-9 w-32 rounded-xl bg-amber-500/30" />
          </div>
        </div>
      </header>

      {/* 3. Hero Section Skeleton */}
      <section className="relative pt-12 pb-20 sm:pt-16 sm:pb-28 px-4 sm:px-6 overflow-hidden">
        {/* Background glow effects */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="max-w-6xl mx-auto text-center space-y-6 relative z-10">
          {/* Floating Pill Badge */}
          <div className="inline-flex justify-center">
            <Skeleton className="h-8 w-60 rounded-full bg-amber-500/20" />
          </div>

          {/* Headline Skeletons */}
          <div className="space-y-3 max-w-4xl mx-auto">
            <Skeleton className="h-10 sm:h-14 w-11/12 sm:w-4/5 mx-auto rounded-2xl" />
            <Skeleton className="h-8 sm:h-12 w-3/4 sm:w-3/5 mx-auto rounded-2xl bg-amber-500/20" />
          </div>

          {/* Subtitle Skeletons */}
          <div className="space-y-2 max-w-2xl mx-auto pt-2">
            <Skeleton className="h-4 w-full sm:w-5/6 mx-auto rounded-md" />
            <Skeleton className="h-4 w-4/5 sm:w-2/3 mx-auto rounded-md" />
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 pt-4">
            <Skeleton className="h-12 w-full sm:w-48 rounded-2xl bg-amber-500/35" />
            <Skeleton className="h-12 w-full sm:w-44 rounded-2xl" />
            <Skeleton className="h-12 w-full sm:w-36 rounded-2xl" />
          </div>

          {/* Trust Metrics Pills */}
          <div className="flex flex-wrap items-center justify-center gap-3 sm:gap-6 pt-6">
            <Skeleton className="h-7 w-32 rounded-full" />
            <Skeleton className="h-7 w-36 rounded-full" />
            <Skeleton className="h-7 w-28 rounded-full" />
            <Skeleton className="h-7 w-32 rounded-full" />
          </div>

          {/* 4. Live Gold Ticker Strip Skeleton */}
          <div className="max-w-5xl mx-auto pt-8">
            <div className="p-4 sm:p-5 rounded-3xl bg-white/60 dark:bg-slate-900/60 border border-slate-200/80 dark:border-slate-800/80 backdrop-blur-xl shadow-xl shadow-slate-900/5">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200/60 dark:border-slate-800/60 mb-3">
                <div className="flex items-center gap-2">
                  <Skeleton className="w-5 h-5 rounded-full bg-amber-500/30" />
                  <Skeleton className="h-4 w-44 rounded-md" />
                </div>
                <Skeleton className="h-4 w-28 rounded-md" />
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
                {[1, 2, 3, 4].map((item) => (
                  <div key={item} className="p-3.5 rounded-2xl bg-slate-100 dark:bg-slate-950/70 border border-slate-200/50 dark:border-slate-800/50 space-y-2">
                    <div className="flex items-center justify-between">
                      <Skeleton className="h-4 w-16 rounded-md" />
                      <Skeleton className="h-4 w-10 rounded-full" />
                    </div>
                    <Skeleton className="h-7 w-24 rounded-lg bg-amber-500/20" />
                    <Skeleton className="h-3 w-16 rounded-md" />
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* 5. Hero Interactive Mockup Preview Frame Skeleton */}
          <div className="pt-8 max-w-5xl mx-auto">
            <div className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur-2xl p-4 sm:p-6 shadow-2xl space-y-6 text-right">
              {/* Mockup Header Window Controls */}
              <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
                <div className="flex items-center gap-2">
                  <Skeleton className="w-3 h-3 rounded-full bg-red-400/40" />
                  <Skeleton className="w-3 h-3 rounded-full bg-amber-400/40" />
                  <Skeleton className="w-3 h-3 rounded-full bg-emerald-400/40" />
                  <Skeleton className="h-4 w-32 rounded-md mr-3" />
                </div>
                <div className="flex items-center gap-2">
                  <Skeleton className="h-7 w-20 rounded-lg" />
                  <Skeleton className="h-7 w-24 rounded-lg bg-amber-500/20" />
                </div>
              </div>

              {/* Mockup Top KPI Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {[1, 2, 3].map((card) => (
                  <div key={card} className="p-4 rounded-2xl bg-slate-100/70 dark:bg-slate-950/60 border border-slate-200/50 dark:border-slate-800/50 space-y-3">
                    <div className="flex items-center justify-between">
                      <Skeleton className="h-4 w-20 rounded-md" />
                      <Skeleton className="w-8 h-8 rounded-xl" />
                    </div>
                    <Skeleton className="h-8 w-28 rounded-lg" />
                    <Skeleton className="h-3 w-32 rounded-md" />
                  </div>
                ))}
              </div>

              {/* Mockup Body Content Rows */}
              <div className="space-y-3 pt-2">
                {[1, 2, 3, 4].map((row) => (
                  <div key={row} className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200/40 dark:border-slate-800/40 flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <Skeleton className="w-9 h-9 rounded-xl" />
                      <div className="space-y-1.5">
                        <Skeleton className="h-4 w-36 sm:w-48 rounded-md" />
                        <Skeleton className="h-3 w-24 sm:w-32 rounded-md" />
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <Skeleton className="h-6 w-18 rounded-full" />
                      <Skeleton className="h-7 w-20 rounded-lg" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Key Features Grid Skeleton */}
      <section className="py-16 sm:py-24 px-4 sm:px-6 bg-slate-100/50 dark:bg-slate-900/30 border-y border-slate-200/60 dark:border-slate-800/60">
        <div className="max-w-7xl mx-auto space-y-12">
          <div className="text-center space-y-3 max-w-2xl mx-auto">
            <Skeleton className="h-6 w-32 rounded-full mx-auto bg-amber-500/20" />
            <Skeleton className="h-9 sm:h-11 w-4/5 mx-auto rounded-xl" />
            <Skeleton className="h-4 w-3/4 mx-auto rounded-md" />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
            {[1, 2, 3, 4, 5, 6].map((item) => (
              <div 
                key={item} 
                className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-900/90 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4"
              >
                <div className="flex items-center justify-between">
                  <Skeleton className="w-12 h-12 rounded-2xl bg-amber-500/20" />
                  <Skeleton className="h-6 w-20 rounded-full" />
                </div>
                <Skeleton className="h-6 w-44 rounded-lg" />
                <div className="space-y-2">
                  <Skeleton className="h-4 w-full rounded-md" />
                  <Skeleton className="h-4 w-5/6 rounded-md" />
                  <Skeleton className="h-4 w-3/4 rounded-md" />
                </div>
                <div className="pt-2 flex items-center justify-between">
                  <Skeleton className="h-4 w-28 rounded-md" />
                  <Skeleton className="w-5 h-5 rounded-full" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 7. Interactive Tutorials & Video Guides Skeleton */}
      <section className="py-16 sm:py-24 px-4 sm:px-6">
        <div className="max-w-7xl mx-auto space-y-10">
          <div className="text-center space-y-3 max-w-2xl mx-auto">
            <Skeleton className="h-6 w-28 rounded-full mx-auto" />
            <Skeleton className="h-9 sm:h-11 w-3/4 mx-auto rounded-xl" />
            <Skeleton className="h-4 w-2/3 mx-auto rounded-md" />
          </div>

          {/* Category Tabs Skeleton */}
          <div className="flex flex-wrap items-center justify-center gap-2 max-w-lg mx-auto">
            <Skeleton className="h-9 w-24 rounded-xl bg-amber-500/20" />
            <Skeleton className="h-9 w-28 rounded-xl" />
            <Skeleton className="h-9 w-24 rounded-xl" />
            <Skeleton className="h-9 w-32 rounded-xl" />
          </div>

          {/* Tutorial Showcase Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
            <div className="lg:col-span-2 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 sm:p-7 space-y-5">
              <Skeleton className="w-full aspect-video rounded-2xl" />
              <div className="flex items-center justify-between">
                <Skeleton className="h-7 w-64 rounded-lg" />
                <Skeleton className="h-6 w-24 rounded-full" />
              </div>
              <div className="space-y-2">
                <Skeleton className="h-4 w-full rounded-md" />
                <Skeleton className="h-4 w-4/5 rounded-md" />
              </div>
            </div>

            <div className="space-y-4">
              {[1, 2, 3].map((tut) => (
                <div key={tut} className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-3">
                  <div className="flex items-center gap-3">
                    <Skeleton className="w-16 h-12 rounded-xl" />
                    <div className="space-y-1.5 flex-1">
                      <Skeleton className="h-4 w-3/4 rounded-md" />
                      <Skeleton className="h-3 w-1/2 rounded-md" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 8. Footer Skeleton */}
      <footer className="border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-950 px-4 sm:px-6 py-12">
        <div className="max-w-7xl mx-auto grid grid-cols-2 sm:grid-cols-4 gap-8">
          {[1, 2, 3, 4].map((col) => (
            <div key={col} className="space-y-3">
              <Skeleton className="h-5 w-28 rounded-md" />
              <Skeleton className="h-3.5 w-36 rounded-md" />
              <Skeleton className="h-3.5 w-32 rounded-md" />
              <Skeleton className="h-3.5 w-40 rounded-md" />
            </div>
          ))}
        </div>
        <div className="max-w-7xl mx-auto pt-8 mt-8 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <Skeleton className="h-4 w-48 rounded-md" />
          <Skeleton className="h-4 w-32 rounded-md" />
        </div>
      </footer>
    </div>
  );
};
