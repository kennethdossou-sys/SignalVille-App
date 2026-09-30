-- V6 : interventions, preuves de resolution, notes, notifications.

CREATE TABLE interventions (
    id                  UUID            PRIMARY KEY,
    report_id           UUID            NOT NULL,
    agent_id            UUID            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'AFFECTEE',
    instruction         VARCHAR(500),
    resolution_comment  VARCHAR(500),
    assigned_at         TIMESTAMP       NOT NULL DEFAULT now(),
    started_at          TIMESTAMP,
    resolved_at         TIMESTAMP,

    CONSTRAINT fk_interventions_report FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_interventions_agent  FOREIGN KEY (agent_id)  REFERENCES users (id),
    CONSTRAINT ck_interventions_status CHECK (status IN ('AFFECTEE', 'EN_COURS', 'RESOLUE', 'REAFFECTEE', 'INTERROMPUE'))
);

CREATE INDEX ix_interventions_report ON interventions (report_id);
CREATE INDEX ix_interventions_agent  ON interventions (agent_id);

-- Un seul et unique signalement peut avoir une intervention "vivante" a la fois
-- (AFFECTEE ou EN_COURS). Les statuts terminaux (RESOLUE, REAFFECTEE, INTERROMPUE)
-- ne sont pas concernes par cette contrainte : ils marquent la fin d'un cycle
-- d'intervention, une nouvelle peut alors etre creee pour le meme report_id.
CREATE UNIQUE INDEX ux_interventions_one_active_per_report
    ON interventions (report_id)
    WHERE status IN ('AFFECTEE', 'EN_COURS');

CREATE TABLE intervention_proofs (
    id              UUID            PRIMARY KEY,
    intervention_id UUID            NOT NULL,
    storage_path    VARCHAR(500)    NOT NULL,
    original_name   VARCHAR(255),
    content_type    VARCHAR(100)    NOT NULL,
    size_bytes      BIGINT          NOT NULL,
    description     VARCHAR(255),
    display_order   INTEGER         NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_intervention_proofs_intervention FOREIGN KEY (intervention_id) REFERENCES interventions (id) ON DELETE CASCADE,
    CONSTRAINT ck_intervention_proofs_order CHECK (display_order BETWEEN 1 AND 3)
);

CREATE INDEX ix_intervention_proofs_intervention ON intervention_proofs (intervention_id);

CREATE TABLE report_notes (
    id          UUID            PRIMARY KEY,
    report_id   UUID            NOT NULL,
    author_id   UUID            NOT NULL,
    content     VARCHAR(1000)   NOT NULL,
    type        VARCHAR(20)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_report_notes_report FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_notes_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT ck_report_notes_type   CHECK (type IN ('INTERNE', 'PUBLIC'))
);

CREATE INDEX ix_report_notes_report ON report_notes (report_id);

CREATE TABLE notifications (
    id              UUID            PRIMARY KEY,
    recipient_id    UUID            NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    message         VARCHAR(500)    NOT NULL,
    read            BOOLEAN         NOT NULL DEFAULT FALSE,
    link            VARCHAR(300),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_notifications_recipient ON notifications (recipient_id);
CREATE INDEX ix_notifications_recipient_unread ON notifications (recipient_id) WHERE read = FALSE;