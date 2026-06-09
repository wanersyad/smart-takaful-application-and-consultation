-- Enable RLS for tables created by the dynamic Takaful application system.
-- The Spring Boot app connects with database credentials and enforces app-level authorization.
-- Direct Supabase Data API access is not used by the browser, so anon/authenticated
-- grants are revoked and RLS is enabled as defense in depth.

revoke select, insert, update, delete on all tables in schema public from anon, authenticated;
revoke usage, select on all sequences in schema public from anon, authenticated;

alter default privileges for role postgres in schema public
    revoke select, insert, update, delete on tables from anon, authenticated;
alter default privileges for role postgres in schema public
    revoke usage, select on sequences from anon, authenticated;
alter default privileges for role postgres in schema public
    revoke execute on functions from anon, authenticated;

alter table if exists public.customers enable row level security;
alter table if exists public.customer_profiles enable row level security;
alter table if exists public.password_reset_tokens enable row level security;
alter table if exists public.products enable row level security;
alter table if exists public.product_benefits enable row level security;
alter table if exists public.product_coverage_items enable row level security;
alter table if exists public.product_requirements enable row level security;
alter table if exists public.product_documents enable row level security;
alter table if exists public.consultation_applications enable row level security;
alter table if exists public.application_nominees enable row level security;
alter table if exists public.stored_files enable row level security;
alter table if exists public.contact_inquiries enable row level security;
alter table if exists public.site_content_blocks enable row level security;
alter table if exists public.quotations enable row level security;
alter table if exists public.quotation_items enable row level security;
alter table if exists public.payments enable row level security;

drop policy if exists "deny direct anon customer access" on public.customers;
create policy "deny direct anon customer access" on public.customers for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon profile access" on public.customer_profiles;
create policy "deny direct anon profile access" on public.customer_profiles for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon password reset access" on public.password_reset_tokens;
create policy "deny direct anon password reset access" on public.password_reset_tokens for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon product access" on public.products;
create policy "deny direct anon product access" on public.products for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon product benefit access" on public.product_benefits;
create policy "deny direct anon product benefit access" on public.product_benefits for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon product coverage access" on public.product_coverage_items;
create policy "deny direct anon product coverage access" on public.product_coverage_items for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon product requirement access" on public.product_requirements;
create policy "deny direct anon product requirement access" on public.product_requirements for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon product document access" on public.product_documents;
create policy "deny direct anon product document access" on public.product_documents for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon application access" on public.consultation_applications;
create policy "deny direct anon application access" on public.consultation_applications for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon nominee access" on public.application_nominees;
create policy "deny direct anon nominee access" on public.application_nominees for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon file access" on public.stored_files;
create policy "deny direct anon file access" on public.stored_files for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon contact inquiry access" on public.contact_inquiries;
create policy "deny direct anon contact inquiry access" on public.contact_inquiries for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon site content access" on public.site_content_blocks;
create policy "deny direct anon site content access" on public.site_content_blocks for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon quotation access" on public.quotations;
create policy "deny direct anon quotation access" on public.quotations for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon quotation item access" on public.quotation_items;
create policy "deny direct anon quotation item access" on public.quotation_items for all to anon, authenticated using (false) with check (false);

drop policy if exists "deny direct anon payment access" on public.payments;
create policy "deny direct anon payment access" on public.payments for all to anon, authenticated using (false) with check (false);

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'takaful-private',
    'takaful-private',
    false,
    10485760,
    array['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
)
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;
