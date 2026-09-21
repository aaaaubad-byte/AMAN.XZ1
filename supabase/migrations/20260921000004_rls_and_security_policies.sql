-- AMAN.XZ1 - Migration 04: Row Level Security (RLS) and Granular Access Policies
-- Authoritative Specification: AMAN.XZ.txt (Lines 1080-1083, 1396, 1676-1708)

-- 1. Helper Function: Verify if caller is an authorized Manager or Administrator
CREATE OR REPLACE FUNCTION is_manager(p_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM users 
        WHERE id = p_user_id 
          AND role IN ('manager', 'admin')
          AND account_status = 'active'
    );
$$;

-- 2. Enable RLS on ALL tables
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

-- ----------------------------------------------------
-- 3. Policies: users
-- ----------------------------------------------------
-- Customers can view and edit their own profile
CREATE POLICY "Users can view own profile" 
ON users FOR SELECT 
TO authenticated 
USING (id = auth.uid() OR is_manager());

CREATE POLICY "Users can update own profile" 
ON users FOR UPDATE 
TO authenticated 
USING (id = auth.uid() OR is_manager())
WITH CHECK (
    -- Customers cannot escalate their own role
    (id = auth.uid() AND role = (SELECT role FROM users WHERE id = auth.uid()))
    OR is_manager()
);

-- Allow new user insertion upon registration (or by managers)
CREATE POLICY "Allow user registration" 
ON users FOR INSERT 
TO authenticated 
WITH CHECK (id = auth.uid() OR is_manager());

-- ----------------------------------------------------
-- 4. Policies: telecom_providers
-- ----------------------------------------------------
-- Active & visible providers are readable by any authenticated user
CREATE POLICY "Allow customers to view active providers" 
ON telecom_providers FOR SELECT 
TO authenticated 
USING ((is_active = TRUE AND is_visible_to_customer = TRUE) OR is_manager());

-- Only managers can insert, update, or delete providers
CREATE POLICY "Managers can manage providers" 
ON telecom_providers FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 5. Policies: telecom_prefixes
-- ----------------------------------------------------
-- Prefixes are internal system data; readable by authenticated users or through RPC
CREATE POLICY "Allow view telecom prefixes" 
ON telecom_prefixes FOR SELECT 
TO authenticated 
USING (is_active = TRUE OR is_manager());

CREATE POLICY "Managers can manage prefixes" 
ON telecom_prefixes FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 6. Policies: customer_numbers
-- ----------------------------------------------------
-- Customers can ONLY see their own numbers; managers can see all
CREATE POLICY "Customers can view own numbers" 
ON customer_numbers FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

-- Direct customer deletion/update is prevented; managed via RPC or manager
CREATE POLICY "Managers can manage numbers" 
ON customer_numbers FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 7. Policies: protection_plans
-- ----------------------------------------------------
-- Customers can view active & visible plans
CREATE POLICY "Customers can view active plans" 
ON protection_plans FOR SELECT 
TO authenticated 
USING ((is_active = TRUE AND is_visible_to_customer = TRUE) OR is_manager());

CREATE POLICY "Managers can manage plans" 
ON protection_plans FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 8. Policies: payment_methods
-- ----------------------------------------------------
-- Customers can view active payment methods
CREATE POLICY "Customers can view active payment methods" 
ON payment_methods FOR SELECT 
TO authenticated 
USING (is_active = TRUE OR is_manager());

CREATE POLICY "Managers can manage payment methods" 
ON payment_methods FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 9. Policies: protection_requests
-- ----------------------------------------------------
-- Customers can only see their own requests
CREATE POLICY "Customers can view own requests" 
ON protection_requests FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

-- Customers cannot update requests directly (approval/rejection is manager RPC)
CREATE POLICY "Managers can manage requests" 
ON protection_requests FOR UPDATE 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 10. Policies: protections
-- ----------------------------------------------------
-- Customers can only see their own protections
CREATE POLICY "Customers can view own protections" 
ON protections FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

-- Direct INSERT/UPDATE/DELETE forbidden to customers.
-- Protections are strictly created through the trusted approve_protection_request RPC!
CREATE POLICY "Managers can manage protections" 
ON protections FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 11. Policies: task_settings
-- ----------------------------------------------------
-- STRICTLY internal operational data. Zero customer access!
CREATE POLICY "Task settings accessible only to managers" 
ON task_settings FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 12. Policies: payment_tasks
-- ----------------------------------------------------
-- STRICTLY internal operational data. Zero customer access!
CREATE POLICY "Payment tasks accessible only to managers" 
ON payment_tasks FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());

-- ----------------------------------------------------
-- 13. Policies: notifications
-- ----------------------------------------------------
-- Customers can view their own notifications
CREATE POLICY "Customers can view own notifications" 
ON notifications FOR SELECT 
TO authenticated 
USING (customer_id = auth.uid() OR is_manager());

-- Customers can only update their own notifications to mark as read
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

-- ----------------------------------------------------
-- 14. Policies: audit_logs
-- ----------------------------------------------------
-- Read-only for managers; customers have zero access; mutations only via SECURITY DEFINER functions
CREATE POLICY "Audit logs visible only to managers" 
ON audit_logs FOR SELECT 
TO authenticated 
USING (is_manager());

-- ----------------------------------------------------
-- 15. Policies: system_settings
-- ----------------------------------------------------
CREATE POLICY "System settings readable by authenticated users" 
ON system_settings FOR SELECT 
TO authenticated 
USING (TRUE);

CREATE POLICY "System settings editable only by managers" 
ON system_settings FOR ALL 
TO authenticated 
USING (is_manager())
WITH CHECK (is_manager());
