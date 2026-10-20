-- Indexes for the order queries used by jobs, webhooks and the admin dashboard.
CREATE INDEX idx_order_status ON orders (status);
CREATE INDEX idx_order_payout_status ON orders (payout_status);
CREATE INDEX idx_order_modempay_payment ON orders (modempay_payment_id);
CREATE INDEX idx_order_payout_transfer ON orders (payout_transfer_id);
CREATE INDEX idx_order_created_at ON orders (created_at);

-- One cart line per (user, product): merge existing duplicates into the oldest row first.
-- (Derived tables are used so MySQL allows reading the table being modified.)
UPDATE cart_items
SET quantity = (SELECT t.total FROM (
        SELECT user_id, product_id, SUM(quantity) AS total FROM cart_items GROUP BY user_id, product_id
    ) t WHERE t.user_id = cart_items.user_id AND t.product_id = cart_items.product_id)
WHERE id IN (SELECT k.keep_id FROM (
        SELECT MIN(id) AS keep_id FROM cart_items GROUP BY user_id, product_id HAVING COUNT(*) > 1
    ) k);
DELETE FROM cart_items
WHERE id NOT IN (SELECT k.keep_id FROM (
        SELECT MIN(id) AS keep_id FROM cart_items GROUP BY user_id, product_id
    ) k);
ALTER TABLE cart_items ADD CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id);

-- One review per (user, product): keep the earliest.
DELETE FROM reviews
WHERE id NOT IN (SELECT k.keep_id FROM (
        SELECT MIN(id) AS keep_id FROM reviews GROUP BY user_id, product_id
    ) k);
ALTER TABLE reviews ADD CONSTRAINT uk_review_user_product UNIQUE (user_id, product_id);
