package utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {

    private static final String FROM_EMAIL = "dabyain@gmail.com";
    private static final String PASSWORD = "wdtg okzu jglc nkjn";

    public static void sendEmail(String toEmail, String subject, String messageBody) throws MessagingException {
        // Gmail properties configuration
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Create session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(messageBody, "text/html; charset=utf-8");

            // Send message
            Transport.send(message);

            System.out.println("Email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            throw e;
        }
    }

    private static final String CSS_STYLE = ""
            + "body { font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif; margin: 0; padding: 0; background-color: #f4f7fa; color: #333; line-height: 1.6; }"
            + ".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }"
            + ".header { background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%); padding: 30px; text-align: center; color: white; }"
            + ".header h2 { margin: 0; font-size: 24px; font-weight: 600; }"
            + ".content { padding: 30px; }"
            + ".greeting { font-size: 16px; margin-bottom: 20px; color: #1f2a44; }"
            + ".message { background-color: #f8fafc; padding: 20px; border-radius: 6px; margin-bottom: 20px; border-left: 4px solid #6366f1; }"
            + ".info-box { background-color: #eef2ff; padding: 20px; border-radius: 6px; margin-bottom: 20px; }"
            + ".info-box p { margin: 0; color: #4b5563; }"
            + ".status { color: #10b981; font-weight: 600; }"
            + ".footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; }"
            + ".footer a { color: #6366f1; text-decoration: none; }"
            + "@media screen and (max-width: 600px) { .container { margin: 10px; } .content { padding: 20px; } }";


    public static String generateTemplate(String title, String content, String infoBoxContent) {
        return ""
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>" + CSS_STYLE + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h2>" + title + "</h2>"
                + "</div>"
                + "<div class='content'>"
                + "<p class='greeting'>Dear Customer,</p>"
                + "<div class='message'>"
                + "<p>" + content + "</p>"
                + "</div>"
                + "<div class='info-box'>"
                + infoBoxContent
                + "</div>"
                + "<p>Thank you for your trust and understanding.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>This is an automated message, please do not reply directly.</p>"
                + "<p>For assistance, contact us at <a href='mailto:support@example.com'>support@example.com</a></p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    public static String generatePasswordResetTemplate(String newPassword) {
        return ""
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>" + CSS_STYLE + ""
                + ".password-box { background-color: #fef2f2; border: 2px dashed #ef4444; padding: 15px; margin: 20px 0; text-align: center; }"
                + ".warning { color: #dc2626; font-weight: 600; margin-top: 20px; }"
                + ".steps { background-color: #f0fdf4; padding: 20px; border-radius: 6px; margin: 20px 0; }"
                + ".steps ol { margin: 0; padding-left: 20px; }"
                + ".steps li { margin-bottom: 10px; color: #166534; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h2>Réinitialisation de Mot de Passe</h2>"
                + "</div>"
                + "<div class='content'>"
                + "<p class='greeting'>Bonjour,</p>"
                + "<div class='message'>"
                + "<p>Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte. Voici votre nouveau mot de passe temporaire :</p>"
                + "</div>"
                + "<div class='password-box'>"
                + "<h3>Votre nouveau mot de passe</h3>"
                + "<p style='font-size: 24px; letter-spacing: 2px; font-family: monospace;'>" + newPassword + "</p>"
                + "</div>"
                + "<div class='steps'>"
                + "<h4>Étapes à suivre :</h4>"
                + "<ol>"
                + "<li>Connectez-vous avec ce nouveau mot de passe</li>"
                + "<li>Pour votre sécurité, changez ce mot de passe dès votre première connexion</li>"
                + "<li>Assurez-vous de choisir un mot de passe fort et unique</li>"
                + "</ol>"
                + "</div>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>Ceci est un message automatique, merci de ne pas y répondre directement.</p>"
                + "<p>Si vous n'avez pas demandé cette réinitialisation, veuillez nous contacter immédiatement.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}