package services;

public interface EmailService {
    void sendEmail(String toEmail, String subject, String messageBody) throws Exception;
    String generateOTP();
}
