# AMAN.XZ1 — Row Level Security (RLS) Policy Architecture
**Authoritative Reference**: `AMAN.XZ.txt` (Lines 1396, 1676–1708)  

---

## 1. Security Philosophy & Invariants
1. **Zero Client Trust**: The database is the ultimate security boundary. Android client logic is for UX only.
2. **Customer Data Isolation**: Customers can ONLY read their own profile, numbers, requests, protections, and notifications.
3. **No Cross-Customer Leakage**: Any attempt to read or mutate another customer's record yields an empty result set or error.
4. **Direct Write Prohibitions**:
   - Customers CANNOT insert directly into `protections`.
   - Customers CANNOT update `protection_requests` status (Approval & Rejection are manager-only RPCs).
   - Customers have ZERO access to `payment_tasks` and `task_settings`.
   - Customers CANNOT forge `audit_logs`.

---

## 2. Manager Authorization Gate
Authorization is checked via the helper function:
```sql
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
```

---

## 3. Table-by-Table Policy Matrix

| Table | SELECT | INSERT | UPDATE | DELETE |
| :--- | :--- | :--- | :--- | :--- |
| `users` | `id = auth.uid() OR is_manager()` | `id = auth.uid() OR is_manager()` | Own profile (role immutable) OR manager | Manager only |
| `telecom_providers` | Active & visible OR manager | Manager only | Manager only | Manager only |
| `telecom_prefixes` | Active OR manager | Manager only | Manager only | Manager only |
| `customer_numbers` | `customer_id = auth.uid() OR is_manager()` | Via RPC `register_customer_number` OR manager | Manager only | Manager only |
| `protection_plans` | Active & visible OR manager | Manager only | Manager only | Manager only |
| `payment_methods` | Active OR manager | Manager only | Manager only | Manager only |
| `protection_requests` | `customer_id = auth.uid() OR is_manager()` | Via RPC `submit_protection_request` | Manager only (via RPC) | Manager only |
| `protections` | `customer_id = auth.uid() OR is_manager()` | Blocked (Only via `approve_protection_request`) | Manager only | Manager only |
| `task_settings` | Manager only | Manager only | Manager only | Manager only |
| `payment_tasks` | Manager only | Manager only (via RPC) | Manager only (via RPC) | Manager only |
| `notifications` | `customer_id = auth.uid() OR is_manager()` | System/Manager only | Own notifications (`is_read = true`) | Manager only |
| `audit_logs` | Manager only | Blocked (Only via RPCs) | Blocked (Immutable) | Blocked (Immutable) |
| `system_settings` | All authenticated | Manager only | Manager only | Manager only |
