-- Add Authorize.Net customer profile ID to customers table
-- This enables proper integration with Authorize.Net Customer Information Manager (CIM)

ALTER TABLE customers 
ADD COLUMN authorizenet_customer_profile_id VARCHAR(50);

-- Add index for faster lookups
CREATE INDEX idx_customers_authorizenet_profile_id 
ON customers(authorizenet_customer_profile_id);
