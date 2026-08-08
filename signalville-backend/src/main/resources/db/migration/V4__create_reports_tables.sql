-- Module 1 : signalements, photos et historique de statut.

CREATE TABLE reports (
    id              UUID            PRIMARY KEY,
    reference       VARCHAR(20)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    description     VARCHAR(1000)   NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'NOUVEAU',
    priority        VARCHAR(20)     NOT NULL,
    category_id     UUID            NOT NULL,
    citizen_id      UUID            NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    address         VARCHAR(300)    NOT NULL,
    district        VARCHAR(120),
    municipality    VARCHAR(120),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_reports_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_reports_citizen  FOREIGN KEY (citizen_id)  REFERENCES users (id),
    CONSTRAINT ck_reports_status   CHECK (status IN ('NOUVEAU', 'AFFECTE', 'EN_COURS', 'RESOLU', 'CLOTURE', 'REOUVERT', 'REJETE', 'ANNULE')),
    CONSTRAINT ck_reports_priority CHECK (priority IN ('BASSE', 'MOYENNE', 'HAUTE', 'CRITIQUE')),
    CONSTRAINT ck_reports_lat      CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_reports_lng      CHECK (longitude BETWEEN -180 AND 180)
);

CREATE UNIQUE INDEX ux_reports_reference ON reports (reference);
CREATE INDEX ix_reports_citizen  ON reports (citizen_id);
CREATE INDEX ix_reports_status   ON reports (status);
CREATE INDEX ix_reports_category ON reports (category_id);

CREATE TABLE report_photos (
    id              UUID            PRIMARY KEY,
    report_id       UUID            NOT NULL,
    storage_path    VARCHAR(500)    NOT NULL,
    original_name   VARCHAR(255),
    content_type    VARCHAR(100)    NOT NULL,
    size_bytes      BIGINT          NOT NULL,
    description     VARCHAR(255),
    display_order   INTEGER         NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_report_photos_report FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE,
    CONSTRAINT ck_report_photos_order  CHECK (display_order BETWEEN 1 AND 3)
);

CREATE INDEX ix_report_photos_report ON report_photos (report_id);

CREATE TABLE report_status_history (
    id              UUID            PRIMARY KEY,
    report_id       UUID            NOT NULL,
    previous_status VARCHAR(20),
    new_status      VARCHAR(20)     NOT NULL,
    comment         VARCHAR(500),
    actor_id        UUID,
    changed_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_history_report FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_actor  FOREIGN KEY (actor_id)  REFERENCES users (id),
    CONSTRAINT ck_history_new_status      CHECK (new_status      IN ('NOUVEAU', 'AFFECTE', 'EN_COURS', 'RESOLU', 'CLOTURE', 'REOUVERT', 'REJETE', 'ANNULE')),
    CONSTRAINT ck_history_previous_status CHECK (previous_status IS NULL OR previous_status IN ('NOUVEAU', 'AFFECTE', 'EN_COURS', 'RESOLU', 'CLOTURE', 'REOUVERT', 'REJETE', 'ANNULE'))
);

CREATE INDEX ix_history_report ON report_status_history (report_id);

-- Compteur de reference SV-YYYY-NNNNNN, remis a zero chaque annee.
-- L'increment se fait par UPSERT atomique (INSERT ... ON CONFLICT DO UPDATE ... RETURNING),
-- ce qui verrouille la ligne de l'annee courante et evite toute collision concurrente.
CREATE TABLE report_reference_counters (
    year        INTEGER     PRIMARY KEY,
    last_value  BIGINT      NOT NULL
);
