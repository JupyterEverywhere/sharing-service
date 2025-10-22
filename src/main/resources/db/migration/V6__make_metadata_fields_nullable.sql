-- Make language, kernel_name, and kernel_display_name nullable to align with Jupyter notebook format
-- All metadata fields are optional according to the nbformat specification

-- Update existing empty string values to NULL for consistency
UPDATE jupyter_notebooks_metadata
SET language = NULL
WHERE language = '';

UPDATE jupyter_notebooks_metadata
SET kernel_name = NULL
WHERE kernel_name = '';

UPDATE jupyter_notebooks_metadata
SET kernel_display_name = NULL
WHERE kernel_display_name = '';

-- Alter columns to allow NULL values
ALTER TABLE jupyter_notebooks_metadata
ALTER COLUMN language DROP NOT NULL;

ALTER TABLE jupyter_notebooks_metadata
ALTER COLUMN kernel_name DROP NOT NULL;

ALTER TABLE jupyter_notebooks_metadata
ALTER COLUMN kernel_display_name DROP NOT NULL;
