CREATE TABLE payments (
    id UUID PRIMARY KEY,
    user_id UUID,
    course_id UUID,
    amount NUMERIC,
    currency VARCHAR(10),
    status VARCHAR(20),
    stripe_payment_id VARCHAR(255) UNIQUE,
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    payment_id UUID,
    amount NUMERIC,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    instructor_id UUID,
    amount NUMERIC,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);
