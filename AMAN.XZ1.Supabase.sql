-- ==============================================================================
-- AMAN (أمان) — Complete Consolidated Supabase Database Specification
-- Authoritative Functional Reference: AMAN.XZ.txt
-- Target Repository: AMAN.XZ1
-- Stage: Final Database SQL Extraction & Consolidation
-- DO NOT APPLY TO PRODUCTION WITHOUT PRIOR REVIEW
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 01. EXTENSIONS
-- ------------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------------------------
-- 02. DOMAIN ENUMS (Idempotent Creation)
-- ------------------------------------------------------------------------------

-- User roles: 'client' (عميل), 'manager' (مدير مسؤول), 'admin' (مدير نظام)
DO $$ BEGIN
    CREATE TYPE app_user_role AS ENUM ('client', 'manager', 'admin');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Protection Request Statuses (AMAN.XZ.txt Lines 1196-1200)
-- قيد المراجعة (pending), مقبول (approved), مرفوض (rejected)
DO $$ BEGIN
    CREATE TYPE request_status AS ENUM ('pending', 'approved', 'rejected');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Stored Protection Statuses (AMAN.XZ.txt Lines 1229-1235 & 1561-1575)
-- نشطة (active), منتهية (expired)
-- CRITICAL INVARIANT: 'needs_renewal' is a dynamic calculation, NEVER stored!
DO $$ BEGIN
    CREATE TYPE stored_protection_status AS ENUM ('active', 'expired');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Payment Task Types (AMAN.XZ.txt Lines 1257-1260)
-- مهمة أولى (first), مهمة دورية (recurring)
DO $$ BEGIN
    CREATE TYPE task_type AS ENUM ('first', 'recurring');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Payment Task Statuses (AMAN.XZ.txt Lines 1261-1267)
-- قادمة (upcoming), مستحقة اليوم (due), متأخرة (overdue), مكتملة (completed), ملغاة (cancelled)
DO $$ BEGIN
    CREATE TYPE task_status AS ENUM ('upcoming', 'due', 'overdue', 'completed', 'cancelled');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Phone Number Operational Statuses
-- نشط (active), معلق (suspended), غير نشط (inactive)
DO $$ BEGIN
    CREATE TYPE number_status AS ENUM ('active', 'suspended', 'inactive');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Notification Event Types (AMAN.XZ.txt Lines 1306-1310)
