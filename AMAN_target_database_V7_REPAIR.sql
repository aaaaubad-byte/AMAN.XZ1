-- AMAN — Target Database FINAL
-- Source of requirements: AMAN.XZ.txt only
-- This script creates the database model from scratch.
-- Implementation choices (types, indexes, exact RPC names/signatures) are technical
-- choices required to implement the explicit requirements in AMAN.XZ.

begin;

create extension if not exists pgcrypto;

-- ============================================================
-- ENUMS
-- ============================================================

do $$ begin
  create type public.user_role as enum ('customer','manager');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.account_status as enum ('active','inactive');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.number_status as enum ('active','inactive');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.protection_request_status as enum ('pending','approved','rejected');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.protection_status as enum ('active','expired');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.task_type as enum ('first','recurring');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.task_status as enum ('upcoming','due','overdue','completed','cancelled');
exception when duplicate_object then null; end $$;

-- ============================================================
-- 1. USERS
-- ============================================================

create table if not exists public.users (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null,
  email text not null,
  role public.user_role not null default 'customer',
  status public.account_status not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists users_email_unique
  on public.users (lower(email));

-- ============================================================
-- 2. TELECOM PROVIDERS
-- ============================================================

create table if not exists public.telecom_providers (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  code text not null,
  phone_length integer not null,
  is_active boolean not null default true,
  is_visible_to_customers boolean not null default true,
  display_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists telecom_providers_code_unique
  on public.telecom_providers (lower(code));

-- AMAN.XZ explicitly requires a provider -> telecom_prefixes relation.
-- Therefore the prefix storage is represented as its own technical table.
create table if not exists public.telecom_prefixes (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.telecom_providers(id) on delete cascade,
  prefix text not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint telecom_prefixes_prefix_nonempty check (length(trim(prefix)) > 0),
  constraint telecom_prefixes_prefix_format check (trim(prefix) ~ '^[0-9+]+$')
);

create unique index if not exists telecom_prefixes_provider_prefix_unique
  on public.telecom_prefixes (provider_id, prefix);

create unique index if not exists telecom_prefixes_active_prefix_unique
  on public.telecom_prefixes (prefix)
  where is_active = true;

-- ============================================================
-- 3. CUSTOMER NUMBERS
-- ============================================================

create table if not exists public.customer_numbers (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.users(id) on delete cascade,
  provider_id uuid not null references public.telecom_providers(id) on delete restrict,
  phone_number text not null,
  status public.number_status not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists customer_numbers_phone_unique
  on public.customer_numbers (phone_number);

create index if not exists customer_numbers_customer_idx
  on public.customer_numbers (customer_id);

create index if not exists customer_numbers_provider_idx
  on public.customer_numbers (provider_id);

-- ============================================================
-- 4. PROTECTION PLANS
-- ============================================================

create table if not exists public.protection_plans (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.telecom_providers(id) on delete restrict,
  name text not null,
  price numeric(14,2) not null,
  protection_duration_days integer not null,
  is_active boolean not null default true,
  is_visible_to_customers boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint protection_plans_price_nonnegative check (price >= 0),
  constraint protection_plans_duration_positive check (protection_duration_days > 0)
);

create index if not exists protection_plans_provider_idx
  on public.protection_plans (provider_id);

-- ============================================================
-- 5. PAYMENT METHODS
-- ============================================================

create table if not exists public.payment_methods (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  account_number text not null,
  account_owner_name text not null,
  payment_instructions text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ============================================================
-- 6. PROTECTION REQUESTS
-- ============================================================

create table if not exists public.protection_requests (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.users(id) on delete restrict,
  customer_number_id uuid not null references public.customer_numbers(id) on delete restrict,
  provider_id uuid not null references public.telecom_providers(id) on delete restrict,
  plan_id uuid not null references public.protection_plans(id) on delete restrict,
  payment_method_id uuid not null references public.payment_methods(id) on delete restrict,

  protection_value numeric(14,2) not null,
  transfer_data jsonb,
  status public.protection_request_status not null default 'pending',
  rejection_reason text,

  reviewed_by uuid references public.users(id) on delete set null,
  reviewed_at timestamptz,

  -- Snapshots prevent later edits to plan/payment configuration
  -- from rewriting the historical request.
  plan_name_snapshot text not null,
  plan_price_snapshot numeric(14,2) not null,
  plan_duration_days_snapshot integer not null,

  payment_method_name_snapshot text not null,
  payment_account_number_snapshot text not null,
  payment_account_owner_snapshot text not null,
  payment_instructions_snapshot text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint protection_requests_value_nonnegative check (protection_value >= 0),
  constraint protection_requests_plan_price_nonnegative check (plan_price_snapshot >= 0),
  constraint protection_requests_plan_duration_positive check (plan_duration_days_snapshot > 0),

  constraint protection_requests_review_consistency check (
    (status = 'pending' and reviewed_at is null)
    or
    (status in ('approved','rejected') and reviewed_at is not null)
  ),

  constraint protection_requests_rejection_reason_consistency check (
    (status = 'rejected' and nullif(trim(rejection_reason), '') is not null)
    or
    (status in ('pending','approved'))
  )
);

create index if not exists protection_requests_customer_idx
  on public.protection_requests (customer_id);

create index if not exists protection_requests_number_idx
  on public.protection_requests (customer_number_id);

create index if not exists protection_requests_status_idx
  on public.protection_requests (status);

create index if not exists protection_requests_created_idx
  on public.protection_requests (created_at desc);

create unique index if not exists protection_requests_one_pending_per_number
  on public.protection_requests (customer_number_id)
  where status = 'pending';

-- ============================================================
-- 7. PROTECTIONS
-- ============================================================

create table if not exists public.protections (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.users(id) on delete restrict,
  customer_number_id uuid not null references public.customer_numbers(id) on delete restrict,
  provider_id uuid not null references public.telecom_providers(id) on delete restrict,
  plan_id uuid not null references public.protection_plans(id) on delete restrict,
  created_from_request_id uuid not null unique references public.protection_requests(id) on delete restrict,

  protection_value numeric(14,2) not null,
  protection_duration_days integer not null,
  start_date date not null,
  end_date date not null,
  status public.protection_status not null default 'active',

  -- Historical snapshots.
  plan_name_snapshot text not null,
  provider_name_snapshot text not null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint protections_value_nonnegative check (protection_value >= 0),
  constraint protections_duration_positive check (protection_duration_days > 0),
  constraint protections_date_order check (end_date >= start_date)
);

create index if not exists protections_customer_idx
  on public.protections (customer_id);

create index if not exists protections_number_idx
  on public.protections (customer_number_id);

create index if not exists protections_status_idx
  on public.protections (status);

create index if not exists protections_end_date_idx
  on public.protections (end_date);

-- No overlapping active protection for the same number.
create extension if not exists btree_gist;

alter table public.protections
  drop constraint if exists protections_no_overlapping_active;

alter table public.protections
  add constraint protections_no_overlapping_active
  exclude using gist (
    customer_number_id with =,
    daterange(start_date, end_date + 1, '[)') with &&
  )
  where (status = 'active');

-- ============================================================
-- 8. TASK SETTINGS
-- ============================================================

create table if not exists public.task_settings (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null unique references public.telecom_providers(id) on delete cascade,

  first_task_enabled boolean not null default false,
  first_task_amount numeric(14,2) not null default 0,

  recurring_task_enabled boolean not null default false,
  recurring_task_amount numeric(14,2) not null default 0,
  repeat_interval_days integer not null default 30,

  days_visible_before_due integer not null default 7,
  manual_reschedule_enabled boolean not null default true,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint task_settings_first_amount_nonnegative check (first_task_amount >= 0),
  constraint task_settings_recurring_amount_nonnegative check (recurring_task_amount >= 0),
  constraint task_settings_interval_positive check (repeat_interval_days > 0),
  constraint task_settings_visibility_nonnegative check (days_visible_before_due >= 0)
);

-- ============================================================
-- 9. PAYMENT TASKS
-- ============================================================

create table if not exists public.payment_tasks (
  id uuid primary key default gen_random_uuid(),
  protection_id uuid not null references public.protections(id) on delete restrict,
  customer_number_id uuid not null references public.customer_numbers(id) on delete restrict,

  task_type public.task_type not null,
  amount numeric(14,2) not null,
  due_date date not null,
  status public.task_status not null default 'upcoming',

  completed_at timestamptz,
  completed_by uuid references public.users(id) on delete set null,

  previous_due_date date,
  rescheduled_at timestamptz,
  rescheduled_by uuid references public.users(id) on delete set null,
  reschedule_reason text,

  cancelled_at timestamptz,
  cancelled_by uuid references public.users(id) on delete set null,
  cancellation_reason text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint payment_tasks_amount_nonnegative check (amount >= 0),

  constraint payment_tasks_completed_consistency check (
    (status = 'completed' and completed_at is not null and completed_by is not null)
    or
    (status <> 'completed')
  ),

  constraint payment_tasks_cancelled_consistency check (
    (status = 'cancelled' and cancelled_at is not null and cancelled_by is not null)
    or
    (status <> 'cancelled')
  )
);

create index if not exists payment_tasks_protection_idx
  on public.payment_tasks (protection_id);

create index if not exists payment_tasks_number_idx
  on public.payment_tasks (customer_number_id);

create index if not exists payment_tasks_due_date_idx
  on public.payment_tasks (due_date);

create index if not exists payment_tasks_status_idx
  on public.payment_tasks (status);

-- ============================================================
-- 10. NOTIFICATIONS
-- ============================================================

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid references public.users(id) on delete cascade,
  title text not null,
  message text not null,
  notification_type text not null,
  is_read boolean not null default false,
  read_at timestamptz,
  created_at timestamptz not null default now(),

  constraint notifications_read_consistency check (
    (is_read = false and read_at is null)
    or
    (is_read = true and read_at is not null)
  )
);

create index if not exists notifications_customer_idx
  on public.notifications (customer_id);

create index if not exists notifications_unread_idx
  on public.notifications (customer_id, is_read, created_at desc);

-- ============================================================
-- 11. AUDIT LOGS
-- ============================================================

create table if not exists public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references public.users(id) on delete set null,
  action_type text not null,
  affected_record_id uuid,
  affected_table text,
  details jsonb,
  old_data jsonb,
  new_data jsonb,
  created_at timestamptz not null default now()
);

create index if not exists audit_logs_created_idx
  on public.audit_logs (created_at desc);

create index if not exists audit_logs_actor_idx
  on public.audit_logs (actor_id);

create index if not exists audit_logs_record_idx
  on public.audit_logs (affected_record_id);

-- ============================================================
-- 12. SYSTEM SETTINGS
-- ============================================================

create table if not exists public.system_settings (
  id boolean primary key default true,
  app_name text not null default 'AMAN',
  contact_data jsonb,
  terms_and_conditions text,
  privacy_policy text,
  updated_at timestamptz not null default now(),
  constraint system_settings_singleton check (id = true)
);

-- ============================================================
-- AUTH -> CUSTOMER PROFILE
-- ============================================================

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_name text;
  v_email text;
begin
  v_name := nullif(trim(coalesce(new.raw_user_meta_data ->> 'name', '')), '');
  v_email := lower(trim(coalesce(new.email, '')));

  if v_name is null then
    raise exception 'NAME_REQUIRED';
  end if;

  if v_email = '' then
    raise exception 'EMAIL_REQUIRED';
  end if;

  insert into public.users(id, name, email, role, status)
  values (new.id, v_name, v_email, 'customer', 'active')
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

-- ============================================================
-- UPDATED_AT
-- ============================================================

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare
  t text;
begin
  foreach t in array array[
    'users','telecom_providers','telecom_prefixes','customer_numbers',
    'protection_plans','payment_methods','protection_requests',
    'protections','task_settings','payment_tasks','system_settings'
  ] loop
    execute format('drop trigger if exists %I_updated_at on public.%I', t, t);
    execute format(
      'create trigger %I_updated_at before update on public.%I
       for each row execute function public.set_updated_at()',
      t, t
    );
  end loop;
end $$;

-- ============================================================
-- HELPERS / SECURITY
-- ============================================================

create or replace function public.current_user_role()
returns public.user_role
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select role from public.users where id = auth.uid();
$$;

create or replace function public.is_manager()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select exists (
    select 1 from public.users
    where id = auth.uid() and role = 'manager' and status = 'active'
  );
$$;

-- ============================================================
-- PROVIDER IDENTIFICATION
-- ============================================================

create or replace function public.detect_provider_for_number(p_phone_number text)
returns uuid
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
  v_provider uuid;
begin
  if p_phone_number is null or length(trim(p_phone_number)) = 0 then
    raise exception 'PHONE_NUMBER_REQUIRED';
  end if;

  p_phone_number := regexp_replace(trim(p_phone_number), '\s+', '', 'g');

  if p_phone_number !~ '^[0-9+]+$' then
    raise exception 'INVALID_PHONE_FORMAT';
  end if;

  select tp.provider_id
    into v_provider
  from public.telecom_prefixes tp
  join public.telecom_providers p on p.id = tp.provider_id
  where tp.is_active = true
    and p.is_active = true
    and left(p_phone_number, length(tp.prefix)) = tp.prefix
  order by length(tp.prefix) desc
  limit 1;

  if v_provider is null then
    raise exception 'UNSUPPORTED_PHONE_PREFIX';
  end if;

  if not exists (
    select 1
    from public.telecom_providers p
    where p.id = v_provider
      and length(p_phone_number) = p.phone_length
  ) then
    raise exception 'INVALID_PHONE_LENGTH';
  end if;

  return v_provider;
end;
$$;

-- ============================================================
-- CUSTOMER NUMBER CREATION
-- ============================================================

create or replace function public.add_customer_number(p_phone_number text)
returns public.customer_numbers
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_row public.customer_numbers;
  v_normalized text;
  v_provider uuid;
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;

  v_normalized := regexp_replace(trim(p_phone_number), '\s+', '', 'g');

  v_provider := public.detect_provider_for_number(v_normalized);

  if not exists (
    select 1 from public.telecom_providers
    where id = v_provider and is_active = true
  ) then
    raise exception 'PROVIDER_NOT_ACTIVE';
  end if;

  if exists (select 1 from public.customer_numbers where phone_number = v_normalized) then
    raise exception 'PHONE_NUMBER_ALREADY_EXISTS';
  end if;

  insert into public.customer_numbers(customer_id, provider_id, phone_number)
  values (auth.uid(), v_provider, v_normalized)
  returning * into v_row;

  return v_row;
end;
$$;

-- ============================================================
-- PROTECTION DISPLAY STATUS
-- ============================================================

-- Run daily from the trusted scheduler/manager maintenance path.
create or replace function public.refresh_expired_protections()
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_count integer;
begin
  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  update public.protections
  set status = 'expired'
  where status = 'active'
    and end_date < current_date;

  get diagnostics v_count = row_count;

  if v_count > 0 then
    perform public.write_audit_log(
      'refresh_expired_protections', null, 'protections',
      jsonb_build_object('expired_count', v_count), null, null
    );
  end if;

  return v_count;
end;
$$;

create or replace function public.protection_display_status(
  p_end_date date
)
returns text
language sql
stable
as $$
  select case
    when p_end_date < current_date then 'expired'
    when p_end_date <= current_date + 30 then 'renewal_needed'
    else 'active'
  end;
$$;

-- ============================================================
-- REQUEST CREATION
-- ============================================================

create or replace function public.create_protection_request(
  p_customer_number_id uuid,
  p_plan_id uuid,
  p_payment_method_id uuid,
  p_transfer_data jsonb
)
returns public.protection_requests
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_number public.customer_numbers;
  v_plan public.protection_plans;
  v_method public.payment_methods;
  v_request public.protection_requests;
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;

  select * into v_number
  from public.customer_numbers
  where id = p_customer_number_id
    and customer_id = auth.uid();

  if not found then
    raise exception 'NUMBER_NOT_FOUND_OR_NOT_OWNED';
  end if;

  if v_number.status <> 'active' then
    raise exception 'NUMBER_NOT_ACTIVE';
  end if;

  select * into v_plan
  from public.protection_plans
  where id = p_plan_id
    and is_active = true
    and is_visible_to_customers = true;

  if not found then
    raise exception 'PLAN_NOT_AVAILABLE';
  end if;

  if v_plan.provider_id <> v_number.provider_id then
    raise exception 'PLAN_PROVIDER_MISMATCH';
  end if;

  if p_transfer_data is null or jsonb_typeof(p_transfer_data) <> 'object' then
    raise exception 'TRANSFER_DATA_REQUIRED';
  end if;

  if not exists (
    select 1 from public.telecom_providers
    where id = v_number.provider_id
      and is_active = true
      and is_visible_to_customers = true
  ) then
    raise exception 'PROVIDER_NOT_AVAILABLE';
  end if;

  select * into v_method
  from public.payment_methods
  where id = p_payment_method_id
    and is_active = true;

  if not found then
    raise exception 'PAYMENT_METHOD_NOT_AVAILABLE';
  end if;

  if exists (
    select 1 from public.protection_requests
    where customer_number_id = p_customer_number_id
      and status = 'pending'
  ) then
    raise exception 'CONFLICTING_REQUEST_EXISTS';
  end if;

  if exists (
    select 1 from public.protections
    where customer_number_id = p_customer_number_id
      and status = 'active'
      and daterange(start_date, end_date + 1, '[)') &&
          daterange(current_date, current_date + 1, '[)')
  ) then
    raise exception 'ACTIVE_PROTECTION_EXISTS';
  end if;

  insert into public.protection_requests (
    customer_id, customer_number_id, provider_id, plan_id, payment_method_id,
    protection_value, transfer_data, status,
    plan_name_snapshot, plan_price_snapshot, plan_duration_days_snapshot,
    payment_method_name_snapshot, payment_account_number_snapshot,
    payment_account_owner_snapshot, payment_instructions_snapshot
  )
  values (
    auth.uid(), v_number.id, v_number.provider_id, v_plan.id, v_method.id,
    v_plan.price, p_transfer_data, 'pending',
    v_plan.name, v_plan.price, v_plan.protection_duration_days,
    v_method.name, v_method.account_number, v_method.account_owner_name,
    v_method.payment_instructions
  )
  returning * into v_request;

  return v_request;
end;
$$;

-- ============================================================
-- MARK NOTIFICATION AS READ
-- ============================================================

create or replace function public.mark_notification_read(p_notification_id uuid)
returns public.notifications
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_notification public.notifications;
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;

  update public.notifications
  set is_read = true,
      read_at = coalesce(read_at, now())
  where id = p_notification_id
    and customer_id = auth.uid()
  returning * into v_notification;

  if not found then
    raise exception 'NOTIFICATION_NOT_FOUND_OR_NOT_OWNED';
  end if;

  return v_notification;
end;
$$;

-- ============================================================
-- AUDIT
-- ============================================================

create or replace function public.write_audit_log(
  p_action_type text,
  p_affected_record_id uuid,
  p_affected_table text,
  p_details jsonb default null,
  p_old_data jsonb default null,
  p_new_data jsonb default null
)
returns uuid
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_id uuid;
begin
  insert into public.audit_logs(
    actor_id, action_type, affected_record_id, affected_table,
    details, old_data, new_data
  )
  values (
    auth.uid(), p_action_type, p_affected_record_id, p_affected_table,
    p_details, p_old_data, p_new_data
  )
  returning id into v_id;

  return v_id;
end;
$$;

-- ============================================================
-- REJECT REQUEST
-- ============================================================

create or replace function public.reject_protection_request(
  p_request_id uuid,
  p_reason text
)
returns public.protection_requests
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_old public.protection_requests;
  v_new public.protection_requests;
begin
  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  if nullif(trim(p_reason), '') is null then
    raise exception 'REJECTION_REASON_REQUIRED';
  end if;

  select * into v_old
  from public.protection_requests
  where id = p_request_id
  for update;

  if not found then
    raise exception 'REQUEST_NOT_FOUND';
  end if;

  if v_old.status <> 'pending' then
    raise exception 'REQUEST_ALREADY_PROCESSED';
  end if;

  update public.protection_requests
  set status = 'rejected',
      rejection_reason = trim(p_reason),
      reviewed_by = auth.uid(),
      reviewed_at = now()
  where id = p_request_id
  returning * into v_new;

  insert into public.notifications(customer_id, title, message, notification_type)
  values (
    v_new.customer_id,
    'رفض طلب الحماية',
    'تم رفض طلب الحماية. سبب الرفض: ' || trim(p_reason),
    'protection_request_rejected'
  );

  perform public.write_audit_log(
    'reject_protection_request',
    p_request_id,
    'protection_requests',
    jsonb_build_object('reason', trim(p_reason)),
    to_jsonb(v_old),
    to_jsonb(v_new)
  );

  return v_new;
end;
$$;

-- ============================================================
-- ACCEPT REQUEST + CREATE PROTECTION + FIRST TASK
-- ============================================================

create or replace function public.approve_protection_request(
  p_request_id uuid
)
returns public.protections
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_req public.protection_requests;
  v_number public.customer_numbers;
  v_plan public.protection_plans;
  v_provider public.telecom_providers;
  v_protection public.protections;
  v_settings public.task_settings;
  v_start date;
  v_end date;
begin
  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  select * into v_req
  from public.protection_requests
  where id = p_request_id
  for update;

  if not found then
    raise exception 'REQUEST_NOT_FOUND';
  end if;

  if v_req.status <> 'pending' then
    raise exception 'REQUEST_ALREADY_PROCESSED';
  end if;

  select * into v_number
  from public.customer_numbers
  where id = v_req.customer_number_id
  for update;

  if not found or v_number.customer_id <> v_req.customer_id then
    raise exception 'REQUEST_NUMBER_MISMATCH';
  end if;

  if v_number.status <> 'active' then
    raise exception 'NUMBER_NOT_ACTIVE';
  end if;

  if v_number.provider_id <> v_req.provider_id then
    raise exception 'REQUEST_PROVIDER_MISMATCH';
  end if;

  select * into v_plan
  from public.protection_plans
  where id = v_req.plan_id;

  if not found or v_plan.provider_id <> v_req.provider_id then
    raise exception 'REQUEST_PLAN_MISMATCH';
  end if;

  if not v_plan.is_active then
    raise exception 'PLAN_NOT_AVAILABLE';
  end if;

  select * into v_provider
  from public.telecom_providers
  where id = v_req.provider_id;

  if not found then
    raise exception 'PROVIDER_NOT_FOUND';
  end if;

  if exists (
    select 1
    from public.protections p
    where p.customer_number_id = v_req.customer_number_id
      and p.status = 'active'
      and daterange(p.start_date, p.end_date + 1, '[)') &&
          daterange(current_date, current_date + 1, '[)')
  ) then
    raise exception 'ACTIVE_PROTECTION_EXISTS';
  end if;

  v_start := current_date;
  v_end := v_start + v_req.plan_duration_days_snapshot - 1;

  insert into public.protections (
    customer_id, customer_number_id, provider_id, plan_id,
    created_from_request_id, protection_value, protection_duration_days,
    start_date, end_date, status, plan_name_snapshot, provider_name_snapshot
  )
  values (
    v_req.customer_id,
    v_req.customer_number_id,
    v_req.provider_id,
    v_req.plan_id,
    v_req.id,
    v_req.protection_value,
    v_req.plan_duration_days_snapshot,
    v_start,
    v_end,
    'active',
    v_req.plan_name_snapshot,
    v_provider.name
  )
  returning * into v_protection;

  update public.protection_requests
  set status = 'approved',
      reviewed_by = auth.uid(),
      reviewed_at = now()
  where id = p_request_id;

  insert into public.notifications(customer_id, title, message, notification_type)
  values (
    v_req.customer_id,
    'قبول طلب الحماية',
    'تم قبول طلب الحماية وتفعيل الحماية على رقمك.',
    'protection_request_approved'
  );

  select * into v_settings
  from public.task_settings
  where provider_id = v_req.provider_id;

  if found and v_settings.first_task_enabled then
    insert into public.payment_tasks(
      protection_id, customer_number_id, task_type, amount, due_date, status
    )
    values (
      v_protection.id,
      v_protection.customer_number_id,
      'first',
      v_settings.first_task_amount,
      v_start,
      'due'
    );
  end if;

  perform public.write_audit_log(
    'approve_protection_request',
    p_request_id,
    'protection_requests',
    jsonb_build_object('protection_id', v_protection.id),
    to_jsonb(v_req),
    to_jsonb(v_protection)
  );

  perform public.write_audit_log(
    'create_protection',
    v_protection.id,
    'protections',
    jsonb_build_object('created_from_request_id', p_request_id),
    null,
    to_jsonb(v_protection)
  );

  return v_protection;
end;
$$;

-- ============================================================
-- COMPLETE TASK
-- ============================================================

create or replace function public.complete_payment_task(
  p_task_id uuid
)
returns public.payment_tasks
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_task public.payment_tasks;
  v_old public.payment_tasks;
  v_settings public.task_settings;
  v_provider uuid;
  v_next_due date;
  v_next public.payment_tasks;
begin
  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  select * into v_task
  from public.payment_tasks
  where id = p_task_id
  for update;

  if not found then
    raise exception 'TASK_NOT_FOUND';
  end if;

  if v_task.status in ('completed','cancelled') then
    raise exception 'TASK_ALREADY_CLOSED';
  end if;

  v_old := v_task;

  update public.payment_tasks
  set status = 'completed',
      completed_at = now(),
      completed_by = auth.uid()
  where id = p_task_id
  returning * into v_task;

  select p.provider_id into v_provider
  from public.protections p
  where p.id = v_task.protection_id;

  select * into v_settings
  from public.task_settings
  where provider_id = v_provider;

  if v_task.task_type = 'recurring'
     and found
     and v_settings.recurring_task_enabled
     and v_task.due_date < (
       select end_date from public.protections where id = v_task.protection_id
     ) then

    v_next_due := v_task.due_date + v_settings.repeat_interval_days;

    if v_next_due <= (
      select end_date from public.protections where id = v_task.protection_id
    ) then
      insert into public.payment_tasks(
        protection_id, customer_number_id, task_type, amount, due_date, status
      )
      values (
        v_task.protection_id,
        v_task.customer_number_id,
        'recurring',
        v_settings.recurring_task_amount,
        v_next_due,
        case
          when v_next_due < current_date then 'overdue'
          when v_next_due = current_date then 'due'
          else 'upcoming'
        end
      )
      returning * into v_next;
    end if;
  elsif v_task.task_type = 'first'
        and found
        and v_settings.recurring_task_enabled then

    v_next_due := v_task.due_date + v_settings.repeat_interval_days;

    if v_next_due <= (
      select end_date from public.protections where id = v_task.protection_id
    ) then
      insert into public.payment_tasks(
        protection_id, customer_number_id, task_type, amount, due_date, status
      )
      values (
        v_task.protection_id,
        v_task.customer_number_id,
        'recurring',
        v_settings.recurring_task_amount,
        v_next_due,
        case
          when v_next_due < current_date then 'overdue'
          when v_next_due = current_date then 'due'
          else 'upcoming'
        end
      )
      returning * into v_next;
    end if;
  end if;

  perform public.write_audit_log(
    'complete_payment_task',
    p_task_id,
    'payment_tasks',
    jsonb_build_object(
      'next_task_id', case when v_next.id is null then null else v_next.id end
    ),
    to_jsonb(v_old),
    to_jsonb(v_task)
  );

  return v_task;
end;
$$;

-- ============================================================
-- RESCHEDULE TASK
-- ============================================================

create or replace function public.reschedule_payment_task(
  p_task_id uuid,
  p_new_due_date date,
  p_reason text default null
)
returns public.payment_tasks
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_task public.payment_tasks;
  v_old public.payment_tasks;
  v_provider uuid;
  v_allowed boolean;
begin
  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  select * into v_task
  from public.payment_tasks
  where id = p_task_id
  for update;

  if not found then
    raise exception 'TASK_NOT_FOUND';
  end if;

  if v_task.status in ('completed','cancelled') then
    raise exception 'TASK_ALREADY_CLOSED';
  end if;

  select p.provider_id into v_provider
  from public.protections p
  where p.id = v_task.protection_id;

  select manual_reschedule_enabled into v_allowed
  from public.task_settings
  where provider_id = v_provider;

  if coalesce(v_allowed, false) = false then
    raise exception 'MANUAL_RESCHEDULE_DISABLED';
  end if;

  if p_new_due_date is null then
    raise exception 'NEW_DUE_DATE_REQUIRED';
  end if;

  v_old := v_task;

  update public.payment_tasks
  set previous_due_date = due_date,
      due_date = p_new_due_date,
      rescheduled_at = now(),
      rescheduled_by = auth.uid(),
      reschedule_reason = p_reason,
      status = case
        when p_new_due_date < current_date then 'overdue'
        when p_new_due_date = current_date then 'due'
        else 'upcoming'
      end
  where id = p_task_id
  returning * into v_task;

  perform public.write_audit_log(
    'reschedule_payment_task',
    p_task_id,
    'payment_tasks',
    jsonb_build_object('reason', p_reason),
    to_jsonb(v_old),
    to_jsonb(v_task)
  );

  return v_task;
end;
$$;

-- ============================================================
-- CANCEL TASK
-- ============================================================

create or replace function public.cancel_payment_task(
  p_task_id uuid,
  p_reason text default null
)
returns public.payment_tasks
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_task public.payment_tasks;
  v_old public.payment_tasks;
begin
  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  select * into v_task
  from public.payment_tasks
  where id = p_task_id
  for update;

  if not found then
    raise exception 'TASK_NOT_FOUND';
  end if;

  if v_task.status in ('completed','cancelled') then
    raise exception 'TASK_ALREADY_CLOSED';
  end if;

  v_old := v_task;

  update public.payment_tasks
  set status = 'cancelled',
      cancelled_at = now(),
      cancelled_by = auth.uid(),
      cancellation_reason = p_reason
  where id = p_task_id
  returning * into v_task;

  perform public.write_audit_log(
    'cancel_payment_task',
    p_task_id,
    'payment_tasks',
    jsonb_build_object('reason', p_reason),
    to_jsonb(v_old),
    to_jsonb(v_task)
  );

  return v_task;
end;
$$;

-- ============================================================
-- READ-ONLY DERIVED TASK STATUS
-- ============================================================

create or replace function public.refresh_task_status(p_task_id uuid)
returns public.task_status
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_task public.payment_tasks;
  v_status public.task_status;
begin
  select * into v_task from public.payment_tasks where id = p_task_id;
  if not found then raise exception 'TASK_NOT_FOUND'; end if;

  if not public.is_manager() then
    raise exception 'MANAGER_REQUIRED';
  end if;

  if v_task.status in ('completed','cancelled') then
    return v_task.status;
  end if;

  v_status := case
    when v_task.due_date < current_date then 'overdue'
    when v_task.due_date = current_date then 'due'
    else 'upcoming'
  end;

  update public.payment_tasks
  set status = v_status
  where id = p_task_id;

  return v_status;
end;
$$;

-- ============================================================
-- ACCOUNT SECURITY GUARD
-- Customers may edit their profile fields, but never elevate their
-- role or change account status. Manager role changes are not part
-- of the application workflow.
-- ============================================================

create or replace function public.guard_user_security_fields()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
begin
  if auth.uid() is not null and not public.is_manager() then
    if new.role <> old.role then
      raise exception 'ROLE_CHANGE_FORBIDDEN';
    end if;
    if new.status <> old.status then
      raise exception 'ACCOUNT_STATUS_CHANGE_FORBIDDEN';
    end if;
  end if;

  if new.role <> old.role then
    raise exception 'ROLE_CHANGE_FORBIDDEN';
  end if;

  return new;
end;
$$;

drop trigger if exists users_security_guard on public.users;
create trigger users_security_guard
before update on public.users
for each row execute function public.guard_user_security_fields();

-- ============================================================
-- CUSTOMER PROFILE UPDATE
-- ============================================================

create or replace function public.update_my_profile(p_name text, p_email text)
returns public.users
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_user public.users;
begin
  if auth.uid() is null then
    raise exception 'AUTH_REQUIRED';
  end if;

  if nullif(trim(p_name), '') is null then
    raise exception 'NAME_REQUIRED';
  end if;

  update public.users
  set name = trim(p_name),
      email = lower(trim(p_email))
  where id = auth.uid()
  returning * into v_user;

  if not found then
    raise exception 'USER_NOT_FOUND';
  end if;

  return v_user;
end;
$$;

-- ============================================================
-- TASK VISIBILITY
-- ============================================================

create or replace function public.is_payment_task_visible(
  p_task_id uuid,
  p_on_date date default current_date
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select exists (
    select 1
    from public.payment_tasks t
    join public.protections p on p.id = t.protection_id
    join public.task_settings s on s.provider_id = p.provider_id
    where t.id = p_task_id
      and (
        t.status in ('completed','cancelled')
        or p_on_date >= (t.due_date - s.days_visible_before_due)
      )
  );
$$;

-- ============================================================
-- AUDIT TRIGGERS FOR ADMIN CONFIGURATION CHANGES
-- ============================================================

create or replace function public.audit_configuration_change()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
begin
  if auth.uid() is null then
    return coalesce(new, old);
  end if;

  perform public.write_audit_log(
    tg_op || '_' || tg_table_name,
    coalesce(new.id, old.id),
    tg_table_name,
    null,
    case when tg_op in ('UPDATE','DELETE') then to_jsonb(old) end,
    case when tg_op in ('INSERT','UPDATE') then to_jsonb(new) end
  );

  return coalesce(new, old);
end;
$$;

drop trigger if exists audit_telecom_providers on public.telecom_providers;
create trigger audit_telecom_providers
after insert or update or delete on public.telecom_providers
for each row execute function public.audit_configuration_change();

drop trigger if exists audit_protection_plans on public.protection_plans;
create trigger audit_protection_plans
after insert or update or delete on public.protection_plans
for each row execute function public.audit_configuration_change();

drop trigger if exists audit_task_settings on public.task_settings;
create trigger audit_task_settings
after insert or update or delete on public.task_settings
for each row execute function public.audit_configuration_change();

drop trigger if exists audit_payment_methods on public.payment_methods;
create trigger audit_payment_methods
after insert or update or delete on public.payment_methods
for each row execute function public.audit_configuration_change();

-- ============================================================
-- PROTECT HISTORICAL CONFIGURATION FROM DELETION
-- ============================================================

create or replace function public.prevent_configuration_delete()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
begin
  raise exception 'DELETE_NOT_ALLOWED_USE_DEACTIVATION';
end;
$$;

drop trigger if exists prevent_provider_delete on public.telecom_providers;
create trigger prevent_provider_delete
before delete on public.telecom_providers
for each row execute function public.prevent_configuration_delete();

drop trigger if exists prevent_plan_delete on public.protection_plans;
create trigger prevent_plan_delete
before delete on public.protection_plans
for each row execute function public.prevent_configuration_delete();

drop trigger if exists prevent_payment_method_delete on public.payment_methods;
create trigger prevent_payment_method_delete
before delete on public.payment_methods
for each row execute function public.prevent_configuration_delete();

-- ============================================================
-- RLS
-- ============================================================

alter table public.users enable row level security;
alter table public.telecom_providers enable row level security;
alter table public.telecom_prefixes enable row level security;
alter table public.customer_numbers enable row level security;
alter table public.protection_plans enable row level security;
alter table public.payment_methods enable row level security;
alter table public.protection_requests enable row level security;
alter table public.protections enable row level security;
alter table public.task_settings enable row level security;
alter table public.payment_tasks enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_logs enable row level security;
alter table public.system_settings enable row level security;

-- USERS
drop policy if exists users_select_self_or_manager on public.users;
drop policy if exists users_select_self_or_manager on public.users;
create policy users_select_self_or_manager
on public.users for select to authenticated
using (id = auth.uid() or public.is_manager());

drop policy if exists users_update_self on public.users;
drop policy if exists users_update_self on public.users;
create policy users_update_self
on public.users for update to authenticated
using (id = auth.uid())
with check (id = auth.uid());

drop policy if exists users_manager_all on public.users;
drop policy if exists users_manager_all on public.users;
create policy users_manager_all
on public.users for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- PROVIDERS
drop policy if exists providers_customer_read_visible on public.telecom_providers;
drop policy if exists providers_customer_read_visible on public.telecom_providers;
create policy providers_customer_read_visible
on public.telecom_providers for select to authenticated
using (is_visible_to_customers and is_active or public.is_manager());

drop policy if exists providers_manager_write on public.telecom_providers;
drop policy if exists providers_manager_write on public.telecom_providers;
create policy providers_manager_write
on public.telecom_providers for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- PREFIXES: manager only
drop policy if exists prefixes_manager_all on public.telecom_prefixes;
drop policy if exists prefixes_manager_all on public.telecom_prefixes;
create policy prefixes_manager_all
on public.telecom_prefixes for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- NUMBERS
drop policy if exists numbers_customer_select on public.customer_numbers;
drop policy if exists numbers_customer_select on public.customer_numbers;
create policy numbers_customer_select
on public.customer_numbers for select to authenticated
using (customer_id = auth.uid() or public.is_manager());

-- Customer number creation is RPC-only so provider_id can never be
-- supplied by the client.
drop policy if exists numbers_customer_insert on public.customer_numbers;
drop policy if exists numbers_customer_update on public.customer_numbers;

drop policy if exists numbers_manager_all on public.customer_numbers;
-- The reference defines numbers as a management/view section, not an edit workflow.
-- Creation is performed by the customer RPC; provider/status changes are not exposed here.

-- PLANS
drop policy if exists plans_customer_read on public.protection_plans;
drop policy if exists plans_customer_read on public.protection_plans;
create policy plans_customer_read
on public.protection_plans for select to authenticated
using ((is_active and is_visible_to_customers) or public.is_manager());

drop policy if exists plans_manager_write on public.protection_plans;
drop policy if exists plans_manager_write on public.protection_plans;
create policy plans_manager_write
on public.protection_plans for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- PAYMENT METHODS
drop policy if exists payment_methods_customer_read on public.payment_methods;
drop policy if exists payment_methods_customer_read on public.payment_methods;
create policy payment_methods_customer_read
on public.payment_methods for select to authenticated
using (is_active or public.is_manager());

drop policy if exists payment_methods_manager_write on public.payment_methods;
drop policy if exists payment_methods_manager_write on public.payment_methods;
create policy payment_methods_manager_write
on public.payment_methods for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- REQUESTS
drop policy if exists requests_customer_select on public.protection_requests;
drop policy if exists requests_customer_select on public.protection_requests;
create policy requests_customer_select
on public.protection_requests for select to authenticated
using (customer_id = auth.uid() or public.is_manager());

drop policy if exists requests_customer_manager_write on public.protection_requests;

-- Request creation and review are RPC-only.
-- No direct INSERT/UPDATE/DELETE is granted to customers or managers.

-- PROTECTIONS
drop policy if exists protections_customer_select on public.protections;
drop policy if exists protections_customer_select on public.protections;
create policy protections_customer_select
on public.protections for select to authenticated
using (customer_id = auth.uid() or public.is_manager());

drop policy if exists protections_manager_write on public.protections;

-- Protection creation is performed only by approve_protection_request().
-- TASK SETTINGS
drop policy if exists task_settings_manager_all on public.task_settings;
drop policy if exists task_settings_manager_all on public.task_settings;
create policy task_settings_manager_all
on public.task_settings for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- TASKS
drop policy if exists tasks_manager_select on public.payment_tasks;
drop policy if exists tasks_manager_select on public.payment_tasks;
create policy tasks_manager_select
on public.payment_tasks for select to authenticated
using (
  public.is_manager()
  and (
    status in ('completed','cancelled')
    or public.is_payment_task_visible(id, current_date)
  )
);

-- Task mutations are RPC-only.
-- NOTIFICATIONS
drop policy if exists notifications_customer_select on public.notifications;
drop policy if exists notifications_customer_select on public.notifications;
create policy notifications_customer_select
on public.notifications for select to authenticated
using (customer_id = auth.uid() or public.is_manager());

drop policy if exists notifications_customer_update_read on public.notifications;
drop policy if exists notifications_customer_mark_read on public.notifications;
-- Read state changes are RPC-only so notification content cannot be edited.

drop policy if exists notifications_manager_all on public.notifications;
drop policy if exists notifications_manager_select on public.notifications;
create policy notifications_manager_select
on public.notifications for select to authenticated
using (public.is_manager());

-- AUDIT: manager only
drop policy if exists audit_manager_select on public.audit_logs;
drop policy if exists audit_manager_select on public.audit_logs;
create policy audit_manager_select
on public.audit_logs for select to authenticated
using (public.is_manager());

-- No direct client insert/update/delete.
-- RPCs use security definer to write audit records.

-- SYSTEM SETTINGS
drop policy if exists system_settings_read on public.system_settings;
drop policy if exists system_settings_read on public.system_settings;
create policy system_settings_read
on public.system_settings for select to authenticated
using (true);

drop policy if exists system_settings_manager_write on public.system_settings;
drop policy if exists system_settings_manager_write on public.system_settings;
create policy system_settings_manager_write
on public.system_settings for all to authenticated
using (public.is_manager())
with check (public.is_manager());

-- ============================================================
-- RPC EXECUTION PRIVILEGES
-- ============================================================

revoke all on function public.update_my_profile(text,text) from public;
grant execute on function public.update_my_profile(text,text) to authenticated;

revoke all on function public.detect_provider_for_number(text) from public;
grant execute on function public.detect_provider_for_number(text) to authenticated;

revoke all on function public.add_customer_number(text) from public;
grant execute on function public.add_customer_number(text) to authenticated;

revoke all on function public.create_protection_request(uuid,uuid,uuid,jsonb) from public;
grant execute on function public.create_protection_request(uuid,uuid,uuid,jsonb) to authenticated;

revoke all on function public.approve_protection_request(uuid) from public;
grant execute on function public.approve_protection_request(uuid) to authenticated;

revoke all on function public.reject_protection_request(uuid,text) from public;
grant execute on function public.reject_protection_request(uuid,text) to authenticated;

revoke all on function public.complete_payment_task(uuid) from public;
grant execute on function public.complete_payment_task(uuid) to authenticated;

revoke all on function public.reschedule_payment_task(uuid,date,text) from public;
grant execute on function public.reschedule_payment_task(uuid,date,text) to authenticated;

revoke all on function public.cancel_payment_task(uuid,text) from public;
grant execute on function public.cancel_payment_task(uuid,text) to authenticated;

revoke all on function public.protection_display_status(date) from public;
grant execute on function public.refresh_expired_protections() to authenticated;

grant execute on function public.protection_display_status(date) to authenticated;

revoke all on function public.refresh_task_status(uuid) from public;
grant execute on function public.refresh_task_status(uuid) to authenticated;

revoke all on function public.is_payment_task_visible(uuid,date) from public;
grant execute on function public.is_payment_task_visible(uuid,date) to authenticated;

revoke all on function public.mark_notification_read(uuid) from public;
grant execute on function public.mark_notification_read(uuid) to authenticated;

revoke all on function public.write_audit_log(text,uuid,text,jsonb,jsonb,jsonb) from public;

revoke all on function public.current_user_role() from public;
grant execute on function public.current_user_role() to authenticated;

revoke all on function public.is_manager() from public;
grant execute on function public.is_manager() to authenticated;

-- ============================================================
-- INITIAL SINGLETON
-- ============================================================

insert into public.system_settings(id, app_name)
values (true, 'AMAN')
on conflict (id) do nothing;

-- ============================================================
-- FINAL V6 HARDENING NOTES
-- ============================================================
-- Protection status is stored as active/expired; refresh_expired_protections()
-- must be invoked by the trusted daily maintenance scheduler.
-- protection_display_status() keeps the reference display rule: renewal_needed
-- is computed and is not stored as a protection status.

commit;



-- Safe-delete RPCs are the only delete path for these configuration tables.
-- The old blanket delete-blocking triggers are removed because the RPCs
-- perform the historical-reference checks themselves.
drop trigger if exists prevent_provider_delete on public.telecom_providers;
drop trigger if exists prevent_plan_delete on public.protection_plans;
drop trigger if exists prevent_payment_method_delete on public.payment_methods;

-- ============================================================
-- V5 ADMIN MANAGEMENT API
-- Built to expose the management operations explicitly required
-- by AMAN.XZ.txt. Configuration writes are manager-only.
-- ============================================================

-- ---------------------------
-- TELECOM PROVIDERS
-- ---------------------------

create or replace function public.admin_create_telecom_provider(
  p_name text,
  p_code text,
  p_phone_length integer,
  p_prefixes text[] default '{}',
  p_is_active boolean default true,
  p_is_visible_to_customers boolean default true,
  p_display_order integer default 0
)
returns public.telecom_providers
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_provider public.telecom_providers;
  v_prefix text;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if nullif(trim(p_name),'') is null then raise exception 'PROVIDER_NAME_REQUIRED'; end if;
  if nullif(trim(p_code),'') is null then raise exception 'PROVIDER_CODE_REQUIRED'; end if;
  if p_phone_length <= 0 then raise exception 'PHONE_LENGTH_INVALID'; end if;

  insert into public.telecom_providers(name,code,phone_length,is_active,is_visible_to_customers,display_order)
  values(trim(p_name),trim(p_code),p_phone_length,p_is_active,p_is_visible_to_customers,p_display_order)
  returning * into v_provider;

  foreach v_prefix in array coalesce(p_prefixes,'{}') loop
    if nullif(trim(v_prefix),'') is not null then
      insert into public.telecom_prefixes(provider_id,prefix)
      values(v_provider.id,trim(v_prefix));
    end if;
  end loop;

  return v_provider;
end;
$$;

create or replace function public.admin_update_telecom_provider(
  p_provider_id uuid,
  p_name text,
  p_code text,
  p_phone_length integer,
  p_prefixes text[],
  p_is_active boolean,
  p_is_visible_to_customers boolean,
  p_display_order integer
)
returns public.telecom_providers
language plpgsql
security definer
set search_path = public, pg_temp, pg_temp
as $$
declare
  v_provider public.telecom_providers;
  v_old_provider public.telecom_providers;
  v_old_prefixes jsonb;
  v_new_prefixes jsonb;
  v_prefix text;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;

  select * into v_old_provider from public.telecom_providers where id=p_provider_id for update;
  if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;

  select coalesce(jsonb_agg(prefix order by prefix), '[]'::jsonb) into v_old_prefixes
  from public.telecom_prefixes where provider_id=p_provider_id;

  update public.telecom_providers
  set name=trim(p_name),
      code=trim(p_code),
      phone_length=p_phone_length,
      is_active=p_is_active,
      is_visible_to_customers=p_is_visible_to_customers,
      display_order=p_display_order
  where id=p_provider_id
  returning * into v_provider;

  if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;

  delete from public.telecom_prefixes where provider_id=p_provider_id;

  foreach v_prefix in array coalesce(p_prefixes,'{}') loop
    if nullif(trim(v_prefix),'') is not null then
      insert into public.telecom_prefixes(provider_id,prefix)
      values(p_provider_id,trim(v_prefix));
    end if;
  end loop;

  select coalesce(jsonb_agg(prefix order by prefix), '[]'::jsonb) into v_new_prefixes
  from public.telecom_prefixes where provider_id=p_provider_id;

  perform public.write_audit_log(
    'update_telecom_provider', p_provider_id, 'telecom_providers',
    jsonb_build_object('old_prefixes', v_old_prefixes, 'new_prefixes', v_new_prefixes),
    to_jsonb(v_old_provider), to_jsonb(v_provider)
  );

  return v_provider;
end;
$$;

create or replace function public.admin_set_telecom_provider_status(
  p_provider_id uuid, p_is_active boolean
)
returns public.telecom_providers
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.telecom_providers;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.telecom_providers set is_active=p_is_active
  where id=p_provider_id returning * into v_row;
  if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_set_telecom_provider_visibility(
  p_provider_id uuid, p_is_visible boolean
)
returns public.telecom_providers
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.telecom_providers;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.telecom_providers set is_visible_to_customers=p_is_visible
  where id=p_provider_id returning * into v_row;
  if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_set_telecom_provider_order(
  p_provider_id uuid, p_display_order integer
)
returns public.telecom_providers
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.telecom_providers;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.telecom_providers set display_order=p_display_order
  where id=p_provider_id returning * into v_row;
  if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_delete_telecom_provider(p_provider_id uuid)
returns boolean
language plpgsql security definer set search_path=public, pg_temp as $$
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;

  if exists(select 1 from public.customer_numbers where provider_id=p_provider_id)
     or exists(select 1 from public.protection_requests where provider_id=p_provider_id)
     or exists(select 1 from public.protections where provider_id=p_provider_id)
     or exists(select 1 from public.protection_plans where provider_id=p_provider_id)
     or exists(select 1 from public.task_settings where provider_id=p_provider_id)
  then
    raise exception 'PROVIDER_HAS_HISTORICAL_DATA_USE_DEACTIVATION';
  end if;

  delete from public.telecom_providers where id=p_provider_id;
  if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;
  return true;
end $$;

-- ---------------------------
-- PROTECTION PLANS
-- ---------------------------

create or replace function public.admin_create_protection_plan(
  p_provider_id uuid, p_name text, p_price numeric,
  p_duration_days integer, p_is_active boolean default true,
  p_is_visible_to_customers boolean default true
)
returns public.protection_plans
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.protection_plans;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if not exists(select 1 from public.telecom_providers where id=p_provider_id)
    then raise exception 'PROVIDER_NOT_FOUND'; end if;
  insert into public.protection_plans(provider_id,name,price,protection_duration_days,is_active,is_visible_to_customers)
  values(p_provider_id,trim(p_name),p_price,p_duration_days,p_is_active,p_is_visible_to_customers)
  returning * into v_row;
  return v_row;
end $$;

create or replace function public.admin_update_protection_plan(
  p_plan_id uuid, p_provider_id uuid, p_name text, p_price numeric,
  p_duration_days integer, p_is_active boolean,
  p_is_visible_to_customers boolean
)
returns public.protection_plans
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.protection_plans;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.protection_plans
  set provider_id=p_provider_id,name=trim(p_name),price=p_price,
      protection_duration_days=p_duration_days,is_active=p_is_active,
      is_visible_to_customers=p_is_visible_to_customers
  where id=p_plan_id returning * into v_row;
  if not found then raise exception 'PLAN_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_set_protection_plan_status(
  p_plan_id uuid, p_is_active boolean
)
returns public.protection_plans
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.protection_plans;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.protection_plans set is_active=p_is_active
  where id=p_plan_id returning * into v_row;
  if not found then raise exception 'PLAN_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_set_protection_plan_visibility(
  p_plan_id uuid, p_is_visible boolean
)
returns public.protection_plans
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.protection_plans;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.protection_plans set is_visible_to_customers=p_is_visible
  where id=p_plan_id returning * into v_row;
  if not found then raise exception 'PLAN_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_delete_protection_plan(p_plan_id uuid)
returns boolean
language plpgsql security definer set search_path=public, pg_temp as $$
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if exists(select 1 from public.protection_requests where plan_id=p_plan_id)
     or exists(select 1 from public.protections where plan_id=p_plan_id)
  then raise exception 'PLAN_HAS_HISTORICAL_DATA_USE_DEACTIVATION'; end if;
  delete from public.protection_plans where id=p_plan_id;
  if not found then raise exception 'PLAN_NOT_FOUND'; end if;
  return true;
end $$;

-- ---------------------------
-- PAYMENT METHODS
-- ---------------------------

create or replace function public.admin_create_payment_method(
  p_name text, p_account_number text, p_account_owner_name text,
  p_payment_instructions text default null, p_is_active boolean default true
)
returns public.payment_methods
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.payment_methods;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  insert into public.payment_methods(name,account_number,account_owner_name,payment_instructions,is_active)
  values(trim(p_name),trim(p_account_number),trim(p_account_owner_name),p_payment_instructions,p_is_active)
  returning * into v_row;
  return v_row;
end $$;

create or replace function public.admin_update_payment_method(
  p_payment_method_id uuid, p_name text, p_account_number text,
  p_account_owner_name text, p_payment_instructions text, p_is_active boolean
)
returns public.payment_methods
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.payment_methods;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.payment_methods
  set name=trim(p_name),account_number=trim(p_account_number),
      account_owner_name=trim(p_account_owner_name),
      payment_instructions=p_payment_instructions,is_active=p_is_active
  where id=p_payment_method_id returning * into v_row;
  if not found then raise exception 'PAYMENT_METHOD_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_set_payment_method_status(
  p_payment_method_id uuid, p_is_active boolean
)
returns public.payment_methods
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.payment_methods;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.payment_methods set is_active=p_is_active
  where id=p_payment_method_id returning * into v_row;
  if not found then raise exception 'PAYMENT_METHOD_NOT_FOUND'; end if;
  return v_row;
