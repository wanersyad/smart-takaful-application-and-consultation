-- Dynamic Takaful Application System - Supabase hardening support
--
-- Run this in Supabase SQL Editor after Hibernate has created/updated the schema.
-- The Spring Boot app uses server-side database credentials and Spring Security,
-- so the public Supabase Data API should not expose direct customer/application rows.

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

alter table if exists public.quotations enable row level security;
alter table if exists public.quotation_items enable row level security;
alter table if exists public.payments enable row level security;

-- Keep the private file bucket private. The Java app uploads/downloads through
-- authenticated server routes using SUPABASE_SERVICE_ROLE_KEY when storage mode
-- is set to supabase.
insert into storage.buckets (id, name, public)
values ('takaful-private', 'takaful-private', false)
on conflict (id) do update set public = false;

-- Defense in depth: do not grant direct browser/API access to sensitive app data.
-- If the project exposes public schema through the Data API, RLS without policies
-- means anon/authenticated API clients cannot read or mutate rows directly.
revoke all on all tables in schema public from anon;
revoke all on all tables in schema public from authenticated;
revoke all on all sequences in schema public from anon;
revoke all on all sequences in schema public from authenticated;
