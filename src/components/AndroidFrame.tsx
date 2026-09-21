import React from 'react';
import { Wifi, Battery, Signal } from 'lucide-react';

interface AndroidFrameProps {
  children: React.ReactNode;
  activeApp: 'client' | 'admin';
  onSwitchApp: (app: 'client' | 'admin') => void;
}

export const AndroidFrame: React.FC<AndroidFrameProps> = ({
  children,
  activeApp,
  onSwitchApp,
}) => {
  return (
    <div className="relative mx-auto my-2 w-[390px] h-[820px] bg-[#18302D] rounded-[50px] p-3.5 shadow-2xl shadow-emerald-950/40 border-4 border-[#2A4B46] flex flex-col select-none">
      {/* Speaker and Front Camera Punch Hole */}
      <div className="absolute top-6 left-1/2 -translate-x-1/2 z-50 flex items-center justify-center">
        <div className="w-4 h-4 bg-black rounded-full border border-gray-800 shadow-inner flex items-center justify-center">
          <div className="w-1.5 h-1.5 bg-[#0a1413] rounded-full"></div>
        </div>
      </div>

      {/* Outer Phone Buttons */}
      <div className="absolute -left-5 top-28 w-1.5 h-12 bg-[#2A4B46] rounded-l-md"></div>
      <div className="absolute -left-5 top-44 w-1.5 h-12 bg-[#2A4B46] rounded-l-md"></div>
      <div className="absolute -right-5 top-32 w-1.5 h-16 bg-[#2A4B46] rounded-r-md"></div>

      {/* Screen Container */}
      <div className="relative w-full h-full bg-[#F7FAF9] rounded-[38px] overflow-hidden flex flex-col font-cairo text-[#18302D]">
        {/* Android Status Bar */}
        <div className="h-10 px-6 pt-2 flex items-center justify-between text-xs text-[#18302D] font-medium z-40 bg-transparent shrink-0">
          <span className="font-inter font-bold">12:30</span>
          <div className="flex items-center gap-1.5">
            <Signal className="w-3.5 h-3.5" />
            <Wifi className="w-3.5 h-3.5" />
            <Battery className="w-4 h-4" />
          </div>
        </div>

        {/* Screen Content */}
        <div className="flex-1 overflow-hidden relative flex flex-col">
          {children}
        </div>

        {/* Android Navigation Gesture Indicator Bar */}
        <div className="h-4 bg-[#F7FAF9] flex items-center justify-center shrink-0">
          <div className="w-32 h-1 bg-[#667A77]/40 rounded-full"></div>
        </div>
      </div>
    </div>
  );
};
