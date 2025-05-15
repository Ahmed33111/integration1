package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import services.ServiceUser;
import utils.EmailSender;
import java.util.Random;
import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Label messageLabel;

    private final ServiceUser serviceUser;

    public ForgotPasswordController() {
        this.serviceUser = new ServiceUser();
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
        System.out.println("ForgotPasswordController initialized");
    }

    private String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "@#$%^&+=!";
        String allChars = upperCase + lowerCase + numbers + specialChars;
        
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one character from each category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // Add 4 more random characters
        for (int i = 0; i < 4; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }

    @FXML
    private void handleResetPassword() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse email");
            return;
        }

        try {
            if (!serviceUser.emailExists(email)) {
                showError("Aucun compte n'est associé à cette adresse email");
                return;
            }

            // Générer un nouveau mot de passe aléatoire
            String newPassword = generateRandomPassword();

            // Mettre à jour le mot de passe dans la base de données
            boolean updated = serviceUser.updatePassword(email, newPassword);
            if (!updated) {
                showError("Erreur lors de la mise à jour du mot de passe");
                return;
            }

            // Utiliser le template spécifique pour la réinitialisation de mot de passe
            String subject = "Réinitialisation de votre mot de passe";
            String message = EmailSender.generatePasswordResetTemplate(newPassword);

            // Envoyer l'email
            EmailSender.sendEmail(email, subject, message);
            showSuccess("Un nouveau mot de passe a été envoyé à votre adresse email");

            // Retourner à la page de connexion après 2 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> handleBack());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            showError("Une erreur est survenue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            emailField.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur lors du retour à la page de connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(message);
        System.err.println("Error: " + message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText(message);
        System.out.println("Success: " + message);
    }
}