end $$;

create or replace function public.admin_delete_payment_method(p_payment_method_id uuid)
returns boolean
language plpgsql security definer set search_path=public, pg_temp as $$
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if exists(select 1 from public.protection_requests where payment_method_id=p_payment_method_id)
    then raise exception 'PAYMENT_METHOD_HAS_HISTORICAL_DATA_USE_DEACTIVATION'; end if;
  delete from public.payment_methods where id=p_payment_method_id;
  if not found then raise exception 'PAYMENT_METHOD_NOT_FOUND'; end if;
  return true;
end $$;

-- ---------------------------
-- TASK SETTINGS
-- ---------------------------

create or replace function public.admin_update_task_settings(
  p_provider_id uuid,
  p_first_task_enabled boolean,
  p_first_task_amount numeric,
  p_recurring_task_enabled boolean,
  p_recurring_task_amount numeric,
  p_repeat_interval_days integer,
  p_days_visible_before_due integer,
  p_manual_reschedule_enabled boolean
)
returns public.task_settings
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.task_settings;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if not exists(select 1 from public.telecom_providers where id=p_provider_id)
    then raise exception 'PROVIDER_NOT_FOUND'; end if;

  insert into public.task_settings(
    provider_id,first_task_enabled,first_task_amount,
    recurring_task_enabled,recurring_task_amount,repeat_interval_days,
    days_visible_before_due,manual_reschedule_enabled
  )
  values(
    p_provider_id,p_first_task_enabled,p_first_task_amount,
    p_recurring_task_enabled,p_recurring_task_amount,p_repeat_interval_days,
    p_days_visible_before_due,p_manual_reschedule_enabled
  )
  on conflict(provider_id) do update set
    first_task_enabled=excluded.first_task_enabled,
    first_task_amount=excluded.first_task_amount,
    recurring_task_enabled=excluded.recurring_task_enabled,
    recurring_task_amount=excluded.recurring_task_amount,
    repeat_interval_days=excluded.repeat_interval_days,
    days_visible_before_due=excluded.days_visible_before_due,
    manual_reschedule_enabled=excluded.manual_reschedule_enabled
  returning * into v_row;

  return v_row;
