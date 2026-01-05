-- V6__Fix_Payment_Methods_Schema.sql
-- This migration adds missing columns to the payment_methods table
-- to match the PaymentMethod entity requirements

-- Add missing columns to payment_methods table
ALTER TABLE payment_methods 
ADD COLUMN IF NOT EXISTS card_number VARCHAR(255),
ADD COLUMN IF NOT EXISTS expiry_month VARCHAR(2),
ADD COLUMN IF NOT EXISTS expiry_year VARCHAR(4),
ADD COLUMN IF NOT EXISTS cvv VARCHAR(4);

-- Add comments to clarify column usage
COMMENT ON COLUMN payment_methods.card_number IS 'Encrypted/tokenized card number for processing';
COMMENT ON COLUMN payment_methods.expiry_month IS 'Card expiry month (string format)';
COMMENT ON COLUMN payment_methods.expiry_year IS 'Card expiry year (string format)';
COMMENT ON COLUMN payment_methods.cvv IS 'Encrypted CVV for processing';

-- Note: In production, these fields should be encrypted or tokenized
-- This is for development/testing purposes only

-- Update table comment
COMMENT ON TABLE payment_methods IS 'Tokenized payment methods with both tokenized data and processing fields';
