-- V7__Fix_Subscription_Metadata_Column.sql
-- This migration fixes the metadata column type in subscriptions table
-- to properly handle JSONB data from Hibernate

-- First, check if the metadata column exists and what type it is
DO $$
BEGIN
    -- The column might be TEXT instead of JSONB
    -- Let's convert it to proper JSONB type
    
    -- If the column has data, we need to preserve it during conversion
    -- For safety, we'll convert text to jsonb
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'subscriptions' 
        AND column_name = 'metadata' 
        AND data_type != 'jsonb'
    ) THEN
        -- Convert the column to JSONB type
        ALTER TABLE subscriptions 
        ALTER COLUMN metadata TYPE JSONB 
        USING CASE 
            WHEN metadata IS NULL THEN NULL
            WHEN metadata = '' THEN '{}'::jsonb
            ELSE metadata::jsonb
        END;
        
        RAISE NOTICE 'Successfully converted metadata column to JSONB type';
    ELSE
        RAISE NOTICE 'Metadata column is already JSONB or does not exist';
    END IF;
END $$;

-- Add a comment to clarify the column purpose
COMMENT ON COLUMN subscriptions.metadata IS 'JSON metadata for subscription with additional properties and configurations';
