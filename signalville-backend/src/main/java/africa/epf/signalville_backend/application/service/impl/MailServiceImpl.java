package africa.epf.signalville_backend.application.service.impl;

import africa.epf.signalville_backend.application.service.MailService;
import africa.epf.signalville_backend.domain.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${signalville.mail.from}")
    private String from;

    @Value("${signalville.mail.from-name}")
    private String fromName;

    @Value("${signalville.mail.login-url}")
    private String loginUrl;

    @Override
    public EmailResult sendWelcomeInternalAccount(User user, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(from, fromName));
            helper.setTo(user.getEmail());
            helper.setSubject("Votre compte SignalVille a ete cree");
            helper.setText(buildWelcomeHtml(user, temporaryPassword), true);

            mailSender.send(message);
            log.info("Email de bienvenue envoye a {}", user.getEmail());
            return EmailResult.success();

        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            log.warn("Echec envoi email bienvenue a {} : {}", user.getEmail(), e.getMessage());
            return EmailResult.failure(e.getMessage());
        }
    }

    private String buildWelcomeHtml(User user, String temporaryPassword) {
        String roleLabel = switch (user.getRole()) {
            case AGENT -> "Agent";
            case SUPERVISEUR -> "Superviseur";
            case ADMINISTRATEUR -> "Administrateur";
            case CITOYEN -> "Citoyen";
        };

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <title>Bienvenue sur SignalVille</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f4f6;font-family:system-ui,-apple-system,sans-serif;color:#111827;">
                  <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="background:#f3f4f6;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="560" style="background:#fff;border-radius:14px;box-shadow:0 1px 3px rgba(0,0,0,0.06);overflow:hidden;">
                          <tr>
                            <td style="padding:32px 32px 16px;text-align:center;">
                              <div style="font-size:2.5rem;font-weight:800;letter-spacing:-0.04em;color:#1e3a8a;line-height:1;">SV</div>
                              <div style="font-size:0.9rem;color:#6b7280;margin-top:4px;">SignalVille</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 32px 24px;">
                              <h1 style="font-size:1.4rem;margin:0 0 16px;color:#111827;">Bonjour %s,</h1>
                              <p style="margin:0 0 16px;line-height:1.55;">
                                Un compte SignalVille a ete cree pour vous par un administrateur avec le role <strong>%s</strong>.
                              </p>
                              <p style="margin:0 0 24px;line-height:1.55;">
                                Voici vos identifiants de connexion :
                              </p>

                              <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;padding:16px;margin-bottom:24px;">
                                <tr>
                                  <td style="padding:8px 0;color:#6b7280;font-size:0.85rem;">Email</td>
                                  <td style="padding:8px 0;font-family:monospace;color:#111827;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:8px 0;color:#6b7280;font-size:0.85rem;">Mot de passe temporaire</td>
                                  <td style="padding:8px 0;font-family:monospace;color:#111827;font-weight:600;">%s</td>
                                </tr>
                              </table>

                              <p style="margin:0 0 24px;line-height:1.55;color:#92400e;background:#fffbeb;border-left:3px solid #f59e0b;padding:12px 16px;border-radius:6px;">
                                <strong>Important :</strong> ce mot de passe est temporaire. Vous devrez le changer lors de votre premiere connexion.
                              </p>

                              <div style="text-align:center;margin:32px 0 16px;">
                                <a href="%s" style="display:inline-block;background:#f97316;color:#fff;text-decoration:none;padding:12px 28px;border-radius:999px;font-weight:500;">
                                  Se connecter
                                </a>
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px 24px;text-align:center;color:#9ca3af;font-size:0.8rem;border-top:1px solid #f3f4f6;">
                              L'equipe SignalVille &middot; %s
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        escapeHtml(user.getFirstName()),
                        roleLabel,
                        escapeHtml(user.getEmail()),
                        escapeHtml(temporaryPassword),
                        loginUrl,
                        java.time.Year.now().getValue());
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}