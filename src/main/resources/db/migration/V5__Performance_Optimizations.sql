-- V5__Performance_Optimizations.sql
-- Performance optimizations for subscription and customer queries

-- Add index on customer_reference for faster customer lookups
CREATE INDEX IF NOT EXISTS idx_customers_customer_reference 
ON customers(customer_reference);

-- Add GIN index on subscription metadata for faster JSON queries
CREATE INDEX IF NOT EXISTS idx_subscriptions_metadata_gin 
ON subscriptions USING GIN (metadata);

-- Add specific index for idempotency key in metadata
CREATE INDEX IF NOT EXISTS idx_subscriptions_idempotency 
ON subscriptions ((metadata->>'idempotencyKey')) 
WHERE metadata->>'idempotencyKey' IS NOT NULL;

-- Add composite index for subscription customer and status lookups
CREATE INDEX IF NOT EXISTS idx_subscriptions_customer_status 
ON subscriptions(customer_id, status);

-- Add index on subscription_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_subscriptions_subscription_id 
ON subscriptions(subscription_id);

-- Add index on payment_method lookups
CREATE INDEX IF NOT EXISTS idx_payment_methods_payment_token 
ON payment_methods(payment_token);

-- Add index on subscription plan code
CREATE INDEX IF NOT EXISTS idx_subscription_plans_plan_code 
ON subscription_plans(plan_code);

-- Add index on subscription plan status
CREATE INDEX IF NOT EXISTS idx_subscription_plans_active 
ON subscription_plans(is_active) 
WHERE is_active = true;

-- Add index for customer email lookups (case insensitive)
CREATE INDEX IF NOT EXISTS idx_customers_email_lower 
ON customers(LOWER(email));

-- Add index for subscription billing dates
CREATE INDEX IF NOT EXISTS idx_subscriptions_next_billing 
ON subscriptions(next_billing_date) 
WHERE status = 'ACTIVE';

-- Add index for subscription trial end dates
CREATE INDEX IF NOT EXISTS idx_subscriptions_trial_end 
ON subscriptions(trial_end) 
WHERE trial_end IS NOT NULL;

-- Analyze tables to update statistics
ANALYZE customers;
ANALYZE subscriptions;
ANALYZE subscription_plans;
ANALYZE payment_methods;