end $$;

-- ---------------------------
-- EXPLICIT MANAGER TASK ACTIONS
-- ---------------------------

-- complete_payment_task, reschedule_payment_task and cancel_payment_task
-- already implement the reference-required manager actions.
-- The grants below explicitly expose them to authenticated managers;
-- the functions themselves enforce MANAGER_ONLY.

-- ---------------------------
-- MANAGER FILTER / CLASSIFICATION READ MODEL
-- ---------------------------

create or replace function public.admin_task_classification(
  p_filter text default 'all',
  p_search text default null,
  p_provider_id uuid default null
)
returns setof public.payment_tasks
language sql stable security definer set search_path=public as $$
  select t.*
  from public.payment_tasks t
  join public.protections p on p.id=t.protection_id
  where public.is_manager()
    and (p_provider_id is null or p.provider_id=p_provider_id)
    and (
      nullif(trim(p_search),'') is null
      or exists(
        select 1 from public.customer_numbers n
        join public.users u on u.id=n.customer_id
        where n.id=t.customer_number_id
          and (
            n.phone_number ilike '%'||trim(p_search)||'%'
            or u.name ilike '%'||trim(p_search)||'%'
            or u.email ilike '%'||trim(p_search)||'%'
          )
      )
    )
    and case lower(coalesce(p_filter,'all'))
      when 'all' then true
      when 'upcoming' then t.status='upcoming'
      when 'today' then t.due_date=current_date and t.status not in ('completed','cancelled')
      when 'overdue' then t.due_date<current_date and t.status not in ('completed','cancelled')
      when 'completed' then t.status='completed'
      when 'cancelled' then t.status='cancelled'
      when 'first' then t.task_type='first'
      when 'recurring' then t.task_type='recurring'
      else false
    end
  order by t.due_date, t.created_at;
