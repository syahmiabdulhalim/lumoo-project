-- V1 baseline: the schema as Hibernate generated it (ddl-auto=update) before Flyway was introduced.
-- Generated from the JPA entities with org.hibernate.dialect.MySQLDialect on 2026-10-20.
--
-- Existing databases: spring.flyway.baseline-on-migrate=true marks this version as applied
-- WITHOUT running it, so prod data is untouched. Only a brand-new empty database executes it.
-- Never edit this file; add V2, V3, ... instead.

create table about_content (
    id bigint not null,
    founded_year varchar(255),
    hero_subtitle TEXT,
    hero_title varchar(255),
    mission_content TEXT,
    products_count varchar(255),
    story_content TEXT,
    team_size varchar(255),
    vision_content TEXT,
    primary key (id)
) engine=InnoDB;

create table audit_logs (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    ip_address varchar(45),
    action varchar(100) not null,
    entity_type varchar(100),
    user_agent varchar(500),
    actor_id varchar(255),
    after_state TEXT,
    before_state TEXT,
    entity_id varchar(255),
    primary key (id)
) engine=InnoDB;

create table blog_posts (
    published bit not null,
    reading_time_minutes integer,
    created_at datetime(6),
    id bigint not null auto_increment,
    published_at datetime(6),
    author varchar(255),
    category varchar(255),
    content LONGTEXT,
    excerpt TEXT,
    featured_image_url varchar(255),
    meta_description TEXT,
    meta_title varchar(255),
    slug varchar(255),
    tags varchar(255),
    title varchar(255),
    primary key (id)
) engine=InnoDB;

create table breach_incidents (
    affected_users integer not null,
    detected_at datetime(6),
    dpa_notified_at datetime(6),
    id bigint not null auto_increment,
    affected_data TEXT,
    description TEXT not null,
    status enum ('CONTAINED','DETECTED','RESOLVED'),
    primary key (id)
) engine=InnoDB;

create table career_applications (
    applied_at datetime(6),
    id bigint not null auto_increment,
    posting_id bigint,
    applicant_name varchar(255),
    cover_letter TEXT,
    email varchar(255),
    phone varchar(255),
    status enum ('NEW','REJECTED','REVIEWED','SHORTLISTED'),
    primary key (id)
) engine=InnoDB;

create table career_postings (
    active bit not null,
    created_at datetime(6),
    id bigint not null auto_increment,
    department varchar(255),
    description TEXT,
    location varchar(255),
    requirements TEXT,
    title varchar(255),
    type enum ('CONTRACT','FULL_TIME','INTERNSHIP','PART_TIME'),
    primary key (id)
) engine=InnoDB;

create table cart_items (
    price float(53) not null,
    quantity integer not null,
    id bigint not null auto_increment,
    product_id bigint,
    user_id bigint,
    name varchar(255),
    primary key (id)
) engine=InnoDB;

create table company_values (
    display_order integer not null,
    id bigint not null auto_increment,
    description TEXT,
    icon varchar(255),
    title varchar(255),
    primary key (id)
) engine=InnoDB;

