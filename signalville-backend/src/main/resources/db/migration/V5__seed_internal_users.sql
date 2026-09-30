-- Comptes internes de test.
-- Ces trois comptes permettent de démontrer la différenciation par rôle
-- dans les modules 2 et 3. Ils utilisent tous le mot de passe "Password123".
-- À supprimer ou modifier avant tout déploiement en production.

INSERT INTO users (
    id, first_name, last_name, email, phone, password_hash,
    role, status, created_at, updated_at
)
VALUES
    (
        gen_random_uuid(),
        'Admin',
        'Racine',
        'admin@signalville.local',
        '+221770000001',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ADMINISTRATEUR',
        'ACTIF',
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'Fatou',
        'Sy',
        'fatou.sy@signalville.local',
        '+221770000002',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'SUPERVISEUR',
        'ACTIF',
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'Moussa',
        'Diop',
        'moussa.diop@signalville.local',
        '+221770000003',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'AGENT',
        'ACTIF',
        now(),
        now()
    );