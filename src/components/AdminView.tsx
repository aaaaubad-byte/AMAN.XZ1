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
  Users,
  Menu,
  X,
  Settings,
  Bell,
  BarChart3,
  Layers,
  Database,
  Search,
  CheckCircle2,
  Calendar
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
  const [activeTab, setActiveTab] = useState<'requests' | 'tasks' | 'customers' | 'providers'>('requests');
  const [showDrawer, setShowDrawer] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [filterCompany, setFilterCompany] = useState<string>('all');

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
      date: 'منذ يومين',
      status: 'pending'
    }
  ]);

  const handleApprove = (id: string, phone: string) => {
    setRequests(prev => prev.map(r => r.id === id ? { ...r, status: 'approved' } : r));
    setToastMessage(`تمت الموافقة على طلب الحماية وتفعيل الرقم (${phone})`);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const handleReject = (id: string, phone: string) => {
    setRequests(prev => prev.map(r => r.id === id ? { ...r, status: 'rejected' } : r));
    setToastMessage(`تم رفض طلب الحماية للرقم (${phone})`);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const filteredRequests = filterCompany === 'all' 
    ? requests 
    : requests.filter(r => r.company.includes(filterCompany));

  return (
    <div className="flex-1 flex flex-col h-full bg-[#F7FAF9] text-[#18302D] relative overflow-hidden font-cairo select-none">
      
      {/* Admin Top Bar */}
      <header className="px-4 py-3 bg-white border-b border-[#DCE9E6] flex items-center justify-between shrink-0 z-20 shadow-xs">
        <div className="flex items-center gap-3">
          <button 
            onClick={() => setShowDrawer(true)}
            aria-label="فتح لوحة الإدارة"
            className="w-9 h-9 rounded-xl bg-[#F7FAF9] hover:bg-[#E9F8F5] border border-[#DCE9E6] flex items-center justify-center text-[#087F6E] transition active:scale-95"
          >
            <Menu className="w-5 h-5" />
          </button>
          
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs bg-[#087F6E] text-white px-2 py-0.5 rounded font-bold">مشرف</span>
              <h1 className="font-bold text-sm text-[#18302D]">لوحة إدارة أمان</h1>
            </div>
            <p className="text-[10px] text-[#667A77]">بوابة الإشراف والمعالجة الميدانية</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button className="w-9 h-9 rounded-xl bg-[#F7FAF9] border border-[#DCE9E6] flex items-center justify-center text-[#087F6E]">
            <Bell className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Admin Side Drawer (80% width) */}
      {showDrawer && (
        <div className="absolute inset-0 z-50 flex">
          <div 
            className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity"
            onClick={() => setShowDrawer(false)}
          />

          <aside className="relative w-[80%] max-w-[320px] bg-white h-full shadow-2xl flex flex-col z-10 animate-in slide-in-from-right duration-250 border-l border-[#DCE9E6]">
            {/* Drawer Header */}
            <div className="p-5 bg-gradient-to-br from-[#18302D] to-[#087F6E] text-white">
              <div className="flex items-center justify-between">
                <div className="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center font-bold text-lg border border-white/30">
                  م
                </div>
                <button 
                  onClick={() => setShowDrawer(false)}
                  className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <div className="mt-3">
                <h3 className="font-bold text-base">إدارة العمليات المركزية</h3>
                <p className="text-xs text-emerald-100">مشرف النظام الرئيسي • أمان</p>
              </div>
            </div>

            {/* Admin Nav Items */}
            <nav className="flex-1 overflow-y-auto py-3 px-3 space-y-1">
              {[
                { id: 'requests', label: 'طلبات الحماية المعلقة', icon: Clock, badge: '3' },
                { id: 'tasks', label: 'مهام التجديد اليومية', icon: Calendar, badge: '8' },
                { id: 'customers', label: 'سجل العملاء والاشتراكات', icon: Users },
                { id: 'providers', label: 'شركات الاتصالات اليمنية', icon: Layers },
              ].map((item) => {
                const IconC = item.icon;
                const isSelected = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => {
                      setActiveTab(item.id as any);
                      setShowDrawer(false);
                    }}
                    className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                      isSelected 
                        ? 'bg-[#E9F8F5] text-[#087F6E] font-bold' 
                        : 'text-[#18302D] hover:bg-[#F7FAF9]'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <IconC className={`w-4 h-4 ${isSelected ? 'text-[#087F6E]' : 'text-[#667A77]'}`} />
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-[#087F6E] text-white">
                        {item.badge}
                      </span>
                    )}
                  </button>
                );
              })}
            </nav>

            <div className="p-3 border-t border-[#DCE9E6] bg-[#F7FAF9] text-[11px] text-[#667A77]">
              <span>نظام إدارة أمان • مزامنة Supabase</span>
            </div>
          </aside>
        </div>
      )}

      {/* Main Content Viewport */}
      <main className="flex-1 overflow-y-auto px-4 py-3 space-y-4 pb-24">
        
        {/* Toast Notification */}
        {toastMessage && (
          <div className="bg-[#D1FAE5] border border-[#059669]/30 p-3 rounded-xl flex items-center gap-2 text-xs font-bold text-[#065F46] animate-in fade-in slide-in-from-top-2 shadow-xs">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-[#059669]" />
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Tab 1: REQUESTS */}
        {activeTab === 'requests' && (
          <div className="space-y-3">
            {/* Stats Metric */}
            <div className="grid grid-cols-2 gap-2.5">
              <div className="bg-white border border-[#DCE9E6] p-3 rounded-2xl">
                <div className="text-[11px] text-[#667A77]">طلبات بانتظار الموافقة</div>
                <div className="text-xl font-bold text-[#D97706] mt-0.5 font-inter">
                  {requests.filter(r => r.status === 'pending').length}
                </div>
              </div>
              <div className="bg-white border border-[#DCE9E6] p-3 rounded-2xl">
                <div className="text-[11px] text-[#667A77]">تم تفعيلها اليوم</div>
                <div className="text-xl font-bold text-[#059669] mt-0.5 font-inter">
                  {requests.filter(r => r.status === 'approved').length + 12}
                </div>
              </div>
            </div>

            {/* Filter Pills */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
              {[
                { id: 'all', label: 'الكل' },
                { id: 'يمن موبايل', label: 'يمن موبايل' },
                { id: 'يو', label: 'يو (YOU)' },
                { id: 'سبأفون', label: 'سبأفون' },
              ].map((pill) => (
                <button
                  key={pill.id}
                  onClick={() => setFilterCompany(pill.id)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition ${
                    filterCompany === pill.id
                      ? 'bg-[#087F6E] text-white font-bold'
                      : 'bg-white border border-[#DCE9E6] text-[#667A77]'
                  }`}
                >
                  {pill.label}
                </button>
              ))}
            </div>

            {/* Requests Cards */}
            <div className="space-y-3">
              {filteredRequests.map((req) => (
                <div key={req.id} className="bg-white border border-[#DCE9E6] rounded-2xl p-4 shadow-xs space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-bold text-sm text-[#18302D]">{req.clientName}</div>
                      <div className="font-inter font-bold text-xs text-[#087F6E] dir-ltr text-right mt-0.5">
                        {req.phoneNumber}
                      </div>
                    </div>
                    <span className="text-xs bg-[#E9F8F5] text-[#087F6E] font-bold px-2.5 py-1 rounded-lg">
                      {req.company}
                    </span>
                  </div>

                  <div className="bg-[#F7FAF9] p-2.5 rounded-xl text-xs space-y-1 text-[#667A77]">
                    <div className="flex justify-between">
                      <span>الباقة: <strong className="text-[#18302D]">{req.packageName}</strong></span>
                      <span className="text-[#087F6E] font-bold">{req.price}</span>
                    </div>
                    <div className="flex justify-between text-[11px]">
                      <span>طريقة الدفع: {req.paymentMethod}</span>
                      <span>{req.date}</span>
                    </div>
                  </div>

                  {req.status === 'pending' ? (
                    <div className="grid grid-cols-2 gap-2 pt-1">
                      <button 
                        onClick={() => handleApprove(req.id, req.phoneNumber)}
                        className="bg-[#087F6E] hover:bg-[#065B4F] text-white py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition active:scale-95"
                      >
                        <CheckCircle className="w-3.5 h-3.5" />
                        <span>قبول وتفعيل</span>
                      </button>
                      <button 
                        onClick={() => handleReject(req.id, req.phoneNumber)}
                        className="bg-white border border-[#DC2626]/30 text-[#DC2626] hover:bg-[#FEE2E2]/30 py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition active:scale-95"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                        <span>رفض</span>
                      </button>
                    </div>
                  ) : (
                    <div className="text-center py-1">
                      <span className={`text-xs font-bold px-3 py-1 rounded-full ${
                        req.status === 'approved' ? 'bg-[#D1FAE5] text-[#059669]' : 'bg-[#FEE2E2] text-[#DC2626]'
                      }`}>
                        {req.status === 'approved' ? 'تم القبول والتفعيل في Supabase' : 'تم الرفض'}
                      </span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab 2: TASKS */}
        {activeTab === 'tasks' && (
          <div className="space-y-3">
            <h2 className="font-bold text-sm text-[#18302D]">مهام التجديد المستحقة اليوم (اليمن)</h2>
            <div className="bg-[#E9F8F5] border border-[#6EE7B7]/50 rounded-2xl p-3.5 text-xs text-[#087F6E]">
              يتم إنشاء المهام تلقائياً عبر دالة Postgres RPC (calculate_displayed_task_status) قبل 3 أيام من تاريخ الاستحقاق.
            </div>

            {[
              { id: 't1', phone: '+967 77 123 4567', op: 'يمن موبايل', type: 'تجديد شهري (باقة مزايا)', time: 'اليوم' },
              { id: 't2', phone: '+967 73 987 6543', op: 'يو (YOU)', type: 'شحن رصيد إبقاء الخط', time: 'اليوم' },
            ].map((t) => (
              <div key={t.id} className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 shadow-xs space-y-2">
                <div className="flex justify-between items-center">
                  <span className="font-inter font-bold text-sm text-[#18302D]">{t.phone}</span>
                  <span className="text-xs font-bold text-[#087F6E]">{t.op}</span>
                </div>
                <div className="text-xs text-[#667A77]">{t.type}</div>
                <button 
                  onClick={() => {
                    setToastMessage(`تم تسجيل إتمام مهمة التجديد للرقم ${t.phone}`);
                    setTimeout(() => setToastMessage(null), 3000);
                  }}
                  className="w-full bg-[#087F6E] text-white py-2 rounded-xl text-xs font-bold transition active:scale-95"
                >
                  تأكيد إتمام المهمة وتحديث السند
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Tab 3: CUSTOMERS */}
        {activeTab === 'customers' && (
          <div className="space-y-3">
            <h2 className="font-bold text-sm text-[#18302D]">قائمة العملاء الموثقين</h2>
            <div className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 space-y-2 shadow-xs">
              <div className="flex justify-between">
                <span className="font-bold text-sm">أحمد محمد حميد</span>
                <span className="text-xs text-[#087F6E] font-bold">4 أرقام مؤمنة</span>
              </div>
              <p className="text-xs text-[#667A77]">صنعاء • هوية رقم 01020304050</p>
            </div>
            <div className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 space-y-2 shadow-xs">
              <div className="flex justify-between">
                <span className="font-bold text-sm">سالم باعباد</span>
                <span className="text-xs text-[#D97706] font-bold">طلب معلق</span>
              </div>
              <p className="text-xs text-[#667A77]">حضرموت • هوية رقم 08070605040</p>
            </div>
          </div>
        )}

        {/* Tab 4: PROVIDERS */}
        {activeTab === 'providers' && (
          <div className="space-y-3">
            <h2 className="font-bold text-sm text-[#18302D]">مشغلو الاتصالات اليمنية المعتمدة</h2>
            {[
              { name: 'يمن موبايل (Yemen Mobile)', code: '77, 78', status: 'نشط 100%' },
              { name: 'يو (YOU Telecom)', code: '73', status: 'نشط 100%' },
              { name: 'سبأفون (SabaFon)', code: '71', status: 'نشط 100%' },
              { name: 'واي (Y Telecom)', code: '70', status: 'نشط 100%' },
            ].map((p, idx) => (
              <div key={idx} className="bg-white border border-[#DCE9E6] rounded-2xl p-3.5 shadow-xs flex justify-between items-center">
                <div>
                  <div className="font-bold text-xs text-[#18302D]">{p.name}</div>
                  <div className="text-[11px] text-[#667A77] mt-0.5">البوادئ: {p.code}</div>
                </div>
                <span className="text-[10px] font-bold bg-[#E9F8F5] text-[#087F6E] px-2.5 py-1 rounded-full">
                  {p.status}
                </span>
              </div>
            ))}
          </div>
        )}

      </main>

      {/* Admin Bottom Navigation */}
      <nav className="absolute bottom-0 inset-x-0 bg-white border-t border-[#DCE9E6] px-3 py-2 flex items-center justify-around z-30 shadow-lg">
        <button 
          onClick={() => setActiveTab('requests')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'requests' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Clock className="w-5 h-5" />
          <span className="mt-0.5">الطلبات</span>
        </button>

        <button 
          onClick={() => setActiveTab('tasks')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'tasks' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Calendar className="w-5 h-5" />
          <span className="mt-0.5">المهام</span>
        </button>

        <button 
          onClick={() => setActiveTab('customers')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'customers' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Users className="w-5 h-5" />
          <span className="mt-0.5">العملاء</span>
        </button>

        <button 
          onClick={() => setActiveTab('providers')}
          className={`flex flex-col items-center py-1 px-2 text-[11px] font-semibold transition ${
            activeTab === 'providers' ? 'text-[#087F6E] font-bold' : 'text-[#667A77]'
          }`}
        >
          <Layers className="w-5 h-5" />
          <span className="mt-0.5">المشغلون</span>
        </button>
      </nav>

    </div>
  );
};
