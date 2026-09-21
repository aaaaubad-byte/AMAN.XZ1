-- ==============================================================================
-- AMAN (أمان) — Supabase PostgreSQL Schema — المرحلة الأولى (Stage 1)
-- منظومة حماية وإدارة أرقام الاتصالات المتصلة بتطبيق أندرويد
-- ==============================================================================

-- تفعيل الامتدادات الضرورية
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------------------------
-- 1. جدول ملفات المستخدمين (Profiles)
-- مرتبط مع نظام المصادقة الأساسي في سوبابيز (auth.users)
-- ------------------------------------------------------------------------------
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

-- ------------------------------------------------------------------------------
-- 2. جدول شركات الاتصالات (Telecom Companies)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.telecom_companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE, -- 'yemen_mobile', 'you', 'sabafon', 'y'
    logo_url TEXT,
    task_cycle_days INT NOT NULL DEFAULT 30, -- عدد الأيام لدورة فحص/تجديد الخط
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 3. جدول باقات الحماية (Protection Packages)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.protection_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL, -- مثلاً: باقة أمان السنوية، باقة 6 أشهر
    duration_days INT NOT NULL, -- 365, 180, 90
    price NUMERIC(12, 2) NOT NULL,
    currency TEXT NOT NULL DEFAULT 'YER',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 4. جدول أرقام العملاء (Customer Numbers)
-- ------------------------------------------------------------------------------
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

-- ------------------------------------------------------------------------------
-- 5. جدول طلبات الحماية (Protection Requests)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.protection_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    number_id UUID NOT NULL REFERENCES public.customer_numbers(id) ON DELETE CASCADE,
    package_id UUID NOT NULL REFERENCES public.protection_packages(id) ON DELETE RESTRICT,
    payment_method TEXT NOT NULL, -- 'mobile_money', 'jawali', 'flousk', 'one_cash', 'bank'
    transfer_reference TEXT,
    receipt_image_url TEXT,
    status TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
    admin_notes TEXT,
    reviewed_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 6. جدول الحمايات الفعالة (Protections)
-- ------------------------------------------------------------------------------
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

-- ------------------------------------------------------------------------------
-- 7. جدول مهام التجديد والفحص الدوري (Protection Tasks)
-- ------------------------------------------------------------------------------
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

-- ------------------------------------------------------------------------------
-- 8. جدول سجل العمليات الإدارية (Audit Logs)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    details JSONB,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- ==============================================================================
-- فهارس تحسين الأداء (Indexes)
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_customer_numbers_user ON public.customer_numbers(user_id);
CREATE INDEX IF NOT EXISTS idx_protection_requests_status ON public.protection_requests(status);
CREATE INDEX IF NOT EXISTS idx_protections_end_date ON public.protections(end_date);
CREATE INDEX IF NOT EXISTS idx_protection_tasks_due_status ON public.protection_tasks(due_date, status);

-- ==============================================================================
-- دوال ومشغلات تلقائية (Functions & Triggers)
-- ==============================================================================

-- 1. إنشاء ملف مستخدم تلقائياً عند التسجيل في auth.users
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name, phone, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', 'مستخدم أمان'),
        NEW.raw_user_meta_data->>'phone',
        COALESCE(NEW.raw_user_meta_data->>'role', 'client')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ==============================================================================
-- سياسات الأمان والحماية (Row Level Security - RLS)
-- ==============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.telecom_companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_packages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- دالة مساعدة لمعرفة هل المستخدم الحالي مدير (Admin)
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- سياسات profiles
CREATE POLICY "Users can view own profile or admins can view all"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id OR public.is_admin());

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- سياسات telecom_companies و protection_packages
CREATE POLICY "Public read active companies"
    ON public.telecom_companies FOR SELECT
    USING (is_active = true OR public.is_admin());

CREATE POLICY "Admin manage companies"
    ON public.telecom_companies FOR ALL
    USING (public.is_admin());

CREATE POLICY "Public read active packages"
    ON public.protection_packages FOR SELECT
    USING (is_active = true OR public.is_admin());

CREATE POLICY "Admin manage packages"
    ON public.protection_packages FOR ALL
    USING (public.is_admin());

-- سياسات customer_numbers
CREATE POLICY "Clients manage their own numbers"
    ON public.customer_numbers FOR ALL
    USING (auth.uid() = user_id OR public.is_admin());

-- سياسات protection_requests
CREATE POLICY "Clients view own requests or admin view all"
    ON public.protection_requests FOR SELECT
    USING (auth.uid() = user_id OR public.is_admin());

CREATE POLICY "Clients can create protection requests"
    ON public.protection_requests FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Admins can update requests"
    ON public.protection_requests FOR UPDATE
    USING (public.is_admin());

-- سياسات protections و tasks
CREATE POLICY "Clients view own protections"
    ON public.protections FOR SELECT
    USING (auth.uid() = user_id OR public.is_admin());

CREATE POLICY "Admins manage protections"
    ON public.protections FOR ALL
    USING (public.is_admin());

CREATE POLICY "Admins manage tasks"
    ON public.protection_tasks FOR ALL
    USING (public.is_admin());
