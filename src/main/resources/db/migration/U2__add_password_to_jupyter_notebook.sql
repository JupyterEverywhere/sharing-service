-- Remove the column 'password' from the 'jupyter_notebook_metadata' table
ALTER TABLE jupyter_notebooks_metadata
DROP COLUMN IF EXISTS password;

-- DELETE FROM public.flyway_schema_history WHERE version = '2';
