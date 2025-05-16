--
-- drop readable ids table
--
DROP TABLE IF EXISTS jupyter_notebooks_metadata_readable_ids;

--
-- drop trigger for inserting readable id
--
DROP TRIGGER IF EXISTS set_jupyter_notebooks_metadata_readable_id_before_insert ON jupyter_notebooks_metadata;
DROP FUNCTION IF EXISTS set_jupyter_notebooks_metadata_readable_id;

--
-- drop metadata table
--
DROP TABLE IF EXISTS jupyter_notebooks_metadata;
DROP EXTENSION IF EXISTS "uuid-ossp";

-- DELETE FROM public.flyway_schema_history WHERE version = '1';
