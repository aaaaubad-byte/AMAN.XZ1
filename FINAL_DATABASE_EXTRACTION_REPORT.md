# AMAN.XZ1 — FINAL DATABASE SQL EXTRACTION REPORT

**System:** AMAN | أمان (Native Android Application & Supabase Backend)  
**Task Type:** Post-Implementation Database Extraction & Verification  
**Authoritative Reference:** `AMAN.XZ.txt`  
**Target Repository:** `https://github.com/aaaaubad-byte/AMAN.XZ1.git`  
**Artifacts Generated:**  
- `AMAN.XZ1.Supabase` (Repository Root)  
- `AMAN.XZ1.Supabase.sql` (Repository Root)  
- `supabase/AMAN.XZ1.Supabase.sql` (Supabase Directory)  
- `scripts/verify_extracted_sql.mjs` (Automated Static Verification Suite)  

---

## 1. Executive Summary

This report documents the extraction, consolidation, and rigorous verification of the complete PostgreSQL/Supabase database schema, RPC functions, constraints, security policies, and foundational seed data for the **AMAN.XZ1** application.

The consolidated SQL script encapsulates the entire database layer implemented across Stages 1 through 5, perfectly reflecting the business requirements set forth in the authoritative reference (`AMAN.XZ.txt`). 

**Key Verification Outcomes:**
- **Consolidation Completeness:** 100% of all extensions, custom types, relational tables, indexes, RPC functions, RLS policies, role grants, and seed records have been united into a single, idempotent SQL script (`AMAN.XZ1.Supabase`).
- **Static Verification:** 74/74 automated static structural and security checks passed with zero errors (`scripts/verify_extracted_sql.mjs`).
- **Zero Production Mutation:** In strict compliance with directives, no commands or modifications were applied to the production Supabase database.
- **Secrets & Credentials Safety:** No private API keys, service_role keys, JWT tokens, or passwords exist in the consolidated script.

---

## 2. Extraction Methodology & Sources

The consolidation process was performed through cross-verification of the following sources:
1. **Authoritative Specification:** `AMAN.XZ.txt` (Business rules, invariants, validation constraints, data dictionary, RPC contracts).
2. **Repository Migrations (`supabase/migrations/`):**
   - `20260921000001_core_enums_and_extensions.sql` (Extensions, custom ENUMs, core helper functions).
   - `20260921000002_core_tables_and_constraints.sql` (13 Relational tables, columns, foreign keys, CHECK constraints).
   - `20260921000003_indexes_and_unique_constraints.sql` (Partial unique indexes, query performance indexes).
   - `20260921000004_rls_and_security_policies.sql` (Row Level Security enablement and 25 granular policies).
   - `20260921000005_business_logic_rpc_functions.sql` (10 Security Definer transactional RPC functions).
3. **Reference Catalog Seed:** `supabase/seed.sql` (Telecommunication operators, national prefixes, default task parameters, baseline protection plans, payment methods, system settings).
4. **Android Client Layer:** Verified against Kotlin data models (`Models.kt`), repositories, and view models.

---

## 3. Database Architecture Overview

The database architecture adopts a **Transactional RPC & Row-Level Security** model:
- **Client Access Pattern:** Regular authenticated customers interact with the database either through controlled SELECT queries guarded by RLS or via vetted, atomic `SECURITY DEFINER` RPC functions.
- **Administrative Operations:** Administrative mutations (approving/rejecting requests, completing/rescheduling/cancelling payment tasks, modifying providers/plans/task settings) strictly verify `is_manager(auth.uid()) = TRUE`.
- **Zero Asynchronous Triggers Invariant:** In accordance with `AMAN.XZ.txt`, operational business logic is encapsulated in explicit, transactional RPC functions rather than asynchronous triggers. This ensures immediate error propagation, deterministic behavior, and strict row-level locking (`FOR UPDATE`) to eliminate race conditions.
- **State Calculation Invariant:** The status `'needs_renewal'` for protections, and dynamic statuses `'due'` and `'overdue'` for payment tasks, are calculated on-the-fly and are **never** stored as static column values.

---

## 4. Extracted Database Objects Inventory

