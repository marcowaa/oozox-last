import React, { useState } from 'react';
import { AdminDashboardSkeleton } from '../skeletons/AdminDashboardSkeleton';
import { Button } from '../ui/button';
import { RefreshCw } from 'lucide-react';

export default function AdminSkeletonPreview() {
  const [key, setKey] = useState(0);

  return (
    <div className="relative">
      <div className="fixed bottom-5 right-5 z-50 bg-slate-900/90 text-white border border-slate-700/80 p-3 rounded-2xl shadow-2xl backdrop-blur-md flex items-center gap-3">
        <span className="text-xs font-bold text-amber-400">معاينة: Skeleton Loading - AdminDashboard</span>
        <Button 
          size="sm" 
          onClick={() => setKey(prev => prev + 1)}
          className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs flex items-center gap-1.5"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          إعادة التشغيل
        </Button>
      </div>
      <AdminDashboardSkeleton key={key} />
    </div>
  );
}
