import React from 'react';
import { Skeleton } from '../ui/skeleton';

export const AdminDashboardSkeleton: React.FC = () => {
  return (
    <div 
      className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col font-sans transition-colors duration-300"
      dir="rtl"
      aria-busy="true"
      aria-label="جاري تحميل لوحة التحكم..."
    >
      {/* 1. Admin Top Header Skeleton */}
      <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 py-3.5 flex items-center justify-between sticky top-0 z-30 transition-colors duration-300">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
            <Skeleton className="w-9 h-9 rounded-xl bg-amber-500/30" />
            <div className="space-y-1.5">
              <div className="flex items-center gap-2">
                <Skeleton className="h-5 w-44 sm:w-56 rounded-md" />
                <Skeleton className="h-4 w-14 rounded-md bg-emerald-500/20" />
              </div>
              <Skeleton className="h-3.5 w-28 rounded-md" />
            </div>
          </div>
        </div>

        {/* Header Right Actions */}
        <div className="flex items-center gap-3">
          {/* Auto-save skeleton indicator */}
          <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-100 dark:bg-slate-950/80">
            <Skeleton className="w-3.5 h-3.5 rounded-full" />
            <Skeleton className="h-3.5 w-28 rounded-md" />
          </div>

          <Skeleton className="h-9 w-36 sm:w-44 rounded-xl" />
          <Skeleton className="h-9 w-24 sm:w-28 rounded-xl bg-red-500/20" />
        </div>
      </header>

      {/* 2. Admin Body (Sidebar + Main Content) */}
      <div className="flex-1 flex flex-col md:flex-row">
        {/* Sidebar Navigation Skeleton */}
        <aside className="w-full md:w-64 bg-white dark:bg-slate-900/90 border-l border-slate-200 dark:border-slate-800 p-4 space-y-2 shrink-0">
          <div className="px-2 py-2">
            <Skeleton className="h-3.5 w-24 rounded-md" />
          </div>

          {/* Nav Tab Items */}
          <Skeleton className="h-11 w-full rounded-xl bg-amber-500/25" />
          <div className="pt-3 pb-1 px-2">
            <Skeleton className="h-3 w-32 rounded-md" />
          </div>
          {[1, 2, 3, 4, 5, 6, 7].map((item) => (
            <div key={item} className="flex items-center justify-between p-3 rounded-xl border border-transparent">
              <div className="flex items-center gap-2.5">
                <Skeleton className="w-4 h-4 rounded-md" />
                <Skeleton className="h-4 w-28 rounded-md" />
              </div>
              <Skeleton className="h-4 w-6 rounded-md" />
            </div>
          ))}
        </aside>

        {/* Main Content Area Skeleton */}
        <main className="flex-1 p-6 lg:p-8 space-y-6 overflow-y-auto">
          {/* Top Title & Action Banner */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-2">
              <Skeleton className="h-8 w-60 sm:w-80 rounded-xl" />
              <Skeleton className="h-4 w-48 sm:w-96 rounded-md" />
            </div>
            <div className="flex items-center gap-2.5">
              <Skeleton className="h-10 w-28 rounded-xl" />
              <Skeleton className="h-10 w-36 rounded-xl bg-amber-500/30" />
            </div>
          </div>

          {/* 4 KPI Metrics Stat Cards Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { color: 'bg-amber-500/20' },
              { color: 'bg-teal-500/20' },
              { color: 'bg-amber-500/20' },
              { color: 'bg-emerald-500/20' }
            ].map((stat, idx) => (
              <div 
                key={idx} 
                className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-3 shadow-xs"
              >
                <div className="flex items-center justify-between">
                  <Skeleton className="h-4 w-28 rounded-md" />
                  <Skeleton className={`w-6 h-6 rounded-lg ${stat.color}`} />
                </div>
                <Skeleton className="h-9 w-24 rounded-lg" />
                <Skeleton className="h-3.5 w-36 rounded-md" />
              </div>
            ))}
          </div>

          {/* Fast-Action CMS Banner Skeleton */}
          <div className="p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-amber-500/10 via-white dark:via-slate-900 to-teal-500/10 border border-amber-500/20 space-y-4">
            <Skeleton className="h-6 w-72 rounded-lg" />
            <div className="space-y-2 max-w-2xl">
              <Skeleton className="h-4 w-full rounded-md" />
              <Skeleton className="h-4 w-3/4 rounded-md" />
            </div>
            <div className="flex flex-wrap gap-3 pt-2">
              <Skeleton className="h-10 w-44 rounded-xl bg-amber-500/30" />
              <Skeleton className="h-10 w-36 rounded-xl" />
              <Skeleton className="h-10 w-40 rounded-xl" />
            </div>
          </div>

          {/* Data Table / Content Management Card Skeleton */}
          <div className="rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 overflow-hidden shadow-xs">
            {/* Card Header with Filters */}
            <div className="p-5 sm:p-6 border-b border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-3 w-full sm:w-auto">
                <Skeleton className="w-8 h-8 rounded-xl bg-amber-500/20" />
                <div className="space-y-1">
                  <Skeleton className="h-5 w-40 rounded-md" />
                  <Skeleton className="h-3.5 w-56 rounded-md" />
                </div>
              </div>
              <div className="flex items-center gap-2.5 w-full sm:w-auto">
                <Skeleton className="h-9 w-48 rounded-xl flex-1 sm:flex-initial" />
                <Skeleton className="h-9 w-28 rounded-xl bg-amber-500/30 shrink-0" />
              </div>
            </div>

            {/* Table / List Rows Skeleton */}
            <div className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {[1, 2, 3, 4, 5].map((row) => (
                <div key={row} className="p-4 sm:p-5 flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5 flex-1 min-w-0">
                    <Skeleton className="w-10 h-10 rounded-xl shrink-0" />
                    <div className="space-y-2 flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <Skeleton className="h-4 w-40 sm:w-64 rounded-md" />
                        <Skeleton className="h-4 w-16 rounded-full bg-emerald-500/20" />
                      </div>
                      <Skeleton className="h-3.5 w-3/4 max-w-md rounded-md" />
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <Skeleton className="h-8 w-8 rounded-lg" />
                    <Skeleton className="h-8 w-8 rounded-lg" />
                    <Skeleton className="h-8 w-8 rounded-lg bg-red-500/20" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};