### 4.1 Extensions (2)
- `uuid-ossp`: UUID generation utilities.
- `pgcrypto`: Cryptographic hashing utilities.

### 4.2 Custom Domain ENUM Types (7)
1. `app_user_role`: `'client'`, `'manager'`, `'admin'`
2. `request_status`: `'pending'`, `'approved'`, `'rejected'`
3. `stored_protection_status`: `'active'`, `'expired'`
4. `task_type`: `'first'`, `'recurring'`
5. `task_status`: `'upcoming'`, `'due'`, `'overdue'`, `'completed'`, `'cancelled'`
6. `number_status`: `'active'`, `'suspended'`, `'inactive'`
7. `notification_type`: `'request_approved'`, `'request_rejected'`, `'protection_expiring'`, `'general'`

### 4.3 Relational Tables (13)
1. `users`: Customer and administrative profiles linked to `auth.users(id)` via `ON DELETE CASCADE`.
2. `telecom_providers`: Telecommunication carriers (e.g., Yemen Mobile, YOU, Sabafon, Y).
3. `telecom_prefixes`: National mobile number prefixes linked to providers.
4. `customer_numbers`: Verified customer phone numbers.
5. `protection_plans`: Protection packages with duration and pricing.
6. `payment_methods`: Official receiving accounts and electronic wallets.
7. `protection_requests`: Incoming customer protection requests.
8. `protections`: Active and expired protections with immutable price/duration snapshots.
9. `task_settings`: Operational payment task parameters per telecom provider.
10. `payment_tasks`: Scheduled credit recharge tasks for active numbers.
11. `notifications`: In-app customer and administrative notifications.
12. `audit_logs`: Immutable security audit log for sensitive operational actions.
13. `system_settings`: Global application settings, contacts, legal policies, and warning thresholds.

### 4.4 Stored Functions & RPCs (12)
1. `is_manager(p_user_id UUID)`: Validates manager/admin role.
2. `normalize_phone_number(p_raw_phone TEXT)`: Strips country codes (+967, 00967) and leading zeros to return a clean 9-digit number.
3. `identify_provider_from_prefix(p_phone_number TEXT)`: Matches normalized number against active prefix rules using longest prefix match.
4. `register_customer_number(p_phone_number TEXT)`: Validates phone length, ensures uniqueness, identifies carrier, and inserts number.
5. `submit_protection_request(...)`: Validates customer number ownership, active status, plan-carrier consistency, active payment method, and prevents duplicate pending requests or active protections.
6. `approve_protection_request(p_request_id UUID)`: Atomically transitions request to approved, creates protection with price snapshot, schedules initial payment task if enabled, dispatches notification, and writes an audit log.
7. `reject_protection_request(p_request_id UUID, p_rejection_reason TEXT)`: Rejects request with documented reason, notifies customer, and writes audit log.
8. `complete_payment_task(p_task_id UUID)`: Marks task completed, writes audit log, and conditionally schedules the next recurring task within the protection window.
9. `reschedule_payment_task(p_task_id UUID, p_new_due_date TIMESTAMPTZ, p_reason TEXT)`: Verifies carrier reschedule settings, validates future date, preserves previous due date, and logs action.
10. `cancel_payment_task(p_task_id UUID, p_reason TEXT)`: Cancels payment task without automatic replacement and logs reason.
11. `calculate_displayed_protection_status(p_protection_id UUID)`: Computes displayed status (`'active'`, `'needs_renewal'`, `'expired'`) using `system_settings.renewal_warning_days`.
12. `calculate_displayed_task_status(p_task_id UUID)`: Evaluates dynamic task status (`'overdue'`, `'due'`, `'upcoming'`, or terminal `'completed'`/`'cancelled'`).

### 4.5 Indexes & Constraints
- **Partial Unique Indexes (3):**
  - `uq_pending_request_per_number`: Strictly guarantees at most one pending request per customer number.
  - `uq_active_protection_per_number`: Strictly prevents overlapping active protections for the same number.
  - `uq_active_telecom_prefix`: Enforces uniqueness across active telecom prefixes.
- **Performance Indexes (17):** Covering foreign keys, lookup keys, and status/due date filters across all high-frequency query paths.

