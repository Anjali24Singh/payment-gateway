-- Add missing setup_fee column to subscription_plans table
ALTER TABLE subscription_plans ADD COLUMN setup_fee DECIMAL(12,2) DEFAULT 0.00;

-- Add constraint to ensure setup_fee is non-negative
ALTER TABLE subscription_plans ADD CONSTRAINT subscription_plans_setup_fee_non_negative CHECK (setup_fee >= 0);

-- Update existing records to have setup_fee = 0.00 if NULL
UPDATE subscription_plans SET setup_fee = 0.00 WHERE setup_fee IS NULL;