$$;

-- ---------------------------
-- REPLACE CONFIGURATION RLS:
-- manager may read directly; writes use the explicit manager RPC API.
-- ---------------------------

drop policy if exists providers_manager_write on public.telecom_providers;
drop policy if exists providers_manager_select on public.telecom_providers;
create policy providers_manager_select
on public.telecom_providers for select to authenticated
using (public.is_manager());

drop policy if exists providers_manager_insert on public.telecom_providers;
create policy providers_manager_insert
on public.telecom_providers for insert to authenticated
with check (public.is_manager());

drop policy if exists providers_manager_update on public.telecom_providers;
create policy providers_manager_update
on public.telecom_providers for update to authenticated
using (public.is_manager())
with check (public.is_manager());

drop policy if exists plans_manager_write on public.protection_plans;
drop policy if exists plans_manager_select on public.protection_plans;
create policy plans_manager_select
on public.protection_plans for select to authenticated
using (public.is_manager());

drop policy if exists plans_manager_insert on public.protection_plans;
create policy plans_manager_insert
on public.protection_plans for insert to authenticated
with check (public.is_manager());

drop policy if exists plans_manager_update on public.protection_plans;
create policy plans_manager_update
on public.protection_plans for update to authenticated
using (public.is_manager())
with check (public.is_manager());

