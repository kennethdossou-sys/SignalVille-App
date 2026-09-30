package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.domain.model.User;

/**
 * Envoi d'emails transactionnels (bienvenue compte interne, etc.).
 *
 * Les methodes ne remontent aucune exception a l'appelant : un echec
 * d'envoi ne doit jamais faire echouer la transaction metier qui l'a
 * declenche (creation de compte, etc.). Le retour boolean/EmailResult
 * permet a l'appelant de communiquer le succes/echec dans sa reponse.
 */
public interface MailService {

    /**
     * Envoi de l'email de bienvenue a un compte interne fraichement cree
     * par un administrateur. Contient l'email + le mot de passe temporaire
     * en clair et un lien vers la page de connexion.
     */
    EmailResult sendWelcomeInternalAccount(User user, String temporaryPassword);

    record EmailResult(boolean sent, String errorMessage) {
        public static EmailResult success() {
            return new EmailResult(true, null);
        }

        public static EmailResult failure(String error) {
            return new EmailResult(false, error);
        }
    }
}