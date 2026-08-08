CREATE TABLE users (
    id              UUID            PRIMARY KEY,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    phone           VARCHAR(20)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIF',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMP,

    CONSTRAINT ck_users_role   CHECK (role IN ('CITOYEN', 'AGENT', 'SUPERVISEUR', 'ADMINISTRATEUR')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIF', 'SUSPENDU', 'DESACTIVE'))
);

CREATE UNIQUE INDEX ux_users_email ON users (lower(email));