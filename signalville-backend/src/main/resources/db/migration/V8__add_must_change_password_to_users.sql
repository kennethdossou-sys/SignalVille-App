ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN users.must_change_password IS
    'Signal incitatif (non bloquant) affiche au frontend apres un mot de passe genere par un administrateur. Jamais impose cote backend.';