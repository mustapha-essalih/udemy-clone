INSERT INTO users (id, username, email, password_hash, status, created_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@lms-platform.com',
    '$2a$10$NSGNXbPj7rXIVVa37EXiaOhKtFBWUWHS2mkFWJo6I4IGwSPD46pRe', -- admin123
    'ACTIVE',
    NOW()
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@lms-platform.com'
  AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
