DROP INDEX IF EXISTS idx_orders_seller;
ALTER TABLE orders DROP COLUMN seller_id;
