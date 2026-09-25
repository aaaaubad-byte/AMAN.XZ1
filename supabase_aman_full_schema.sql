BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================
-- AMAN - Full database schema for a fresh Supabase project
-- Matches the application contracts used in the Android app
-- =============================================================

-- 1) users
CREATE TABLE IF NOT EXISTS public.users (
    id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name text NOT NULL,
    email text NOT NULL UNIQUE,
    role text NOT NULL DEFAULT 'customer' CHECK (role IN ('customer', 'client', 'manager', 'admin')),
    status text NOT NULL DEFAULT 'active',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 2) telecom_providers
CREATE TABLE IF NOT EXISTS public.telecom_providers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    code text NOT NULL UNIQUE,
    phone_length integer NOT NULL DEFAULT 9,
    is_active boolean NOT NULL DEFAULT true,
    is_visible_to_customers boolean NOT NULL DEFAULT true,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 3) telecom_prefixes
CREATE TABLE IF NOT EXISTS public.telecom_prefixes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL REFERENCES public.telecom_providers(id) ON DELETE CASCADE,
    prefix text NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(provider_id, prefix)
);

-- 4) customer_numbers
CREATE TABLE IF NOT EXISTS public.customer_numbers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    provider_id uuid NOT NULL REFERENCES public.telecom_providers(id),
    phone_number text NOT NULL,
    status text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'inactive')),
    protection_status text NOT NULL DEFAULT 'unprotected' CHECK (protection_status IN ('unprotected', 'pending', 'protected')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(customer_id, phone_number)
);

CREATE INDEX IF NOT EXISTS idx_customer_numbers_customer ON public.customer_numbers(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_numbers_provider ON public.customer_numbers(provider_id);

-- 5) protection_plans
CREATE TABLE IF NOT EXISTS public.protection_plans (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL REFERENCES public.telecom_providers(id) ON DELETE CASCADE,
    name text NOT NULL,
    price numeric(12,2) NOT NULL DEFAULT 0,
    currency text NOT NULL DEFAULT 'SAR',
    protection_duration_days integer NOT NULL DEFAULT 30,
    is_active boolean NOT NULL DEFAULT true,
    is_visible_to_customers boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 6) payment_methods
CREATE TABLE IF NOT EXISTS public.payment_methods (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    account_number text NOT NULL,
    account_owner_name text NOT NULL DEFAULT '',
    payment_instructions text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 7) protection_requests
CREATE TABLE IF NOT EXISTS public.protection_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    customer_number_id uuid NOT NULL REFERENCES public.customer_numbers(id) ON DELETE CASCADE,
    provider_id uuid NOT NULL REFERENCES public.telecom_providers(id),
    plan_id uuid NOT NULL REFERENCES public.protection_plans(id),
    payment_method_id uuid NOT NULL REFERENCES public.payment_methods(id),
    protection_value numeric(12,2) NOT NULL DEFAULT 0,
    transfer_data jsonb,
    status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    rejection_reason text,
    reviewed_by uuid REFERENCES public.users(id),
    reviewed_at timestamptz,
    plan_name_snapshot text,
    plan_price_snapshot numeric(12,2),
    plan_duration_days_snapshot integer,
    payment_method_name_snapshot text,
    payment_account_number_snapshot text,
    payment_account_owner_snapshot text,
    payment_instructions_snapshot text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 8) protections
CREATE TABLE IF NOT EXISTS public.protections (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    customer_number_id uuid NOT NULL REFERENCES public.customer_numbers(id) ON DELETE CASCADE,
    provider_id uuid NOT NULL REFERENCES public.telecom_providers(id),
    plan_id uuid NOT NULL REFERENCES public.protection_plans(id),
    created_from_request_id uuid REFERENCES public.protection_requests(id),
    protection_value numeric(12,2) NOT NULL DEFAULT 0,
    protection_duration_days integer NOT NULL DEFAULT 30,
    start_date date NOT NULL,
    end_date date NOT NULL,
    status text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'expired')),
    plan_name_snapshot text,
    provider_name_snapshot text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 9) payment_tasks
