INSERT INTO users (id, username, email, password_hash, status, created_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@lms-platform.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
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
