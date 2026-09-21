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
  Smartphone
} from 'lucide-react';

interface NumberItem {
  id: string;
  number: string;
  company: string;
  status: 'protected' | 'pending' | 'unprotected';
  expireDate?: string;
}

export const ClientView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'home' | 'numbers' | 'requests' | 'profile'>('home');
  const [showPaymentSheet, setShowPaymentSheet] = useState(false);
  const [selectedPayment, setSelectedPayment] = useState('jawali');
  const [requestSuccess, setRequestSuccess] = useState(false);

  const [numbers, setNumbers] = useState<NumberItem[]>([
    { id: '1', number: '+967 77 123 4567', company: 'يمن موبايل', status: 'protected', expireDate: '2027-03-15' },
    { id: '2', number: '+967 73 987 6543', company: 'يو (YOU)', status: 'pending' },
    { id: '3', number: '+967 71 555 8899', company: 'سبأفون', status: 'protected', expireDate: '2026-11-20' },
  ]);

  const handleCreateRequest = () => {
    setShowPaymentSheet(false);
    setRequestSuccess(true);
    setTimeout(() => {
      setRequestSuccess(false);
    }, 3500);
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-[#F7FAF9] text-[#18302D] relative overflow-hidden">
      {/* Top Bar */}
      <div className="px-5 py-3 bg-white border-b border-[#DCE9E6] flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#E9F8F5] flex items-center justify-center font-bold text-[#087F6E]">
            ع
          </div>
          <div>
            <div className="font-bold text-sm text-[#18302D]">أحمد محمد</div>
            <div className="text-xs text-[#667A77]">عميل أمان • حساب موثق</div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button className="w-9 h-9 rounded-full bg-[#F7FAF9] border border-[#DCE9E6] flex items-center justify-center text-[#18302D] hover:bg-[#E9F8F5] transition">
            <Bell className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Main Scrollable Content */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-4 pb-20">
        
        {/* Success Alert */}
        {requestSuccess && (
          <div className="bg-[#D1FAE5] border border-[#A7F3D0] p-3 rounded-2xl flex items-center gap-3 animate-in fade-in slide-in-from-top-2">
            <div className="w-7 h-7 rounded-full bg-[#059669] text-white flex items-center justify-center shrink-0">
              <Check className="w-4 h-4" />
            </div>
            <div className="text-xs text-[#065F46] font-medium leading-relaxed">
              تم إرسال طلب الحماية بنجاح! جاري المراجعة والربط مع قاعدة بيانات Supabase.
            </div>
          </div>
        )}

        {/* Protection Summary Banner (From Reference) */}
        <div className="rounded-2xl p-4 text-white shadow-sm bg-gradient-to-l from-[#087F6E] to-[#19B99A] relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-emerald-100">ملخص الحماية</span>
            <span className="text-[11px] bg-white/20 backdrop-blur-sm px-2.5 py-0.5 rounded-full font-bold">
              نشط 100%
            </span>
          </div>

          <div className="mt-3">
            <div className="text-2xl font-bold">3 أرقام مسجلة</div>
            <p className="text-xs text-emerald-50 mt-1">
              جميع الأرقام خاضعة لجدولة التجديد التلقائي ومحمية من السحب أو الإيقاف.
            </p>
          </div>

          <div className="mt-4 pt-3 border-t border-white/15 flex items-center justify-between text-xs">
            <span className="text-emerald-100">أقرب موعد تجديد:</span>
            <span className="font-bold">20 أكتوبر 2026</span>
          </div>
        </div>

        {/* Quick Action Button */}
        <button 
          onClick={() => setShowPaymentSheet(true)}
          className="w-full bg-[#087F6E] hover:bg-[#065B4F] text-white py-3 px-4 rounded-xl font-bold text-sm shadow-sm flex items-center justify-center gap-2 transition"
        >
          <Plus className="w-4 h-4" />
          <span>طلب حماية لرقم جديد</span>
        </button>

        {/* Numbers Section Header */}
        <div className="flex items-center justify-between pt-1">
          <h2 className="font-bold text-sm text-[#18302D]">أرقامي وحالات الحماية</h2>
          <span className="text-xs text-[#087F6E] font-semibold cursor-pointer">سجل العمليات</span>
        </div>

        {/* Numbers Cards */}
        <div className="space-y-2.5">
          {numbers.map((item) => (
            <div 
              key={item.id} 
              className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 shadow-sm hover:border-[#087F6E]/40 transition"
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
                      {item.company} • {item.status === 'protected' ? 'باقة أمان السنوية' : 'طلب قيد المراجعة'}
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
                  <span>ينتهي الاشتراك في: {item.expireDate}</span>
                  <span className="text-[#087F6E] font-semibold cursor-pointer">تفاصيل الحماية</span>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Info Card */}
        <div className="bg-[#E9F8F5] border border-[#6EE7B7]/40 rounded-2xl p-3.5 text-xs text-[#18302D] space-y-1">
          <div className="font-bold flex items-center gap-1.5 text-[#087F6E]">
            <Shield className="w-4 h-4" />
            <span>نظام الضمان التلقائي لأمان</span>
          </div>
          <p className="text-[#667A77] leading-relaxed">
            يقوم النظام بإجراء مهام التجديد والفحص الدوري كل 30 يوماً لدى شركات الاتصالات المعتمدة لضمان بقاء خطك نشطاً.
          </p>
        </div>

      </div>

      {/* Bottom Navigation Bar */}
      <div className="absolute bottom-0 inset-x-0 bg-white border-t border-[#DCE9E6] px-3 py-2 flex items-center justify-around z-30 shadow-lg">
        <button 
          onClick={() => setActiveTab('home')}
          className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-semibold ${
            activeTab === 'home' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <Smartphone className="w-5 h-5" />
          <span>الرئيسية</span>
        </button>

        <button 
          onClick={() => setActiveTab('numbers')}
          className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-semibold ${
            activeTab === 'numbers' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <Phone className="w-5 h-5" />
          <span>أرقامي</span>
        </button>

        <button 
          onClick={() => setShowPaymentSheet(true)}
          className="flex flex-col items-center -mt-5"
        >
          <div className="w-12 h-12 rounded-full bg-[#087F6E] text-white flex items-center justify-center shadow-md shadow-[#087F6E]/40">
            <Plus className="w-6 h-6" />
          </div>
          <span className="text-[10px] font-bold text-[#087F6E] mt-1">طلب جديد</span>
        </button>

        <button 
          onClick={() => setActiveTab('requests')}
          className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-semibold ${
            activeTab === 'requests' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <Shield className="w-5 h-5" />
          <span>الحمايات</span>
        </button>

        <button 
          onClick={() => setActiveTab('profile')}
          className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-semibold ${
            activeTab === 'profile' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <User className="w-5 h-5" />
          <span>حسابي</span>
        </button>
      </div>

      {/* Bottom Sheet for Payment & Protection Request */}
      {showPaymentSheet && (
        <div className="absolute inset-0 bg-black/50 z-50 flex flex-col justify-end animate-in fade-in duration-200">
          <div className="bg-white rounded-t-3xl p-5 space-y-4 max-h-[90%] overflow-y-auto animate-in slide-in-from-bottom duration-300">
            
            <div className="flex items-center justify-between pb-2 border-b border-[#DCE9E6]">
              <div>
                <h3 className="font-bold text-base text-[#18302D]">طلب حماية جديد</h3>
                <p className="text-xs text-[#667A77]">اختر طريقة الدفع اليمنية المعتمدة</p>
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
                    placeholder="77 123 4567" 
                    defaultValue="77 444 8899"
                    className="w-full bg-[#F7FAF9] border border-[#DCE9E6] rounded-xl px-3 py-2.5 text-sm font-inter text-[#18302D] pl-16 focus:outline-none focus:border-[#087F6E]"
                  />
                  <div className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-[#667A77] font-semibold border-r border-[#DCE9E6] pr-2">
                    +967
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#18302D] mb-1">باقة الحماية</label>
                <select className="w-full bg-[#F7FAF9] border border-[#DCE9E6] rounded-xl px-3 py-2.5 text-xs text-[#18302D] focus:outline-none focus:border-[#087F6E]">
                  <option>باقة الحماية السنوية الكاملة — 20,000 ريال</option>
                  <option>باقة الحماية نصف السنوية — 11,000 ريال</option>
                  <option>باقة الحماية الربعية (3 أشهر) — 6,000 ريال</option>
                </select>
              </div>
            </div>

            {/* Payment Methods (From Visual Guide) */}
            <div className="space-y-2 pt-1">
              <label className="block text-xs font-bold text-[#18302D]">اختر طريقة الدفع</label>
              
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
                className="w-full bg-[#087F6E] hover:bg-[#065B4F] text-white py-3 rounded-xl font-bold text-sm shadow transition"
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