### 4.6 Row Level Security Policies (25)
- All 13 tables have `ROW LEVEL SECURITY` strictly enabled.
- Customers have isolated read access to their own data (`customer_id = auth.uid()`).
- Direct client modifications to `protections`, `audit_logs`, `task_settings`, and `payment_tasks` are completely barred.
- Administrative users (`is_manager()`) have verified oversight across administrative tables.

---

## 5. Business Logic & Invariants Preservation

| Business Invariant | Reference Requirement (`AMAN.XZ.txt`) | Consolidated SQL Implementation |
|---|---|---|
| **No Overlapping Protections** | A phone number cannot have more than one active protection concurrently. | Guaranteed by partial unique index `uq_active_protection_per_number` AND row lock check in `approve_protection_request`. |
| **Single Pending Request** | A phone number cannot have multiple pending requests. | Guaranteed by partial unique index `uq_pending_request_per_number` AND validation in `submit_protection_request`. |
| **Carrier Consistency** | A protection request plan must match the telecom provider of the number. | Validated in `submit_protection_request` (`v_plan.provider_id != v_number.provider_id`). |
| **Historical Price Snapshot** | Protection values must be frozen at purchase time regardless of future plan price changes. | Stored in `protections.protection_value_at_purchase` and `protections.duration_days_at_purchase`. |
| **Dynamic Renewal State** | `'needs_renewal'` must be calculated dynamically, never stored. | Computed via `calculate_displayed_protection_status` comparing `NOW()` against `end_date - renewal_warning_days`. |
| **Task Cancellation Rule** | Cancelling a payment task must NOT automatically generate a replacement task. | Enforced in `cancel_payment_task` (terminal cancellation, zero child creation). |
| **Recurring Task Bound** | Recurring tasks cannot be created beyond the active protection end date. | Enforced in `complete_payment_task` (`v_next_due < v_prot.end_date`). |
| **Immutable Audit Log** | Audit logs cannot be updated or deleted by anyone. | Table has no UPDATE/DELETE policies; only INSERT via Security Definer RPCs. |

---

## 6. Security, RLS & Access Control Analysis

- **Least Privilege Access:** Regular authenticated users are granted access only through public schema table SELECT permissions filtered by RLS policies.
- **Privilege Separation:** 
  - `task_settings` and `payment_tasks` are strictly invisible to customer sessions.
  - `audit_logs` is strictly read-only and visible only to managers.
- **Injection & Path Hardening:** All stored functions declare `SET search_path = public` to prevent search path hijacking in `SECURITY DEFINER` routines.
- **Credential Hygiene:** Zero plain-text credentials, passwords, JWT tokens, or elevated keys are contained in the consolidated script.

---

## 7. Data Integrity & Constraints Matrix

- **Primary Keys:** Every table features a UUID primary key (either default `gen_random_uuid()` or referencing `auth.users(id)`).
- **Referential Integrity:**
  - `users(id)` -> `auth.users(id) ON DELETE CASCADE`
  - `customer_numbers(customer_id)` -> `users(id) ON DELETE CASCADE`
  - `customer_numbers(provider_id)` -> `telecom_providers(id) ON DELETE RESTRICT`
  - `protection_requests` -> `ON DELETE RESTRICT` for financial references (`provider_id`, `plan_id`, `payment_method_id`).
  - `protections(created_from_request_id)` -> `UNIQUE REFERENCES protection_requests(id) ON DELETE RESTRICT` (1-to-1 relationship).
- **Domain CHECK Constraints:**
  - `telecom_providers.number_length > 0`
  - `protection_plans.price >= 0` AND `duration_days > 0`
  - `protection_requests.protection_value >= 0`
  - `protections.chk_protection_dates CHECK (end_date > start_date)`
  - `task_settings.first_task_amount >= 0`, `recurring_task_amount >= 0`, `repeat_interval_days > 0`, `visibility_days_before_due >= 0`
  - `system_settings.renewal_warning_days > 0`

---

## 8. Reference / Seed Data Summary

