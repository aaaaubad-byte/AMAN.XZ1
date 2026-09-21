# AMAN.XZ1 — Database Architecture & Entity Specifications
**Authoritative Reference**: `AMAN.XZ.txt` (Lines 1062–1792)  
**Target Environment**: PostgreSQL 15+ / Supabase  

---

## 1. Relational Entity Hierarchy
```text
users (linked to auth.users)
├── customer_numbers
├── protection_requests
├── protections
└── notifications

telecom_providers
├── customer_numbers
├── telecom_prefixes
├── protection_plans
└── task_settings

customer_numbers
├── protection_requests
├── protections
└── payment_tasks

protection_plans
└── protection_requests / protections

payment_methods
└── protection_requests

protection_requests
└── protections

protections
└── payment_tasks
```

---

## 2. Table Catalog

### `users`
* **Purpose**: Application-level profile linked to `auth.users(id)` via Foreign Key Cascade.
* **Fields**:
  * `id UUID PRIMARY KEY REFERENCES auth.users(id)`
  * `name TEXT NOT NULL`
  * `email TEXT NOT NULL UNIQUE`
  * `role app_user_role NOT NULL DEFAULT 'client'` ('client', 'manager', 'admin')
  * `account_status TEXT NOT NULL DEFAULT 'active'`
  * `created_at`, `updated_at`

### `telecom_providers`
* **Purpose**: Telecommunication providers supported in the system.
* **Fields**:
  * `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  * `name TEXT NOT NULL` (e.g. 'يمن موبايل', 'يو (YOU)', 'سبأفون', 'واي (Y)')
  * `code TEXT NOT NULL UNIQUE` (e.g. 'YE-YM', 'YE-YOU')
  * `number_length INT NOT NULL DEFAULT 9`
  * `is_active BOOLEAN NOT NULL DEFAULT TRUE`
  * `is_visible_to_customer BOOLEAN NOT NULL DEFAULT TRUE`
  * `display_order INT NOT NULL DEFAULT 0`

### `telecom_prefixes`
* **Purpose**: Phone prefixes used for 100% automatic provider detection.
* **Fields**:
  * `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  * `provider_id UUID REFERENCES telecom_providers(id) ON DELETE CASCADE`
  * `prefix TEXT NOT NULL UNIQUE` (e.g. '77', '78', '73', '71', '70')
  * `is_active BOOLEAN NOT NULL DEFAULT TRUE`

### `customer_numbers`
* **Purpose**: Numbers registered by customers.
* **Rules**:
  * Each number belongs to exactly one customer.
  * Duplicate phone numbers across the entire system are prohibited (`phone_number TEXT NOT NULL UNIQUE`).
  * `provider_id` is resolved automatically via prefix lookup. Customers never choose providers manually.

### `protection_plans`
* **Purpose**: Protection packages offered per provider.
* **Fields**:
  * `id UUID PRIMARY KEY`
  * `provider_id UUID REFERENCES telecom_providers(id)`
  * `name TEXT NOT NULL`
  * `price NUMERIC(12, 2) NOT NULL CHECK (price >= 0)`
  * `duration_days INT NOT NULL CHECK (duration_days > 0)`
  * `is_active BOOLEAN NOT NULL DEFAULT TRUE`
  * `is_visible_to_customer BOOLEAN NOT NULL DEFAULT TRUE`

### `payment_methods`
* **Purpose**: Approved electronic wallets / bank accounts for payments.
* **Fields**:
  * `id UUID PRIMARY KEY`
  * `name TEXT NOT NULL` (e.g. 'محفظة جوالي')
  * `account_number TEXT NOT NULL`
  * `account_holder_name TEXT NOT NULL`
  * `instructions TEXT`
  * `is_active BOOLEAN NOT NULL DEFAULT TRUE`

### `protection_requests`
* **Purpose**: Protection subscription requests submitted by customers for administrative review.
* **Statuses**: `'pending'`, `'approved'`, `'rejected'`.
* **Rules**:
  * Plan must belong to the exact same provider as the customer number.
  * Partial unique index prevents multiple pending requests for the same number.

### `protections`
* **Purpose**: Active and historical protections created upon approval.
* **Stored Statuses**: `'active'`, `'expired'` (NEVER 'needs_renewal'!).
* **Snapshots**: Captures `protection_value_at_purchase` and `duration_days_at_purchase` so subsequent plan edits never alter historical contracts.
* **Concurrency**: Partial unique index prevents overlapping active protections on the same number.

### `task_settings`
* **Purpose**: Operational configuration per telecom provider.
* **Fields**: `first_task_enabled`, `first_task_amount`, `recurring_task_enabled`, `recurring_task_amount`, `repeat_interval_days`, `visibility_days_before_due`, `manual_reschedule_enabled`.

### `payment_tasks`
* **Purpose**: Internal administrative tasks for periodic line recharge / maintenance.
* **Types**: `'first'`, `'recurring'`.
* **Statuses**: `'upcoming'`, `'due'`, `'overdue'`, `'completed'`, `'cancelled'`.
* **Rules**: Strictly internal data. Completely hidden from customer interface.

### `notifications`
* **Purpose**: Customer and administrative notifications.
* **Types**: `'request_approved'`, `'request_rejected'`, `'protection_expiring'`, `'general'`.

### `audit_logs`
* **Purpose**: Immutable audit log of all sensitive operations executed via trusted RPCs.

### `system_settings`
* **Purpose**: General application configuration (Terms, Privacy, Contact, Warning Window).
