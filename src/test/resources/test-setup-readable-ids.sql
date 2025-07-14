-- Create the readable_ids table
CREATE TABLE IF NOT EXISTS jupyter_notebooks_metadata_readable_ids(
    readable_id VARCHAR(128) PRIMARY KEY,
    is_taken BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create the trigger function
CREATE OR REPLACE FUNCTION set_jupyter_notebooks_metadata_readable_id() RETURNS TRIGGER
    LANGUAGE plpgsql AS $$
DECLARE
    v_readable_id VARCHAR(128);
BEGIN
    SELECT r.readable_id INTO v_readable_id
    FROM jupyter_notebooks_metadata_readable_ids r
    WHERE r.is_taken IS FALSE
    LIMIT 1;

    UPDATE jupyter_notebooks_metadata_readable_ids
    SET is_taken = TRUE
    WHERE readable_id = v_readable_id;

    NEW.readable_id = v_readable_id;
    RETURN NEW;
END $$;

-- Create the trigger
CREATE OR REPLACE TRIGGER set_jupyter_notebooks_metadata_readable_id_before_insert
    BEFORE INSERT ON jupyter_notebooks_metadata
    FOR EACH ROW
EXECUTE FUNCTION set_jupyter_notebooks_metadata_readable_id();

-- Insert a test readable ID
INSERT INTO jupyter_notebooks_metadata_readable_ids(readable_id, is_taken) VALUES ('test-readable-id-unique', false);
