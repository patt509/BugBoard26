-- Script di inizializzazione database BugBoard
-- Crea un utente admin di default con credenziali admin/admin

-- L'hash BCrypt della password "admin" (con salt rounds = 12)
-- Hash generato: $2a$12$LQv3c1yqBWVHxkd0LHAkCOeMt0VD0VD8kLMwpfXEzJxQzr7z6pJ6m

INSERT INTO users (email, username, password_hash, role, first_login, created_at)
VALUES (
    'admin@bugboard.com',
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOeMt0VD0VD8kLMwpfXEzJxQzr7z6pJ6m',
    'ADMIN',
    false,
    NOW()
)
ON CONFLICT (email) DO NOTHING;