CREATE TABLE IF NOT EXISTS public.payment_tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    protection_id uuid NOT NULL REFERENCES public.protections(id) ON DELETE CASCADE,
    customer_number_id uuid NOT NULL REFERENCES public.customer_numbers(id),
    provider_id uuid REFERENCES public.telecom_providers(id),
    task_type text NOT NULL DEFAULT 'recurring' CHECK (task_type IN ('first', 'recurring', 'manual')),
    amount numeric(12,2) NOT NULL DEFAULT 0,
    due_date timestamptz NOT NULL,
    status text NOT NULL DEFAULT 'upcoming' CHECK (status IN ('upcoming', 'pending', 'due_soon', 'due', 'overdue', 'completed', 'cancelled')),
    completed_at timestamptz,
    completed_by uuid REFERENCES public.users(id),
    previous_due_date timestamptz,
    rescheduled_at timestamptz,
    rescheduled_by uuid REFERENCES public.users(id),
    reschedule_reason text,
    cancelled_at timestamptz,
    cancelled_by uuid REFERENCES public.users(id),
    cancellation_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_tasks_status ON public.payment_tasks(status);
CREATE INDEX IF NOT EXISTS idx_payment_tasks_due ON public.payment_tasks(due_date);

-- 10) task_settings
CREATE TABLE IF NOT EXISTS public.task_settings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id uuid NOT NULL UNIQUE REFERENCES public.telecom_providers(id) ON DELETE CASCADE,
    first_task_enabled boolean NOT NULL DEFAULT false,
    first_task_amount numeric(12,2) DEFAULT 0,
    recurring_task_enabled boolean NOT NULL DEFAULT true,
    recurring_task_amount numeric(12,2) NOT NULL DEFAULT 0,
    repeat_interval_days integer NOT NULL DEFAULT 30,
    days_visible_before_due integer NOT NULL DEFAULT 7,
    manual_reschedule_enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 11) notifications
CREATE TABLE IF NOT EXISTS public.notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid REFERENCES public.users(id),
    title text NOT NULL,
    message text NOT NULL,
    notification_type text NOT NULL DEFAULT 'general',
    is_read boolean NOT NULL DEFAULT false,
    read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 12) audit_logs
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id uuid REFERENCES public.users(id),
    action_type text NOT NULL,
    affected_record_id uuid,
    affected_table text,
    details jsonb,
    old_data jsonb,
    new_data jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 13) system_settings
CREATE TABLE IF NOT EXISTS public.system_settings (
    id boolean PRIMARY KEY DEFAULT true CHECK (id = true),
    app_name text NOT NULL DEFAULT 'AMAN',
    contact_data jsonb,
    terms_and_conditions text,
    privacy_policy text,
    renewal_threshold_days integer NOT NULL DEFAULT 30,
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.system_settings (id, app_name, renewal_threshold_days)
VALUES (true, 'AMAN', 30)
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- Triggers: updated_at
-- =============================================================
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'users_set_updated_at'
    ) THEN
        CREATE TRIGGER users_set_updated_at
        BEFORE UPDATE ON public.users
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'telecom_providers_set_updated_at'
    ) THEN
        CREATE TRIGGER telecom_providers_set_updated_at
        BEFORE UPDATE ON public.telecom_providers
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'telecom_prefixes_set_updated_at'
    ) THEN
        CREATE TRIGGER telecom_prefixes_set_updated_at
        BEFORE UPDATE ON public.telecom_prefixes
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'customer_numbers_set_updated_at'
    ) THEN
        CREATE TRIGGER customer_numbers_set_updated_at
        BEFORE UPDATE ON public.customer_numbers
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'protection_plans_set_updated_at'
    ) THEN
        CREATE TRIGGER protection_plans_set_updated_at
        BEFORE UPDATE ON public.protection_plans
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'payment_methods_set_updated_at'
    ) THEN
        CREATE TRIGGER payment_methods_set_updated_at
        BEFORE UPDATE ON public.payment_methods
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'protection_requests_set_updated_at'
    ) THEN
        CREATE TRIGGER protection_requests_set_updated_at
        BEFORE UPDATE ON public.protection_requests
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'protections_set_updated_at'
    ) THEN
        CREATE TRIGGER protections_set_updated_at
        BEFORE UPDATE ON public.protections
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'payment_tasks_set_updated_at'
    ) THEN
        CREATE TRIGGER payment_tasks_set_updated_at
        BEFORE UPDATE ON public.payment_tasks
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'task_settings_set_updated_at'
    ) THEN
        CREATE TRIGGER task_settings_set_updated_at
        BEFORE UPDATE ON public.task_settings
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'system_settings_set_updated_at'
    ) THEN
        CREATE TRIGGER system_settings_set_updated_at
        BEFORE UPDATE ON public.system_settings
        FOR EACH ROW
        EXECUTE FUNCTION public.set_updated_at();
    END IF;
