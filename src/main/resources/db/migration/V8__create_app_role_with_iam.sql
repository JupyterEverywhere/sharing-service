-- V8: Create app_user role with IAM authentication and least-privilege DML access
-- This migration is idempotent and safe on both RDS and local Docker PostgreSQL.

-- 1. Idempotent role creation (PostgreSQL < 16 lacks CREATE ROLE IF NOT EXISTS)
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
    CREATE ROLE app_user WITH LOGIN;
  END IF;
END $$;

-- 2. Grant IAM auth capability (silently skipped on non-RDS PostgreSQL where rds_iam doesn't exist)
DO $$ BEGIN
  GRANT rds_iam TO app_user;
EXCEPTION
  WHEN undefined_object THEN NULL;
END $$;

-- 3. Grant access on existing objects (idempotent)
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- 4. Auto-grant on future objects created by the current user (the Flyway admin role)
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO app_user;