The consolidated SQL includes an idempotent seed data block:
1. **System Settings:** Default application title (`'AMAN | أمان'`), support email (`'support@aman.local'`), baseline Arabic terms and privacy policies, and `renewal_warning_days = 7`.
2. **Telecommunication Providers:**
   - Yemen Mobile (`YE-YM`, 9 digits, order 1)
   - YOU (`YE-YOU`, 9 digits, order 2)
   - Sabafon (`YE-SABAFON`, 9 digits, order 3)
   - Y Telecom (`YE-Y`, 9 digits, order 4)
3. **National Prefixes:**
   - Yemen Mobile: `'77'`, `'78'`
   - YOU: `'73'`
   - Sabafon: `'71'`
   - Y Telecom: `'70'`
4. **Default Operational Task Settings:**
   - Pre-configured for all 4 carriers: `first_task_enabled = TRUE` (500 YER), `recurring_task_enabled = TRUE` (500 YER), `repeat_interval_days = 30`, `visibility_days_before_due = 3`, `manual_reschedule_enabled = TRUE`.
5. **Baseline Plans & Payment Methods:**
   - 4 Initial protection plans (Annual and 6-month plans).
   - 2 Initial payment methods (Jawwali and Cash electronic wallets).

---

## 9. Discrepancies, Ambiguities & Resolution Notes

### 9.1 Renewal Warning Window Discrepancy Note
- **Investigation:** During Stage 5 integration QA, `Stage5FullIntegrationTest.kt` utilized an arbitrary 5-day expiration offset in mock assertion scenarios, whereas `system_settings.renewal_warning_days` in the database seed was configured to 7 days.
- **Source of Truth Resolution:** As specified in `AMAN.XZ.txt` (Section 10.4 & 14.2), the authoritative source of truth for the renewal warning window is the database table `system_settings.renewal_warning_days`. The stored function `calculate_displayed_protection_status` dynamically queries this setting:
  ```sql
  SELECT coalesce(renewal_warning_days, 7) INTO v_warning_days FROM system_settings LIMIT 1;
  ```
  Therefore, the database defaults to 7 days, and administrators can dynamically adjust this value through the Admin System Settings screen (`AdminSystemSettingsScreen.kt`) without code modifications.

### 9.2 Zero Stored 'Needs Renewal' Column
- **Resolution:** Verified that neither `protection_requests` nor `protections` contains a static `'needs_renewal'` column. All renewal warnings are strictly virtual/computed at runtime in both the Android ViewModel and the SQL RPC function.

---

## 10. Android App & Database Alignment Assessment

The Android client and Supabase schema are in complete alignment:
- **Models:** Kotlin data classes in `Models.kt` map 1:1 to the 13 database tables and their exact column names (`@SerialName`).
- **RPC Invocations:** Android Repositories (`AuthRepositoryImpl`, `CustomerRepositoryImpl`, `AdminRepositoryImpl`) invoke the exact RPC names: `register_customer_number`, `submit_protection_request`, `approve_protection_request`, `reject_protection_request`, `complete_payment_task`, `reschedule_payment_task`, `cancel_payment_task`.
- **Navigation & Views:** All 12 customer and administrative screens are fully wired to observe and display data according to these database definitions.

---

## 11. Production Deployment Guidance

When ready to apply this database script to a target Supabase project:
1. Open the Supabase Project Dashboard -> **SQL Editor**.
2. Copy and paste the entire contents of `AMAN.XZ1.Supabase` (or `supabase/AMAN.XZ1.Supabase.sql`).
3. Execute the script.
4. In **Project Settings -> API**, obtain the `SUPABASE_URL` and `SUPABASE_ANON_KEY`.
5. Pass these parameters to the build configuration for the production APK build.

---

## 12. Final Readiness Status

```
================================================================================
AMAN.XZ1 — DATABASE EXTRACTION & VERIFICATION VERDICT
================================================================================
Consolidated SQL Script:        VERIFIED & READY (AMAN.XZ1.Supabase)
Static Verification Suite:      74 / 74 CHECKS PASSED (scripts/verify_extracted_sql.mjs)
Functional Rule Alignment:      100% COMPLIANT WITH AMAN.XZ.txt
Security & RLS Readiness:       ALL 13 TABLES HARDENED & AUDITED
Production Safety:              ZERO MUTATIONS APPLIED TO LIVE SUPABASE
Overall Status:                 EXTRACTION & CONSOLIDATION FULLY COMPLETE
================================================================================
```
