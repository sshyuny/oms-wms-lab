create table product (
    id          bigint       not null auto_increment,
    sku_code    varchar(50)  not null,
    name        varchar(200) not null,
    price       bigint       not null,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    primary key (id),
    unique key uk_product_sku_code (sku_code)
);

create table warehouse (
    id          bigint       not null auto_increment,
    code        varchar(50)  not null,
    name        varchar(100) not null,
    status      varchar(20)  not null,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    primary key (id),
    unique key uk_warehouse_code (code)
);

insert into warehouse (code, name, status, created_at, updated_at)
values ('WH-01', '기본 물류센터', 'ACTIVE', now(6), now(6));

create table stock (
    id          bigint      not null auto_increment,
    product_id  bigint      not null,
    quantity    int         not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null,
    primary key (id),
    unique key uk_stock_product_id (product_id)
);

create table coupon (
    id                bigint      not null auto_increment,
    code              varchar(50) not null,
    discount_amount   bigint      not null,
    min_order_amount  bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    primary key (id),
    unique key uk_coupon_code (code)
);

create table orders (
    id                      bigint      not null auto_increment,
    customer_id             bigint      not null,
    status                  varchar(20) not null,
    total_product_amount    bigint      not null,
    shipping_fee            bigint      not null,
    coupon_code             varchar(50),
    coupon_discount_amount  bigint      not null,
    payment_amount          bigint      not null,
    ordered_at              datetime(6) not null,
    paid_at                 datetime(6),
    cancelled_at            datetime(6),
    created_at              datetime(6) not null,
    updated_at              datetime(6) not null,
    primary key (id),
    key idx_orders_customer_id (customer_id)
);

create table order_line (
    id            bigint       not null auto_increment,
    order_id      bigint       not null,
    product_id    bigint       not null,
    product_name  varchar(200) not null,
    unit_price    bigint       not null,
    quantity      int          not null,
    line_amount   bigint       not null,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    primary key (id),
    key idx_order_line_order_id (order_id)
);

create table payment (
    id                 bigint      not null auto_increment,
    order_id           bigint      not null,
    pg_transaction_id  varchar(64) not null,
    amount             bigint      not null,
    status             varchar(20) not null,
    approved_at        datetime(6) not null,
    cancelled_at       datetime(6),
    created_at         datetime(6) not null,
    updated_at         datetime(6) not null,
    primary key (id),
    unique key uk_payment_order_id (order_id)
);

create table shipment (
    id            bigint      not null auto_increment,
    order_id      bigint      not null,
    warehouse_id  bigint      not null,
    status        varchar(20) not null,
    shipped_at    datetime(6),
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    primary key (id),
    unique key uk_shipment_order_id (order_id)
);