drop policy if exists payment_methods_manager_write on public.payment_methods;
drop policy if exists payment_methods_manager_select on public.payment_methods;
create policy payment_methods_manager_select
on public.payment_methods for select to authenticated
using (public.is_manager());

drop policy if exists payment_methods_manager_insert on public.payment_methods;
create policy payment_methods_manager_insert
on public.payment_methods for insert to authenticated
with check (public.is_manager());

drop policy if exists payment_methods_manager_update on public.payment_methods;
create policy payment_methods_manager_update
on public.payment_methods for update to authenticated
using (public.is_manager())
with check (public.is_manager());

drop policy if exists task_settings_manager_all on public.task_settings;
drop policy if exists task_settings_manager_select on public.task_settings;
create policy task_settings_manager_select
on public.task_settings for select to authenticated
using (public.is_manager());

drop policy if exists task_settings_manager_insert on public.task_settings;
create policy task_settings_manager_insert
on public.task_settings for insert to authenticated
with check (public.is_manager());

drop policy if exists task_settings_manager_update on public.task_settings;
create policy task_settings_manager_update
on public.task_settings for update to authenticated
using (public.is_manager())
with check (public.is_manager());

-- No direct DELETE policy for configuration. Safe deletes go through RPC,
-- which checks for historical references and otherwise rejects the delete.

