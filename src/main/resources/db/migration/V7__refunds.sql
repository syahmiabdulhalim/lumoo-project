-- Refunds: Lumoo pays the buyer back (manually, outside the system) and records it here. The vendor's
-- share is taken off the order's payout while it is still held, or recovered from the vendor's next
-- payouts once it has been paid (vendor_debt minus orders.payout_deduction = what the vendor still owes).
create table refunds (
    id bigint not null auto_increment,
    order_id bigint not null,
    vendor_id bigint,
    amount float(53) not null,
    shipping_amount float(53) not null default 0,
    vendor_deduction float(53) not null default 0,
    vendor_debt float(53) not null default 0,
    items_summary varchar(500),
    reason varchar(255) not null,
    status varchar(20) not null,
    method varchar(50),
    reference varchar(100),
    created_by varchar(255),
    created_at datetime(6) not null,
    due_by date,
    completed_by varchar(255),
    completed_at datetime(6),
    primary key (id),
    constraint fk_refund_order foreign key (order_id) references orders (id),
    constraint fk_refund_vendor foreign key (vendor_id) references users (id)
) engine=InnoDB;
create index idx_refund_status on refunds (status);
create index idx_refund_order on refunds (order_id);
create index idx_refund_vendor on refunds (vendor_id);

alter table orders add column refunded_amount float(53) not null default 0;
alter table orders add column shipping_refunded bit not null default 0;
-- Part of this order's payout kept back to cover the vendor's refund debt.
alter table orders add column payout_deduction float(53) not null default 0;
alter table order_items add column refunded_quantity integer not null default 0;
