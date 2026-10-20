-- Promo codes are now applied at checkout: record which code and how much it took off each order.
ALTER TABLE orders ADD COLUMN promo_code varchar(50);
ALTER TABLE orders ADD COLUMN discount_amount float(53) NOT NULL DEFAULT 0;