DO $$ BEGIN
    CREATE TYPE notification_type AS ENUM (
        'request_approved',
        'request_rejected',
        'protection_expiring',
        'general'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ------------------------------------------------------------------------------
-- 03. CORE TABLES AND CONSTRAINTS (13 Relational Tables)
-- ------------------------------------------------------------------------------

-- Table 1: users (المستخدمون / العملاء والمدراء)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    role app_user_role NOT NULL DEFAULT 'client',
    account_status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 2: telecom_providers (شركات الاتصالات)
CREATE TABLE IF NOT EXISTS telecom_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    number_length INT NOT NULL DEFAULT 9 CHECK (number_length > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_visible_to_customer BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 3: telecom_prefixes (بادئات أرقام شركات الاتصالات)
CREATE TABLE IF NOT EXISTS telecom_prefixes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE CASCADE,
    prefix TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_telecom_prefix UNIQUE(prefix)
);

-- Table 4: customer_numbers (أرقام هواتف العملاء)
CREATE TABLE IF NOT EXISTS customer_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE RESTRICT,
    phone_number TEXT NOT NULL UNIQUE,
    status number_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 5: protection_plans (باقات الحماية)
CREATE TABLE IF NOT EXISTS protection_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    duration_days INT NOT NULL CHECK (duration_days > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_visible_to_customer BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 6: payment_methods (طرق الدفع والمحافظ الإلكترونية)
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    account_number TEXT NOT NULL,
    account_holder_name TEXT NOT NULL,
    instructions TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 7: protection_requests (طلبات الحماية المقدمة)
CREATE TABLE IF NOT EXISTS protection_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_number_id UUID NOT NULL REFERENCES customer_numbers(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE RESTRICT,
    plan_id UUID NOT NULL REFERENCES protection_plans(id) ON DELETE RESTRICT,
    payment_method_id UUID NOT NULL REFERENCES payment_methods(id) ON DELETE RESTRICT,
    protection_value NUMERIC(12, 2) NOT NULL CHECK (protection_value >= 0),
    transfer_reference TEXT,
    transfer_proof_url TEXT,
    status request_status NOT NULL DEFAULT 'pending',
    rejection_reason TEXT,
    reviewing_manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 8: protections (الحمايات المعتمدة والنشطة والمنتهية)
CREATE TABLE IF NOT EXISTS protections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_number_id UUID NOT NULL REFERENCES customer_numbers(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE RESTRICT,
    plan_id UUID NOT NULL REFERENCES protection_plans(id) ON DELETE RESTRICT,
    created_from_request_id UUID NOT NULL UNIQUE REFERENCES protection_requests(id) ON DELETE RESTRICT,
    protection_value_at_purchase NUMERIC(12, 2) NOT NULL CHECK (protection_value_at_purchase >= 0),
    duration_days_at_purchase INT NOT NULL CHECK (duration_days_at_purchase > 0),
    start_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_date TIMESTAMPTZ NOT NULL,
    status stored_protection_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_protection_dates CHECK (end_date > start_date)
);

-- Table 9: task_settings (إعدادات المهام التشغيلية لكل مشغل)
CREATE TABLE IF NOT EXISTS task_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL UNIQUE REFERENCES telecom_providers(id) ON DELETE CASCADE,
    first_task_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    first_task_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0 CHECK (first_task_amount >= 0),
    recurring_task_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    recurring_task_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0 CHECK (recurring_task_amount >= 0),
    repeat_interval_days INT NOT NULL DEFAULT 30 CHECK (repeat_interval_days > 0),
    visibility_days_before_due INT NOT NULL DEFAULT 3 CHECK (visibility_days_before_due >= 0),
    manual_reschedule_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 10: payment_tasks (مهام سداد الرصيد الدورية والتشغيلية)
CREATE TABLE IF NOT EXISTS payment_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    protection_id UUID NOT NULL REFERENCES protections(id) ON DELETE CASCADE,
    customer_number_id UUID NOT NULL REFERENCES customer_numbers(id) ON DELETE CASCADE,
    task_type task_type NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    due_date TIMESTAMPTZ NOT NULL,
    status task_status NOT NULL DEFAULT 'upcoming',
    completion_date TIMESTAMPTZ,
    completed_by_manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    previous_due_date TIMESTAMPTZ,
    reschedule_reason TEXT,
    rescheduled_by_manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    cancellation_reason TEXT,
    cancelled_by_manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 11: notifications (الإشعارات وسجل التنبيهات)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type notification_type NOT NULL DEFAULT 'general',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 12: audit_logs (سجل العمليات الحساسة غير القابل للتعديل)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    operation_type TEXT NOT NULL,
    affected_record_id UUID,
    affected_table TEXT NOT NULL,
    operation_details TEXT,
    previous_data JSONB,
    new_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table 13: system_settings (إعدادات النظام العامة)
CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_name TEXT NOT NULL DEFAULT 'AMAN | أمان',
    contact_email TEXT,
    contact_phone TEXT,
    terms_and_conditions TEXT,
    privacy_policy TEXT,
    renewal_warning_days INT NOT NULL DEFAULT 7 CHECK (renewal_warning_days > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------------------------
-- 04. CORE HELPER FUNCTIONS (Foundational Authentication & Data Utilities)
-- ------------------------------------------------------------------------------

-- Helper 1: Verify Manager or Administrator Role via Supabase Auth
CREATE OR REPLACE FUNCTION is_manager(p_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM users 
        WHERE id = p_user_id 
          AND role IN ('manager', 'admin')
          AND account_status = 'active'
    );
END;
$$;

-- Helper 2: Standardize Yemen Phone Number to National 9 Digits
CREATE OR REPLACE FUNCTION normalize_phone_number(p_raw_phone TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    v_clean TEXT;
BEGIN
    v_clean := regexp_replace(coalesce(p_raw_phone, ''), '\s+', '', 'g');
    
    -- Strip country prefix
    IF v_clean LIKE '+967%' THEN
        v_clean := substring(v_clean FROM 5);
    ELSIF v_clean LIKE '00967%' THEN
        v_clean := substring(v_clean FROM 6);
    ELSIF v_clean LIKE '967%' AND length(v_clean) > 9 THEN
        v_clean := substring(v_clean FROM 4);
    END IF;

    -- Strip leading zero if present
    IF v_clean LIKE '0%' AND length(v_clean) > 9 THEN
        v_clean := substring(v_clean FROM 2);
    END IF;

    RETURN v_clean;
END;
$$;

-- ------------------------------------------------------------------------------
-- 05. INDEXES & CONCURRENCY-SAFE CONSTRAINTS
-- ------------------------------------------------------------------------------

-- 1. Partial Unique Index: Prevent multiple pending protection requests for the same number
CREATE UNIQUE INDEX IF NOT EXISTS uq_pending_request_per_number 
ON protection_requests(customer_number_id) 
WHERE status = 'pending';

-- 2. Partial Unique Index: Prevent overlapping active protections for the same number
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_protection_per_number 
ON protections(customer_number_id) 
WHERE status = 'active';

-- 3. Partial Unique Index: Active telecom prefixes must be unique across active providers
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_telecom_prefix 
ON telecom_prefixes(prefix) 
WHERE is_active = TRUE;

-- 4. Foreign Key and Query Performance Indexes
CREATE INDEX IF NOT EXISTS idx_customer_numbers_customer_id ON customer_numbers(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_numbers_provider_id ON customer_numbers(provider_id);
CREATE INDEX IF NOT EXISTS idx_customer_numbers_phone ON customer_numbers(phone_number);

CREATE INDEX IF NOT EXISTS idx_telecom_prefixes_provider ON telecom_prefixes(provider_id);
CREATE INDEX IF NOT EXISTS idx_protection_plans_provider ON protection_plans(provider_id) WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_protection_requests_customer_id ON protection_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_protection_requests_status ON protection_requests(status);
CREATE INDEX IF NOT EXISTS idx_protection_requests_number ON protection_requests(customer_number_id);

CREATE INDEX IF NOT EXISTS idx_protections_customer_id ON protections(customer_id);
CREATE INDEX IF NOT EXISTS idx_protections_number_id ON protections(customer_number_id);
CREATE INDEX IF NOT EXISTS idx_protections_status_end_date ON protections(status, end_date);

CREATE INDEX IF NOT EXISTS idx_payment_tasks_protection ON payment_tasks(protection_id);
CREATE INDEX IF NOT EXISTS idx_payment_tasks_number ON payment_tasks(customer_number_id);
CREATE INDEX IF NOT EXISTS idx_payment_tasks_status_due ON payment_tasks(status, due_date);

CREATE INDEX IF NOT EXISTS idx_notifications_customer ON notifications(customer_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_record ON audit_logs(affected_table, affected_record_id);

-- ------------------------------------------------------------------------------
-- 06. TRANSACTIONAL RPC ARCHITECTURE (Zero Client Triggers Invariant)
-- In accordance with AMAN.XZ.txt, all business mutations are strictly handled
-- through explicit, transactional, audited RPC functions rather than asynchronous
-- triggers, ensuring strict concurrency isolation and immediate error propagation.
-- ------------------------------------------------------------------------------

-- ------------------------------------------------------------------------------
-- 07. TRANSACTION-SAFE BUSINESS LOGIC RPC FUNCTIONS
-- ------------------------------------------------------------------------------

-- RPC 1: Automatic Telecom Provider Identification from Prefix
CREATE OR REPLACE FUNCTION identify_provider_from_prefix(p_phone_number TEXT)
RETURNS UUID
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_normalized TEXT;
    v_provider_id UUID;
BEGIN
    v_normalized := normalize_phone_number(p_phone_number);

    -- Find matching active prefix ordered by length descending (longest prefix match)
    SELECT tp.provider_id INTO v_provider_id
    FROM telecom_prefixes tp
    JOIN telecom_providers prov ON prov.id = tp.provider_id
    WHERE tp.is_active = TRUE
      AND prov.is_active = TRUE
      AND v_normalized LIKE (tp.prefix || '%')
    ORDER BY length(tp.prefix) DESC
    LIMIT 1;

    IF v_provider_id IS NULL THEN
        RAISE EXCEPTION 'تعذر التعرف على شركة الاتصالات: البادئة غير مدعومة أو غير نشطة'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_provider_id;
END;
$$;

-- RPC 2: Register Customer Number (Auto-assigns provider, validates length and uniqueness)
CREATE OR REPLACE FUNCTION register_customer_number(p_phone_number TEXT)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_caller_id UUID;
    v_normalized TEXT;
    v_provider_id UUID;
    v_expected_len INT;
    v_new_number_id UUID;
BEGIN
    v_caller_id := auth.uid();
    IF v_caller_id IS NULL THEN
        RAISE EXCEPTION 'غير مصرح: يجب تسجيل الدخول لإضافة رقم' USING ERRCODE = '42501';
    END IF;

    v_normalized := normalize_phone_number(p_phone_number);
    IF length(v_normalized) < 7 THEN
        RAISE EXCEPTION 'رقم الهاتف غير صالح' USING ERRCODE = '22000';
    END IF;

    -- Check if phone already registered
    IF EXISTS (SELECT 1 FROM customer_numbers WHERE phone_number = v_normalized) THEN
        RAISE EXCEPTION 'رقم الهاتف مسجل مسبقاً في النظام' USING ERRCODE = '23505';
    END IF;

    -- Auto-identify provider from prefix
    v_provider_id := identify_provider_from_prefix(v_normalized);

    -- Validate number length against provider rules
    SELECT number_length INTO v_expected_len
    FROM telecom_providers
    WHERE id = v_provider_id;

    IF length(v_normalized) != v_expected_len THEN
        RAISE EXCEPTION 'طول الرقم غير متوافق مع شركة الاتصالات (المتوقع % أرقام)', v_expected_len
            USING ERRCODE = '22000';
    END IF;

    -- Insert record
    INSERT INTO customer_numbers (
        customer_id,
        provider_id,
        phone_number,
        status
    ) VALUES (
        v_caller_id,
        v_provider_id,
        v_normalized,
        'active'
    ) RETURNING id INTO v_new_number_id;

    RETURN v_new_number_id;
END;
$$;

-- RPC 3: Submit Protection Request (Validates ownership, plan provider alignment, duplicate checks)
CREATE OR REPLACE FUNCTION submit_protection_request(
    p_customer_number_id UUID,
    p_plan_id UUID,
    p_payment_method_id UUID,
    p_transfer_reference TEXT DEFAULT NULL,
    p_transfer_proof_url TEXT DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_caller_id UUID;
    v_number RECORD;
    v_plan RECORD;
    v_payment RECORD;
    v_new_request_id UUID;
BEGIN
    v_caller_id := auth.uid();
    IF v_caller_id IS NULL THEN
        RAISE EXCEPTION 'غير مصرح: يجب تسجيل الدخول' USING ERRCODE = '42501';
    END IF;

    -- 1. Validate customer number ownership & status
    SELECT * INTO v_number
    FROM customer_numbers
    WHERE id = p_customer_number_id;

    IF v_number IS NULL THEN
        RAISE EXCEPTION 'الرقم غير موجود' USING ERRCODE = 'P0002';
    END IF;

    IF v_number.customer_id != v_caller_id AND NOT is_manager(v_caller_id) THEN
        RAISE EXCEPTION 'لا تملك صلاحية الوصول لهذا الرقم' USING ERRCODE = '42501';
    END IF;

    IF v_number.status != 'active' THEN
        RAISE EXCEPTION 'لا يمكن طلب حماية لرقم غير نشط' USING ERRCODE = 'P0003';
    END IF;

    -- 2. Check for existing pending request
    IF EXISTS (
        SELECT 1 FROM protection_requests 
        WHERE customer_number_id = p_customer_number_id 
          AND status = 'pending'
    ) THEN
        RAISE EXCEPTION 'يوجد طلب حماية قيد المراجعة بالفعل لهذا الرقم' USING ERRCODE = '23505';
    END IF;

    -- 3. Check for existing active protection
    IF EXISTS (
        SELECT 1 FROM protections 
        WHERE customer_number_id = p_customer_number_id 
          AND status = 'active'
    ) THEN
        RAISE EXCEPTION 'الرقم يتمتع بحماية نشطة حالياً ولا يحتاج إلى طلب جديد' USING ERRCODE = '23505';
    END IF;

    -- 4. Validate protection plan and provider consistency
    SELECT * INTO v_plan
    FROM protection_plans
    WHERE id = p_plan_id;

    IF v_plan IS NULL OR NOT v_plan.is_active THEN
        RAISE EXCEPTION 'باقة الحماية المختارة غير متاحة حالياً' USING ERRCODE = 'P0002';
    END IF;

    IF v_plan.provider_id != v_number.provider_id THEN
        RAISE EXCEPTION 'الباقة المختارة لا تتطابق مع شركة اتصالات الرقم' USING ERRCODE = 'P0004';
    END IF;

    -- 5. Validate payment method
    SELECT * INTO v_payment
    FROM payment_methods
    WHERE id = p_payment_method_id;

    IF v_payment IS NULL OR NOT v_payment.is_active THEN
        RAISE EXCEPTION 'طريقة الدفع المختارة غير مفعلة' USING ERRCODE = 'P0002';
    END IF;

    -- 6. Insert request with plan price snapshot
    INSERT INTO protection_requests (
        customer_id,
        customer_number_id,
        provider_id,
        plan_id,
        payment_method_id,
        protection_value,
        transfer_reference,
        transfer_proof_url,
        status
    ) VALUES (
        v_caller_id,
        p_customer_number_id,
        v_number.provider_id,
        p_plan_id,
        p_payment_method_id,
        v_plan.price,
        p_transfer_reference,
        p_transfer_proof_url,
        'pending'
    ) RETURNING id INTO v_new_request_id;

    RETURN v_new_request_id;
END;
$$;

-- RPC 4: Approve Protection Request (Atomic Approval, Protection Snapshot, Initial Task & Audit)
CREATE OR REPLACE FUNCTION approve_protection_request(p_request_id UUID)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_manager_id UUID;
    v_req RECORD;
    v_plan RECORD;
    v_number RECORD;
    v_settings RECORD;
    v_new_protection_id UUID;
    v_start_time TIMESTAMPTZ;
    v_end_time TIMESTAMPTZ;
    v_task_due TIMESTAMPTZ;
BEGIN
    v_manager_id := auth.uid();
    IF NOT is_manager(v_manager_id) THEN
        RAISE EXCEPTION 'غير مصرح: هذه العملية مخصصة للإدارة فقط' USING ERRCODE = '42501';
    END IF;

    -- Row-level lock on request to prevent concurrent duplicate approvals
    SELECT * INTO v_req
    FROM protection_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF v_req IS NULL THEN
        RAISE EXCEPTION 'طلب الحماية غير موجود' USING ERRCODE = 'P0002';
    END IF;

    IF v_req.status != 'pending' THEN
        RAISE EXCEPTION 'لا يمكن قبول طلب غير معلق (الحالة الحالية: %)', v_req.status
            USING ERRCODE = 'P0005';
    END IF;

    -- Lock and verify no concurrent active protection on this number
    PERFORM 1 FROM protections 
    WHERE customer_number_id = v_req.customer_number_id 
      AND status = 'active'
    FOR UPDATE;

    IF EXISTS (
        SELECT 1 FROM protections 
        WHERE customer_number_id = v_req.customer_number_id 
          AND status = 'active'
    ) THEN
        RAISE EXCEPTION 'الرقم لديه حماية نشطة بالفعل، لا يمكن قبول حماية متداخلة' 
            USING ERRCODE = '23505';
    END IF;

    -- Fetch plan and number details
    SELECT * INTO v_plan FROM protection_plans WHERE id = v_req.plan_id;
    SELECT * INTO v_number FROM customer_numbers WHERE id = v_req.customer_number_id;

    v_start_time := NOW();
    v_end_time := v_start_time + (v_plan.duration_days || ' days')::INTERVAL;

    -- 1. Update Request to approved
    UPDATE protection_requests
    SET status = 'approved',
        reviewing_manager_id = v_manager_id,
        reviewed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_request_id;

    -- 2. Create Protection with historical snapshot
    INSERT INTO protections (
        customer_id,
        customer_number_id,
        provider_id,
        plan_id,
        created_from_request_id,
        protection_value_at_purchase,
        duration_days_at_purchase,
        start_date,
        end_date,
        status
    ) VALUES (
        v_req.customer_id,
        v_req.customer_number_id,
        v_req.provider_id,
        v_req.plan_id,
        p_request_id,
        v_req.protection_value,
        v_plan.duration_days,
        v_start_time,
        v_end_time,
        'active'
    ) RETURNING id INTO v_new_protection_id;

    -- 3. Check Task Settings for this telecom provider and create initial task
    SELECT * INTO v_settings
    FROM task_settings
    WHERE provider_id = v_req.provider_id;

    IF v_settings IS NOT NULL AND v_settings.first_task_enabled THEN
        v_task_due := v_start_time + (coalesce(v_settings.visibility_days_before_due, 3) || ' days')::INTERVAL;
        INSERT INTO payment_tasks (
            protection_id,
            customer_number_id,
            task_type,
            amount,
            due_date,
            status
        ) VALUES (
            v_new_protection_id,
            v_req.customer_number_id,
            'first',
            v_settings.first_task_amount,
            v_task_due,
            'upcoming'
        );
    END IF;

    -- 4. Create Notification for Customer
    INSERT INTO notifications (
        customer_id,
        title,
        message,
        notification_type
    ) VALUES (
        v_req.customer_id,
        'تم قبول طلب الحماية',
        'تم تفعيل الحماية بنجاح للرقم ' || v_number.phone_number || ' حتى ' || to_char(v_end_time, 'YYYY-MM-DD'),
        'request_approved'
    );

    -- 5. Immutable Audit Log
    INSERT INTO audit_logs (
        actor_id,
        operation_type,
        affected_record_id,
        affected_table,
        operation_details,
        new_data
    ) VALUES (
        v_manager_id,
        'accept_protection_request',
        p_request_id,
        'protection_requests',
        'تم اعتماد طلب الحماية وإنشاء حماية جديدة برقم ' || v_new_protection_id,
        jsonb_build_object(
            'protection_id', v_new_protection_id,
            'customer_number', v_number.phone_number,
            'value', v_req.protection_value,
            'end_date', v_end_time
        )
    );

    RETURN v_new_protection_id;
END;
$$;

-- RPC 5: Reject Protection Request (Records reason, updates status, notifies customer, audits)
CREATE OR REPLACE FUNCTION reject_protection_request(
    p_request_id UUID,
    p_rejection_reason TEXT
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_manager_id UUID;
    v_req RECORD;
    v_number RECORD;
BEGIN
    v_manager_id := auth.uid();
    IF NOT is_manager(v_manager_id) THEN
        RAISE EXCEPTION 'غير مصرح: هذه العملية مخصصة للإدارة فقط' USING ERRCODE = '42501';
    END IF;

    -- Lock request row
    SELECT * INTO v_req
    FROM protection_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF v_req IS NULL THEN
        RAISE EXCEPTION 'طلب الحماية غير موجود' USING ERRCODE = 'P0002';
    END IF;

    IF v_req.status != 'pending' THEN
        RAISE EXCEPTION 'لا يمكن رفض طلب غير معلق (الحالة الحالية: %)', v_req.status
            USING ERRCODE = 'P0005';
    END IF;

    SELECT * INTO v_number FROM customer_numbers WHERE id = v_req.customer_number_id;

    -- 1. Update Request
    UPDATE protection_requests
    SET status = 'rejected',
        rejection_reason = p_rejection_reason,
        reviewing_manager_id = v_manager_id,
        reviewed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_request_id;

    -- 2. Notify Customer
    INSERT INTO notifications (
        customer_id,
        title,
        message,
        notification_type
    ) VALUES (
        v_req.customer_id,
        'تم رفض طلب الحماية',
        'نعتذر، تم رفض طلب الحماية للرقم ' || v_number.phone_number || '. السبب: ' || coalesce(p_rejection_reason, 'غير محدد'),
        'request_rejected'
    );

    -- 3. Audit Log
    INSERT INTO audit_logs (
        actor_id,
        operation_type,
        affected_record_id,
        affected_table,
        operation_details,
        new_data
    ) VALUES (
        v_manager_id,
        'reject_protection_request',
        p_request_id,
        'protection_requests',
        'تم رفض طلب الحماية للرقم ' || v_number.phone_number,
        jsonb_build_object(
            'reason', p_rejection_reason
        )
    );
END;
$$;

-- RPC 6: Complete Payment Task (Terminal completion, audits, schedules next recurring task)
CREATE OR REPLACE FUNCTION complete_payment_task(p_task_id UUID)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_manager_id UUID;
    v_task RECORD;
    v_prot RECORD;
    v_settings RECORD;
    v_next_due TIMESTAMPTZ;
    v_next_task_id UUID := NULL;
BEGIN
    v_manager_id := auth.uid();
    IF NOT is_manager(v_manager_id) THEN
        RAISE EXCEPTION 'غير مصرح: هذه العملية مخصصة للإدارة فقط' USING ERRCODE = '42501';
    END IF;

    -- Lock task
    SELECT * INTO v_task
    FROM payment_tasks
    WHERE id = p_task_id
    FOR UPDATE;

    IF v_task IS NULL THEN
        RAISE EXCEPTION 'المهمة غير موجودة' USING ERRCODE = 'P0002';
    END IF;

    IF v_task.status = 'completed' THEN
        RAISE EXCEPTION 'المهمة مكتملة بالفعل' USING ERRCODE = 'P0006';
    END IF;

    IF v_task.status = 'cancelled' THEN
        RAISE EXCEPTION 'لا يمكن إكمال مهمة ملغاة' USING ERRCODE = 'P0006';
    END IF;

    -- 1. Mark task completed
    UPDATE payment_tasks
    SET status = 'completed',
        completion_date = NOW(),
        completed_by_manager_id = v_manager_id,
        updated_at = NOW()
    WHERE id = p_task_id;

    -- 2. Audit log for completion
    INSERT INTO audit_logs (
        actor_id,
        operation_type,
        affected_record_id,
        affected_table,
        operation_details,
        previous_data,
        new_data
    ) VALUES (
        v_manager_id,
        'complete_task',
        p_task_id,
        'payment_tasks',
        'تم إكمال مهمة السداد بنجاح',
        jsonb_build_object('status', v_task.status),
        jsonb_build_object('status', 'completed', 'completion_date', NOW())
    );

    -- 3. Check for next recurring task
    SELECT * INTO v_prot FROM protections WHERE id = v_task.protection_id;
    
    IF v_prot IS NOT NULL AND v_prot.status = 'active' THEN
        SELECT * INTO v_settings FROM task_settings WHERE provider_id = v_prot.provider_id;

        IF v_settings IS NOT NULL AND v_settings.recurring_task_enabled THEN
            v_next_due := NOW() + (v_settings.repeat_interval_days || ' days')::INTERVAL;
            
            -- Only create if next due date is within active protection window
            IF v_next_due < v_prot.end_date THEN
                INSERT INTO payment_tasks (
                    protection_id,
                    customer_number_id,
                    task_type,
                    amount,
                    due_date,
                    status
                ) VALUES (
                    v_prot.id,
                    v_task.customer_number_id,
                    'recurring',
                    v_settings.recurring_task_amount,
                    v_next_due,
                    'upcoming'
                ) RETURNING id INTO v_next_task_id;

                INSERT INTO audit_logs (
                    actor_id,
                    operation_type,
                    affected_record_id,
                    affected_table,
                    operation_details,
                    new_data
                ) VALUES (
                    v_manager_id,
                    'create_recurring_task',
                    v_next_task_id,
                    'payment_tasks',
                    'تم إنشاء المهمة الدورية التالية تلقائياً',
                    jsonb_build_object('due_date', v_next_due, 'amount', v_settings.recurring_task_amount)
                );
            END IF;
        END IF;
    END IF;

    RETURN v_next_task_id;
END;
$$;

-- RPC 7: Reschedule Payment Task (Preserves previous due date, updates with reason)
CREATE OR REPLACE FUNCTION reschedule_payment_task(
    p_task_id UUID,
    p_new_due_date TIMESTAMPTZ,
    p_reason TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_manager_id UUID;
    v_task RECORD;
    v_prot RECORD;
    v_settings RECORD;
BEGIN
    v_manager_id := auth.uid();
    IF NOT is_manager(v_manager_id) THEN
        RAISE EXCEPTION 'غير مصرح: هذه العملية مخصصة للإدارة فقط' USING ERRCODE = '42501';
    END IF;

    IF p_new_due_date <= NOW() THEN
        RAISE EXCEPTION 'تاريخ إعادة الجدولة يجب أن يكون في المستقبل' USING ERRCODE = '22000';
    END IF;

    SELECT * INTO v_task FROM payment_tasks WHERE id = p_task_id FOR UPDATE;

    IF v_task IS NULL THEN
        RAISE EXCEPTION 'المهمة غير موجودة' USING ERRCODE = 'P0002';
    END IF;

    IF v_task.status IN ('completed', 'cancelled') THEN
        RAISE EXCEPTION 'لا يمكن إعادة جدولة مهمة مكتملة أو ملغاة' USING ERRCODE = 'P0006';
    END IF;

    SELECT * INTO v_prot FROM protections WHERE id = v_task.protection_id;
    SELECT * INTO v_settings FROM task_settings WHERE provider_id = v_prot.provider_id;

    IF v_settings IS NOT NULL AND NOT v_settings.manual_reschedule_enabled THEN
        RAISE EXCEPTION 'إعادة الجدولة اليدوية غير مسموحة لهذه الشركة بحسب الإعدادات'
            USING ERRCODE = 'P0007';
    END IF;

    UPDATE payment_tasks
    SET previous_due_date = v_task.due_date,
        due_date = p_new_due_date,
        reschedule_reason = p_reason,
        rescheduled_by_manager_id = v_manager_id,
        status = 'upcoming',
        updated_at = NOW()
    WHERE id = p_task_id;

    INSERT INTO audit_logs (
        actor_id,
        operation_type,
        affected_record_id,
        affected_table,
        operation_details,
        previous_data,
        new_data
    ) VALUES (
        v_manager_id,
        'reschedule_task',
        p_task_id,
        'payment_tasks',
        'تمت إعادة جدولة المهمة إلى ' || to_char(p_new_due_date, 'YYYY-MM-DD'),
        jsonb_build_object('old_due_date', v_task.due_date),
        jsonb_build_object('new_due_date', p_new_due_date, 'reason', p_reason)
    );
END;
$$;

-- RPC 8: Cancel Payment Task (Cancels task without automatic replacement)
CREATE OR REPLACE FUNCTION cancel_payment_task(
    p_task_id UUID,
    p_reason TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_manager_id UUID;
    v_task RECORD;
BEGIN
    v_manager_id := auth.uid();
    IF NOT is_manager(v_manager_id) THEN
        RAISE EXCEPTION 'غير مصرح: هذه العملية مخصصة للإدارة فقط' USING ERRCODE = '42501';
    END IF;

    SELECT * INTO v_task FROM payment_tasks WHERE id = p_task_id FOR UPDATE;

    IF v_task IS NULL THEN
        RAISE EXCEPTION 'المهمة غير موجودة' USING ERRCODE = 'P0002';
    END IF;

    IF v_task.status IN ('completed', 'cancelled') THEN
        RAISE EXCEPTION 'المهمة بالفعل بحالة نهائية (مكتملة أو ملغاة)' USING ERRCODE = 'P0006';
    END IF;

    UPDATE payment_tasks
    SET status = 'cancelled',
        cancellation_reason = p_reason,
        cancelled_by_manager_id = v_manager_id,
        cancelled_at = NOW(),
        updated_at = NOW()
    WHERE id = p_task_id;

    INSERT INTO audit_logs (
        actor_id,
        operation_type,
        affected_record_id,
        affected_table,
        operation_details,
        previous_data,
        new_data
    ) VALUES (
        v_manager_id,
        'cancel_task',
        p_task_id,
        'payment_tasks',
        'تم إلغاء المهمة',
        jsonb_build_object('status', v_task.status),
        jsonb_build_object('status', 'cancelled', 'reason', p_reason)
    );
END;
$$;

-- RPC 9: Calculate Displayed Protection Status
-- Dynamically returns 'active', 'needs_renewal', or 'expired' without mutating database
CREATE OR REPLACE FUNCTION calculate_displayed_protection_status(p_protection_id UUID)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_prot RECORD;
    v_warning_days INT := 7;
BEGIN
    SELECT * INTO v_prot FROM protections WHERE id = p_protection_id;
    IF v_prot IS NULL THEN
        RETURN NULL;
    END IF;

    IF v_prot.status = 'expired' OR NOW() >= v_prot.end_date THEN
        RETURN 'expired';
    END IF;

    SELECT coalesce(renewal_warning_days, 7) INTO v_warning_days
    FROM system_settings
    LIMIT 1;

    IF NOW() >= (v_prot.end_date - (v_warning_days || ' days')::INTERVAL) THEN
        RETURN 'needs_renewal';
    END IF;

    RETURN 'active';
END;
$$;

-- RPC 10: Calculate Displayed Task Status
-- Preserves terminal states ('completed', 'cancelled') and evaluates 'overdue', 'due', 'upcoming'
CREATE OR REPLACE FUNCTION calculate_displayed_task_status(p_task_id UUID)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_task RECORD;
BEGIN
    SELECT * INTO v_task FROM payment_tasks WHERE id = p_task_id;
    IF v_task IS NULL THEN
        RETURN NULL;
    END IF;

    -- Preserves terminal states
    IF v_task.status IN ('completed', 'cancelled') THEN
        RETURN v_task.status::TEXT;
    END IF;

    IF CURRENT_DATE > v_task.due_date::DATE THEN
        RETURN 'overdue';
    ELSIF CURRENT_DATE = v_task.due_date::DATE THEN
        RETURN 'due';
    ELSE
        RETURN 'upcoming';
    END IF;
END;
$$;

-- ------------------------------------------------------------------------------
-- 08. ROW LEVEL SECURITY (RLS) ENABLEMENT
-- ------------------------------------------------------------------------------
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE telecom_providers ENABLE ROW LEVEL SECURITY;
ALTER TABLE telecom_prefixes ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE protection_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_methods ENABLE ROW LEVEL SECURITY;
ALTER TABLE protection_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE protections ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE system_settings ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 09. ROW LEVEL SECURITY POLICIES (25 Granular Policies)
-- ------------------------------------------------------------------------------

-- Policies on users
CREATE POLICY "Users can view own profile" 
ON users FOR SELECT 
TO authenticated 
USING (id = auth.uid() OR is_manager());

CREATE POLICY "Users can update own profile" 
ON users FOR UPDATE 
TO authenticated 
USING (id = auth.uid() OR is_manager())
WITH CHECK (
    (id = auth.uid() AND role = (SELECT role FROM users WHERE id = auth.uid()))
    OR is_manager()
);

CREATE POLICY "Allow user registration" 
ON users FOR INSERT 
TO authenticated 
WITH CHECK (id = auth.uid() OR is_manager());

-- Policies on telecom_providers
CREATE POLICY "Allow customers to view active providers" 
ON telecom_providers FOR SELECT 
TO authenticated 
USING ((is_active = TRUE AND is_visible_to_customer = TRUE) OR is_manager());

CREATE POLICY "Managers can manage providers" 
ON telecom_providers FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on telecom_prefixes
CREATE POLICY "Allow view telecom prefixes" 
ON telecom_prefixes FOR SELECT 
TO authenticated 
USING (is_active = TRUE OR is_manager());

CREATE POLICY "Managers can manage prefixes" 
ON telecom_prefixes FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on customer_numbers
CREATE POLICY "Customers can view own numbers" 
ON customer_numbers FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

CREATE POLICY "Managers can manage numbers" 
ON customer_numbers FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on protection_plans
CREATE POLICY "Customers can view active plans" 
ON protection_plans FOR SELECT 
TO authenticated 
USING ((is_active = TRUE AND is_visible_to_customer = TRUE) OR is_manager());

CREATE POLICY "Managers can manage plans" 
ON protection_plans FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on payment_methods
CREATE POLICY "Customers can view active payment methods" 
ON payment_methods FOR SELECT 
TO authenticated 
USING (is_active = TRUE OR is_manager());

CREATE POLICY "Managers can manage payment methods" 
ON payment_methods FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on protection_requests
CREATE POLICY "Customers can view own requests" 
ON protection_requests FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

CREATE POLICY "Managers can manage requests" 
ON protection_requests FOR UPDATE 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on protections
CREATE POLICY "Customers can view own protections" 
ON protections FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

CREATE POLICY "Managers can manage protections" 
ON protections FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on task_settings (Internal operational data, zero customer access)
CREATE POLICY "Task settings accessible only to managers" 
ON task_settings FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on payment_tasks (Internal operational data, zero customer access)
CREATE POLICY "Payment tasks accessible only to managers" 
ON payment_tasks FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on notifications
CREATE POLICY "Customers can view own notifications" 
ON notifications FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

CREATE POLICY "Customers can mark own notifications as read" 
ON notifications FOR UPDATE 
TO authenticated 
USING (customer_id = auth.uid())
WITH CHECK (customer_id = auth.uid());

CREATE POLICY "Managers can manage notifications" 
ON notifications FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- Policies on audit_logs (Read-only for managers, zero customer access)
CREATE POLICY "Audit logs visible only to managers" 
ON audit_logs FOR SELECT 
TO authenticated 
USING (is_manager());

-- Policies on system_settings
CREATE POLICY "System settings readable by authenticated users" 
ON system_settings FOR SELECT 
TO authenticated 
USING (TRUE);

CREATE POLICY "System settings editable only by managers" 
ON system_settings FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ------------------------------------------------------------------------------
-- 10. ROLE GRANTS AND PERMISSIONS
-- ------------------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO anon, authenticated;

-- Authenticated application users access tables through RLS
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO authenticated;

-- Unauthenticated anon users can view foundational public catalog if needed
GRANT SELECT ON telecom_providers, protection_plans, payment_methods, system_settings TO anon;
GRANT EXECUTE ON FUNCTION normalize_phone_number, identify_provider_from_prefix TO anon;

-- ------------------------------------------------------------------------------
-- 11. BASELINE REFERENCE DATA / CATALOG SEEDS
-- ------------------------------------------------------------------------------

-- 1. Default System Settings
INSERT INTO system_settings (app_name, contact_email, terms_and_conditions, privacy_policy, renewal_warning_days)
VALUES (
    'AMAN | أمان',
    'support@aman.local',
    'شروط وأحكام استخدام منظومة أمان لحماية أرقام الاتصالات.',
    'سياسة خصوصية البيانات الخاصة بمنظومة أمان.',
    7
) ON CONFLICT DO NOTHING;

-- 2. Foundational Yemen Telecom Providers & Deterministic Prefixes
DO $$
DECLARE
    v_ym_id UUID;
    v_you_id UUID;
    v_saba_id UUID;
    v_y_id UUID;
BEGIN
    -- Yemen Mobile (يمن موبايل)
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('يمن موبايل', 'YE-YM', 9, TRUE, TRUE, 1)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_ym_id;

    -- YOU / MTN previously (يو)
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('يو (YOU)', 'YE-YOU', 9, TRUE, TRUE, 2)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_you_id;

    -- Sabafon (سبأفون)
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('سبأفون', 'YE-SABAFON', 9, TRUE, TRUE, 3)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_saba_id;

    -- Y Telecom (واي)
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('واي (Y)', 'YE-Y', 9, TRUE, TRUE, 4)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_y_id;

    -- Prefixes for Yemen Mobile: 77, 78
    INSERT INTO telecom_prefixes (provider_id, prefix, is_active) VALUES
        (v_ym_id, '77', TRUE),
        (v_ym_id, '78', TRUE)
    ON CONFLICT (prefix) DO NOTHING;

    -- Prefixes for YOU: 73
    INSERT INTO telecom_prefixes (provider_id, prefix, is_active) VALUES
        (v_you_id, '73', TRUE)
    ON CONFLICT (prefix) DO NOTHING;

    -- Prefixes for Sabafon: 71
    INSERT INTO telecom_prefixes (provider_id, prefix, is_active) VALUES
        (v_saba_id, '71', TRUE)
    ON CONFLICT (prefix) DO NOTHING;

    -- Prefixes for Y Telecom: 70
    INSERT INTO telecom_prefixes (provider_id, prefix, is_active) VALUES
        (v_y_id, '70', TRUE)
    ON CONFLICT (prefix) DO NOTHING;

    -- Default Operational Task Settings for each telecom provider (AMAN.XZ.txt Lines 1274-1290)
    INSERT INTO task_settings (provider_id, first_task_enabled, first_task_amount, recurring_task_enabled, recurring_task_amount, repeat_interval_days, visibility_days_before_due, manual_reschedule_enabled)
    VALUES
        (v_ym_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE),
        (v_you_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE),
        (v_saba_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE),
        (v_y_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE)
    ON CONFLICT (provider_id) DO NOTHING;

    -- Baseline Protection Plans for Providers
    INSERT INTO protection_plans (provider_id, name, price, duration_days, is_active, is_visible_to_customer)
    VALUES
        (v_ym_id, 'باقة حماية يمن موبايل السنوية', 20000.0, 365, TRUE, TRUE),
        (v_ym_id, 'باقة حماية يمن موبايل 6 أشهر', 11000.0, 180, TRUE, TRUE),
        (v_you_id, 'باقة حماية يو السنوية', 20000.0, 365, TRUE, TRUE),
        (v_saba_id, 'باقة حماية سبأفون السنوية', 20000.0, 365, TRUE, TRUE)
    ON CONFLICT DO NOTHING;

    -- Baseline Payment Methods
    INSERT INTO payment_methods (name, account_number, account_holder_name, instructions, is_active)
    VALUES
        ('محفظة جوالي', '777000111', 'أمان لخدمات حماية الأرقام', 'يرجى إرفاق رقم الحوالة أو صورة السند بعد إتمام الدفع', TRUE),
        ('محفظة كاش', '733000222', 'أمان لخدمات حماية الأرقام', 'يرجى إرفاق رقم العملية في خانة مرجع التحويل', TRUE)
    ON CONFLICT DO NOTHING;

END $$;

-- ------------------------------------------------------------------------------
-- 12. VERIFICATION AND OPERATIONAL INVARIANTS
-- 1. All 13 tables are secured with Row Level Security.
-- 2. Administrative operations strictly check is_manager() = TRUE.
-- 3. Direct client mutations on protections, audit_logs, task_settings, and payment_tasks are barred.
-- 4. Invariant 'needs_renewal' is dynamically calculated from end_date and system_settings.renewal_warning_days.
-- 5. No sensitive secrets, administrative keys, or private JWT tokens are present in this definition.
-- ==============================================================================
