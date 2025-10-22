--
-- Fix race condition in readable_id assignment
-- Problem: SELECT + UPDATE allows concurrent transactions to get same readable_id
-- Solution: Use SELECT FOR UPDATE SKIP LOCKED to lock row during transaction
--

-- Drop and recreate the trigger function with proper row locking
DROP FUNCTION IF EXISTS set_jupyter_notebooks_metadata_readable_id() CASCADE;

CREATE OR REPLACE FUNCTION set_jupyter_notebooks_metadata_readable_id() RETURNS TRIGGER
    LANGUAGE plpgsql AS $$
DECLARE
    v_readable_id VARCHAR(128);
    v_retry_count INTEGER := 0;
    v_max_retries INTEGER := 5;
BEGIN
    -- Only assign a readable_id if one wasn't already provided
    IF NEW.readable_id IS NULL OR NEW.readable_id = '' THEN
        -- Retry loop in case of concurrent conflicts
        WHILE v_retry_count < v_max_retries LOOP
            BEGIN
                -- SELECT FOR UPDATE locks the row, preventing concurrent access
                -- SKIP LOCKED ensures we don't wait for locked rows
                SELECT r.readable_id INTO v_readable_id
                FROM jupyter_notebooks_metadata_readable_ids r
                WHERE r.is_taken IS FALSE
                LIMIT 1
                FOR UPDATE SKIP LOCKED;

                -- Exit if no available IDs found
                IF v_readable_id IS NULL THEN
                    RAISE EXCEPTION 'No available readable IDs in pool';
                END IF;

                -- Atomically mark as taken (still within same transaction)
                UPDATE jupyter_notebooks_metadata_readable_ids
                SET is_taken = TRUE
                WHERE readable_id = v_readable_id;

                -- Success - assign to new row
                NEW.readable_id = v_readable_id;

                RETURN NEW;

            EXCEPTION
                WHEN OTHERS THEN
                    -- Log retry attempt (visible in PostgreSQL logs)
                    RAISE NOTICE 'Readable ID collision detected, retry % of %',
                        v_retry_count + 1, v_max_retries;
                    v_retry_count := v_retry_count + 1;

                    -- If max retries exceeded, re-raise the exception
                    IF v_retry_count >= v_max_retries THEN
                        RAISE EXCEPTION 'Failed to assign readable_id after % retries', v_max_retries;
                    END IF;
            END;
        END LOOP;
    END IF;

    RETURN NEW;
END $$;

-- Recreate the trigger (must be done after function is recreated)
DROP TRIGGER IF EXISTS set_jupyter_notebooks_metadata_readable_id_before_insert ON jupyter_notebooks_metadata;

CREATE TRIGGER set_jupyter_notebooks_metadata_readable_id_before_insert
    BEFORE INSERT ON jupyter_notebooks_metadata
    FOR EACH ROW
EXECUTE FUNCTION set_jupyter_notebooks_metadata_readable_id();

-- Create index on is_taken for faster lookups
CREATE INDEX IF NOT EXISTS idx_readable_ids_is_taken
ON jupyter_notebooks_metadata_readable_ids(is_taken)
WHERE is_taken IS FALSE;

-- Verify the fix: test that concurrent inserts work correctly
-- This comment documents the expected behavior for future developers
COMMENT ON FUNCTION set_jupyter_notebooks_metadata_readable_id() IS
'Assigns unique readable IDs from pool with row-level locking to prevent race conditions.
Uses FOR UPDATE SKIP LOCKED to avoid deadlocks during concurrent inserts.
Includes retry logic as defense-in-depth.';