END $$;

-- =============================================================
-- helper: detect provider by phone prefix
-- =============================================================
CREATE OR REPLACE FUNCTION public.get_provider_by_prefix(p_phone_number text)
RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefix text;
    v_provider_id uuid;
BEGIN
    IF p_phone_number IS NULL OR length(trim(p_phone_number)) < 3 THEN
        RETURN NULL;
    END IF;

    v_prefix := substring(regexp_replace(trim(p_phone_number), '[^0-9]', '', 'g'), 1, 4);

    SELECT tp.provider_id
    INTO v_provider_id
    FROM public.telecom_prefixes tp
    WHERE tp.is_active = true
      AND tp.prefix = v_prefix
    LIMIT 1;

    RETURN v_provider_id;
END;
$$;

-- =============================================================
-- RPC: approve_protection_request
-- =============================================================
DROP FUNCTION IF EXISTS public.approve_protection_request(uuid);
CREATE FUNCTION public.approve_protection_request(p_request_id uuid)
RETURNS public.protections
LANGUAGE plpgsql
AS $$
DECLARE
    v_request public.protection_requests%ROWTYPE;
    v_protection public.protections%ROWTYPE;
BEGIN
    SELECT * INTO v_request
    FROM public.protection_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF v_request.id IS NULL THEN
        RAISE EXCEPTION 'Protection request not found';
    END IF;

    IF v_request.status <> 'pending' THEN
        RAISE EXCEPTION 'Only pending requests can be approved';
    END IF;

    INSERT INTO public.protections (
        customer_id, customer_number_id, provider_id, plan_id,
        created_from_request_id, protection_value, protection_duration_days,
        start_date, end_date, status, plan_name_snapshot, provider_name_snapshot
    )
    VALUES (
        v_request.customer_id,
        v_request.customer_number_id,
        v_request.provider_id,
        v_request.plan_id,
        v_request.id,
        COALESCE(v_request.protection_value, 0),
        COALESCE(v_request.plan_duration_days_snapshot, 30),
        current_date,
        current_date + (COALESCE(v_request.plan_duration_days_snapshot, 30) || ' days')::interval,
        'active',
        v_request.plan_name_snapshot,
        (SELECT name FROM public.telecom_providers WHERE id = v_request.provider_id)
    )
    RETURNING * INTO v_protection;

    UPDATE public.customer_numbers
    SET protection_status = 'protected', updated_at = now()
    WHERE id = v_request.customer_number_id;

    UPDATE public.protection_requests
    SET status = 'approved', reviewed_at = now(), updated_at = now()
    WHERE id = p_request_id;

    INSERT INTO public.audit_logs (actor_id, action_type, affected_record_id, affected_table, details)
    VALUES (
        v_request.customer_id,
        'approve_protection_request',
        v_request.id,
        'protection_requests',
        jsonb_build_object('request_id', v_request.id, 'protection_id', v_protection.id)
    );

    RETURN v_protection;
END;
$$;

-- =============================================================
-- RPC: reject_protection_request
-- =============================================================
DROP FUNCTION IF EXISTS public.reject_protection_request(uuid, text);
CREATE FUNCTION public.reject_protection_request(p_request_id uuid, p_reason text)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.protection_requests
    SET status = 'rejected', rejection_reason = p_reason, reviewed_at = now(), updated_at = now()
    WHERE id = p_request_id;

    UPDATE public.customer_numbers
    SET protection_status = 'unprotected', updated_at = now()
    WHERE id = (
        SELECT customer_number_id
        FROM public.protection_requests
        WHERE id = p_request_id
    );

    INSERT INTO public.audit_logs (action_type, affected_record_id, affected_table, details)
    VALUES (
        'reject_protection_request',
        p_request_id,
        'protection_requests',
        jsonb_build_object('reason', p_reason)
    );