-- ---------------------------
-- EXECUTE GRANTS
-- ---------------------------

do $$
begin
  revoke all on function public.admin_create_telecom_provider(text,text,integer,text[],boolean,boolean,integer) from public;
  revoke all on function public.admin_update_telecom_provider(uuid,text,text,integer,text[],boolean,boolean,integer) from public;
  revoke all on function public.admin_set_telecom_provider_status(uuid,boolean) from public;
  revoke all on function public.admin_set_telecom_provider_visibility(uuid,boolean) from public;
  revoke all on function public.admin_set_telecom_provider_order(uuid,integer) from public;
  revoke all on function public.admin_delete_telecom_provider(uuid) from public;

  revoke all on function public.admin_create_protection_plan(uuid,text,numeric,integer,boolean,boolean) from public;
  revoke all on function public.admin_update_protection_plan(uuid,uuid,text,numeric,integer,boolean,boolean) from public;
  revoke all on function public.admin_set_protection_plan_status(uuid,boolean) from public;
  revoke all on function public.admin_set_protection_plan_visibility(uuid,boolean) from public;
  revoke all on function public.admin_delete_protection_plan(uuid) from public;

  revoke all on function public.admin_create_payment_method(text,text,text,text,boolean) from public;
  revoke all on function public.admin_update_payment_method(uuid,text,text,text,text,boolean) from public;
  revoke all on function public.admin_set_payment_method_status(uuid,boolean) from public;
  revoke all on function public.admin_delete_payment_method(uuid) from public;

  revoke all on function public.admin_update_task_settings(uuid,boolean,numeric,boolean,numeric,integer,integer,boolean) from public;
  revoke all on function public.admin_task_classification(text,text,uuid) from public;
exception when undefined_function then
  null;
end $$;

grant execute on function public.admin_create_telecom_provider(text,text,integer,text[],boolean,boolean,integer) to authenticated;
grant execute on function public.admin_update_telecom_provider(uuid,text,text,integer,text[],boolean,boolean,integer) to authenticated;
grant execute on function public.admin_set_telecom_provider_status(uuid,boolean) to authenticated;
grant execute on function public.admin_set_telecom_provider_visibility(uuid,boolean) to authenticated;
grant execute on function public.admin_set_telecom_provider_order(uuid,integer) to authenticated;
grant execute on function public.admin_delete_telecom_provider(uuid) to authenticated;

grant execute on function public.admin_create_protection_plan(uuid,text,numeric,integer,boolean,boolean) to authenticated;
grant execute on function public.admin_update_protection_plan(uuid,uuid,text,numeric,integer,boolean,boolean) to authenticated;
grant execute on function public.admin_set_protection_plan_status(uuid,boolean) to authenticated;
grant execute on function public.admin_set_protection_plan_visibility(uuid,boolean) to authenticated;
grant execute on function public.admin_delete_protection_plan(uuid) to authenticated;

grant execute on function public.admin_create_payment_method(text,text,text,text,boolean) to authenticated;
grant execute on function public.admin_update_payment_method(uuid,text,text,text,text,boolean) to authenticated;
grant execute on function public.admin_set_payment_method_status(uuid,boolean) to authenticated;
grant execute on function public.admin_delete_payment_method(uuid) to authenticated;

grant execute on function public.admin_update_task_settings(uuid,boolean,numeric,boolean,numeric,integer,integer,boolean) to authenticated;
grant execute on function public.admin_task_classification(text,text,uuid) to authenticated;

-- ============================================================
-- FINAL DATABASE-LAYER HARDENING / V6
-- Source of requirements: AMAN.XZ.txt only
-- ============================================================

-- 1) System settings must be changed through an explicit manager RPC.
create or replace function public.admin_update_system_settings(
  p_app_name text,
  p_contact_data jsonb,
  p_terms_and_conditions text,
  p_privacy_policy text
)
returns public.system_settings
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.system_settings;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  update public.system_settings
  set app_name=trim(p_app_name),
      contact_data=coalesce(p_contact_data,'{}'::jsonb),
      terms_and_conditions=p_terms_and_conditions,
      privacy_policy=p_privacy_policy,
      updated_at=now()
  where id=true
  returning * into v_row;
  if not found then
    insert into public.system_settings(id,app_name,contact_data,terms_and_conditions,privacy_policy)
    values(true,trim(p_app_name),coalesce(p_contact_data,'{}'::jsonb),p_terms_and_conditions,p_privacy_policy)
    returning * into v_row;
  end if;
  return v_row;
end $$;

-- 2) Notification creation is a protected database operation. Business events
-- use their own RPCs; this function exists for legitimate manager/system events.
create or replace function public.admin_create_notification(
  p_customer_id uuid,
  p_title text,
  p_message text,
  p_notification_type text
)
returns public.notifications
language plpgsql security definer set search_path=public, pg_temp as $$
declare v_row public.notifications;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if p_customer_id is not null and not exists(select 1 from public.users where id=p_customer_id) then
    raise exception 'CUSTOMER_NOT_FOUND';
  end if;
  if nullif(trim(p_title),'') is null then raise exception 'NOTIFICATION_TITLE_REQUIRED'; end if;
  if nullif(trim(p_message),'') is null then raise exception 'NOTIFICATION_MESSAGE_REQUIRED'; end if;
  insert into public.notifications(customer_id,title,message,notification_type)
  values(p_customer_id,trim(p_title),trim(p_message),trim(p_notification_type))
  returning * into v_row;
  return v_row;
end $$;

