-- Add a new column 'password' to the 'jupyter_notebook_metadata' table
ALTER TABLE jupyter_notebooks_metadata
    ADD COLUMN password VARCHAR(255);