create table data_access_requests (
    fulfilled_at datetime(6),
    id bigint not null auto_increment,
    requested_at datetime(6),
    reference_id varchar(36) not null,
    email varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table erasure_requests (
    orders_affected integer not null,
    id bigint not null auto_increment,
    processed_at datetime(6),
    requested_at datetime(6),
    reference_id varchar(36) not null,
    email varchar(255) not null,
    status enum ('COMPLETED','PENDING','PROCESSING'),
    primary key (id)
) engine=InnoDB;

create table inquiries (
    created_at datetime(6),
    id bigint not null auto_increment,
    product_id bigint,
    buyer_name varchar(255),
    email varchar(255),
    message TEXT,
    name varchar(255),
    subject varchar(255),
    primary key (id)
) engine=InnoDB;

create table notifications (
    is_read bit,
    created_at datetime(6),
    id bigint not null auto_increment,
    user_id bigint,
    message varchar(255),
    primary key (id)
) engine=InnoDB;

create table order_items (
    price float(53),
    quantity integer,
    id bigint not null auto_increment,
    order_id bigint,
    product_id bigint,
    product_name varchar(255),
    primary key (id)
) engine=InnoDB;

create table order_reports (
    resolved bit not null,
    created_at datetime(6),
    id bigint not null auto_increment,
    order_id bigint not null,
    user_id bigint not null,
    admin_note varchar(255),
    details TEXT,
    reason varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table orders (
    admin_commission float(53) not null,
    estimated_delivery_date date,
    marketing_consent bit not null,
    privacy_accepted bit not null,
    shipping_cost float(53),
    terms_accepted bit not null,
    total_amount float(53) not null,
    vendor_earnings float(53) not null,
    consent_timestamp datetime(6),
    created_at datetime(6),
    delivered_at datetime(6),
    id bigint not null auto_increment,
    order_date datetime(6),
    rider_id bigint,
    shipped_at datetime(6),
    user_id bigint,
    consent_ip varchar(45),
    address varchar(255),
    courier_name varchar(255),
    customer_name varchar(255),
    delivery_area varchar(255),
    modempay_payment_id varchar(255),
    payment_method varchar(255),
    payment_proof_url varchar(255),
    payout_status varchar(255),
    payout_transfer_id varchar(255),
    return_reason varchar(255),
    rider_name varchar(255),
    rider_phone varchar(255),
    shipping_tracking_url varchar(255),
    status varchar(255),
    tracking_number varchar(255),
    primary key (id)
) engine=InnoDB;

create table partnership_applications (
    id bigint not null auto_increment,
    submitted_at datetime(6),
    company_name varchar(255),
    contact_name varchar(255),
    email varchar(255),
    message TEXT,
    phone varchar(255),
    partner_type enum ('DISTRIBUTOR','LOGISTICS','OTHER','RESELLER','SUPPLIER'),
    status enum ('APPROVED','NEW','REJECTED','REVIEWED'),
    primary key (id)
) engine=InnoDB;

create table products (
    approved bit,
    image_approved bit,
    price float(53),
    stock int default 0 not null,
    weight_kg float(53),
    id bigint not null auto_increment,
    vendor_id bigint,
    category varchar(255),
    description TEXT,
    image_url varchar(255),
    name varchar(255),
    primary key (id)
) engine=InnoDB;

create table promo_codes (
    active bit not null,
    discount_value float(53) not null,
    minimum_order float(53) not null,
    usage_limit integer,
    used_count integer not null,
    created_at datetime(6),
    expires_at datetime(6),
    id bigint not null auto_increment,
    code varchar(50) not null,
    discount_type enum ('FIXED','PERCENTAGE'),
    primary key (id)
) engine=InnoDB;

create table reviews (
    rating integer not null,
    created_at datetime(6),
    id bigint not null auto_increment,
    product_id bigint,
    user_id bigint,
    comment TEXT,
    reviewer_name varchar(255),
    primary key (id)
) engine=InnoDB;

create table riders (
    active bit not null,
    total_deliveries integer not null,
    id bigint not null auto_increment,
    joined_at datetime(6),
    area varchar(255),
    name varchar(255) not null,
    phone varchar(255) not null,
    vehicle_type enum ('BICYCLE','MOTORCYCLE','TRUCK','VAN'),
    primary key (id)
) engine=InnoDB;

create table shipping_rates (
    active bit not null,
    base_rate_gmd float(53) not null,
    display_order integer not null,
    estimated_days_max integer not null,
    estimated_days_min integer not null,
    max_weight_kg float(53) not null,
    min_weight_kg float(53) not null,
    per_kg_rate_gmd float(53) not null,
    id bigint not null auto_increment,
    tracking_url_template varchar(500),
    courier_emoji varchar(255),
    courier_name varchar(255) not null,
    description varchar(255),
    coverage enum ('BOTH','INTERNATIONAL','NATIONAL'),
    primary key (id)
) engine=InnoDB;

create table site_settings (
    cookie_consent_enabled bit not null,
    data_retention_days integer not null,
    payout_hold_days integer not null,
    id bigint not null,
    afrimoney_name varchar(255),
    afrimoney_number varchar(255),
    bank_account_name varchar(255),
    bank_account_number varchar(255),
    bank_branch varchar(255),
    bank_name varchar(255),
    business_address varchar(255),
    business_email varchar(255),
    business_hours varchar(255),
    business_name varchar(255),
    business_phone varchar(255),
    business_reg_no varchar(255),
    business_whatsapp varchar(255),
    cookie_policy_text TEXT,
    copyright_text varchar(255),
    data_controller_email varchar(255),
    data_controller_name varchar(255),
    google_analytics_id varchar(255),
    pdpa_registration_number varchar(255),
    privacy_policy_text TEXT,
    qmoney_name varchar(255),
    qmoney_number varchar(255),
    returns_text TEXT,
    social_facebook varchar(255),
    social_instagram varchar(255),
    social_twitter varchar(255),
    terms_text TEXT,
    primary key (id)
) engine=InnoDB;

create table subscribers (
    active bit not null,
    id bigint not null auto_increment,
    subscribed_at datetime(6),
    email varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table team_members (
    active bit not null,
    display_order integer not null,
    id bigint not null auto_increment,
    bio TEXT,
    image_url varchar(255),
    name varchar(255),
    role varchar(255),
    primary key (id)
) engine=InnoDB;

create table users (
    email_verified bit not null,
    suspended bit not null,
    verified bit not null,
    id bigint not null auto_increment,
    token_expiry datetime(6),
    vendor_id bigint,
    address varchar(255),
    email varchar(255),
    email_verification_token varchar(255),
    full_name varchar(255),
    password varchar(255),
    payout_network varchar(255),
    payout_phone varchar(255),
    phone varchar(255),
    profile_picture varchar(255),
    reset_token varchar(255),
    username varchar(255),
    role enum ('ADMIN','USER','VENDOR'),
    primary key (id)
) engine=InnoDB;

create table vendor_applications (
    applied_at datetime(6),
    id bigint not null auto_increment,
    reviewed_at datetime(6),
    user_id bigint,
    bank_account_name varchar(255),
    bank_account_number varchar(255),
    bank_name varchar(255),
    business_address TEXT,
    business_name varchar(255),
    business_reg_doc_url varchar(255),
    business_reg_number varchar(255),
    business_region varchar(255),
    business_type varchar(255),
    estimated_monthly_turnover varchar(255),
    mobile_money_number varchar(255),
    national_id_doc_url varchar(255),
    owner_address TEXT,
    owner_full_name varchar(255),
    owner_id_number varchar(255),
    owner_id_type varchar(255),
    phone varchar(255),
    products_to_sell TEXT,
    reason TEXT,
    rejection_note TEXT,
    status varchar(255),
    tin_number varchar(255),
    years_in_business varchar(255),
    primary key (id)
) engine=InnoDB;

create table webhook_events (
    order_id bigint,
    processed_at datetime(6) not null,
    event_id varchar(100) not null,
    event_type varchar(100) not null,
    primary key (event_id)
) engine=InnoDB;

create index idx_audit_action 
   on audit_logs (action);

create index idx_audit_actor 
   on audit_logs (actor_id);

create index idx_audit_created 
   on audit_logs (created_at);

create index idx_blog_published_at 
   on blog_posts (published, published_at);

create index idx_blog_category 
   on blog_posts (category, published);

alter table blog_posts 
   add constraint UKfmrqlsu8hgt4xyp3ewt66h287 unique (slug);

create index idx_breach_detected 
   on breach_incidents (detected_at);

create index idx_dar_email 
   on data_access_requests (email);

alter table data_access_requests 
   add constraint UKbjw4o5t395tv8152sh5m34lkx unique (reference_id);

create index idx_erasure_email 
   on erasure_requests (email);

create index idx_erasure_status 
   on erasure_requests (status);

alter table erasure_requests 
   add constraint UK1v6o5f22d8uj0uw5oj7tdpfk6 unique (reference_id);

create index idx_order_user 
   on orders (user_id);

create index idx_product_approved 
   on products (approved);

create index idx_product_vendor 
   on products (vendor_id);

create index idx_product_category_approved 
   on products (category, approved);

alter table promo_codes 
   add constraint idx_promo_code unique (code);

alter table subscribers 
   add constraint UKe65wvbb19pmpo5f1u3w216i5j unique (email);

alter table users 
   add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

alter table career_applications 
   add constraint FK7l9nhqyisayohi463ljy84el3 
   foreign key (posting_id) 
   references career_postings (id);

alter table cart_items 
   add constraint FK1re40cjegsfvw58xrkdp6bac6 
   foreign key (product_id) 
   references products (id);

alter table cart_items 
   add constraint FK709eickf3kc0dujx3ub9i7btf 
   foreign key (user_id) 
   references users (id);

alter table inquiries 
   add constraint FKql8ttelqbdprtofuiosbjvbmu 
   foreign key (product_id) 
   references products (id);

alter table notifications 
   add constraint FK9y21adhxn0ayjhfocscqox7bh 
   foreign key (user_id) 
   references users (id);

alter table order_items 
   add constraint FKbioxgbv59vetrxe0ejfubep1w 
   foreign key (order_id) 
   references orders (id);

alter table order_items 
   add constraint FKocimc7dtr037rh4ls4l95nlfi 
   foreign key (product_id) 
   references products (id);

alter table order_reports 
   add constraint FKde9jtx94srcy7gqjkf14fchyq 
   foreign key (order_id) 
   references orders (id);

alter table order_reports 
   add constraint FKajfo2ti9o3xcpla01kc2m9w7u 
   foreign key (user_id) 
   references users (id);

alter table orders 
   add constraint FK32ql8ubntj5uh44ph9659tiih 
   foreign key (user_id) 
   references users (id);

alter table products 
   add constraint FKlag4lc2yiaaehog28yd6lsoao 
   foreign key (vendor_id) 
   references users (id);

alter table reviews 
   add constraint FKpl51cejpw4gy5swfar8br9ngi 
   foreign key (product_id) 
   references products (id);

alter table reviews 
   add constraint FKcgy7qjc1r99dp117y9en6lxye 
   foreign key (user_id) 
   references users (id);

alter table users 
   add constraint FK23l9q8qw51nsjft32s4qujd0p 
   foreign key (vendor_id) 
   references users (id);

alter table vendor_applications 
   add constraint FK5gop0nvhpi99cvrs4f60gc0co 
   foreign key (user_id) 
   references users (id);