-- 3) Manager read models for the reference-required search/filter/detail lists.
create or replace function public.admin_customers(
  p_search text default null
)
returns setof public.users
language sql stable security definer set search_path=public as $$
  select u.* from public.users u
  where public.is_manager()
    and (nullif(trim(p_search),'') is null
      or u.name ilike '%'||trim(p_search)||'%'
      or u.email ilike '%'||trim(p_search)||'%')
  order by u.created_at desc;
$$;

create or replace function public.admin_customer_numbers(
  p_search text default null,
  p_provider_id uuid default null,
  p_customer_id uuid default null
)
returns setof public.customer_numbers
language sql stable security definer set search_path=public as $$
  select n.* from public.customer_numbers n
  where public.is_manager()
    and (p_provider_id is null or n.provider_id=p_provider_id)
    and (p_customer_id is null or n.customer_id=p_customer_id)
    and (nullif(trim(p_search),'') is null or n.phone_number ilike '%'||trim(p_search)||'%')
  order by n.created_at desc;
$$;

create or replace function public.admin_protection_requests(
  p_status text default null,
  p_search text default null,
  p_provider_id uuid default null
)
returns setof public.protection_requests
language sql stable security definer set search_path=public as $$
  select r.* from public.protection_requests r
  where public.is_manager()
    and (p_status is null or r.status::text=lower(trim(p_status)))
    and (p_provider_id is null or r.provider_id=p_provider_id)
    and (
      nullif(trim(p_search),'') is null
      or exists (
        select 1 from public.customer_numbers n
        join public.users u on u.id=n.customer_id
        where n.id=r.customer_number_id
          and (n.phone_number ilike '%'||trim(p_search)||'%' or u.name ilike '%'||trim(p_search)||'%' or u.email ilike '%'||trim(p_search)||'%')
      )
    )
  order by r.created_at desc;
$$;

create or replace function public.admin_protections(
  p_filter text default 'all',
  p_search text default null,
  p_provider_id uuid default null
)
returns setof public.protections
language sql stable security definer set search_path=public as $$
  select p.* from public.protections p
  where public.is_manager()
    and (p_provider_id is null or p.provider_id=p_provider_id)
    and (
      nullif(trim(p_search),'') is null
      or exists (
        select 1 from public.customer_numbers n
        join public.users u on u.id=n.customer_id
        where n.id=p.customer_number_id
          and (n.phone_number ilike '%'||trim(p_search)||'%' or u.name ilike '%'||trim(p_search)||'%' or u.email ilike '%'||trim(p_search)||'%')
      )
    )
    and case lower(coalesce(p_filter,'all'))
      when 'all' then true
      when 'active' then p.end_date >= current_date
      when 'expired' then p.end_date < current_date
      when 'renewal' then p.end_date >= current_date and p.end_date <= current_date + 30
      else false
    end
  order by p.end_date, p.created_at desc;
$$;

create or replace function public.admin_audit_logs(
  p_search text default null,
  p_action_type text default null,
  p_from timestamptz default null,
  p_to timestamptz default null
)
returns setof public.audit_logs
language sql stable security definer set search_path=public as $$
  select a.* from public.audit_logs a
  where public.is_manager()
    and (p_action_type is null or a.action_type=trim(p_action_type))
    and (p_from is null or a.created_at>=p_from)
    and (p_to is null or a.created_at<=p_to)
    and (nullif(trim(p_search),'') is null
      or a.action_type ilike '%'||trim(p_search)||'%'
      or a.affected_table ilike '%'||trim(p_search)||'%'
      or a.affected_record_id::text ilike '%'||trim(p_search)||'%')
  order by a.created_at desc;
$$;

-- 4) Configuration mutations must not be bypassed by direct manager table writes.
-- All mutations go through the explicit RPC layer above.
drop policy if exists providers_manager_all on public.telecom_providers;
drop policy if exists providers_manager_insert on public.telecom_providers;
drop policy if exists providers_manager_update on public.telecom_providers;
drop policy if exists providers_manager_write on public.telecom_providers;
drop policy if exists providers_manager_select on public.telecom_providers;
drop policy if exists providers_manager_select on public.telecom_providers;
create policy providers_manager_select
on public.telecom_providers for select to authenticated
using (public.is_manager());

drop policy if exists prefixes_manager_all on public.telecom_prefixes;
drop policy if exists prefixes_manager_select on public.telecom_prefixes;
create policy prefixes_manager_select
on public.telecom_prefixes for select to authenticated
using (public.is_manager());

drop policy if exists plans_manager_all on public.protection_plans;
drop policy if exists plans_manager_insert on public.protection_plans;
drop policy if exists plans_manager_update on public.protection_plans;
drop policy if exists plans_manager_write on public.protection_plans;
drop policy if exists plans_manager_select on public.protection_plans;
drop policy if exists plans_manager_select on public.protection_plans;
create policy plans_manager_select
on public.protection_plans for select to authenticated
using (public.is_manager());

drop policy if exists payment_methods_manager_all on public.payment_methods;
drop policy if exists payment_methods_manager_insert on public.payment_methods;
drop policy if exists payment_methods_manager_update on public.payment_methods;
drop policy if exists payment_methods_manager_write on public.payment_methods;
drop policy if exists payment_methods_manager_select on public.payment_methods;
drop policy if exists payment_methods_manager_select on public.payment_methods;
create policy payment_methods_manager_select
on public.payment_methods for select to authenticated
using (public.is_manager());

drop policy if exists task_settings_manager_all on public.task_settings;
drop policy if exists task_settings_manager_insert on public.task_settings;
drop policy if exists task_settings_manager_update on public.task_settings;
drop policy if exists task_settings_manager_select on public.task_settings;
drop policy if exists task_settings_manager_select on public.task_settings;
create policy task_settings_manager_select
on public.task_settings for select to authenticated
using (public.is_manager());

-- 5) System settings are read-only directly; changes go through RPC.
drop policy if exists system_settings_manager_write on public.system_settings;
drop policy if exists system_settings_manager_select on public.system_settings;
create policy system_settings_manager_select
on public.system_settings for select to authenticated
using (public.is_manager() or auth.role() = 'authenticated');

-- 6) Users: managers may inspect all users but cannot directly change role/status.
drop policy if exists users_manager_all on public.users;
drop policy if exists users_manager_select on public.users;
create policy users_manager_select
on public.users for select to authenticated
using (public.is_manager());

-- 7) The previous unconditional DELETE triggers would also block the safe-delete
-- RPCs. Remove them. Direct DELETE is still impossible because no DELETE RLS
-- policies exist; only the explicit admin_*_delete RPCs can delete an unreferenced row.
drop trigger if exists prevent_provider_delete on public.telecom_providers;
drop trigger if exists prevent_plan_delete on public.protection_plans;
drop trigger if exists prevent_payment_method_delete on public.payment_methods;
drop function if exists public.prevent_configuration_delete();

-- 8) Manager mutations must be able to audit DELETE/INSERT as well as UPDATE.
-- Existing configuration audit trigger already handles all three operations.

-- 9) Explicit grants for every new database-layer operation.
revoke all on function public.admin_create_notification(uuid,text,text,text) from public;
revoke all on function public.admin_customers(text) from public;
revoke all on function public.admin_customer_numbers(text,uuid,uuid) from public;
revoke all on function public.admin_protection_requests(text,text,uuid) from public;
revoke all on function public.admin_protections(text,text,uuid) from public;
revoke all on function public.admin_audit_logs(text,text,timestamptz,timestamptz) from public;

grant execute on function public.admin_update_system_settings(text,jsonb,text,text) to authenticated;
grant execute on function public.admin_create_notification(uuid,text,text,text) to authenticated;
grant execute on function public.admin_customers(text) to authenticated;
grant execute on function public.admin_customer_numbers(text,uuid,uuid) to authenticated;
grant execute on function public.admin_protection_requests(text,text,uuid) to authenticated;
grant execute on function public.admin_protections(text,text,uuid) to authenticated;
grant execute on function public.admin_audit_logs(text,text,timestamptz,timestamptz) to authenticated;

-- 10) Final statement: customer number/provider/status mutations remain RPC-only.
-- Manager numbers are reference/read operations per AMAN.XZ; no manager CRUD is invented.


-- ============================================================
-- V7 FINAL HARDENING / APPLICATION CONTRACT
-- ============================================================
-- This section intentionally strengthens V6 without adding product features
-- outside AMAN.XZ. It makes lifecycle/configuration rules database-owned.

-- 1) Renewal threshold is configurable, not hard-coded.
alter table public.system_settings
  add column if not exists renewal_threshold_days integer not null default 30;

alter table public.system_settings
  drop constraint if exists system_settings_renewal_threshold_nonnegative;
alter table public.system_settings
  add constraint system_settings_renewal_threshold_nonnegative
  check (renewal_threshold_days >= 0);

-- Ensure the singleton row exists with the AMAN defaults.
insert into public.system_settings(id, app_name, contact_data, renewal_threshold_days)
values (true, 'AMAN', '{}'::jsonb, 30)
on conflict (id) do nothing;

-- 2) Auth email is the source of truth. Sync users.email whenever Auth email changes.
create or replace function public.sync_auth_email_to_user_profile()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if new.email is distinct from old.email then
    update public.users
    set email = lower(trim(coalesce(new.email, '')))
    where id = new.id;
  end if;
  return new;
end;
$$;

drop trigger if exists on_auth_user_email_changed on auth.users;
create trigger on_auth_user_email_changed
after update of email on auth.users
for each row execute function public.sync_auth_email_to_user_profile();

-- Profile RPC may update the display name, but email changes must go through Supabase Auth.
create or replace function public.update_my_profile(p_name text, p_email text)
returns public.users
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user public.users;
  v_auth_email text;
begin
  if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;
  if nullif(trim(p_name), '') is null then raise exception 'NAME_REQUIRED'; end if;

  select lower(trim(email)) into v_auth_email from auth.users where id = auth.uid();
  if v_auth_email is null then raise exception 'AUTH_USER_NOT_FOUND'; end if;

  if nullif(trim(p_email), '') is null or lower(trim(p_email)) <> v_auth_email then
    raise exception 'EMAIL_UPDATE_MUST_USE_AUTH';
  end if;

  update public.users
  set name = trim(p_name), email = v_auth_email
  where id = auth.uid()
  returning * into v_user;

  if not found then raise exception 'USER_NOT_FOUND'; end if;
  return v_user;
end;
$$;

-- 3) Authoritative lifecycle status: every application read can derive the current
-- state from today's date. Persisted status remains active/expired for lifecycle/audit.
create or replace function public.protection_display_status(p_end_date date)
returns text
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
  v_threshold integer;
begin
  select renewal_threshold_days into v_threshold
  from public.system_settings where id = true;
  v_threshold := coalesce(v_threshold, 30);

  if p_end_date < current_date then return 'expired'; end if;
  if p_end_date <= current_date + v_threshold then return 'renewal_needed'; end if;
  return 'active';
end;
$$;

