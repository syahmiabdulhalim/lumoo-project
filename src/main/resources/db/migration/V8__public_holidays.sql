-- Extra non-working days for refund due dates (Koriteh, Tobaski, Maulud, Ashura move with the moon,
-- so the admin enters them once announced). Fixed-date holidays and Easter are built in.
alter table site_settings add column public_holidays TEXT;
