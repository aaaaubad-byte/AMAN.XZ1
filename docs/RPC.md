# AMAN.XZ1 — Business Logic RPC Functions
**Authoritative Reference**: `AMAN.XZ.txt` (Lines 1399–1412, 1710–1728)  

---

## 1. Catalog of Trusted Operations

### `identify_provider_from_prefix(p_phone_number TEXT) -> UUID`
* **Purpose**: Automatically and deterministically resolves the provider ID from the phone prefix.
* **Security**: `STABLE SECURITY DEFINER`.
* **Behavior**: Normalizes national phone number, queries `telecom_prefixes` for the longest matching active prefix, and returns the provider ID. Raises exception `P0001` if prefix is unknown or inactive.

---

### `register_customer_number(p_phone_number TEXT) -> UUID`
* **Purpose**: Registers a new customer number under the caller's account.
* **Security**: `SECURITY DEFINER SET search_path = public`.
* **Checks**:
  1. Caller authenticated (`auth.uid() IS NOT NULL`).
  2. Number format valid and not previously registered (`23505` if duplicate).
  3. Prefix identified deterministically.
  4. Number length matches provider's `number_length`.
* **Returns**: New `customer_numbers.id`.

---

### `submit_protection_request(...) -> UUID`
* **Purpose**: Submits a protection request with validation of all relationships.
* **Parameters**: `p_customer_number_id`, `p_plan_id`, `p_payment_method_id`, `p_transfer_reference`, `p_transfer_proof_url`.
* **Checks**:
  1. Caller owns the customer number.
  2. Customer number status is active.
  3. No pending request already exists for this number (`23505`).
  4. No active protection already exists for this number (`23505`).
  5. Plan is active and matches the customer number's provider (`P0004` if mismatch).
  6. Payment method is active.
* **Returns**: New `protection_requests.id`.

---

### `approve_protection_request(p_request_id UUID) -> UUID`
* **Purpose**: Atomically approves a pending request, activates protection, schedules first task, and notifies the customer.
* **Security**: `SECURITY DEFINER SET search_path = public` (Manager only).
* **Transaction Flow**:
  1. Locks request row `FOR UPDATE`.
  2. Verifies `status = 'pending'`.
  3. Verifies no concurrent active protection exists for this number.
  4. Sets request `status = 'approved'`, `reviewing_manager_id`, `reviewed_at`.
  5. Creates `protections` row capturing historical snapshots (`protection_value_at_purchase`, `duration_days_at_purchase`).
  6. Inspects `task_settings`: if `first_task_enabled = TRUE`, creates first task.
  7. Inserts customer notification (`request_approved`).
  8. Writes immutable audit log record.
  9. Commits atomically or rolls back entirely on any error.
* **Returns**: New `protections.id`.

---

### `reject_protection_request(p_request_id UUID, p_rejection_reason TEXT) -> VOID`
* **Purpose**: Rejects a pending request, records manager notes, and notifies customer.
* **Security**: Manager only.
* **Checks**: Row lock, verifies `status = 'pending'`. Does NOT create a protection.

---

### `complete_payment_task(p_task_id UUID) -> UUID`
* **Purpose**: Marks a task as completed and generates the next recurring task if enabled and eligible.
* **Security**: Manager only.
* **Checks**: Task status is `upcoming`, `due`, or `overdue`.
* **Recurring Flow**:
  1. Checks `task_settings.recurring_task_enabled`.
  2. Calculates next due date (`NOW() + repeat_interval_days`).
  3. Ensures next due date is within active protection end date.
  4. Creates recurring task and logs audit.

---

### `reschedule_payment_task(p_task_id UUID, p_new_due_date TIMESTAMPTZ, p_reason TEXT) -> VOID`
* **Purpose**: Reschedules a task to a future date.
* **Rules**: Enforces `task_settings.manual_reschedule_enabled`. Preserves `previous_due_date` for operational audit.

---

### `cancel_payment_task(p_task_id UUID, p_reason TEXT) -> VOID`
* **Purpose**: Cancels a task with documented reason.
* **Rules**: Does NOT automatically create a replacement task.

---

### `calculate_displayed_protection_status(p_protection_id UUID) -> TEXT`
* **Purpose**: Dynamically computes customer-facing protection state.
* **States**:
  - `'expired'`: If `NOW() >= end_date` or stored status is `expired`.
  - `'needs_renewal'`: If `NOW() >= (end_date - renewal_warning_days)`.
  - `'active'`: Otherwise.

---

### `calculate_displayed_task_status(p_task_id UUID) -> TEXT`
* **Purpose**: Classifies task status for operational displays without mutating terminal records.
* **States**:
  - `'completed'`, `'cancelled'`: Preserved directly from stored status.
  - `'overdue'`: If `CURRENT_DATE > due_date::DATE`.
  - `'due'`: If `CURRENT_DATE = due_date::DATE`.
  - `'upcoming'`: If `CURRENT_DATE < due_date::DATE`.
