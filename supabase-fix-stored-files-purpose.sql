-- Fix: the stored_files.purpose CHECK constraint was created by an earlier schema
-- version and does not include the product file purposes (PRODUCT_IMAGE,
-- PRODUCT_DOCUMENT) or SUPPORTING_DOCUMENT. Because Hibernate ddl-auto=update never
-- alters an existing CHECK constraint, uploading a product image/document fails with:
--   ERROR: new row for relation "stored_files" violates check constraint "stored_files_purpose_check"
--
-- Run this once in the Supabase SQL Editor. It drops the stale constraint and recreates
-- it allowing every value in the current FilePurpose enum.

alter table public.stored_files
    drop constraint if exists stored_files_purpose_check;

alter table public.stored_files
    add constraint stored_files_purpose_check
    check (purpose in (
        'PROFILE_PICTURE',
        'IC_FRONT',
        'IC_BACK',
        'SIGNATURE',
        'SUPPORTING_DOCUMENT',
        'PRODUCT_IMAGE',
        'PRODUCT_DOCUMENT'
    ));