END;
$$;

-- =============================================================
-- RPC: complete_payment_task
-- =============================================================
DROP FUNCTION IF EXISTS public.complete_payment_task(uuid);
CREATE FUNCTION public.complete_payment_task(p_task_id uuid)
RETURNS public.payment_tasks
LANGUAGE plpgsql
AS $$
DECLARE
    v_task public.payment_tasks%ROWTYPE;
BEGIN
    SELECT * INTO v_task
    FROM public.payment_tasks
    WHERE id = p_task_id
    FOR UPDATE;

    IF v_task.id IS NULL THEN
        RAISE EXCEPTION 'Payment task not found';
    END IF;

    UPDATE public.payment_tasks
    SET status = 'completed', completed_at = now(), updated_at = now()
    WHERE id = p_task_id;

    SELECT * INTO v_task
    FROM public.payment_tasks
    WHERE id = p_task_id;

    INSERT INTO public.audit_logs (action_type, affected_record_id, affected_table, details)
    VALUES (
        'complete_payment_task',
        p_task_id,
        'payment_tasks',
        jsonb_build_object('protection_id', v_task.protection_id)
    );

    RETURN v_task;
END;
$$;

-- =============================================================
-- Other admin helper functions
-- =============================================================
DROP FUNCTION IF EXISTS public.admin_create_telecom_provider(text, text, integer, boolean, boolean, integer);
CREATE FUNCTION public.admin_create_telecom_provider(
    p_name text,
    p_code text,
    p_phone_length integer,
    p_is_active boolean,
    p_is_visible_to_customers boolean,
    p_display_order integer
)
RETURNS public.telecom_providers
LANGUAGE plpgsql
AS $$
DECLARE
    v_row public.telecom_providers%ROWTYPE;
BEGIN
    INSERT INTO public.telecom_providers (
        name, code, phone_length, is_active, is_visible_to_customers, display_order
    )
    VALUES (
        p_name,
        upper(p_code),
        p_phone_length,
        p_is_active,
        p_is_visible_to_customers,
        p_display_order
    )
    RETURNING * INTO v_row;

    RETURN v_row;
END;
$$;

DROP FUNCTION IF EXISTS public.admin_update_telecom_provider(uuid, text, text, integer, text[], boolean, boolean, integer);
CREATE FUNCTION public.admin_update_telecom_provider(
    p_provider_id uuid,
    p_name text,
    p_code text,
    p_phone_length integer,
    p_prefixes text[],
    p_is_active boolean,
    p_is_visible_to_customers boolean,
    p_display_order integer
)
RETURNS public.telecom_providers
LANGUAGE plpgsql
AS $$
DECLARE
    v_row public.telecom_providers%ROWTYPE;
    v_prefix text;
BEGIN
    UPDATE public.telecom_providers
    SET
        name = p_name,
        code = upper(p_code),
        phone_length = p_phone_length,
        is_active = p_is_active,
        is_visible_to_customers = p_is_visible_to_customers,
        display_order = p_display_order,
        updated_at = now()
    WHERE id = p_provider_id;

    DELETE FROM public.telecom_prefixes WHERE provider_id = p_provider_id;

    IF p_prefixes IS NOT NULL THEN
        FOREACH v_prefix IN ARRAY p_prefixes LOOP
            INSERT INTO public.telecom_prefixes (provider_id, prefix, is_active)
            VALUES (p_provider_id, trim(v_prefix), true);
        END LOOP;
    END IF;

    SELECT * INTO v_row
    FROM public.telecom_providers
    WHERE id = p_provider_id;

    RETURN v_row;
END;
$$;

DROP FUNCTION IF EXISTS public.admin_create_protection_plan(uuid, text, numeric, integer, boolean, boolean);
CREATE FUNCTION public.admin_create_protection_plan(
    p_provider_id uuid,
    p_name text,
    p_price numeric,
    p_duration_days integer,
    p_is_active boolean,
    p_is_visible_to_customers boolean
)
RETURNS public.protection_plans
LANGUAGE plpgsql
AS $$
DECLARE
    v_row public.protection_plans%ROWTYPE;
