import React, { useState } from 'react';
import { 
  Shield, 
  Phone, 
  Plus, 
  Bell, 
  CheckCircle2, 
  Clock, 
  ChevronLeft, 
  User, 
  CreditCard, 
  Check, 
  X, 
  Menu, 
  Home, 
  FileText, 
  Settings, 
  HelpCircle, 
  FileCheck, 
  Info, 
  LogOut,
  Sparkles,
  RefreshCw,
  Search
} from 'lucide-react';

interface NumberItem {
  id: string;
  number: string;
  company: string;
  status: 'protected' | 'pending' | 'unprotected';
  expireDate?: string;
  planName?: string;
}

export const ClientView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'home' | 'numbers' | 'protections' | 'requests' | 'settings'>('home');
  const [showDrawer, setShowDrawer] = useState(false);
  const [showPaymentSheet, setShowPaymentSheet] = useState(false);
  const [selectedPayment, setSelectedPayment] = useState('jawali');
  const [requestSuccess, setRequestSuccess] = useState(false);
  const [newPhone, setNewPhone] = useState('77 444 8899');
  const [searchQuery, setSearchQuery] = useState('');

  const [numbers, setNumbers] = useState<NumberItem[]>([
    { id: '1', number: '+967 77 123 4567', company: 'يمن موبايل', status: 'protected', expireDate: '2027-03-15', planName: 'باقة الحماية السنوية الكاملة' },
    { id: '2', number: '+967 73 987 6543', company: 'يو (YOU)', status: 'pending', planName: 'باقة الحماية نصف السنوية' },
    { id: '3', number: '+967 71 555 8899', company: 'سبأفون', status: 'protected', expireDate: '2026-11-20', planName: 'باقة الحماية الربعية' },
    { id: '4', number: '+967 70 222 3344', company: 'واي (Y Telecom)', status: 'protected', expireDate: '2027-01-10', planName: 'باقة الحماية السنوية' },
  ]);

  const handleCreateRequest = () => {
    setShowPaymentSheet(false);
    setRequestSuccess(true);
    setTimeout(() => {
      setRequestSuccess(false);
    }, 4000);
  };

  const navToRoute = (tab: 'home' | 'numbers' | 'protections' | 'requests' | 'settings') => {
    setActiveTab(tab);
    setShowDrawer(false);
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-[#F7FAF9] text-[#18302D] relative overflow-hidden font-cairo select-none">
      
      {/* Top Bar (AmanTopAppBar visual equivalence) */}
      <header className="px-4 py-3 bg-white border-b border-[#DCE9E6] flex items-center justify-between shrink-0 z-20 shadow-xs">
        <div className="flex items-center gap-3">
          <button 
            onClick={() => setShowDrawer(true)}
            aria-label="فتح القائمة الجانبية"
            className="w-9 h-9 rounded-xl bg-[#F7FAF9] hover:bg-[#E9F8F5] border border-[#DCE9E6] flex items-center justify-center text-[#087F6E] transition active:scale-95"
          >
            <Menu className="w-5 h-5" />
          </button>
          
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-[#087F6E] text-white font-bold flex items-center justify-center text-sm shadow-xs">
              أ
            </div>
            <div>
              <h1 className="font-bold text-sm text-[#18302D] leading-tight">AMAN | أمان</h1>
              <p className="text-[10px] text-[#087F6E] font-medium">أمان حماية وضمان</p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button 
            onClick={() => navToRoute('requests')}
            className="relative w-9 h-9 rounded-xl bg-[#F7FAF9] hover:bg-[#E9F8F5] border border-[#DCE9E6] flex items-center justify-center text-[#18302D] transition active:scale-95"
          >
            <Bell className="w-4 h-4 text-[#087F6E]" />
            <span className="absolute -top-1 -right-1 w-4 h-4 bg-[#DC2626] text-white text-[9px] font-bold rounded-full flex items-center justify-center border-2 border-white">
              2
            </span>
          </button>
        </div>
      </header>

      {/* Side Navigation Drawer (75-82% viewport width) */}
      {showDrawer && (
        <div className="absolute inset-0 z-50 flex">
          {/* Backdrop */}
          <div 
            className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity duration-200"
            onClick={() => setShowDrawer(false)}
          />

          {/* Drawer Sheet (80% width) */}
          <aside className="relative w-[80%] max-w-[320px] bg-white h-full shadow-2xl flex flex-col z-10 animate-in slide-in-from-right duration-250 border-l border-[#DCE9E6]">
            {/* Drawer Header */}
            <div className="p-5 bg-gradient-to-br from-[#087F6E] to-[#065B4F] text-white">
              <div className="flex items-center justify-between">
                <div className="w-12 h-12 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center text-white font-bold text-lg border border-white/30">
                  ع
                </div>
                <button 
                  onClick={() => setShowDrawer(false)}
                  className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="mt-3">
                <h3 className="font-bold text-base leading-tight">أحمد محمد حميد</h3>
                <p className="text-xs text-emerald-100 mt-0.5">عميل أمان الموثق • اليمن</p>
                <div className="inline-flex items-center gap-1.5 mt-2 px-2.5 py-0.5 rounded-full bg-[#6EE7B7]/20 border border-[#6EE7B7]/40 text-[#6EE7B7] text-[10px] font-bold">
                  <Shield className="w-3 h-3" />
                  <span>الحساب نشط ومحمي</span>
                </div>
              </div>
            </div>

            {/* Drawer Navigation Links */}
            <nav className="flex-1 overflow-y-auto py-3 px-3 space-y-1">
              {[
                { id: 'home', label: 'الرئيسية', icon: Home },
                { id: 'numbers', label: 'أرقامي المسجلة', icon: Phone },
                { id: 'protections', label: 'حماياتي النشطة', icon: Shield },
                { id: 'requests', label: 'طلبات الحماية', icon: FileText, badge: '2' },
                { id: 'settings', label: 'إعدادات الحساب', icon: Settings },
              ].map((item) => {
                const IconComp = item.icon;
                const isSelected = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => navToRoute(item.id as any)}
                    className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                      isSelected 
                        ? 'bg-[#E9F8F5] text-[#087F6E] font-bold' 
                        : 'text-[#18302D] hover:bg-[#F7FAF9]'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <IconComp className={`w-4 h-4 ${isSelected ? 'text-[#087F6E]' : 'text-[#667A77]'}`} />
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-[#FEF3C7] text-[#D97706]">
                        {item.badge}
                      </span>
                    )}
                  </button>
                );
              })}

              <div className="my-2 border-t border-[#DCE9E6]/60"></div>

              {[
                { label: 'المساعدة والدعم', icon: HelpCircle },
                { label: 'الشروط والأحكام', icon: FileCheck },
                { label: 'عن تطبيق أمان', icon: Info },
              ].map((item, idx) => {
                const SubIcon = item.icon;
                return (
                  <button
                    key={idx}
                    onClick={() => setShowDrawer(false)}
                    className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs text-[#667A77] hover:text-[#18302D] hover:bg-[#F7FAF9] transition"
                  >
                    <SubIcon className="w-4 h-4 text-[#667A77]" />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>

            {/* Drawer Footer */}
            <div className="p-3 border-t border-[#DCE9E6] bg-[#F7FAF9] flex items-center justify-between text-xs">
              <span className="text-[11px] text-[#667A77]">إصدار 1.0.0 (رسمي)</span>
              <button 
                onClick={() => setShowDrawer(false)}
                className="flex items-center gap-1 text-[#DC2626] font-bold hover:underline"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>خروج</span>
              </button>
            </div>
          </aside>
        </div>
      )}

      {/* Main Screen Content Viewport */}
      <main className="flex-1 overflow-y-auto px-4 py-3 space-y-4 pb-24">
        
        {/* Success Alert Banner */}
        {requestSuccess && (
          <div className="bg-[#D1FAE5] border border-[#059669]/30 p-3.5 rounded-2xl flex items-center gap-3 animate-in fade-in slide-in-from-top-2 shadow-xs">
            <div className="w-8 h-8 rounded-full bg-[#059669] text-white flex items-center justify-center shrink-0">
              <Check className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-bold text-[#065F46]">تم تقديم طلب الحماية بنجاح!</div>
              <div className="text-[11px] text-[#065F46]/80 mt-0.5 leading-relaxed">
                تم ربط الطلب برقم المعاملة في قاعدة بيانات Supabase. سيتم تفعيله فوراً بعد التحقق.
              </div>
            </div>
          </div>
        )}

        {/* Tab 1: HOME DASHBOARD */}
        {activeTab === 'home' && (
          <div className="space-y-4">
            {/* Protection Hero Banner */}
            <div className="rounded-2xl p-4 text-white shadow-md bg-gradient-to-l from-[#087F6E] via-[#098C7A] to-[#19B99A] relative overflow-hidden">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-[#6EE7B7]" />
                  <span className="text-xs font-semibold text-emerald-100">درع الحماية الشامل</span>
                </div>
                <span className="text-[10px] bg-white/20 backdrop-blur-xs px-2.5 py-0.5 rounded-full font-bold border border-white/30">
                  نشط 100%
                </span>
              </div>

              <div className="mt-3">
                <div className="text-2xl font-bold font-cairo">4 أرقام مؤمنة بالكامل</div>
                <p className="text-xs text-emerald-50 mt-1 leading-relaxed">
                  أرقامك محمية من إعادة البيع، السحب، أو إسقاط الملكية في شبكات الاتصالات اليمنية.
                </p>
              </div>

              <div className="mt-4 pt-3 border-t border-white/20 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1 text-emerald-100">
                  <RefreshCw className="w-3 h-3 animate-spin" />
                  <span>أقرب مهمة تجديد:</span>
                </div>
                <span className="font-bold text-white">20 أكتوبر 2026</span>
              </div>
            </div>

            {/* Quick Action Button */}
            <button 
              onClick={() => setShowPaymentSheet(true)}
              className="w-full bg-[#087F6E] hover:bg-[#065B4F] text-white py-3.5 px-4 rounded-xl font-bold text-sm shadow-md shadow-[#087F6E]/20 flex items-center justify-center gap-2 transition active:scale-[0.99]"
            >
              <Plus className="w-4 h-4" />
              <span>تقديم طلب حماية لرقم جديد</span>
            </button>

            {/* Numbers List Overview */}
            <div className="space-y-2.5">
              <div className="flex items-center justify-between pt-1">
                <div className="flex items-center gap-1.5">
                  <Phone className="w-4 h-4 text-[#087F6E]" />
                  <h2 className="font-bold text-sm text-[#18302D]">أرقامي وحالات الحماية</h2>
                </div>
                <button 
                  onClick={() => navToRoute('numbers')}
                  className="text-xs text-[#087F6E] font-semibold hover:underline"
                >
                  عرض الكل (4)
                </button>
              </div>

              {numbers.slice(0, 3).map((item) => (
                <div 
                  key={item.id} 
                  className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 shadow-xs hover:border-[#087F6E]/40 transition"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        item.status === 'protected' ? 'bg-[#E9F8F5] text-[#087F6E]' : 'bg-[#FEF3C7] text-[#D97706]'
                      }`}>
                        {item.status === 'protected' ? <Shield className="w-5 h-5" /> : <Clock className="w-5 h-5" />}
                      </div>
                      <div>
                        <div className="font-inter font-bold text-sm text-[#18302D] dir-ltr text-right">
                          {item.number}
                        </div>
                        <div className="text-xs text-[#667A77] mt-0.5">
                          {item.company} • {item.planName}
                        </div>
                      </div>
                    </div>

                    <div>
                      {item.status === 'protected' ? (
                        <span className="bg-[#E9F8F5] text-[#087F6E] text-[11px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          محمي
                        </span>
                      ) : (
                        <span className="bg-[#FEF3C7] text-[#D97706] text-[11px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          قيد المراجعة
                        </span>
                      )}
                    </div>
                  </div>

                  {item.expireDate && (
                    <div className="mt-3 pt-2.5 border-t border-[#DCE9E6]/60 flex items-center justify-between text-[11px] text-[#667A77]">
                      <span>ينتهي التجديد في: <strong className="text-[#18302D] font-inter">{item.expireDate}</strong></span>
                      <button 
                        onClick={() => navToRoute('protections')}
                        className="text-[#087F6E] font-semibold hover:underline"
                      >
                        عرض السند
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Informational Guarantee Card */}
            <div className="bg-[#E9F8F5] border border-[#6EE7B7]/50 rounded-2xl p-4 text-xs text-[#18302D] space-y-1.5">
              <div className="font-bold flex items-center gap-2 text-[#087F6E]">
                <Shield className="w-4 h-4" />
                <span>نظام الضمان الآلي لتطبيق أمان</span>
              </div>
              <p className="text-[#667A77] leading-relaxed">
                يقوم فريق المشرفين والأنظمة الآلية بإجراء عمليات الشحن والتنشيط الدورية كل 30 يوماً لدى شركات الاتصالات المعتمدة باليمن لضمان استمرارية خطك دون انقطاع.
              </p>
            </div>
          </div>
        )}

        {/* Tab 2: ALL NUMBERS SCREEN */}
        {activeTab === 'numbers' && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-sm text-[#18302D]">أرقامي المسجلة (4 أرقام)</h2>
              <button 
                onClick={() => setShowPaymentSheet(true)}
                className="bg-[#087F6E] text-white text-xs font-bold px-3 py-1.5 rounded-lg flex items-center gap-1 shadow-xs"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>إضافة رقم</span>
              </button>
            </div>

            {numbers.map((item) => (
              <div key={item.id} className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 shadow-xs">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-inter font-bold text-sm text-[#18302D] dir-ltr text-right">
                      {item.number}
                    </div>
                    <div className="text-xs text-[#667A77] mt-0.5">{item.company}</div>
                  </div>
                  <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${
                    item.status === 'protected' ? 'bg-[#E9F8F5] text-[#087F6E]' : 'bg-[#FEF3C7] text-[#D97706]'
                  }`}>
                    {item.status === 'protected' ? 'محمي ونشط' : 'قيد التدقيق'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Tab 3: PROTECTIONS */}
        {activeTab === 'protections' && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-sm text-[#18302D]">حماياتي وسندات الاشتراك</h2>
              <span className="text-xs text-[#667A77]">3 حمايات سارية</span>
            </div>

            {numbers.filter(n => n.status === 'protected').map(item => (
              <div key={item.id} className="bg-white border border-[#DCE9E6] rounded-2xl p-4 shadow-xs space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-[#087F6E] bg-[#E9F8F5] px-2.5 py-1 rounded-lg">
                    {item.company}
                  </span>
                  <span className="text-xs text-[#667A77] font-inter">حتى {item.expireDate}</span>
                </div>
                <div className="font-inter font-bold text-base text-[#18302D] dir-ltr text-right">
                  {item.number}
                </div>
                <p className="text-xs text-[#667A77]">{item.planName}</p>
              </div>
            ))}
          </div>
        )}

        {/* Tab 4: REQUESTS */}
        {activeTab === 'requests' && (
          <div className="space-y-3">
            <h2 className="font-bold text-sm text-[#18302D]">طلبات الحماية وسجل العمليات</h2>
            
            <div className="bg-white border border-[#DCE9E6] rounded-2xl p-4 shadow-xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bold text-xs text-[#D97706] bg-[#FEF3C7] px-2.5 py-0.5 rounded-full">
                  قيد المراجعة
                </span>
                <span className="text-xs text-[#667A77]">أمس، 04:30 م</span>
              </div>
              <div className="font-inter font-bold text-sm text-[#18302D] dir-ltr text-right">
                +967 73 987 6543
              </div>
              <div className="text-xs text-[#667A77]">
                يو (YOU) • باقة الحماية نصف السنوية (11,000 ريال) • محفظة جوالي
              </div>
            </div>

            <div className="bg-white border border-[#DCE9E6] rounded-2xl p-4 shadow-xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bold text-xs text-[#059669] bg-[#D1FAE5] px-2.5 py-0.5 rounded-full">
                  مكتمل ومفعل
                </span>
                <span className="text-xs text-[#667A77]">15 مارس 2026</span>
              </div>
              <div className="font-inter font-bold text-sm text-[#18302D] dir-ltr text-right">
                +967 77 123 4567
              </div>
              <div className="text-xs text-[#667A77]">
                يمن موبايل • باقة الحماية السنوية الكاملة (20,000 ريال) • ون كاش
              </div>
            </div>
          </div>
        )}

        {/* Tab 5: SETTINGS */}
        {activeTab === 'settings' && (
          <div className="space-y-3">
            <h2 className="font-bold text-sm text-[#18302D]">إعدادات الحساب والتوثيق</h2>
            <div className="bg-white border border-[#DCE9E6] rounded-2xl p-4 shadow-xs space-y-3">
              <div>
                <label className="text-xs text-[#667A77]">الاسم الكامل</label>
                <div className="font-bold text-sm text-[#18302D] mt-0.5">أحمد محمد حميد</div>
              </div>
              <div>
                <label className="text-xs text-[#667A77]">رقم الهوية / الجواز</label>
                <div className="font-bold text-sm text-[#18302D] mt-0.5">01020304050 (موثق)</div>
              </div>
              <div>
                <label className="text-xs text-[#667A77]">تنبيهات الرسائل والتجديد</label>
                <div className="text-xs text-[#087F6E] font-bold mt-0.5">مفعلة عبر الرسائل القصيرة SMS</div>
              </div>
            </div>
          </div>
        )}

      </main>

      {/* Modern Bottom Navigation Bar (5 tabs with Center Action) */}
      <nav className="absolute bottom-0 inset-x-0 bg-white border-t border-[#DCE9E6] px-2 py-1.5 flex items-center justify-around z-30 shadow-lg">
        <button 
          onClick={() => navToRoute('home')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'home' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Home className="w-5 h-5" />
          <span className="mt-0.5">الرئيسية</span>
        </button>

        <button 
          onClick={() => navToRoute('numbers')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'numbers' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Phone className="w-5 h-5" />
          <span className="mt-0.5">أرقامي</span>
        </button>

        {/* Center Floating Action Button */}
        <button 
          onClick={() => setShowPaymentSheet(true)}
          className="flex flex-col items-center -mt-5 focus:outline-none"
          aria-label="تقديم طلب حماية جديد"
        >
          <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-[#087F6E] to-[#19B99A] text-white flex items-center justify-center shadow-lg shadow-[#087F6E]/40 border-2 border-white transition active:scale-95">
            <Plus className="w-6 h-6" />
          </div>
          <span className="text-[10px] font-bold text-[#087F6E] mt-0.5">طلب جديد</span>
        </button>

        <button 
          onClick={() => navToRoute('protections')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'protections' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Shield className="w-5 h-5" />
          <span className="mt-0.5">الحمايات</span>
        </button>

        <button 
          onClick={() => navToRoute('settings')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'settings' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Settings className="w-5 h-5" />
          <span className="mt-0.5">الإعدادات</span>
        </button>
      </nav>

      {/* Bottom Sheet Modal: Payment & Protection Request */}
      {showPaymentSheet && (
        <div className="absolute inset-0 bg-black/50 z-50 flex flex-col justify-end animate-in fade-in duration-200">
          <div className="bg-white rounded-t-3xl p-5 space-y-4 max-h-[90%] overflow-y-auto animate-in slide-in-from-bottom duration-250 border-t border-[#DCE9E6]">
            
            <div className="flex items-center justify-between pb-2 border-b border-[#DCE9E6]">
              <div>
                <h3 className="font-bold text-base text-[#18302D]">طلب حماية رقم جديد</h3>
                <p className="text-xs text-[#667A77]">حدد المشغل وطريقة الدفع المعتمدة باليمن</p>
              </div>
              <button 
                onClick={() => setShowPaymentSheet(false)}
                className="w-8 h-8 rounded-full bg-[#F7FAF9] flex items-center justify-center text-[#667A77]"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Inputs */}
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-[#18302D] mb-1">رقم الهاتف المراد حمايته</label>
                <div className="relative">
                  <input 
                    type="tel" 
                    value={newPhone}
                    onChange={(e) => setNewPhone(e.target.value)}
                    placeholder="77 123 4567" 
                    className="w-full bg-[#F7FAF9] border border-[#DCE9E6] rounded-xl px-3 py-2.5 text-sm font-inter text-[#18302D] pl-16 focus:outline-none focus:border-[#087F6E]"
                  />
                  <div className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-[#667A77] font-semibold border-r border-[#DCE9E6] pr-2 font-inter">
                    +967
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#18302D] mb-1">باقة الحماية المختارة</label>
                <select className="w-full bg-[#F7FAF9] border border-[#DCE9E6] rounded-xl px-3 py-2.5 text-xs text-[#18302D] focus:outline-none focus:border-[#087F6E]">
                  <option>باقة الحماية السنوية الكاملة — 20,000 ريال</option>
                  <option>باقة الحماية نصف السنوية — 11,000 ريال</option>
                  <option>باقة الحماية الربعية (3 أشهر) — 6,000 ريال</option>
                </select>
              </div>
            </div>

            {/* Yemeni Payment Gateways */}
            <div className="space-y-2 pt-1">
              <label className="block text-xs font-bold text-[#18302D]">طرق الدفع الإلكترونية المعتمدة</label>
              
              {[
                { id: 'mobile_money', name: 'محفظة موبايل موني (Mobile Money)' },
                { id: 'jawali', name: 'محفظة جوالي (Jawali)' },
                { id: 'flousk', name: 'محفظة فلوسك (Flousk)' },
                { id: 'one_cash', name: 'محفظة ون كاش (OneCash)' },
              ].map((method) => (
                <div 
                  key={method.id}
                  onClick={() => setSelectedPayment(method.id)}
                  className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition ${
                    selectedPayment === method.id 
                      ? 'border-[#087F6E] bg-[#E9F8F5]' 
                      : 'border-[#DCE9E6] bg-white hover:bg-[#F7FAF9]'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <CreditCard className="w-4 h-4 text-[#087F6E]" />
                    <span className="text-xs font-medium text-[#18302D]">{method.name}</span>
                  </div>
                  <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                    selectedPayment === method.id ? 'border-[#087F6E] bg-[#087F6E]' : 'border-[#DCE9E6]'
                  }`}>
                    {selectedPayment === method.id && <div className="w-1.5 h-1.5 rounded-full bg-white"></div>}
                  </div>
                </div>
              ))}
            </div>

            {/* Submit Action */}
            <div className="pt-2">
              <button 
                onClick={handleCreateRequest}
                className="w-full bg-[#087F6E] hover:bg-[#065B4F] text-white py-3.5 rounded-xl font-bold text-sm shadow-md shadow-[#087F6E]/30 transition active:scale-[0.99]"
              >
                تأكيد وإرسال طلب الحماية
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};
