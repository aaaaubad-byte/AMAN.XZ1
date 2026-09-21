import React, { useState } from 'react';
import { AndroidFrame } from './components/AndroidFrame';
import { ClientView } from './components/ClientView';
import { AdminView } from './components/AdminView';
import { SqlViewerModal } from './components/SqlViewerModal';
import { PlatformVerificationCard } from './components/PlatformVerificationCard';
import { 
  Smartphone, 
  ShieldCheck, 
  Database, 
  Layers, 
  FolderTree, 
  ExternalLink,
  Check,
  Copy
} from 'lucide-react';

export default function App() {
  const [activeApp, setActiveApp] = useState<'client' | 'admin'>('client');
  const [isSqlModalOpen, setIsSqlModalOpen] = useState(false);
  const [activeInfoTab, setActiveInfoTab] = useState<'verification' | 'files'>('verification');

  return (
    <div className="min-h-screen bg-[#0F2220] text-[#F7FAF9] flex flex-col font-cairo antialiased">
      {/* Top Bar Header */}
      <header className="border-b border-[#1F3D38] bg-[#142A27]/90 backdrop-blur-md px-6 py-3.5 flex flex-wrap items-center justify-between gap-4 sticky top-0 z-50">
        <div className="flex items-center gap-3">
          {/* Logo Badge */}
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#087F6E] to-[#19B99A] flex items-center justify-center text-white font-bold text-xl shadow-md shadow-[#087F6E]/40 border border-[#6EE7B7]/30">
            A
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-bold text-base text-white tracking-wide">AMAN | أمان</h1>
              <span className="bg-[#087F6E]/40 border border-[#6EE7B7]/40 text-[#6EE7B7] text-[10px] font-bold px-2 py-0.5 rounded-full">
                Native Android Application (APK)
              </span>
            </div>
            <p className="text-xs text-[#6EE7B7]/80">
              أمان حماية وضمان • متصل مباشرة مع Supabase
            </p>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2.5 flex-wrap">
          {/* App Switcher Tabs */}
          <div className="bg-[#0c1c1a] p-1 rounded-xl border border-[#203D38] flex items-center gap-1">
            <button
              onClick={() => setActiveApp('client')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                activeApp === 'client'
                  ? 'bg-[#087F6E] text-white shadow-sm'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>تطبيق العميل</span>
            </button>
            <button
              onClick={() => setActiveApp('admin')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                activeApp === 'admin'
                  ? 'bg-[#087F6E] text-white shadow-sm'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>تطبيق الإدارة</span>
            </button>
          </div>

          {/* SQL Button */}
          <button
            onClick={() => setIsSqlModalOpen(true)}
            className="bg-[#1B3632] hover:bg-[#234540] border border-[#2B544E] text-white px-3.5 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition"
          >
            <Database className="w-3.5 h-3.5 text-[#6EE7B7]" />
            <span>أوامر Supabase SQL (المرحلة 1)</span>
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* Left/Main Column: Native Mobile Simulator */}
        <div className="lg:col-span-6 flex flex-col items-center justify-center">
          <div className="w-full flex items-center justify-between px-2 mb-2">
            <div className="text-xs font-bold text-gray-300 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-[#19B99A] animate-pulse"></span>
              <span>معاينة واجهة الأندرويد الحية (Mobile APK Experience)</span>
            </div>
            <span className="text-[11px] text-gray-400">
              {activeApp === 'client' ? 'وضع العميل (Client)' : 'وضع الإدارة (Admin)'}
            </span>
          </div>

          {/* Android Device */}
          <AndroidFrame activeApp={activeApp} onSwitchApp={setActiveApp}>
            {activeApp === 'client' ? <ClientView /> : <AdminView />}
          </AndroidFrame>
        </div>

        {/* Right Column: Android Project Architecture & Supabase Verification */}
        <div className="lg:col-span-6 space-y-4">
          
          {/* Sub Navigation Tabs */}
          <div className="flex items-center gap-2 border-b border-[#1F3D38] pb-2">
            <button
              onClick={() => setActiveInfoTab('verification')}
              className={`pb-2 px-1 text-xs font-bold border-b-2 transition ${
                activeInfoTab === 'verification'
                  ? 'border-[#6EE7B7] text-[#6EE7B7]'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              تقرير التحقق من منصة أندرويد (Stage 1)
            </button>
            <button
              onClick={() => setActiveInfoTab('files')}
              className={`pb-2 px-1 text-xs font-bold border-b-2 transition ${
                activeInfoTab === 'files'
                  ? 'border-[#6EE7B7] text-[#6EE7B7]'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              هيكل ملفات مشروع الأندرويد الأصلي (Project Tree)
            </button>
          </div>

          {activeInfoTab === 'verification' ? (
            <div className="space-y-4">
              <PlatformVerificationCard />

              {/* Supabase Schema Card */}
              <div className="bg-[#18302D] border border-[#2A4B46] rounded-2xl p-4 text-white shadow-xl">
                <div className="flex items-center justify-between pb-3 border-b border-[#2A4B46]">
                  <div className="flex items-center gap-2">
                    <Database className="w-4 h-4 text-[#6EE7B7]" />
                    <h4 className="font-bold text-sm">مخطط قاعدة بيانات Supabase (المرحلة 1)</h4>
                  </div>
                  <button
                    onClick={() => setIsSqlModalOpen(true)}
                    className="text-xs text-[#6EE7B7] hover:underline font-semibold"
                  >
                    عرض ونسخ كود SQL كامل
                  </button>
                </div>
                
                <div className="grid grid-cols-2 gap-2 text-xs mt-3">
                  <div className="bg-[#122422] p-2.5 rounded-xl border border-[#2A4B46]/60">
                    <span className="font-bold text-[#6EE7B7] block mb-1">الجداول الأساسية (Tables)</span>
                    <ul className="text-gray-300 space-y-1 text-[11px]">
                      <li>• <code>profiles</code> (بيانات العملاء والمدراء)</li>
                      <li>• <code>telecom_companies</code> (يمن موبايل، يو، سبأفون، واي)</li>
                      <li>• <code>protection_packages</code> (باقات الحماية والمدد)</li>
                      <li>• <code>customer_numbers</code> (أرقام العملاء المسجلة)</li>
                    </ul>
                  </div>

                  <div className="bg-[#122422] p-2.5 rounded-xl border border-[#2A4B46]/60">
                    <span className="font-bold text-[#6EE7B7] block mb-1">العمليات والمهام (Operations)</span>
                    <ul className="text-gray-300 space-y-1 text-[11px]">
                      <li>• <code>protection_requests</code> (طلبات وسندات الدفع)</li>
                      <li>• <code>protections</code> (الحمايات الفعالة وتواريخها)</li>
                      <li>• <code>protection_tasks</code> (مهام التجديد والفحص)</li>
                      <li>• <code>audit_logs</code> (سجل العمليات الإدارية)</li>
                    </ul>
                  </div>
                </div>

                <div className="mt-3 pt-3 border-t border-[#2A4B46] text-xs text-gray-300 flex items-center justify-between">
                  <span>تم تطبيق سياسات الأمان (RLS) ومحفزات التسجيل التلقائي.</span>
                  <button 
                    onClick={() => setIsSqlModalOpen(true)}
                    className="bg-[#087F6E] px-3 py-1 rounded-lg text-white font-bold text-[11px]"
                  >
                    فتح محرر SQL
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-[#18302D] border border-[#2A4B46] rounded-2xl p-4 text-white shadow-xl space-y-3 font-mono text-xs">
              <div className="flex items-center justify-between pb-2 border-b border-[#2A4B46]">
                <span className="font-bold text-[#6EE7B7] font-cairo">شجرة مشروع أندرويد الأصلي (AMAN.XZ1)</span>
                <span className="text-[11px] text-gray-400">Kotlin & Jetpack Compose</span>
              </div>
              <div className="bg-[#0d1a18] p-3 rounded-xl border border-[#2A4B46] text-gray-300 leading-relaxed overflow-x-auto text-[11px] dir-ltr">
                <pre>{`AMAN/
├── settings.gradle.kts (Plugin & Repository Management)
├── build.gradle.kts (Root Android & Kotlin plugins)
├── gradle.properties (JVM args, AndroidX, Non-transitive R)
├── gradlew & gradlew.bat (Gradle Wrapper executables)
├── gradle/wrapper/gradle-wrapper.properties (Gradle 8.7)
├── supabase/
│   ├── schema.sql (Stage 1 Tables, RLS, Functions)
│   └── seed.sql (Telecom Companies & Packages)
└── app/
    ├── build.gradle.kts (CompileSdk 34, Compose, Supabase Kotlin SDK)
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml (RTL, Permissions, Launcher)
            ├── res/
            │   ├── values/
            │   │   ├── colors.xml (#087F6E, #19B99A, #F7FAF9)
            │   │   ├── strings.xml (Arabic strings, AMAN Slogan)
            │   │   └── themes.xml (Material3 Theme.AMAN)
            │   └── xml/ (data_extraction_rules & backup_rules)
            └── kotlin/com/aman/app/
                ├── AmanApplication.kt (Supabase Android init)
                ├── MainActivity.kt (Compose entry point)
                ├── data/
                │   ├── remote/SupabaseClient.kt (Official SDK)
                │   ├── model/Models.kt (Serializable Entities)
                │   └── repository/ (AuthRepository, AmanRepository)
                └── ui/
                    ├── theme/ (Color.kt, Type.kt, Theme.kt)
                    ├── navigation/ (Screen.kt, AmanNavGraph.kt)
                    ├── components/ (AmanComponents.kt)
                    └── screens/ (Splash, Login, ClientHome, AdminHome)`}</pre>
              </div>
              <p className="text-[11px] font-cairo text-gray-400">
                المشروع جاهز تماماً للفتح في بيئة Android Studio أو تجميعه عبر أداة Gradle إلى ملفات APK / AAB.
              </p>
            </div>
          )}

        </div>

      </main>

      {/* SQL Code Modal */}
      <SqlViewerModal isOpen={isSqlModalOpen} onClose={() => setIsSqlModalOpen(false)} />
    </div>
  );
}
