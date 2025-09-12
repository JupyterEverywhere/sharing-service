-- Make language_version and file_extension nullable to align with Jupyter notebook format 4.5
-- which only requires 'name' in the language_info metadata

-- Update existing empty string values to NULL for consistency
UPDATE jupyter_notebooks_metadata
SET language_version = NULL
WHERE language_version = '';

UPDATE jupyter_notebooks_metadata
SET file_extension = NULL
WHERE file_extension = '';

-- Alter columns to allow NULL values
ALTER TABLE jupyter_notebooks_metadata
ALTER COLUMN language_version DROP NOT NULL;

ALTER TABLE jupyter_notebooks_metadata
ALTER COLUMN file_extension DROP NOT NULL;
