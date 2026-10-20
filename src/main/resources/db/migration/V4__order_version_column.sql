-- Optimistic locking for orders (JPA @Version).
ALTER TABLE orders ADD COLUMN version bigint NOT NULL DEFAULT 0;
