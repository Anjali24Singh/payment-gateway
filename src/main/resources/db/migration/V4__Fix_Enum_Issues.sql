-- Fix subscription status enum comparison issues by adding explicit cast functions
CREATE OR REPLACE FUNCTION subscription_status_cast(text) RETURNS subscription_status AS $$
BEGIN
    RETURN $1::subscription_status;
EXCEPTION
    WHEN invalid_text_representation THEN
        RETURN 'PENDING'::subscription_status;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
