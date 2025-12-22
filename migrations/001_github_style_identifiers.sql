-- Migration: Change to GitHub-style repository identifiers
-- This migration removes separate owner and name columns and uses only identifier in format "username/repository"
-- Date: 2025-12-21

-- Note: This migration assumes existing data follows the pattern where identifier already contains "owner/name" format
-- If not, you may need to update existing identifiers first before dropping columns

-- Drop the separate owner and name columns
ALTER TABLE repositories DROP COLUMN IF EXISTS owner;
ALTER TABLE repositories DROP COLUMN IF EXISTS name;

-- The identifier column remains and should be in format "username/repository"
-- Add a constraint to validate the format if desired (PostgreSQL)
-- ALTER TABLE repositories ADD CONSTRAINT identifier_format_check
--   CHECK (identifier ~ '^[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+$');

-- Note: Hibernate with ddl-auto=update will handle this automatically, but this script
-- can be used for manual migrations or rollbacks if needed.

