-- Categories d'incidents. Les champs suivent le schema CategoryRequest/CategoryResponse
-- du contrat OpenAPI (name, description, icon, defaultPriority, targetDelayHours, active).
CREATE TABLE categories (
    id                  UUID            PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    description         VARCHAR(500),
    icon                VARCHAR(50),
    default_priority    VARCHAR(20)     NOT NULL,
    target_delay_hours  INTEGER         NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT ck_categories_priority CHECK (default_priority IN ('BASSE', 'MOYENNE', 'HAUTE', 'CRITIQUE')),
    CONSTRAINT ck_categories_delay    CHECK (target_delay_hours >= 1)
);

CREATE UNIQUE INDEX ux_categories_name ON categories (lower(name));

-- Seed initial : 4 categories operationnelles.
INSERT INTO categories (id, name, description, icon, default_priority, target_delay_hours, active) VALUES
    (gen_random_uuid(), 'Voirie',            'Nids-de-poule, chaussee degradee, trottoirs endommages',   'road',      'HAUTE',   48,  TRUE),
    (gen_random_uuid(), 'Eclairage public',  'Lampadaires en panne, zones non eclairees',                'lightbulb', 'MOYENNE', 72,  TRUE),
    (gen_random_uuid(), 'Dechets',           'Depots sauvages, bacs debordants, collecte non effectuee',  'trash',     'MOYENNE', 24,  TRUE),
    (gen_random_uuid(), 'Eau et assainissement', 'Fuites, canalisations rompues, inondations',           'droplet',   'CRITIQUE', 12, TRUE);
