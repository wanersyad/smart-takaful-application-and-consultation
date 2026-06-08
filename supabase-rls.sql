-- Enable RLS for tables created by the dynamic Takaful application system.
-- The Spring Boot app connects with database credentials and enforces app-level authorization.
-- Keep policies restrictive; direct anon/client access should not be used for these tables.

alter table if exists public.customers enable row level security;
alter table if exists public.customer_profiles enable row level security;
alter table if exists public.products enable row level security;
alter table if exists public.product_benefits enable row level security;
alter table if exists public.product_coverage_items enable row level security;
alter table if exists public.product_requirements enable row level security;
alter table if exists public.product_documents enable row level security;
alter table if exists public.consultation_applications enable row level security;
alter table if exists public.application_nominees enable row level security;
alter table if exists public.stored_files enable row level security;
alter table if exists public.quotations enable row level security;
alter table if exists public.quotation_items enable row level security;
alter table if exists public.payments enable row level security;

drop policy if exists "deny direct anon customer access" on public.customers;
create policy "deny direct anon customer access" on public.customers for all to anon using (false) with check (false);

drop policy if exists "deny direct anon profile access" on public.customer_profiles;
create policy "deny direct anon profile access" on public.customer_profiles for all to anon using (false) with check (false);

drop policy if exists "deny direct anon application access" on public.consultation_applications;
create policy "deny direct anon application access" on public.consultation_applications for all to anon using (false) with check (false);

drop policy if exists "deny direct anon file access" on public.stored_files;
create policy "deny direct anon file access" on public.stored_files for all to anon using (false) with check (false);

drop policy if exists "deny direct anon quotation access" on public.quotations;
create policy "deny direct anon quotation access" on public.quotations for all to anon using (false) with check (false);

drop policy if exists "deny direct anon payment access" on public.payments;
create policy "deny direct anon payment access" on public.payments for all to anon using (false) with check (false);
