-- ==============================================================================
-- AMAN (أمان) — بيانات البداية الأولية (Initial Seed Data)
-- ==============================================================================

-- 1. إدراج شركات الاتصالات اليمنية
INSERT INTO public.telecom_companies (name, code, task_cycle_days, is_active)
VALUES
    ('يمن موبايل', 'yemen_mobile', 30, true),
    ('يو (YOU)', 'you', 30, true),
    ('سبأفون', 'sabafon', 30, true),
    ('واي (Y)', 'y', 30, true)
ON CONFLICT (code) DO NOTHING;

-- 2. إدراج باقات الحماية القياسية
INSERT INTO public.protection_packages (name, duration_days, price, currency, description, is_active)
VALUES
    ('باقة الحماية السنوية الكاملة', 365, 20000, 'YER', 'تجديد دوري كل 30 يوماً وضمان بقاء الرقم فعالاً طوال العام', true),
    ('باقة الحماية نصف السنوية', 180, 11000, 'YER', 'تجديد دوري كل 30 يوماً لمدة 6 أشهر مع تنبيهات استباقية', true),
    ('باقة الحماية الربعية (3 أشهر)', 90, 6000, 'YER', 'حماية الرقم ومتابعته لمدة 90 يوماً', true)
ON CONFLICT DO NOTHING;
