-- Corrige le hash BCrypt errone du seed V5 : le hash original ne
-- correspondait a aucun mot de passe exploitable (verifie par test direct
-- BCrypt hors projet, aucune correspondance trouvee avec Password123 ni
-- variantes plausibles). V5 est immuable une fois appliquee (regle Flyway) :
-- on corrige donc par une nouvelle migration plutot que d'editer V5.
--
-- Nouveau hash valide pour le mot de passe Password123, genere avec
-- BCryptPasswordEncoder (cost 10), applique aux 3 comptes internes de test.

UPDATE users
SET password_hash = '$2b$10$lhzHj55k8dzyt6.rQ5jUD.mUVy0xbEINnLq3f5UXdJcmaIeJwSqj.'
WHERE email IN (
    'admin@signalville.local',
    'fatou.sy@signalville.local',
    'moussa.diop@signalville.local'
);