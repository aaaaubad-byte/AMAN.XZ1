import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Clock, 
  AlertCircle, 
  CheckCircle, 
  XCircle, 
  Smartphone, 
  FileText, 
  Check, 
  Filter,
  Users
} from 'lucide-react';

interface PendingRequest {
  id: string;
  clientName: string;
  phoneNumber: string;
  company: string;
  packageName: string;
  price: string;
  paymentMethod: string;
  date: string;
  status: 'pending' | 'approved' | 'rejected';
}

export const AdminView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'requests' | 'tasks' | 'companies'>('requests');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const [requests, setRequests] = useState<PendingRequest[]>([
    {
      id: 'req-1',
      clientName: 'سالم باعباد',
      phoneNumber: '+967 77 000 1122',
      company: 'يمن موبايل',
      packageName: 'باقة سنوية',
      price: '20,000 ريال',
      paymentMethod: 'محفظة جوالي',
      date: 'اليوم، 11:20 ص',
      status: 'pending'
    },
    {
      id: 'req-2',
      clientName: 'طارق الأنسي',
      phoneNumber: '+967 73 444 9900',
      company: 'يو (YOU)',
      packageName: 'باقة نصف سنوية',
      price: '11,000 ريال',
      paymentMethod: 'موبايل موني',
      date: 'أمس، 05:40 م',
      status: 'pending'
    },
    {
      id: 'req-3',
      clientName: 'عماد الشميري',
      phoneNumber: '+967 71 888 2211',
      company: 'سبأفون',
      packageName: 'باقة 3 أشهر',
      price: '6,000 ريال',
      paymentMethod: 'ون كاش',
      date: '20 سبتمبر 2026',
      status: 'pending'
    }
  ]);

  const handleApprove = (id: string, phone: string) => {
    setRequests(prev => prev.map(r => r.id === id ? { ...r, status: 'approved' } : r));
    showToast(`تم قبول وتفعيل حماية الرقم ${phone} بنجاح وإصدار مهمة التجديد الدورية`);
  };

  const handleReject = (id: string, phone: string) => {
    setRequests(prev => prev.map(r => r.id === id ? { ...r, status: 'rejected' } : r));
    showToast(`تم رفض طلب الحماية للرقم ${phone}`);
  };

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 4000);
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-[#F7FAF9] text-[#18302D] relative overflow-hidden">
      {/* Admin Mobile Top Bar */}
      <div className="px-4 py-3 bg-white border-b border-[#DCE9E6] flex items-center justify-between shrink-0">
        <div>
          <div className="flex items-center gap-1.5">
            <span className="font-bold text-sm text-[#18302D]">لوحة إدارة المنظومة</span>
            <span className="bg-[#E9F8F5] text-[#087F6E] text-[10px] font-bold px-2 py-0.5 rounded">
              مدير النظام
            </span>
          </div>
          <div className="text-xs text-[#667A77]">تطبيق الإدارة المحمول • Supabase Active</div>
        </div>

        <div className="w-8 h-8 rounded-full bg-[#087F6E] text-white flex items-center justify-center text-xs font-bold">
          م
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-20">
        {/* Toast */}
        {toastMessage && (
          <div className="bg-[#18302D] text-white text-xs p-3 rounded-xl shadow-lg flex items-center gap-2 animate-in fade-in slide-in-from-top-2">
            <CheckCircle className="w-4 h-4 text-[#6EE7B7] shrink-0" />
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Admin KPI Stats */}
        <div className="grid grid-cols-2 gap-2.5">
          <div className="bg-white border border-[#DCE9E6] p-3 rounded-2xl">
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#667A77]">طلبات معلقة</span>
              <div className="w-6 h-6 rounded-lg bg-[#FEF3C7] text-[#D97706] flex items-center justify-center">
                <Clock className="w-3.5 h-3.5" />
              </div>
            </div>
            <div className="text-xl font-bold font-inter text-[#18302D] mt-1.5">
              {requests.filter(r => r.status === 'pending').length}
            </div>
            <div className="text-[10px] text-[#D97706] font-semibold mt-0.5">تحتاج مراجعة فورية</div>
          </div>

          <div className="bg-white border border-[#DCE9E6] p-3 rounded-2xl">
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#667A77]">الحمايات النشطة</span>
              <div className="w-6 h-6 rounded-lg bg-[#E9F8F5] text-[#087F6E] flex items-center justify-center">
                <ShieldCheck className="w-3.5 h-3.5" />
              </div>
            </div>
            <div className="text-xl font-bold font-inter text-[#18302D] mt-1.5">182</div>
            <div className="text-[10px] text-[#087F6E] font-semibold mt-0.5">مؤمنة وتعمل بنجاح</div>
          </div>
        </div>

        {/* Section Header */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-1.5">
            <FileText className="w-4 h-4 text-[#087F6E]" />
            <h3 className="font-bold text-sm text-[#18302D]">طلبات الحماية الواردة</h3>
          </div>
          <span className="text-xs text-[#667A77]">فرز حسب الأحدث</span>
        </div>

        {/* Requests List */}
        <div className="space-y-3">
          {requests.map(req => (
            <div key={req.id} className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 shadow-sm space-y-2.5">
              <div className="flex items-start justify-between">
                <div>
                  <div className="font-bold text-sm text-[#18302D]">{req.clientName}</div>
                  <div className="text-xs font-inter font-bold text-[#087F6E] dir-ltr text-right mt-0.5">
                    {req.phoneNumber}
                  </div>
                  <div className="text-[11px] text-[#667A77] mt-0.5">
                    {req.company} • {req.packageName} ({req.price})
                  </div>
                </div>

                <div>
                  {req.status === 'pending' && (
                    <span className="bg-[#FEF3C7] text-[#D97706] text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                      قيد المراجعة
                    </span>
                  )}
                  {req.status === 'approved' && (
                    <span className="bg-[#D1FAE5] text-[#059669] text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                      تم القبول والتفعيل
                    </span>
                  )}
                  {req.status === 'rejected' && (
                    <span className="bg-[#FEE2E2] text-[#DC2626] text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                      مرفوض
                    </span>
                  )}
                </div>
              </div>

              <div className="bg-[#F7FAF9] p-2.5 rounded-xl text-[11px] text-[#667A77] flex items-center justify-between">
                <span>طريقة الدفع: <strong className="text-[#18302D]">{req.paymentMethod}</strong></span>
                <span>{req.date}</span>
              </div>

              {req.status === 'pending' && (
                <div className="grid grid-cols-2 gap-2 pt-1">
                  <button 
                    onClick={() => handleApprove(req.id, req.phoneNumber)}
                    className="bg-[#087F6E] hover:bg-[#065B4F] text-white py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition"
                  >
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>قبول وتفعيل</span>
                  </button>
                  <button 
                    onClick={() => handleReject(req.id, req.phoneNumber)}
                    className="bg-white border border-[#DC2626]/30 text-[#DC2626] hover:bg-[#FEE2E2]/30 py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition"
                  >
                    <XCircle className="w-3.5 h-3.5" />
                    <span>رفض الطلب</span>
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Scheduled Tasks Banner */}
        <div className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5">
          <div className="flex items-center justify-between mb-2">
            <span className="font-bold text-xs text-[#18302D]">مهام التجديد اليومية (اليمن)</span>
            <span className="text-[11px] text-[#087F6E] font-bold">8 مهام اليوم</span>
          </div>
          <p className="text-[11px] text-[#667A77] leading-relaxed">
            يقوم المشرفون بتنفيذ دورة التجديد لأرقام يمن موبايل ويو وسبأفون المسجلة، ثم تأكيد المهمة في قاعدة بيانات Supabase.
          </p>
        </div>

      </div>

      {/* Admin Bottom Nav */}
      <div className="absolute bottom-0 inset-x-0 bg-white border-t border-[#DCE9E6] px-4 py-2.5 flex items-center justify-around z-30 shadow-lg">
        <button 
          onClick={() => setActiveTab('requests')}
          className={`flex flex-col items-center gap-1 text-xs font-semibold ${
            activeTab === 'requests' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <Clock className="w-5 h-5" />
          <span>الطلبات</span>
        </button>

        <button 
          onClick={() => setActiveTab('tasks')}
          className={`flex flex-col items-center gap-1 text-xs font-semibold ${
            activeTab === 'tasks' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <ShieldCheck className="w-5 h-5" />
          <span>الحمايات والمهام</span>
        </button>

        <button 
          onClick={() => setActiveTab('companies')}
          className={`flex flex-col items-center gap-1 text-xs font-semibold ${
            activeTab === 'companies' ? 'text-[#087F6E]' : 'text-[#667A77]'
          }`}
        >
          <Users className="w-5 h-5" />
          <span>العملاء والشركات</span>
        </button>
      </div>
    </div>
  );
};
