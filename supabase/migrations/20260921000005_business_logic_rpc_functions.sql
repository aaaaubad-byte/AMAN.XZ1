-- AMAN.XZ1 - Migration 05: Transaction-Safe Business Logic RPC Functions
-- Authoritative Specification: AMAN.XZ.txt (Lines 1399-1412, 1433-1452, 1505-1675, 1710-1728)

-- ============================================================================
-- 1. Helper: Normalize Phone Number and Extract National Number
-- Strips country code (+967, 00967), whitespace, and leading zero.
-- ============================================================================
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

-- ============================================================================
-- 2. Function: Identify Telecom Provider from Prefix (التعرف على شركة الاتصالات)
-- Rule: Deterministic lookup in telecom_prefixes. Rejects unknown/inactive prefixes.
-- ============================================================================
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

    -- Find matching active prefix ordered by length descending (most specific match)
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

-- ============================================================================
-- 3. Function: Register Customer Number (إضافة رقم جديد للعميل)
-- Rule: The customer NEVER chooses the provider manually!
-- ============================================================================
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

    -- Auto-identify provider
    v_provider_id := identify_provider_from_prefix(v_normalized);

    -- Validate number length
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

-- ============================================================================
-- 4. Function: Submit Protection Request (إنشاء طلب حماية والتحقق منه)
-- Validates: ownership, no duplicate pending request, no overlapping active protection,
-- provider consistency between number and plan, active payment method.
-- ============================================================================
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

-- ============================================================================
-- 5. Function: Approve Protection Request (قبول طلب الحماية وتفعيلها ذرياً)
-- Atomic Operation:
-- - Row lock request
-- - Status transition PENDING -> APPROVED
-- - Create protection with historical price and duration snapshot
-- - Evaluate task_settings; create first task if enabled
-- - Create customer notification
-- - Write immutable audit log
-- ============================================================================
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

    -- Row-level lock on request
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

    -- 1. Update Request
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

    -- 3. Check Task Settings for this telecom provider
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

    -- 5. Audit Log
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

-- ============================================================================
-- 6. Function: Reject Protection Request (رفض طلب الحماية)
-- ============================================================================
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

-- ============================================================================
-- 7. Function: Complete Payment Task (إكمال مهمة سداد وإنشاء المهمة الدورية التالية)
-- ============================================================================
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

-- ============================================================================
-- 8. Function: Reschedule Payment Task (إعادة جدولة مهمة)
-- Rule: Requires manual_reschedule_enabled = true. Preserves previous due date.
-- ============================================================================
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

-- ============================================================================
-- 9. Function: Cancel Payment Task (إلغاء مهمة سداد)
-- Rule: Cancellation does NOT automatically generate a replacement task!
-- ============================================================================
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

-- ============================================================================
-- 10. Function: Calculate Displayed Protection Status (حساب حالة الحماية المعروضة)
-- Invariant: 'needs_renewal' is calculated based on end_date and system_settings.
-- ============================================================================
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

-- ============================================================================
-- 11. Function: Calculate Displayed Task Status (تصنيف حالة المهمة المعروضة)
-- ============================================================================
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
