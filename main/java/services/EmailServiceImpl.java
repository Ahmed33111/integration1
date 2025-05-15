package services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailServiceImpl implements EmailService {
    private static final String EMAIL = "luminaapp29@gmail.com";
    private static final String PASSWORD = "mhye ihey sqrb zysc";
    private static final String HOST = "smtp.gmail.com";
    private static final String PORT = "587";

    @Override
    public String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Override
    public void sendEmail(String toEmail, String subject, String messageBody) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }
    
    // Méthode spécifique pour l'envoi d'OTP
    public void sendOTPEmail(String toEmail, String otp) throws Exception {
        String subject = "Code de réinitialisation de mot de passe";
        String message = "Votre code de réinitialisation de mot de passe est : " + otp;
        sendEmail(toEmail, subject, message);
    }
}