BEGIN
    INSERT INTO public.protection_plans (
        provider_id, name, price, protection_duration_days, is_active, is_visible_to_customers
    )
    VALUES (
        p_provider_id,
        p_name,
        p_price,
        p_duration_days,
        p_is_active,
        p_is_visible_to_customers
    )
    RETURNING * INTO v_row;

    RETURN v_row;
END;
$$;

DROP FUNCTION IF EXISTS public.admin_create_payment_method(text, text, text, text, boolean);
CREATE FUNCTION public.admin_create_payment_method(
    p_name text,
    p_account_number text,
    p_account_owner_name text,
    p_payment_instructions text,
    p_is_active boolean
)
RETURNS public.payment_methods
LANGUAGE plpgsql
AS $$
DECLARE
    v_row public.payment_methods%ROWTYPE;
BEGIN
    INSERT INTO public.payment_methods (
        name, account_number, account_owner_name, payment_instructions, is_active
    )
    VALUES (
        p_name,
        p_account_number,
        p_account_owner_name,
        p_payment_instructions,
        p_is_active
    )
    RETURNING * INTO v_row;

    RETURN v_row;
END;
$$;

-- =============================================================
-- Security: enable RLS and create simple public-read policies
-- =============================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.telecom_providers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.telecom_prefixes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_methods ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protection_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_settings ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'telecom_providers'
          AND policyname = 'public_read_telecom_providers'
    ) THEN
        CREATE POLICY public_read_telecom_providers
        ON public.telecom_providers
        FOR SELECT
        USING (true);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'protection_plans'
          AND policyname = 'public_read_protection_plans'
    ) THEN
        CREATE POLICY public_read_protection_plans
        ON public.protection_plans
        FOR SELECT
        USING (true);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'payment_methods'
          AND policyname = 'public_read_payment_methods'
    ) THEN
        CREATE POLICY public_read_payment_methods
        ON public.payment_methods
        FOR SELECT
        USING (true);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'task_settings'
          AND policyname = 'public_read_task_settings'
    ) THEN
        CREATE POLICY public_read_task_settings
        ON public.task_settings
        FOR SELECT
        USING (true);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'system_settings'
          AND policyname = 'public_read_system_settings'
    ) THEN
        CREATE POLICY public_read_system_settings
        ON public.system_settings
        FOR SELECT
        USING (true);
    END IF;
END $$;

-- =============================================================
-- Grants for app access
-- =============================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO service_role;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO service_role;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO anon, authenticated, service_role;

GRANT SELECT ON public.telecom_providers, public.protection_plans, public.payment_methods, public.task_settings, public.system_settings TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.users, public.customer_numbers, public.protection_requests, public.protections, public.payment_tasks, public.notifications, public.audit_logs TO service_role;
GRANT SELECT, INSERT, UPDATE ON public.users, public.customer_numbers, public.protection_requests, public.protections, public.payment_tasks, public.notifications TO authenticated;

-- =============================================================
-- Runtime contract fixes for current live database
-- =============================================================
ALTER TABLE public.customer_numbers
    ADD COLUMN IF NOT EXISTS protection_status text DEFAULT 'unprotected';

UPDATE public.customer_numbers
SET protection_status = 'unprotected'
WHERE protection_status IS NULL;

ALTER TABLE public.customer_numbers
    ALTER COLUMN protection_status SET DEFAULT 'unprotected';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'customer_numbers_protection_status_check'
    ) THEN
        ALTER TABLE public.customer_numbers
            ADD CONSTRAINT customer_numbers_protection_status_check
            CHECK (protection_status IN ('unprotected', 'pending', 'protected'));
    END IF;
END $$;

DROP FUNCTION IF EXISTS public.cancel_payment_task(uuid, text);
DROP FUNCTION IF EXISTS public.reschedule_payment_task(uuid, timestamptz, text);
DROP FUNCTION IF EXISTS public.admin_set_telecom_provider_status(uuid, boolean);
DROP FUNCTION IF EXISTS public.admin_update_protection_plan(uuid, uuid, text, numeric, integer, boolean, boolean);
DROP FUNCTION IF EXISTS public.admin_set_protection_plan_status(uuid, boolean);
DROP FUNCTION IF EXISTS public.admin_update_payment_method(uuid, text, text, text, text, boolean);
DROP FUNCTION IF EXISTS public.admin_set_payment_method_status(uuid, boolean);

