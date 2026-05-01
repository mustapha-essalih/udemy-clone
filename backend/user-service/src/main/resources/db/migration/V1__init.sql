CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE,
    full_name VARCHAR(255),
    bio TEXT,
    avatar_url TEXT
);

CREATE TABLE instructor_profiles (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE,
    payout_account_id VARCHAR(255)
);