-- 4) Database-owned lifecycle maintenance. A trusted scheduler may call this daily.
create or replace function public.process_protection_lifecycle()
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_count integer;
begin
  -- This operation is intended for the trusted maintenance/scheduler path.
  update public.protections
  set status = 'expired', updated_at = now()
  where status = 'active' and end_date < current_date;
  get diagnostics v_count = row_count;

  if v_count > 0 then
    perform public.write_audit_log(
      'process_protection_lifecycle', null, 'protections',
      jsonb_build_object('expired_count', v_count), null, null
    );
  end if;
  return v_count;
end;
$$;

-- Keep the legacy maintenance function, but make it use the lifecycle processor.
create or replace function public.refresh_expired_protections()
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not public.is_manager() then raise exception 'MANAGER_REQUIRED'; end if;
  return public.process_protection_lifecycle();
end;
$$;

-- 5) Manager protection list uses the authoritative current display status.
create or replace function public.admin_protections(
  p_filter text default 'all',
  p_search text default null,
  p_provider_id uuid default null
)
returns setof public.protections
language sql stable security definer set search_path=public, pg_temp as $$
  select p.* from public.protections p
  where public.is_manager()
    and (p_provider_id is null or p.provider_id=p_provider_id)
    and (
      nullif(trim(p_search),'') is null
      or exists (
        select 1 from public.customer_numbers n
        join public.users u on u.id=n.customer_id
        where n.id=p.customer_number_id
          and (n.phone_number ilike '%'||trim(p_search)||'%' or u.name ilike '%'||trim(p_search)||'%' or u.email ilike '%'||trim(p_search)||'%')
      )
    )
    and case lower(coalesce(p_filter,'all'))
      when 'all' then true
      when 'active' then public.protection_display_status(p.end_date) = 'active'
      when 'expired' then public.protection_display_status(p.end_date) = 'expired'
      when 'renewal' then public.protection_display_status(p.end_date) = 'renewal_needed'
      else false
    end
  order by p.end_date, p.created_at desc;
$$;

-- 6) Prevent historical plan identity from changing after use.
create or replace function public.guard_used_plan_provider_change()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if new.provider_id is distinct from old.provider_id then
    if exists (select 1 from public.protection_requests where plan_id = old.id)
       or exists (select 1 from public.protections where plan_id = old.id) then
      raise exception 'USED_PLAN_PROVIDER_IMMUTABLE';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists protection_plan_provider_immutability on public.protection_plans;
create trigger protection_plan_provider_immutability
before update of provider_id on public.protection_plans
for each row execute function public.guard_used_plan_provider_change();

-- 7) Stronger task validity rules.
create or replace function public.complete_payment_task(p_task_id uuid)
returns public.payment_tasks
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_task public.payment_tasks;
  v_old public.payment_tasks;
  v_protection public.protections;
  v_settings public.task_settings;
  v_provider uuid;
  v_next_due date;
  v_next public.payment_tasks;
begin
  if not public.is_manager() then raise exception 'MANAGER_REQUIRED'; end if;

  select * into v_task from public.payment_tasks where id = p_task_id for update;
  if not found then raise exception 'TASK_NOT_FOUND'; end if;
  if v_task.status in ('completed','cancelled') then raise exception 'TASK_ALREADY_CLOSED'; end if;

  select * into v_protection from public.protections where id = v_task.protection_id for update;
  if not found then raise exception 'PROTECTION_NOT_FOUND'; end if;
  if v_protection.customer_number_id <> v_task.customer_number_id then
    raise exception 'TASK_PROTECTION_NUMBER_MISMATCH';
  end if;
  if v_protection.status <> 'active' or v_protection.end_date < current_date then
    raise exception 'PROTECTION_NOT_VALID_FOR_TASK';
  end if;

  v_old := v_task;
  update public.payment_tasks
  set status='completed', completed_at=now(), completed_by=auth.uid()
  where id=p_task_id
  returning * into v_task;

  select * into v_settings from public.task_settings where provider_id=v_protection.provider_id;

  if found and v_settings.recurring_task_enabled then
    v_next_due := v_task.due_date + v_settings.repeat_interval_days;
    if v_next_due <= v_protection.end_date then
      insert into public.payment_tasks(
        protection_id, customer_number_id, task_type, amount, due_date, status
      ) values (
        v_protection.id, v_protection.customer_number_id, 'recurring',
        v_settings.recurring_task_amount, v_next_due,
        case when v_next_due < current_date then 'overdue'
             when v_next_due = current_date then 'due'
             else 'upcoming' end
      ) returning * into v_next;
    end if;
  end if;

  perform public.write_audit_log(
    'complete_payment_task', p_task_id, 'payment_tasks',
    jsonb_build_object('next_task_id', v_next.id),
    to_jsonb(v_old), to_jsonb(v_task)
  );
  return v_task;
end;
$$;

-- 8) Rescheduling must remain within the protection lifecycle and cannot be in the past.
create or replace function public.reschedule_payment_task(
  p_task_id uuid,
  p_new_due_date date,
  p_reason text default null
)
returns public.payment_tasks
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_task public.payment_tasks;
  v_old public.payment_tasks;
  v_protection public.protections;
  v_provider uuid;
  v_allowed boolean;
begin
  if not public.is_manager() then raise exception 'MANAGER_REQUIRED'; end if;
  select * into v_task from public.payment_tasks where id=p_task_id for update;
  if not found then raise exception 'TASK_NOT_FOUND'; end if;
  if v_task.status in ('completed','cancelled') then raise exception 'TASK_ALREADY_CLOSED'; end if;
  if p_new_due_date is null then raise exception 'NEW_DUE_DATE_REQUIRED'; end if;
  if p_new_due_date < current_date then raise exception 'NEW_DUE_DATE_IN_PAST'; end if;

  select * into v_protection from public.protections where id=v_task.protection_id for update;
  if not found then raise exception 'PROTECTION_NOT_FOUND'; end if;
  if v_protection.customer_number_id <> v_task.customer_number_id then
    raise exception 'TASK_PROTECTION_NUMBER_MISMATCH';
  end if;
  if v_protection.end_date < current_date then raise exception 'PROTECTION_EXPIRED'; end if;
  if p_new_due_date > v_protection.end_date then raise exception 'DUE_DATE_AFTER_PROTECTION_END'; end if;

  select manual_reschedule_enabled into v_allowed
  from public.task_settings where provider_id=v_protection.provider_id;
  if coalesce(v_allowed,false)=false then raise exception 'MANUAL_RESCHEDULE_DISABLED'; end if;

  v_old := v_task;
  update public.payment_tasks
  set previous_due_date=due_date,
      due_date=p_new_due_date,
      rescheduled_at=now(),
      rescheduled_by=auth.uid(),
      reschedule_reason=p_reason,
      status=case when p_new_due_date=current_date then 'due' else 'upcoming' end
  where id=p_task_id returning * into v_task;

  perform public.write_audit_log(
    'reschedule_payment_task', p_task_id, 'payment_tasks',
    jsonb_build_object('reason',p_reason), to_jsonb(v_old), to_jsonb(v_task)
  );
  return v_task;
end;
$$;

-- 9) Prevent direct manager writes that bypass RPC-only configuration contracts.
-- (Existing RLS policies already provide the access boundary.)

-- 10) Add the configurable lifecycle field to the manager settings RPC.
drop function if exists public.admin_update_system_settings(text,jsonb,text,text);
create or replace function public.admin_update_system_settings(
  p_app_name text,
  p_contact_data jsonb,
  p_terms_and_conditions text,
  p_privacy_policy text,
  p_renewal_threshold_days integer default 30
)
returns public.system_settings
language plpgsql security definer set search_path=public, pg_temp
as $$
declare v_row public.system_settings;
begin
  if not public.is_manager() then raise exception 'MANAGER_ONLY'; end if;
  if p_renewal_threshold_days < 0 then raise exception 'RENEWAL_THRESHOLD_INVALID'; end if;

  update public.system_settings
  set app_name=trim(p_app_name),
      contact_data=coalesce(p_contact_data,'{}'::jsonb),
      terms_and_conditions=p_terms_and_conditions,
      privacy_policy=p_privacy_policy,
      renewal_threshold_days=p_renewal_threshold_days,
      updated_at=now()
  where id=true
  returning * into v_row;

  if not found then
    insert into public.system_settings(
      id,app_name,contact_data,terms_and_conditions,privacy_policy,renewal_threshold_days
    ) values (
      true,trim(p_app_name),coalesce(p_contact_data,'{}'::jsonb),
      p_terms_and_conditions,p_privacy_policy,p_renewal_threshold_days
    ) returning * into v_row;
  end if;
  return v_row;
end;
$$;

revoke all on function public.admin_update_system_settings(text,jsonb,text,text,integer) from public;
grant execute on function public.admin_update_system_settings(text,jsonb,text,text,integer) to authenticated;

-- 11) Customer protection read contract with database-owned current lifecycle status.
create or replace function public.get_my_protections()
returns table (
  id uuid,
  customer_id uuid,
  customer_number_id uuid,
  provider_id uuid,
  plan_id uuid,
  created_from_request_id uuid,
  protection_value numeric,
  protection_duration_days integer,
  start_date date,
  end_date date,
  status public.protection_status,
  display_status text,
  days_remaining integer,
  plan_name_snapshot text,
  provider_name_snapshot text,
  created_at timestamptz,
  updated_at timestamptz
)
language sql stable security definer set search_path=public, pg_temp
as $$
  select p.id,p.customer_id,p.customer_number_id,p.provider_id,p.plan_id,
         p.created_from_request_id,p.protection_value,p.protection_duration_days,
         p.start_date,p.end_date,p.status,
         public.protection_display_status(p.end_date) as display_status,
         greatest(p.end_date-current_date,0) as days_remaining,
         p.plan_name_snapshot,p.provider_name_snapshot,p.created_at,p.updated_at
  from public.protections p
  where p.customer_id=auth.uid()
  order by p.end_date desc,p.created_at desc;
$$;

-- 12) Optional Supabase pg_cron lifecycle scheduling.
-- If pg_cron is enabled in the project, schedule a daily maintenance run.
do $$
begin
  if exists (select 1 from pg_extension where extname='pg_cron') then
    begin
      perform cron.unschedule(jobid)
      from cron.job
      where jobname='aman_process_protection_lifecycle';
    exception when others then
      null;
    end;
    begin
      perform cron.schedule(
        'aman_process_protection_lifecycle',
        '5 0 * * *',
        $cron$select public.process_protection_lifecycle();$cron$
      );
    exception when others then
      raise notice 'AMAN: pg_cron exists but automatic schedule could not be created: %', sqlerrm;
    end;
  else
    raise notice 'AMAN: pg_cron is not enabled; configure a trusted daily scheduler to call process_protection_lifecycle().';
  end if;
end $$;

-- 13) Final contract grants.
revoke all on function public.process_protection_lifecycle() from public;
revoke all on function public.get_my_protections() from public;
grant execute on function public.get_my_protections() to authenticated;

-- ============================================================
-- END AMAN TARGET DATABASE V7 FINAL
-- ============================================================
