-- ==============================================================================
-- AMAN (أمان) — Initial / Testing Seed Data
-- Authoritative Specification: AMAN.XZ.txt
-- Note: Contains only foundational catalog entries for Yemen Telecom & default system settings
-- ==============================================================================

-- 1. Default System Settings
INSERT INTO system_settings (app_name, contact_email, terms_and_conditions, privacy_policy, renewal_warning_days)
VALUES (
    'AMAN | أمان',
    'support@aman.local',
    'شروط وأحكام استخدام منظومة أمان لحماية أرقام الاتصالات.',
    'سياسة خصوصية البيانات الخاصة بمنظومة أمان.',
    7
) ON CONFLICT DO NOTHING;

-- 2. Foundational Telecom Providers & Prefixes (Yemen Mobile, YOU, Sabafon, Y)
DO $$
DECLARE
    v_ym_id UUID;
    v_you_id UUID;
    v_saba_id UUID;
    v_y_id UUID;
BEGIN
    -- Yemen Mobile
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('يمن موبايل', 'YE-YM', 9, TRUE, TRUE, 1)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_ym_id;

    -- YOU (MTN previously)
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('يو (YOU)', 'YE-YOU', 9, TRUE, TRUE, 2)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_you_id;

    -- Sabafon
    INSERT INTO telecom_providers (name, code, number_length, is_active, is_visible_to_customer, display_order)
    VALUES ('سبأفون', 'YE-SABAFON', 9, TRUE, TRUE, 3)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
    RETURNING id INTO v_saba_id;

    -- Y Telecom
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

    -- Default Task Settings for each provider (Line 1274-1290)
    INSERT INTO task_settings (provider_id, first_task_enabled, first_task_amount, recurring_task_enabled, recurring_task_amount, repeat_interval_days, visibility_days_before_due, manual_reschedule_enabled)
    VALUES
        (v_ym_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE),
        (v_you_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE),
        (v_saba_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE),
        (v_y_id, TRUE, 500.0, TRUE, 500.0, 30, 3, TRUE)
    ON CONFLICT (provider_id) DO NOTHING;

    -- Sample Protection Plans for each provider
    INSERT INTO protection_plans (provider_id, name, price, duration_days, is_active, is_visible_to_customer)
    VALUES
        (v_ym_id, 'باقة حماية يمن موبايل السنوية', 20000.0, 365, TRUE, TRUE),
        (v_ym_id, 'باقة حماية يمن موبايل 6 أشهر', 11000.0, 180, TRUE, TRUE),
        (v_you_id, 'باقة حماية يو السنوية', 20000.0, 365, TRUE, TRUE),
        (v_saba_id, 'باقة حماية سبأفون السنوية', 20000.0, 365, TRUE, TRUE)
    ON CONFLICT DO NOTHING;

    -- Sample Payment Method
    INSERT INTO payment_methods (name, account_number, account_holder_name, instructions, is_active)
    VALUES
        ('محفظة جوالي', '777000111', 'أمان لخدمات حماية الأرقام', 'يرجى إرفاق رقم الحوالة أو صورة السند بعد إتمام الدفع', TRUE),
        ('محفظة كاش', '733000222', 'أمان لخدمات حماية الأرقام', 'يرجى إرفاق رقم العملية في خانة مرجع التحويل', TRUE)
    ON CONFLICT DO NOTHING;

END $$;
