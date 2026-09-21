-- AMAN.XZ1 - Migration 01: Core Extensions and Domain ENUMs
-- Authoritative Specification: AMAN.XZ.txt (Lines 1062 - 1792)

-- 1. Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Domain ENUMs matching AMAN.XZ.txt strictly

-- User roles: 'client' (عميل), 'manager' (مدير مسؤل), 'admin' (مدير نظام)
DO $$ BEGIN
    CREATE TYPE app_user_role AS ENUM ('client', 'manager', 'admin');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Protection Request Statuses (Lines 1196-1200)
-- قيد المراجعة (pending), مقبول (approved), مرفوض (rejected)
DO $$ BEGIN
    CREATE TYPE request_status AS ENUM ('pending', 'approved', 'rejected');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Stored Protection Statuses (Lines 1229-1235 & 1561-1575)
-- نشطة (active), منتهية (expired)
-- CRITICAL INVARIANT: 'needs_renewal' is a dynamically calculated display state, NEVER stored in the database!
DO $$ BEGIN
    CREATE TYPE stored_protection_status AS ENUM ('active', 'expired');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Payment Task Types (Lines 1257-1260)
-- مهمة أولى (first), مهمة دورية (recurring)
DO $$ BEGIN
    CREATE TYPE task_type AS ENUM ('first', 'recurring');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Payment Task Statuses (Lines 1261-1267)
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

-- Notification Event Types (Lines 1306-1310)
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
