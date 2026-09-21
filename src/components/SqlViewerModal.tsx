import React, { useState } from 'react';
import { Copy, Check, Database, X, Code, Shield } from 'lucide-react';

interface SqlViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SqlViewerModal: React.FC<SqlViewerModalProps> = ({ isOpen, onClose }) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const sqlCode = `-- ==============================================================================
-- AMAN (أمان) — Supabase PostgreSQL Schema — المرحلة الأولى (Stage 1)
-- منظومة حماية وإدارة أرقام الاتصالات المتصلة بتطبيق أندرويد
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. جدول ملفات المستخدمين (Profiles)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone TEXT,
    governorate TEXT,
    role TEXT NOT NULL CHECK (role IN ('client', 'admin')) DEFAULT 'client',
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 2. جدول شركات الاتصالات (Telecom Companies)
CREATE TABLE IF NOT EXISTS public.telecom_companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    logo_url TEXT,
    task_cycle_days INT NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 3. جدول باقات الحماية (Protection Packages)
CREATE TABLE IF NOT EXISTS public.protection_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    duration_days INT NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency TEXT NOT NULL DEFAULT 'YER',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 4. جدول أرقام العملاء (Customer Numbers)
CREATE TABLE IF NOT EXISTS public.customer_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    telecom_company_id UUID REFERENCES public.telecom_companies(id) ON DELETE SET NULL,
    status TEXT NOT NULL CHECK (status IN ('unprotected', 'pending', 'protected', 'expired')) DEFAULT 'unprotected',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 5. جدول طلبات الحماية (Protection Requests)
CREATE TABLE IF NOT EXISTS public.protection_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    number_id UUID NOT NULL REFERENCES public.customer_numbers(id) ON DELETE CASCADE,
    package_id UUID NOT NULL REFERENCES public.protection_packages(id) ON DELETE RESTRICT,
    payment_method TEXT NOT NULL,
    transfer_reference TEXT,
    receipt_image_url TEXT,
    status TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
    admin_notes TEXT,
    reviewed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 6. جدول الحمايات الفعالة (Protections)
CREATE TABLE IF NOT EXISTS public.protections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    number_id UUID NOT NULL REFERENCES public.customer_numbers(id) ON DELETE CASCADE,
    request_id UUID REFERENCES public.protection_requests(id) ON DELETE SET NULL,
    package_id UUID NOT NULL REFERENCES public.protection_packages(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('active', 'needs_renewal', 'expired')) DEFAULT 'active',
    next_task_date DATE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 7. جدول مهام التجديد والفحص الدوري (Protection Tasks)
CREATE TABLE IF NOT EXISTS public.protection_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    protection_id UUID NOT NULL REFERENCES public.protections(id) ON DELETE CASCADE,
    telecom_company_id UUID NOT NULL REFERENCES public.telecom_companies(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    due_date DATE NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('pending', 'completed', 'overdue', 'cancelled')) DEFAULT 'pending',
    completed_at TIMESTAMPTZ,
    completed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- تفعيل سياسات الأمان RLS
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.telecom_companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_packages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_tasks ENABLE ROW LEVEL SECURITY;

-- بيانات أولية للشركات
INSERT INTO public.telecom_companies (name, code, task_cycle_days, is_active)
VALUES
    ('يمن موبايل', 'yemen_mobile', 30, true),
    ('يو (YOU)', 'you', 30, true),
    ('سبأفون', 'sabafon', 30, true),
    ('واي (Y)', 'y', 30, true)
ON CONFLICT (code) DO NOTHING;`;

  const copyToClipboard = () => {
    navigator.clipboard.writeText(sqlCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-[#18302D] text-white w-full max-w-2xl rounded-2xl border border-[#2A4B46] shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        <div className="p-4 border-b border-[#2A4B46] flex items-center justify-between bg-[#122422]">
          <div className="flex items-center gap-2">
            <Database className="w-5 h-5 text-[#6EE7B7]" />
            <span className="font-bold text-sm">أوامر Supabase SQL للمرحلة الأولى</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={copyToClipboard}
              className="px-3 py-1.5 rounded-lg bg-[#087F6E] hover:bg-[#19B99A] text-white text-xs font-bold flex items-center gap-1.5 transition"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-[#6EE7B7]" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'تم النسخ بنجاح' : 'نسخ كود SQL'}</span>
            </button>
            <button onClick={onClose} className="p-1.5 text-gray-400 hover:text-white rounded-lg">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="p-4 overflow-y-auto flex-1 font-mono text-xs text-emerald-100/90 leading-relaxed bg-[#0d1a18]">
          <pre className="whitespace-pre-wrap dir-ltr">{sqlCode}</pre>
        </div>

        <div className="p-3 bg-[#122422] border-t border-[#2A4B46] text-xs text-gray-300 flex items-center justify-between">
          <span>ملف المخطط متوفر في مسار: <code className="text-[#6EE7B7]">/supabase/schema.sql</code></span>
          <span className="text-[11px] text-gray-400">يشمل 7 جداول + RLS + Triggers</span>
        </div>
      </div>
    </div>
  );
};
