-- AMAN.XZ1 - Migration 02: Core Tables, Foreign Keys, and Referential Constraints
-- Authoritative Specification: AMAN.XZ.txt (Lines 1062 - 1792)

-- 1. Table: users (المستخدمون / العملاء)
-- Linked directly to Supabase auth.users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    role app_user_role NOT NULL DEFAULT 'client',
    account_status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Table: telecom_providers (شركات الاتصالات)
-- Governs providers supported by AMAN (Yemen Mobile, YOU, Sabafon, Y, etc.)
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

-- 3. Table: telecom_prefixes (بادئات أرقام شركات الاتصالات)
-- Used for 100% deterministic automatic provider identification
CREATE TABLE IF NOT EXISTS telecom_prefixes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE CASCADE,
    prefix TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_telecom_prefix UNIQUE(prefix)
);

-- 4. Table: customer_numbers (أرقام العملاء)
-- Rule: Every phone belongs to exactly one customer. Provider is determined automatically.
CREATE TABLE IF NOT EXISTS customer_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES telecom_providers(id) ON DELETE RESTRICT,
    phone_number TEXT NOT NULL UNIQUE,
    status number_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. Table: protection_plans (باقات الحماية)
-- Rule: Each plan belongs to one telecom provider.
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

-- 6. Table: payment_methods (طرق الدفع / المحافظ الإلكترونية)
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

-- 7. Table: protection_requests (طلبات الحماية)
-- Rule: Created by customer, reviewed by manager. Contains plan snapshot and provider consistency.
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

-- 8. Table: protections (الحمايات الفعالة والمنتهية)
-- Rule: Created ONLY via approved request workflow. Preserves historical price & duration snapshots!
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

-- 9. Table: task_settings (إعدادات المهام التشغيلية لكل شركة اتصالات)
-- Rule: One setting set per telecom provider. Modifying settings never modifies existing tasks!
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

-- 10. Table: payment_tasks (مهام السداد الدورية والتشغيلية)
-- Rule: Operational administration data only. Never managed directly by customer client!
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

-- 11. Table: notifications (الإشعارات)
-- Rule: Associated with customer; isolated via RLS.
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

-- 12. Table: audit_logs (سجل العمليات الحساسة)
-- Rule: Immutable operational audit log generated by trusted database RPCs.
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

-- 13. Table: system_settings (إعدادات النظام العامة)
-- Rule: Application-wide settings (Terms, Privacy, Contact, Warning Window).
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
