-- Fix the trigger to only assign readable_id when it's NULL
--
CREATE OR REPLACE FUNCTION set_jupyter_notebooks_metadata_readable_id() RETURNS TRIGGER
    LANGUAGE plpgsql AS $$
DECLARE
    v_readable_id VARCHAR(128);
BEGIN
    -- Only assign a readable_id if one wasn't already provided
    IF NEW.readable_id IS NULL THEN
        SELECT r.readable_id INTO v_readable_id
        FROM jupyter_notebooks_metadata_readable_ids r
        WHERE r.is_taken IS FALSE
        LIMIT 1;

        UPDATE jupyter_notebooks_metadata_readable_ids
        SET is_taken = TRUE
        WHERE readable_id = v_readable_id;

        NEW.readable_id = v_readable_id;
    END IF;

    RETURN NEW;
END $$;
