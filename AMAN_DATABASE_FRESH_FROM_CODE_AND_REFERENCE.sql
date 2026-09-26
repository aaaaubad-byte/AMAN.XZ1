
-- AMAN fresh database schema
-- Source of truth for this build:
--   1) Current Kotlin application code + Gradle build files
--   2) AMAN.XZ.txt
-- Intentionally NOT based on any SQL/CSV/audit/database-export file in the repository.
--
-- IMPORTANT AUTH SETTING:
-- In Supabase Dashboard -> Authentication -> Providers -> Email:
-- disable "Confirm email" for this project.
-- The Android code expects signUpWith(Email) to return an authenticated session
-- immediately; no email verification step is required.

begin;

create extension if not exists pgcrypto;

-- =========================================================
-- 1. USERS / AUTH PROFILE
-- =========================================================

create table if not exists public.users (
    id uuid primary key references auth.users(id) on delete cascade,
    name text not null default '',
    email text not null,
    role text not null default 'customer'
        check (role in ('customer','client','manager','admin')),
    status text not null default 'active'
        check (status in ('active','suspended','disabled')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists users_email_lower_uidx
    on public.users (lower(email));

-- =========================================================
-- 2. TELECOM PROVIDERS / PREFIXES
-- =========================================================

create table if not exists public.telecom_providers (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    code text not null,
    phone_length integer not null default 9 check (phone_length > 0),
    is_active boolean not null default true,
    is_visible_to_customers boolean not null default true,
    display_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(code)
);

create table if not exists public.telecom_prefixes (
    id uuid primary key default gen_random_uuid(),
    provider_id uuid not null references public.telecom_providers(id) on delete restrict,
    prefix text not null,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(provider_id, prefix)
);

create index if not exists telecom_prefixes_prefix_idx
    on public.telecom_prefixes(prefix);

-- =========================================================
-- 3. CUSTOMER NUMBERS
-- =========================================================

create table if not exists public.customer_numbers (
    id uuid primary key default gen_random_uuid(),
    customer_id uuid not null references public.users(id) on delete cascade,
    provider_id uuid not null references public.telecom_providers(id) on delete restrict,
    phone_number text not null,
    status text not null default 'active'
        check (status in ('active','suspended','inactive')),
    protection_status text not null default 'unprotected'
        check (protection_status in ('unprotected','pending','protected')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(phone_number)
);

create index if not exists customer_numbers_customer_idx
    on public.customer_numbers(customer_id);

create index if not exists customer_numbers_provider_idx
    on public.customer_numbers(provider_id);

-- =========================================================
-- 4. PROTECTION PLANS
-- =========================================================

create table if not exists public.protection_plans (
    id uuid primary key default gen_random_uuid(),
    provider_id uuid not null references public.telecom_providers(id) on delete restrict,
    name text not null,
    price double precision not null check (price >= 0),
    currency text not null default 'SAR',
    protection_duration_days integer not null check (protection_duration_days > 0),
    is_active boolean not null default true,
    is_visible_to_customers boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists protection_plans_provider_idx
    on public.protection_plans(provider_id);

-- =========================================================
-- 5. PAYMENT METHODS
-- =========================================================

create table if not exists public.payment_methods (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    account_number text not null,
    account_owner_name text not null default '',
    payment_instructions text,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- =========================================================
-- 6. PROTECTION REQUESTS
-- =========================================================

create table if not exists public.protection_requests (
    id uuid primary key default gen_random_uuid(),
    customer_id uuid not null references public.users(id) on delete restrict,
    customer_number_id uuid not null references public.customer_numbers(id) on delete restrict,
    provider_id uuid not null references public.telecom_providers(id) on delete restrict,
    plan_id uuid not null references public.protection_plans(id) on delete restrict,
    payment_method_id uuid not null references public.payment_methods(id) on delete restrict,
    protection_value double precision not null default 0 check (protection_value >= 0),
    transfer_data jsonb,
    status text not null default 'pending'
        check (status in ('pending','approved','rejected')),
    rejection_reason text,
    reviewed_by uuid references public.users(id) on delete set null,
    reviewed_at timestamptz,
    plan_name_snapshot text,
    plan_price_snapshot double precision,
    plan_duration_days_snapshot integer,
    payment_method_name_snapshot text,
    payment_account_number_snapshot text,
    payment_account_owner_snapshot text,
    payment_instructions_snapshot text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists protection_requests_customer_idx
    on public.protection_requests(customer_id);

create index if not exists protection_requests_number_idx
    on public.protection_requests(customer_number_id);

create index if not exists protection_requests_status_idx
    on public.protection_requests(status);

create unique index if not exists unique_pending_request_per_number
    on public.protection_requests(customer_number_id)
    where status = 'pending';

-- =========================================================
-- 7. PROTECTIONS
-- =========================================================

create table if not exists public.protections (
    id uuid primary key default gen_random_uuid(),
    customer_id uuid not null references public.users(id) on delete restrict,
    customer_number_id uuid not null references public.customer_numbers(id) on delete restrict,
    provider_id uuid not null references public.telecom_providers(id) on delete restrict,
    plan_id uuid not null references public.protection_plans(id) on delete restrict,
    created_from_request_id uuid unique references public.protection_requests(id) on delete restrict,
    protection_value double precision not null default 0 check (protection_value >= 0),
    protection_duration_days integer not null check (protection_duration_days > 0),
    start_date timestamptz not null,
    end_date timestamptz not null,
    status text not null default 'active'
        check (status in ('active','expired')),
    plan_name_snapshot text,
    provider_name_snapshot text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (end_date > start_date)
);

create index if not exists protections_customer_idx
    on public.protections(customer_id);

create index if not exists protections_number_idx
    on public.protections(customer_number_id);

create index if not exists protections_status_idx
    on public.protections(status);

create unique index if not exists unique_active_protection_per_number
    on public.protections(customer_number_id)
    where status = 'active';

-- =========================================================
-- 8. PAYMENT TASKS
-- =========================================================

create table if not exists public.payment_tasks (
    id uuid primary key default gen_random_uuid(),
    protection_id uuid not null references public.protections(id) on delete restrict,
    customer_number_id uuid not null references public.customer_numbers(id) on delete restrict,
    customer_id uuid not null references public.users(id) on delete restrict,
    provider_id uuid references public.telecom_providers(id) on delete restrict,
    task_type text not null default 'recurring'
        check (task_type in ('first','recurring','manual')),
    amount double precision not null default 0 check (amount >= 0),
    due_date timestamptz not null,
    status text not null default 'upcoming'
        check (status in ('upcoming','pending','due_soon','due','overdue','completed','cancelled')),
    completed_at timestamptz,
    completed_by uuid references public.users(id) on delete set null,
    previous_due_date timestamptz,
    rescheduled_at timestamptz,
    rescheduled_by uuid references public.users(id) on delete set null,
    reschedule_reason text,
    cancelled_at timestamptz,
    cancelled_by uuid references public.users(id) on delete set null,
    cancellation_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists payment_tasks_protection_idx on public.payment_tasks(protection_id);
create index if not exists payment_tasks_customer_idx on public.payment_tasks(customer_id);
create index if not exists payment_tasks_number_idx on public.payment_tasks(customer_number_id);
create index if not exists payment_tasks_status_idx on public.payment_tasks(status);
create index if not exists payment_tasks_due_idx on public.payment_tasks(due_date);

-- =========================================================
-- 9. TASK SETTINGS
-- =========================================================

create table if not exists public.task_settings (
    id uuid primary key default gen_random_uuid(),
    provider_id uuid not null unique references public.telecom_providers(id) on delete restrict,
    first_task_enabled boolean not null default false,
    first_task_amount double precision default 0,
    recurring_task_enabled boolean not null default true,
    recurring_task_amount double precision not null default 0,
    repeat_interval_days integer not null default 30 check (repeat_interval_days > 0),
    days_visible_before_due integer not null default 7 check (days_visible_before_due >= 0),
    manual_reschedule_enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- =========================================================
-- 10. NOTIFICATIONS
-- =========================================================

create table if not exists public.notifications (
    id uuid primary key default gen_random_uuid(),
    customer_id uuid references public.users(id) on delete cascade,
    title text not null,
    message text not null,
    notification_type text not null default 'general',
    is_read boolean not null default false,
    read_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists notifications_customer_idx
    on public.notifications(customer_id);

create index if not exists notifications_unread_idx
    on public.notifications(customer_id, is_read)
    where is_read = false;

-- =========================================================
-- 11. AUDIT LOGS
-- =========================================================

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

create index if not exists audit_logs_created_idx on public.audit_logs(created_at desc);
create index if not exists audit_logs_actor_idx on public.audit_logs(actor_id);

-- =========================================================
-- 12. SYSTEM SETTINGS
-- =========================================================

create table if not exists public.system_settings (
    id boolean primary key default true check (id = true),
    app_name text not null default 'AMAN',
    contact_data jsonb,
    terms_and_conditions text,
    privacy_policy text,
    renewal_threshold_days integer not null default 30 check (renewal_threshold_days >= 0),
    updated_at timestamptz not null default now()
);

insert into public.system_settings(id)
values (true)
on conflict (id) do nothing;

-- =========================================================
-- 13. COMMON HELPERS
-- =========================================================

create or replace function public.is_manager_or_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.users u
        where u.id = auth.uid()
          and u.role in ('manager','admin')
          and u.status = 'active'
    );
$$;

create or replace function public.touch_updated_at()
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
        'protections','payment_tasks','task_settings','system_settings'
    ] loop
        execute format('drop trigger if exists %I on public.%I', 'trg_'||t||'_updated_at', t);
        execute format(
            'create trigger %I before update on public.%I for each row execute function public.touch_updated_at()',
            'trg_'||t||'_updated_at', t
        );
    end loop;
end $$;

-- =========================================================
-- 14. AUTH -> public.users
-- =========================================================

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    display_name text;
begin
    display_name :=
        coalesce(
            nullif(new.raw_user_meta_data->>'name',''),
            nullif(new.raw_user_meta_data->>'full_name',''),
            split_part(coalesce(new.email,''),'@',1),
            'User'
        );

    insert into public.users(id, name, email, role, status)
    values (new.id, display_name, coalesce(new.email,''), 'customer', 'active')
    on conflict (id) do update
        set name = excluded.name,
            email = excluded.email,
            updated_at = now();

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

create or replace function public.handle_auth_user_email_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.email is distinct from old.email then
        update public.users
        set email = coalesce(new.email,''),
            updated_at = now()
        where id = new.id;
    end if;
    return new;
end;
$$;

drop trigger if exists on_auth_user_email_updated on auth.users;
create trigger on_auth_user_email_updated
after update of email on auth.users
for each row execute function public.handle_auth_user_email_update();

-- =========================================================
-- 15. RLS
-- =========================================================

alter table public.users enable row level security;
alter table public.telecom_providers enable row level security;
alter table public.telecom_prefixes enable row level security;
alter table public.customer_numbers enable row level security;
alter table public.protection_plans enable row level security;
alter table public.payment_methods enable row level security;
alter table public.protection_requests enable row level security;
alter table public.protections enable row level security;
alter table public.payment_tasks enable row level security;
alter table public.task_settings enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_logs enable row level security;
alter table public.system_settings enable row level security;

-- Users
drop policy if exists users_select on public.users;
create policy users_select on public.users
for select to authenticated
using (id = auth.uid() or public.is_manager_or_admin());

drop policy if exists users_update on public.users;
create policy users_update on public.users
for update to authenticated
using (id = auth.uid() or public.is_manager_or_admin())
with check (id = auth.uid() or public.is_manager_or_admin());

-- Providers
drop policy if exists providers_select on public.telecom_providers;
create policy providers_select on public.telecom_providers
for select to authenticated
using (
    public.is_manager_or_admin()
    or (is_active = true and is_visible_to_customers = true)
);

drop policy if exists providers_update on public.telecom_providers;
create policy providers_update on public.telecom_providers
for update to authenticated
using (public.is_manager_or_admin())
with check (public.is_manager_or_admin());

-- Prefixes
drop policy if exists prefixes_select on public.telecom_prefixes;
create policy prefixes_select on public.telecom_prefixes
for select to authenticated
using (
    public.is_manager_or_admin()
    or is_active = true
);

drop policy if exists prefixes_insert on public.telecom_prefixes;
create policy prefixes_insert on public.telecom_prefixes
for insert to authenticated
with check (public.is_manager_or_admin());

drop policy if exists prefixes_update on public.telecom_prefixes;
create policy prefixes_update on public.telecom_prefixes
for update to authenticated
using (public.is_manager_or_admin())
with check (public.is_manager_or_admin());

-- Customer numbers
drop policy if exists customer_numbers_select on public.customer_numbers;
create policy customer_numbers_select on public.customer_numbers
for select to authenticated
using (customer_id = auth.uid() or public.is_manager_or_admin());

drop policy if exists customer_numbers_insert on public.customer_numbers;
create policy customer_numbers_insert on public.customer_numbers
for insert to authenticated
with check (
    customer_id = auth.uid()
    and status = 'active'
    and protection_status = 'unprotected'
    and exists (
        select 1
        from public.telecom_providers p
        where p.id = provider_id
          and p.is_active
          and p.is_visible_to_customers
          and length(phone_number) = p.phone_length
    )
    and exists (
        select 1
        from public.telecom_prefixes x
        where x.provider_id = provider_id
          and x.is_active = true
          and phone_number like x.prefix || '%'
    )
);

-- Plans
drop policy if exists plans_select on public.protection_plans;
create policy plans_select on public.protection_plans
for select to authenticated
using (
    public.is_manager_or_admin()
    or (
        is_active = true
        and is_visible_to_customers = true
        and exists (
            select 1 from public.telecom_providers p
            where p.id = provider_id and p.is_active and p.is_visible_to_customers
        )
    )
);

drop policy if exists plans_update on public.protection_plans;
create policy plans_update on public.protection_plans
for update to authenticated
using (public.is_manager_or_admin())
with check (public.is_manager_or_admin());

-- Payment methods
drop policy if exists payment_methods_select on public.payment_methods;
create policy payment_methods_select on public.payment_methods
for select to authenticated
using (public.is_manager_or_admin() or is_active = true);

-- Protection requests
drop policy if exists protection_requests_select on public.protection_requests;
create policy protection_requests_select on public.protection_requests
for select to authenticated
using (customer_id = auth.uid() or public.is_manager_or_admin());

-- Protections
drop policy if exists protections_select on public.protections;
create policy protections_select on public.protections
for select to authenticated
using (customer_id = auth.uid() or public.is_manager_or_admin());

-- Tasks are internal/admin only
drop policy if exists payment_tasks_select on public.payment_tasks;
create policy payment_tasks_select on public.payment_tasks
for select to authenticated
using (public.is_manager_or_admin());

-- Task settings admin only
drop policy if exists task_settings_select on public.task_settings;
create policy task_settings_select on public.task_settings
for select to authenticated
using (public.is_manager_or_admin());

drop policy if exists task_settings_insert on public.task_settings;
create policy task_settings_insert on public.task_settings
for insert to authenticated
with check (public.is_manager_or_admin());

drop policy if exists task_settings_update on public.task_settings;
create policy task_settings_update on public.task_settings
for update to authenticated
using (public.is_manager_or_admin())
with check (public.is_manager_or_admin());

-- Notifications
drop policy if exists notifications_select on public.notifications;
create policy notifications_select on public.notifications
for select to authenticated
using (customer_id = auth.uid() or public.is_manager_or_admin());

drop policy if exists notifications_update on public.notifications;
create policy notifications_update on public.notifications
for update to authenticated
using (customer_id = auth.uid() or public.is_manager_or_admin())
with check (customer_id = auth.uid() or public.is_manager_or_admin());

-- Audit logs
drop policy if exists audit_logs_select on public.audit_logs;
create policy audit_logs_select on public.audit_logs
for select to authenticated
using (public.is_manager_or_admin());

-- System settings
drop policy if exists system_settings_select on public.system_settings;
create policy system_settings_select on public.system_settings
for select to authenticated
using (true);

drop policy if exists system_settings_update on public.system_settings;
create policy system_settings_update on public.system_settings
for update to authenticated
using (public.is_manager_or_admin())
with check (public.is_manager_or_admin());

-- =========================================================
-- 16. CUSTOMER RPC: ADD NUMBER
-- =========================================================

create or replace function public.add_customer_number(p_phone_number text)
returns public.customer_numbers
language plpgsql
security definer
set search_path = public
as $$
declare
    clean_phone text := regexp_replace(coalesce(p_phone_number,''), '\D', '', 'g');
    v_provider_id uuid;
    v_provider public.telecom_providers%rowtype;
    v_number public.customer_numbers%rowtype;
begin
    if auth.uid() is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    if clean_phone = '' then
        raise exception 'INVALID_PHONE_NUMBER';
    end if;

    select p.*
      into v_provider
      from public.telecom_prefixes x
      join public.telecom_providers p on p.id = x.provider_id
     where x.is_active = true
       and p.is_active = true
       and p.is_visible_to_customers = true
       and clean_phone like x.prefix || '%'
       and length(clean_phone) = p.phone_length
     order by length(x.prefix) desc
     limit 1;

    if v_provider.id is null then
        raise exception 'PROVIDER_NOT_FOUND';
    end if;

    if exists (select 1 from public.customer_numbers where phone_number = clean_phone) then
        raise exception 'PHONE_ALREADY_REGISTERED';
    end if;

    insert into public.customer_numbers(
        customer_id, provider_id, phone_number, status, protection_status
    )
    values (
        auth.uid(), v_provider.id, clean_phone, 'active', 'unprotected'
    )
    returning * into v_number;

    return v_number;
end;
$$;

-- =========================================================
-- 17. CUSTOMER RPC: PROVIDER BY PREFIX
-- =========================================================

create or replace function public.get_provider_by_prefix(p_phone_number text)
returns text
language sql
stable
security definer
set search_path = public
as $$
    select p.id::text
    from public.telecom_prefixes x
    join public.telecom_providers p on p.id = x.provider_id
    where x.is_active = true
      and p.is_active = true
      and p.is_visible_to_customers = true
      and regexp_replace(coalesce(p_phone_number,''), '\D', '', 'g') like x.prefix || '%'
      and length(regexp_replace(coalesce(p_phone_number,''), '\D', '', 'g')) = p.phone_length
    order by length(x.prefix) desc
    limit 1;
$$;

-- =========================================================
-- 18. CUSTOMER RPC: CREATE PROTECTION REQUEST
-- =========================================================

create or replace function public.create_protection_request(
    p_customer_number_id uuid,
    p_plan_id uuid,
    p_payment_method_id uuid,
    p_transfer_data jsonb
)
returns public.protection_requests
language plpgsql
security definer
set search_path = public
as $$
declare
    n public.customer_numbers%rowtype;
    pl public.protection_plans%rowtype;
    pm public.payment_methods%rowtype;
    req public.protection_requests%rowtype;
begin
    if auth.uid() is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    select * into n
    from public.customer_numbers
    where id = p_customer_number_id
      and customer_id = auth.uid()
    for update;

    if n.id is null then raise exception 'NUMBER_NOT_FOUND_OR_NOT_OWNED'; end if;
    if n.status <> 'active' then raise exception 'NUMBER_NOT_ACTIVE'; end if;

    if exists (
        select 1 from public.protections
        where customer_number_id = n.id and status = 'active'
    ) then
        raise exception 'ACTIVE_PROTECTION_EXISTS';
    end if;

    if exists (
        select 1 from public.protection_requests
        where customer_number_id = n.id and status = 'pending'
    ) then
        raise exception 'CONFLICTING_REQUEST_EXISTS';
    end if;

    select * into pl
    from public.protection_plans
    where id = p_plan_id
      and provider_id = n.provider_id
      and is_active = true
      and is_visible_to_customers = true;

    if pl.id is null then raise exception 'PLAN_NOT_AVAILABLE'; end if;

    select * into pm
    from public.payment_methods
    where id = p_payment_method_id
      and is_active = true;

    if pm.id is null then raise exception 'PAYMENT_METHOD_NOT_AVAILABLE'; end if;

    insert into public.protection_requests(
        customer_id, customer_number_id, provider_id, plan_id,
        payment_method_id, protection_value, transfer_data, status,
        plan_name_snapshot, plan_price_snapshot, plan_duration_days_snapshot,
        payment_method_name_snapshot, payment_account_number_snapshot,
        payment_account_owner_snapshot, payment_instructions_snapshot
    )
    values (
        auth.uid(), n.id, n.provider_id, pl.id,
        pm.id, pl.price, coalesce(p_transfer_data,'{}'::jsonb), 'pending',
        pl.name, pl.price, pl.protection_duration_days,
        pm.name, pm.account_number, pm.account_owner_name, pm.payment_instructions
    )
    returning * into req;

    update public.customer_numbers
       set protection_status = 'pending'
     where id = n.id;

    return req;
exception
    when unique_violation then
        raise exception 'CONFLICTING_REQUEST_EXISTS';
end;
$$;

-- =========================================================
-- 19. ADMIN RPC: APPROVE REQUEST
-- =========================================================

create or replace function public.approve_protection_request(p_request_id uuid)
returns public.protections
language plpgsql
security definer
set search_path = public
as $$
declare
    r public.protection_requests%rowtype;
    n public.customer_numbers%rowtype;
    pl public.protection_plans%rowtype;
    pr public.protections%rowtype;
    ts public.task_settings%rowtype;
    v_start timestamptz := now();
    v_end timestamptz;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;

    select * into r from public.protection_requests where id = p_request_id for update;
    if r.id is null then raise exception 'REQUEST_NOT_FOUND'; end if;
    if r.status <> 'pending' then raise exception 'REQUEST_ALREADY_PROCESSED'; end if;

    select * into n from public.customer_numbers where id = r.customer_number_id for update;
    if n.id is null or n.status <> 'active' then raise exception 'NUMBER_NOT_VALID'; end if;
    if n.provider_id <> r.provider_id then raise exception 'PROVIDER_MISMATCH'; end if;

    if exists (
        select 1 from public.protections
        where customer_number_id = n.id and status = 'active'
    ) then
        raise exception 'ACTIVE_PROTECTION_EXISTS';
    end if;

    select * into pl
    from public.protection_plans
    where id = r.plan_id and provider_id = n.provider_id;

    if pl.id is null then raise exception 'PLAN_NOT_FOUND'; end if;

    -- Use the snapshot saved at request time for historical integrity.
    v_end := v_start + make_interval(days => coalesce(r.plan_duration_days_snapshot, pl.protection_duration_days));

    insert into public.protections(
        customer_id, customer_number_id, provider_id, plan_id,
        created_from_request_id, protection_value, protection_duration_days,
        start_date, end_date, status, plan_name_snapshot, provider_name_snapshot
    )
    values (
        r.customer_id, r.customer_number_id, r.provider_id, r.plan_id,
        r.id, coalesce(r.plan_price_snapshot, r.protection_value),
        coalesce(r.plan_duration_days_snapshot, pl.protection_duration_days),
        v_start, v_end, 'active', r.plan_name_snapshot,
        (select name from public.telecom_providers where id = r.provider_id)
    )
    returning * into pr;

    update public.protection_requests
       set status = 'approved',
           reviewed_by = auth.uid(),
           reviewed_at = now()
     where id = r.id;

    update public.customer_numbers
       set protection_status = 'protected'
     where id = n.id;

    select * into ts from public.task_settings where provider_id = n.provider_id;

    if coalesce(ts.first_task_enabled, false) then
        insert into public.payment_tasks(
            protection_id, customer_number_id, customer_id, provider_id,
            task_type, amount, due_date, status
        )
        values (
            pr.id, n.id, r.customer_id, n.provider_id,
            'first', coalesce(ts.first_task_amount,0),
            v_start, 'due'
        );
    end if;

    insert into public.notifications(customer_id,title,message,notification_type)
    values (
        r.customer_id,
        'تم تفعيل الحماية',
        'تم اعتماد طلب الحماية وتفعيل الحماية على رقمك.',
        'protection_approved'
    );

    insert into public.audit_logs(
        actor_id, action_type, affected_record_id, affected_table, details, new_data
    )
    values (
        auth.uid(), 'approve_protection_request', r.id, 'protection_requests',
        jsonb_build_object('protection_id', pr.id),
        to_jsonb(pr)
    );

    return pr;
exception
    when unique_violation then
        raise exception 'ACTIVE_PROTECTION_EXISTS';
end;
$$;

-- =========================================================
-- 20. ADMIN RPC: REJECT REQUEST
-- =========================================================

create or replace function public.reject_protection_request(
    p_request_id uuid,
    p_reason text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    r public.protection_requests%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    if nullif(trim(coalesce(p_reason,'')),'') is null then raise exception 'REJECTION_REASON_REQUIRED'; end if;

    select * into r from public.protection_requests where id = p_request_id for update;
    if r.id is null then raise exception 'REQUEST_NOT_FOUND'; end if;
    if r.status <> 'pending' then raise exception 'REQUEST_ALREADY_PROCESSED'; end if;

    update public.protection_requests
       set status='rejected',
           rejection_reason=trim(p_reason),
           reviewed_by=auth.uid(),
           reviewed_at=now()
     where id=r.id;

    update public.customer_numbers
       set protection_status='unprotected'
     where id=r.customer_number_id
       and not exists (
           select 1 from public.protections
           where customer_number_id=r.customer_number_id and status='active'
       )
       and not exists (
           select 1 from public.protection_requests
           where customer_number_id=r.customer_number_id and status='pending'
       );

    insert into public.notifications(customer_id,title,message,notification_type)
    values (
        r.customer_id,
        'تم رفض طلب الحماية',
        'تم رفض طلب الحماية. السبب: ' || trim(p_reason),
        'protection_rejected'
    );

    insert into public.audit_logs(
        actor_id, action_type, affected_record_id, affected_table, details
    )
    values (
        auth.uid(), 'reject_protection_request', r.id, 'protection_requests',
        jsonb_build_object('reason',trim(p_reason))
    );
end;
$$;

-- =========================================================
-- 21. ADMIN: TELECOM PROVIDERS
-- =========================================================

create or replace function public.admin_create_telecom_provider(
    p_name text, p_code text, p_phone_length integer,
    p_prefixes jsonb, p_is_active boolean, p_is_visible_to_customers boolean,
    p_display_order integer
)
returns public.telecom_providers
language plpgsql security definer set search_path=public
as $$
declare
    p public.telecom_providers%rowtype;
    x text;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;

    insert into public.telecom_providers(name,code,phone_length,is_active,is_visible_to_customers,display_order)
    values(trim(p_name),upper(trim(p_code)),p_phone_length,p_is_active,p_is_visible_to_customers,p_display_order)
    returning * into p;

    for x in select jsonb_array_elements_text(coalesce(p_prefixes,'[]'::jsonb)) loop
        insert into public.telecom_prefixes(provider_id,prefix,is_active)
        values(p.id,trim(x),true);
    end loop;

    return p;
end;
$$;

create or replace function public.admin_update_telecom_provider(
    p_provider_id uuid, p_name text, p_code text, p_phone_length integer,
    p_prefixes jsonb, p_is_active boolean, p_is_visible_to_customers boolean,
    p_display_order integer
)
returns public.telecom_providers
language plpgsql security definer set search_path=public
as $$
declare
    p public.telecom_providers%rowtype;
    x text;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;

    update public.telecom_providers
       set name=trim(p_name), code=upper(trim(p_code)), phone_length=p_phone_length,
           is_active=p_is_active, is_visible_to_customers=p_is_visible_to_customers,
           display_order=p_display_order
     where id=p_provider_id
     returning * into p;

    if p.id is null then raise exception 'PROVIDER_NOT_FOUND'; end if;

    -- Preserve existing prefix rows but make the submitted list authoritative.
    update public.telecom_prefixes set is_active=false where provider_id=p.id;

    for x in select jsonb_array_elements_text(coalesce(p_prefixes,'[]'::jsonb)) loop
        insert into public.telecom_prefixes(provider_id,prefix,is_active)
        values(p.id,trim(x),true)
        on conflict(provider_id,prefix) do update set is_active=true, updated_at=now();
    end loop;

    return p;
end;
$$;

create or replace function public.admin_set_telecom_provider_status(
    p_provider_id uuid, p_is_active boolean
)
returns void
language plpgsql security definer set search_path=public
as $$
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    update public.telecom_providers set is_active=p_is_active where id=p_provider_id;
    if not found then raise exception 'PROVIDER_NOT_FOUND'; end if;
end;
$$;

-- =========================================================
-- 22. ADMIN: PLANS
-- =========================================================

create or replace function public.admin_create_protection_plan(
    p_provider_id uuid, p_name text, p_price double precision,
    p_duration_days integer, p_is_active boolean, p_is_visible_to_customers boolean
)
returns public.protection_plans
language plpgsql security definer set search_path=public
as $$
declare p public.protection_plans%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    if not exists(select 1 from public.telecom_providers where id=p_provider_id) then raise exception 'PROVIDER_NOT_FOUND'; end if;

    insert into public.protection_plans(provider_id,name,price,protection_duration_days,is_active,is_visible_to_customers)
    values(p_provider_id,trim(p_name),p_price,p_duration_days,p_is_active,p_is_visible_to_customers)
    returning * into p;
    return p;
end;
$$;

create or replace function public.admin_update_protection_plan(
    p_plan_id uuid, p_provider_id uuid, p_name text, p_price double precision,
    p_duration_days integer, p_is_active boolean, p_is_visible_to_customers boolean
)
returns public.protection_plans
language plpgsql security definer set search_path=public
as $$
declare p public.protection_plans%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;

    update public.protection_plans
       set provider_id=p_provider_id, name=trim(p_name), price=p_price,
           protection_duration_days=p_duration_days,
           is_active=p_is_active, is_visible_to_customers=p_is_visible_to_customers
     where id=p_plan_id
     returning * into p;

    if p.id is null then raise exception 'PLAN_NOT_FOUND'; end if;
    return p;
end;
$$;

create or replace function public.admin_set_protection_plan_status(
    p_plan_id uuid, p_is_active boolean
)
returns void
language plpgsql security definer set search_path=public
as $$
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    update public.protection_plans set is_active=p_is_active where id=p_plan_id;
    if not found then raise exception 'PLAN_NOT_FOUND'; end if;
end;
$$;

-- =========================================================
-- 23. ADMIN: PAYMENT METHODS
-- =========================================================

create or replace function public.admin_create_payment_method(
    p_name text, p_account_number text, p_account_owner_name text,
    p_payment_instructions text, p_is_active boolean
)
returns public.payment_methods
language plpgsql security definer set search_path=public
as $$
declare p public.payment_methods%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    insert into public.payment_methods(name,account_number,account_owner_name,payment_instructions,is_active)
    values(trim(p_name),trim(p_account_number),trim(p_account_owner_name),p_payment_instructions,p_is_active)
    returning * into p;
    return p;
end;
$$;

create or replace function public.admin_update_payment_method(
    p_payment_method_id uuid, p_name text, p_account_number text,
    p_account_owner_name text, p_payment_instructions text, p_is_active boolean
)
returns public.payment_methods
language plpgsql security definer set search_path=public
as $$
declare p public.payment_methods%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    update public.payment_methods
       set name=trim(p_name), account_number=trim(p_account_number),
           account_owner_name=trim(p_account_owner_name),
           payment_instructions=p_payment_instructions, is_active=p_is_active
     where id=p_payment_method_id
     returning * into p;
    if p.id is null then raise exception 'PAYMENT_METHOD_NOT_FOUND'; end if;
    return p;
end;
$$;

create or replace function public.admin_set_payment_method_status(
    p_payment_method_id uuid, p_is_active boolean
)
returns void
language plpgsql security definer set search_path=public
as $$
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    update public.payment_methods set is_active=p_is_active where id=p_payment_method_id;
    if not found then raise exception 'PAYMENT_METHOD_NOT_FOUND'; end if;
end;
$$;

-- =========================================================
-- 24. ADMIN: TASK LIFECYCLE
-- =========================================================

create or replace function public.complete_payment_task(p_task_id uuid)
returns public.payment_tasks
language plpgsql security definer set search_path=public
as $$
declare
    t public.payment_tasks%rowtype;
    p public.protections%rowtype;
    s public.task_settings%rowtype;
    next_due timestamptz;
    result_task public.payment_tasks%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;

    select * into t from public.payment_tasks where id=p_task_id for update;
    if t.id is null then raise exception 'TASK_NOT_FOUND'; end if;
    if t.status in ('completed','cancelled') then raise exception 'TASK_ALREADY_FINAL'; end if;

    update public.payment_tasks
       set status='completed', completed_at=now(), completed_by=auth.uid()
     where id=t.id
     returning * into result_task;

    select * into p from public.protections where id=t.protection_id;
    select * into s from public.task_settings where provider_id=coalesce(t.provider_id,p.provider_id);

    if t.task_type in ('first','recurring')
       and coalesce(s.recurring_task_enabled,false)
       and coalesce(s.repeat_interval_days,0) > 0
       and p.status='active'
       and p.end_date > now()
    then
        next_due := result_task.due_date + make_interval(days => s.repeat_interval_days);

        if next_due < p.end_date then
            insert into public.payment_tasks(
                protection_id,customer_number_id,customer_id,provider_id,
                task_type,amount,due_date,status
            )
            values(
                p.id,p.customer_number_id,p.customer_id,p.provider_id,
                'recurring',coalesce(s.recurring_task_amount,0),next_due,'upcoming'
            );
        end if;
    end if;

    insert into public.audit_logs(
        actor_id,action_type,affected_record_id,affected_table,details,new_data
    )
    values(
        auth.uid(),'complete_payment_task',t.id,'payment_tasks',
        jsonb_build_object('next_task_created',
            exists(select 1 from public.payment_tasks x
                   where x.protection_id=t.protection_id
                     and x.created_at > result_task.updated_at)),
        to_jsonb(result_task)
    );

    return result_task;
end;
$$;

create or replace function public.cancel_payment_task(
    p_task_id uuid, p_reason text
)
returns void
language plpgsql security definer set search_path=public
as $$
declare t public.payment_tasks%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    if nullif(trim(coalesce(p_reason,'')),'') is null then raise exception 'CANCELLATION_REASON_REQUIRED'; end if;

    select * into t from public.payment_tasks where id=p_task_id for update;
    if t.id is null then raise exception 'TASK_NOT_FOUND'; end if;
    if t.status in ('completed','cancelled') then raise exception 'TASK_ALREADY_FINAL'; end if;

    update public.payment_tasks
       set status='cancelled', cancelled_at=now(), cancelled_by=auth.uid(),
           cancellation_reason=trim(p_reason)
     where id=t.id;

    insert into public.audit_logs(actor_id,action_type,affected_record_id,affected_table,details)
    values(auth.uid(),'cancel_payment_task',t.id,'payment_tasks',
           jsonb_build_object('reason',trim(p_reason)));
end;
$$;

create or replace function public.reschedule_payment_task(
    p_task_id uuid, p_new_due_date timestamptz, p_reason text
)
returns void
language plpgsql security definer set search_path=public
as $$
declare t public.payment_tasks%rowtype;
    allow_reschedule boolean;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    if p_new_due_date is null then raise exception 'DUE_DATE_REQUIRED'; end if;

    select * into t from public.payment_tasks where id=p_task_id for update;
    if t.id is null then raise exception 'TASK_NOT_FOUND'; end if;
    if t.status in ('completed','cancelled') then raise exception 'TASK_ALREADY_FINAL'; end if;

    select coalesce(manual_reschedule_enabled,false)
      into allow_reschedule
      from public.task_settings
     where provider_id=t.provider_id;

    if not coalesce(allow_reschedule,false) then raise exception 'RESCHEDULE_NOT_ALLOWED'; end if;

    update public.payment_tasks
       set previous_due_date=due_date,
           due_date=p_new_due_date,
           rescheduled_at=now(),
           rescheduled_by=auth.uid(),
           reschedule_reason=nullif(trim(coalesce(p_reason,'')),''),
           status='upcoming'
     where id=t.id;

    insert into public.audit_logs(actor_id,action_type,affected_record_id,affected_table,details)
    values(auth.uid(),'reschedule_payment_task',t.id,'payment_tasks',
           jsonb_build_object('old_due_date',t.due_date,'new_due_date',p_new_due_date,'reason',p_reason));
end;
$$;

-- =========================================================
-- 25. ADMIN: TASK SETTINGS
-- =========================================================

create or replace function public.admin_update_task_settings(
    p_provider_id uuid,
    p_first_task_enabled boolean,
    p_first_task_amount double precision,
    p_recurring_task_enabled boolean,
    p_recurring_task_amount double precision,
    p_repeat_interval_days integer,
    p_days_visible_before_due integer,
    p_manual_reschedule_enabled boolean
)
returns public.task_settings
language plpgsql security definer set search_path=public
as $$
declare s public.task_settings%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;
    if not exists(select 1 from public.telecom_providers where id=p_provider_id) then raise exception 'PROVIDER_NOT_FOUND'; end if;

    insert into public.task_settings(
        provider_id,first_task_enabled,first_task_amount,
        recurring_task_enabled,recurring_task_amount,repeat_interval_days,
        days_visible_before_due,manual_reschedule_enabled
    )
    values(
        p_provider_id,p_first_task_enabled,coalesce(p_first_task_amount,0),
        p_recurring_task_enabled,coalesce(p_recurring_task_amount,0),
        p_repeat_interval_days,p_days_visible_before_due,p_manual_reschedule_enabled
    )
    on conflict(provider_id) do update set
        first_task_enabled=excluded.first_task_enabled,
        first_task_amount=excluded.first_task_amount,
        recurring_task_enabled=excluded.recurring_task_enabled,
        recurring_task_amount=excluded.recurring_task_amount,
        repeat_interval_days=excluded.repeat_interval_days,
        days_visible_before_due=excluded.days_visible_before_due,
        manual_reschedule_enabled=excluded.manual_reschedule_enabled,
        updated_at=now()
    returning * into s;

    return s;
end;
$$;

-- =========================================================
-- 26. ADMIN: SYSTEM SETTINGS
-- =========================================================

create or replace function public.admin_update_system_settings(
    p_app_name text,
    p_contact_data jsonb,
    p_terms_and_conditions text,
    p_privacy_policy text,
    p_renewal_threshold_days integer
)
returns public.system_settings
language plpgsql security definer set search_path=public
as $$
declare s public.system_settings%rowtype;
begin
    if not public.is_manager_or_admin() then raise exception 'ADMIN_REQUIRED'; end if;

    update public.system_settings
       set app_name=p_app_name,
           contact_data=p_contact_data,
           terms_and_conditions=p_terms_and_conditions,
           privacy_policy=p_privacy_policy,
           renewal_threshold_days=p_renewal_threshold_days
     where id=true
     returning * into s;

    if s.id is null then
        insert into public.system_settings(
            id,app_name,contact_data,terms_and_conditions,privacy_policy,renewal_threshold_days
        )
        values(true,p_app_name,p_contact_data,p_terms_and_conditions,p_privacy_policy,p_renewal_threshold_days)
        returning * into s;
    end if;

    return s;
end;
$$;

-- =========================================================
-- 27. NOTIFICATION READ RPC
-- =========================================================

create or replace function public.mark_notification_read(
    p_notification_id uuid
)
returns void
language plpgsql security definer set search_path=public
as $$
begin
    if auth.uid() is null then raise exception 'AUTH_REQUIRED'; end if;

    update public.notifications
       set is_read=true, read_at=coalesce(read_at,now())
     where id=p_notification_id
       and (customer_id=auth.uid() or public.is_manager_or_admin());

    if not found then raise exception 'NOTIFICATION_NOT_FOUND_OR_FORBIDDEN'; end if;
end;
$$;

-- =========================================================
-- 28. FUNCTION PRIVILEGES
-- =========================================================

revoke all on function public.add_customer_number(text) from public;
grant execute on function public.add_customer_number(text) to authenticated;

revoke all on function public.get_provider_by_prefix(text) from public;
grant execute on function public.get_provider_by_prefix(text) to authenticated;

revoke all on function public.create_protection_request(uuid,uuid,uuid,jsonb) from public;
grant execute on function public.create_protection_request(uuid,uuid,uuid,jsonb) to authenticated;

revoke all on function public.approve_protection_request(uuid) from public;
grant execute on function public.approve_protection_request(uuid) to authenticated;

revoke all on function public.reject_protection_request(uuid,text) from public;
grant execute on function public.reject_protection_request(uuid,text) to authenticated;

revoke all on function public.admin_create_telecom_provider(text,text,integer,jsonb,boolean,boolean,integer) from public;
grant execute on function public.admin_create_telecom_provider(text,text,integer,jsonb,boolean,boolean,integer) to authenticated;

revoke all on function public.admin_update_telecom_provider(uuid,text,text,integer,jsonb,boolean,boolean,integer) from public;
grant execute on function public.admin_update_telecom_provider(uuid,text,text,integer,jsonb,boolean,boolean,integer) to authenticated;

revoke all on function public.admin_set_telecom_provider_status(uuid,boolean) from public;
grant execute on function public.admin_set_telecom_provider_status(uuid,boolean) to authenticated;

revoke all on function public.admin_create_protection_plan(uuid,text,double precision,integer,boolean,boolean) from public;
grant execute on function public.admin_create_protection_plan(uuid,text,double precision,integer,boolean,boolean) to authenticated;

revoke all on function public.admin_update_protection_plan(uuid,uuid,text,double precision,integer,boolean,boolean) from public;
grant execute on function public.admin_update_protection_plan(uuid,uuid,text,double precision,integer,boolean,boolean) to authenticated;

revoke all on function public.admin_set_protection_plan_status(uuid,boolean) from public;
grant execute on function public.admin_set_protection_plan_status(uuid,boolean) to authenticated;

revoke all on function public.admin_create_payment_method(text,text,text,text,boolean) from public;
grant execute on function public.admin_create_payment_method(text,text,text,text,boolean) to authenticated;

revoke all on function public.admin_update_payment_method(uuid,text,text,text,text,boolean) from public;
grant execute on function public.admin_update_payment_method(uuid,text,text,text,text,boolean) to authenticated;

revoke all on function public.admin_set_payment_method_status(uuid,boolean) from public;
grant execute on function public.admin_set_payment_method_status(uuid,boolean) to authenticated;

revoke all on function public.complete_payment_task(uuid) from public;
grant execute on function public.complete_payment_task(uuid) to authenticated;

revoke all on function public.cancel_payment_task(uuid,text) from public;
grant execute on function public.cancel_payment_task(uuid,text) to authenticated;

revoke all on function public.reschedule_payment_task(uuid,timestamptz,text) from public;
grant execute on function public.reschedule_payment_task(uuid,timestamptz,text) to authenticated;

revoke all on function public.admin_update_task_settings(uuid,boolean,double precision,boolean,double precision,integer,integer,boolean) from public;
grant execute on function public.admin_update_task_settings(uuid,boolean,double precision,boolean,double precision,integer,integer,boolean) to authenticated;

revoke all on function public.admin_update_system_settings(text,jsonb,text,text,integer) from public;
grant execute on function public.admin_update_system_settings(text,jsonb,text,text,integer) to authenticated;

revoke all on function public.mark_notification_read(uuid) from public;
grant execute on function public.mark_notification_read(uuid) to authenticated;

commit;