CREATE OR REPLACE FUNCTION public.cancel_payment_task(p_task_id uuid, p_reason text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_task public.payment_tasks%ROWTYPE;
BEGIN
    SELECT * INTO v_task
    FROM public.payment_tasks
    WHERE id = p_task_id
    FOR UPDATE;

    IF v_task.id IS NULL THEN
        RAISE EXCEPTION 'Payment task not found';
    END IF;

    UPDATE public.payment_tasks
    SET status = 'cancelled',
        cancelled_at = now(),
        cancelled_by = auth.uid(),
        cancellation_reason = p_reason,
        updated_at = now()
    WHERE id = p_task_id;

    INSERT INTO public.audit_logs (action_type, affected_record_id, affected_table, details)
    VALUES (
        'cancel_payment_task',
        p_task_id,
        'payment_tasks',
        jsonb_build_object('reason', p_reason)
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.reschedule_payment_task(p_task_id uuid, p_new_due_date timestamptz, p_reason text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_task public.payment_tasks%ROWTYPE;
BEGIN
    SELECT * INTO v_task
    FROM public.payment_tasks
    WHERE id = p_task_id
    FOR UPDATE;

    IF v_task.id IS NULL THEN
        RAISE EXCEPTION 'Payment task not found';
    END IF;

    UPDATE public.payment_tasks
    SET previous_due_date = due_date,
        due_date = p_new_due_date,
        status = 'upcoming',
        rescheduled_at = now(),
        rescheduled_by = auth.uid(),
        reschedule_reason = p_reason,
        updated_at = now()
    WHERE id = p_task_id;

    INSERT INTO public.audit_logs (action_type, affected_record_id, affected_table, details)
    VALUES (
        'reschedule_payment_task',
        p_task_id,
        'payment_tasks',
        jsonb_build_object('new_due_date', p_new_due_date, 'reason', p_reason)
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_set_telecom_provider_status(p_provider_id uuid, p_is_active boolean)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.telecom_providers
    SET is_active = p_is_active,
        updated_at = now()
    WHERE id = p_provider_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_update_protection_plan(
    p_plan_id uuid,
    p_provider_id uuid,
    p_name text,
    p_price numeric,
    p_duration_days integer,
    p_is_active boolean,
    p_is_visible_to_customers boolean
)
RETURNS public.protection_plans
LANGUAGE plpgsql
AS $$
DECLARE
    v_row public.protection_plans%ROWTYPE;
BEGIN
    UPDATE public.protection_plans
    SET provider_id = p_provider_id,
        name = p_name,
        price = p_price,
        protection_duration_days = p_duration_days,
        is_active = p_is_active,
        is_visible_to_customers = p_is_visible_to_customers,
        updated_at = now()
    WHERE id = p_plan_id;

    SELECT * INTO v_row
    FROM public.protection_plans
    WHERE id = p_plan_id;

    RETURN v_row;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_set_protection_plan_status(p_plan_id uuid, p_is_active boolean)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.protection_plans
    SET is_active = p_is_active,
        updated_at = now()
    WHERE id = p_plan_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_update_payment_method(
    p_payment_method_id uuid,
    p_name text,
    p_account_number text,
    p_account_owner_name text,
    p_payment_instructions text,
    p_is_active boolean
)
RETURNS public.payment_methods
LANGUAGE plpgsql
AS $$
DECLARE
    v_row public.payment_methods%ROWTYPE;
BEGIN
    UPDATE public.payment_methods
    SET name = p_name,
        account_number = p_account_number,
        account_owner_name = p_account_owner_name,
        payment_instructions = p_payment_instructions,
        is_active = p_is_active,
        updated_at = now()
    WHERE id = p_payment_method_id;

    SELECT * INTO v_row
    FROM public.payment_methods
    WHERE id = p_payment_method_id;

    RETURN v_row;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_set_payment_method_status(p_payment_method_id uuid, p_is_active boolean)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.payment_methods
    SET is_active = p_is_active,
        updated_at = now()
    WHERE id = p_payment_method_id;
END;
$$;

COMMIT;
