-- Migration V2: Additional indexes and database optimizations
-- Version: 2.0
-- Description: Add performance indexes and database optimizations

-- Create additional composite indexes for better query performance
-- Note: Using conditional creation to avoid conflicts

DO $$ 
BEGIN 
    -- Transaction indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_transactions_customer_status_created') THEN
        CREATE INDEX idx_transactions_customer_status_created 
        ON transactions(customer_id, status, created_at DESC);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_transactions_order_type_status') THEN
        CREATE INDEX idx_transactions_order_type_status 
        ON transactions(order_id, transaction_type, status);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_transactions_authnet_created') THEN
        CREATE INDEX idx_transactions_authnet_created 
        ON transactions(authnet_transaction_id, created_at DESC) 
        WHERE authnet_transaction_id IS NOT NULL;
    END IF;
    
    -- Order indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_orders_customer_payment_status_created') THEN
        CREATE INDEX idx_orders_customer_payment_status_created 
        ON orders(customer_id, payment_status, created_at DESC);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_orders_total_amount_created') THEN
        CREATE INDEX idx_orders_total_amount_created 
        ON orders(total_amount DESC, created_at DESC);
    END IF;
    
    -- Subscription indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_subscriptions_status_next_billing') THEN
        CREATE INDEX idx_subscriptions_status_next_billing 
        ON subscriptions(status, next_billing_date) 
        WHERE status = 'ACTIVE';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_subscriptions_trial_end') THEN
        CREATE INDEX idx_subscriptions_trial_end 
        ON subscriptions(trial_end) 
        WHERE trial_end IS NOT NULL AND status = 'ACTIVE';
    END IF;
    
    -- Payment method indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_payment_methods_customer_default_active') THEN
        CREATE INDEX idx_payment_methods_customer_default_active 
        ON payment_methods(customer_id, is_default, is_active);
    END IF;
    
    -- Webhook indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_webhooks_status_next_attempt') THEN
        CREATE INDEX idx_webhooks_status_next_attempt 
        ON webhooks(status, next_attempt_at) 
        WHERE status IN ('PENDING', 'RETRYING', 'FAILED');
    END IF;
    
    -- User/Customer indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_users_active_verified') THEN
        CREATE INDEX idx_users_active_verified 
        ON users(username, email) 
        WHERE is_active = true AND is_verified = true;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_customers_active_with_payment_methods') THEN
        CREATE INDEX idx_customers_active_with_payment_methods 
        ON customers(id, email) 
        WHERE is_active = true;
    END IF;
    
    RAISE NOTICE 'Indexes created successfully';
    
EXCEPTION 
    WHEN OTHERS THEN
        RAISE NOTICE 'Some indexes may already exist: %', SQLERRM;
END $$;

-- Comments for documentation
DO $$
BEGIN
    EXECUTE 'COMMENT ON INDEX idx_transactions_customer_status_created IS ''Optimizes customer transaction history queries''';
    EXECUTE 'COMMENT ON INDEX idx_subscriptions_status_next_billing IS ''Optimizes billing due date queries for active subscriptions''';
    EXECUTE 'COMMENT ON INDEX idx_webhooks_status_next_attempt IS ''Optimizes webhook retry processing queries''';
EXCEPTION 
    WHEN OTHERS THEN
        RAISE NOTICE 'Could not add comments to indexes: %', SQLERRM;
END $$;