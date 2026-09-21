import React from 'react';
import { ShieldCheck, AlertTriangle, CheckCircle2, FileCode, Server } from 'lucide-react';

export const PlatformVerificationCard: React.FC = () => {
  const checks = [
    { name: 'Genuine Android Gradle project', status: 'VERIFIED', evidence: 'settings.gradle.kts, root build.gradle.kts, gradlew, gradle-wrapper.properties' },
    { name: 'Android application module', status: 'VERIFIED', evidence: 'app/build.gradle.kts configured with com.android.application (SDK 34)' },
    { name: 'AndroidManifest.xml', status: 'VERIFIED', evidence: 'app/src/main/AndroidManifest.xml with RTL, AmanApplication, permissions' },
    { name: 'Kotlin Android source', status: 'VERIFIED', evidence: 'app/src/main/kotlin/com/aman/app/ (AmanApplication, MainActivity)' },
    { name: 'Android UI framework', status: 'VERIFIED', evidence: 'Jetpack Compose (BOM 2024.09.00, Material3, Cairo typography)' },
    { name: 'Android navigation', status: 'VERIFIED', evidence: 'Navigation Compose (NavHost, AmanNavGraph, Screen sealed routes)' },
    { name: 'Android ViewModel/state architecture', status: 'VERIFIED', evidence: 'StateFlow, Compose Reactive state, Model entities (Models.kt)' },
    { name: 'Supabase Android integration', status: 'VERIFIED', evidence: 'io.github.jan-tennert.supabase (Auth, Postgrest, Realtime, Storage)' },
    { name: 'APK build capability', status: 'BLOCKED', evidence: 'Linux container environment lacks Java/Android SDK (java: not found)' },
    { name: 'AAB build capability', status: 'BLOCKED', evidence: 'Linux container environment lacks Java/Android SDK (java: not found)' },
  ];

  return (
    <div className="bg-[#18302D] border border-[#2A4B46] rounded-2xl p-4 text-white shadow-xl">
      <div className="flex items-center justify-between pb-3 border-b border-[#2A4B46]">
        <div className="flex items-center gap-2">
          <ShieldCheck className="w-5 h-5 text-[#6EE7B7]" />
          <h3 className="font-bold text-sm">ANDROID PLATFORM VERIFICATION — STAGE 1</h3>
        </div>
        <span className="text-[11px] bg-[#087F6E] px-2 py-0.5 rounded text-emerald-100 font-mono">
          AMAN.XZ1 Native
        </span>
      </div>

      <div className="mt-3 overflow-x-auto">
        <table className="w-full text-xs text-right">
          <thead>
            <tr className="border-b border-[#2A4B46] text-gray-400 font-semibold">
              <th className="pb-2 text-right">Check</th>
              <th className="pb-2 text-center w-28">Status</th>
              <th className="pb-2 text-right pr-3">Evidence</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#2A4B46]/60">
            {checks.map((row, idx) => (
              <tr key={idx} className="hover:bg-[#1f3a35]/40 transition">
                <td className="py-2.5 font-medium text-gray-200">{row.name}</td>
                <td className="py-2.5 text-center">
                  {row.status === 'VERIFIED' ? (
                    <span className="bg-[#059669]/20 border border-[#059669]/40 text-[#6EE7B7] text-[10px] font-bold px-2 py-0.5 rounded-full inline-flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3" />
                      VERIFIED
                    </span>
                  ) : (
                    <span className="bg-[#D97706]/20 border border-[#D97706]/40 text-[#FBBF24] text-[10px] font-bold px-2 py-0.5 rounded-full inline-flex items-center gap-1">
                      <AlertTriangle className="w-3 h-3" />
                      BLOCKED
                    </span>
                  )}
                </td>
                <td className="py-2.5 text-[11px] text-gray-400 font-mono pr-3">
                  {row.evidence}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-3 pt-3 border-t border-[#2A4B46] text-[11px] text-gray-400 flex items-center justify-between">
        <span>* تم حفظ جميع ملفات مشروع الأندرويد الأصلي وجاهزة للفتح في <strong>Android Studio</strong> أو تصديرها كـ ZIP.</span>
      </div>
    </div>
  );
};
