-- AMAN.XZ1 - Migration 03: Indexes and Concurrency-Safe Unique Constraints
-- Authoritative Specification: AMAN.XZ.txt (Lines 1236-1238, 1431, 1463, 1491, 1524-1530)

-- 1. Partial Unique Index: Prevent conflicting pending requests for the same number
-- Rule: A phone number cannot have more than one pending protection request at any given time!
CREATE UNIQUE INDEX IF NOT EXISTS uq_pending_request_per_number 
ON protection_requests(customer_number_id) 
WHERE status = 'pending';

-- 2. Partial Unique Index: Prevent overlapping active protections for the same number
-- Rule: A phone number cannot have more than one active protection concurrently!
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_protection_per_number 
ON protections(customer_number_id) 
WHERE status = 'active';

-- 3. Partial Unique Index: Active telecom prefixes must be globally unique across active providers
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_telecom_prefix 
ON telecom_prefixes(prefix) 
WHERE is_active = TRUE;

-- 4. Foreign Key and Query Performance Indexes

-- Customer Numbers lookup
CREATE INDEX IF NOT EXISTS idx_customer_numbers_customer_id ON customer_numbers(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_numbers_provider_id ON customer_numbers(provider_id);
CREATE INDEX IF NOT EXISTS idx_customer_numbers_phone ON customer_numbers(phone_number);

-- Telecom Providers and Plans
CREATE INDEX IF NOT EXISTS idx_telecom_prefixes_provider ON telecom_prefixes(provider_id);
CREATE INDEX IF NOT EXISTS idx_protection_plans_provider ON protection_plans(provider_id) WHERE is_active = TRUE;

-- Protection Requests
CREATE INDEX IF NOT EXISTS idx_protection_requests_customer_id ON protection_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_protection_requests_status ON protection_requests(status);
CREATE INDEX IF NOT EXISTS idx_protection_requests_number ON protection_requests(customer_number_id);

-- Protections
CREATE INDEX IF NOT EXISTS idx_protections_customer_id ON protections(customer_id);
CREATE INDEX IF NOT EXISTS idx_protections_number_id ON protections(customer_number_id);
CREATE INDEX IF NOT EXISTS idx_protections_status_end_date ON protections(status, end_date);

-- Payment Tasks (Administration operations)
CREATE INDEX IF NOT EXISTS idx_payment_tasks_protection ON payment_tasks(protection_id);
CREATE INDEX IF NOT EXISTS idx_payment_tasks_number ON payment_tasks(customer_number_id);
CREATE INDEX IF NOT EXISTS idx_payment_tasks_status_due ON payment_tasks(status, due_date);

-- Notifications
CREATE INDEX IF NOT EXISTS idx_notifications_customer ON notifications(customer_id, is_read, created_at DESC);

-- Audit Logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_record ON audit_logs(affected_table, affected_record_id);
