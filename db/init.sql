CREATE TABLE customers (
    id integer PRIMARY KEY,
    name text NOT NULL,
    country text NOT NULL
);

CREATE TABLE orders (
    id bigint PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers(id),
    order_date date NOT NULL,
    amount_cents integer NOT NULL,
    status text NOT NULL
);

INSERT INTO customers (id, name, country)
SELECT
    id,
    'Customer ' || id,
    (ARRAY['US', 'IN', 'GB', 'DE', 'AU', 'CA'])[1 + floor(random() * 6)::int]
FROM generate_series(1, 5000) AS id;

INSERT INTO orders (id, customer_id, order_date, amount_cents, status)
SELECT
    id,
    1 + floor(random() * 5000)::int,
    current_date - floor(random() * 730)::int,
    500 + floor(random() * 200000)::int,
    (ARRAY['pending', 'paid', 'shipped', 'cancelled'])[1 + floor(random() * 4)::int]
FROM generate_series(1, 300000) AS id;

ANALYZE customers;
ANALYZE orders;
