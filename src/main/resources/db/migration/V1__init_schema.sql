-- EXTENSIONS
CREATE EXTENSION "uuid-ossp";

--
-- create metadata table
--
CREATE TABLE IF NOT EXISTS jupyter_notebooks_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    file_extension VARCHAR(255) NOT NULL DEFAULT '',
    kernel_display_name VARCHAR(255) NOT NULL DEFAULT '',
    kernel_name VARCHAR(255) NOT NULL DEFAULT '',
    language VARCHAR(255) NOT NULL DEFAULT '',
    language_version VARCHAR(255) NOT NULL DEFAULT '',
    session_id UUID NOT NULL DEFAULT uuid_generate_v4(),
    storage_url VARCHAR(255) NOT NULL DEFAULT '',
    domain VARCHAR(255) NOT NULL DEFAULT '',
    readable_id VARCHAR(128) UNIQUE DEFAULT ''
);

--
-- create readable ids table
--
CREATE TABLE IF NOT EXISTS jupyter_notebooks_metadata_readable_ids(
    readable_id VARCHAR(128) PRIMARY KEY,
    is_taken BOOLEAN NOT NULL DEFAULT FALSE
);

--
-- create trigger for inserting readable id
--
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

CREATE OR REPLACE TRIGGER set_jupyter_notebooks_metadata_readable_id_before_insert
    BEFORE INSERT ON jupyter_notebooks_metadata
    FOR EACH ROW
EXECUTE FUNCTION set_jupyter_notebooks_metadata_readable_id();
