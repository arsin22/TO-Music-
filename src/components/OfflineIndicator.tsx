import React from 'react';
import { WifiOff } from 'lucide-react';
import { useOnlineStatus } from '../hooks/useOnlineStatus';

export const OfflineIndicator: React.FC = () => {
  const isOnline = useOnlineStatus();

  if (isOnline) return null;

  return (
    <div
      role="status"
      className="border-b border-amber-500/25 bg-amber-500/10 px-4 py-2 text-xs font-medium text-amber-200 backdrop-blur-md"
    >
      <div className="mx-auto flex max-w-7xl items-center justify-center gap-2">
        <WifiOff className="h-3.5 w-3.5 shrink-0 text-amber-300" aria-hidden="true" />
        <span>حالت آفلاین — پخش آهنگ‌ها از حافظه ذخیره‌شده دستگاه انجام می‌شود.</span>
      </div>
    </div>
  );
};
