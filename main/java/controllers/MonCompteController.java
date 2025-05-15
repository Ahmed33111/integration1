package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.ServiceUser;
import entities.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class MonCompteController {
    @FXML
    private TextField nomField;
    
    @FXML
    private TextField prenomField;
    
    @FXML
    private Label emailLabel;
    
    @FXML
    private PasswordField oldPasswordField;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label messageLabel;
    
    private ServiceUser serviceUser;
    private User currentUser;
    
    @FXML
    private void initialize() {
        serviceUser = new ServiceUser();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadUserData();
    }
    
    private void loadUserData() {
        if (currentUser != null) {
            nomField.setText(currentUser.getNom());
            prenomField.setText(currentUser.getPrenom());
            emailLabel.setText(currentUser.getEmail());
        }
    }
    
    @FXML
    private void handleSave() {
        if (currentUser == null) {
            showError("Erreur: Utilisateur non connecté");
            return;
        }
        
        // Validate fields
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (nom.isEmpty() || prenom.isEmpty()) {
            showError("Les champs nom et prénom sont obligatoires");
            return;
        }
        
        try {
            // Update name and surname
            currentUser.setNom(nom);
            currentUser.setPrenom(prenom);
            
            // Check if password change is requested
            if (!oldPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty()) {
                String storedPassword = currentUser.getPassword();
                boolean isOldPasswordCorrect = false;

                // Try BCrypt first
                try {
                    isOldPasswordCorrect = BCrypt.checkpw(oldPassword, storedPassword);
                } catch (IllegalArgumentException e) {
                    // If BCrypt fails, it might be a plain text password
                    isOldPasswordCorrect = oldPassword.equals(storedPassword);
                }

                if (!isOldPasswordCorrect) {
                    showError("L'ancien mot de passe est incorrect");
                    return;
                }
                
                if (newPassword.isEmpty()) {
                    showError("Le nouveau mot de passe ne peut pas être vide");
                    return;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    showError("Les nouveaux mots de passe ne correspondent pas");
                    return;
                }
                
                // Store the new password (it will be hashed in ServiceUser.modifier)
                currentUser.setPassword(newPassword);
            }
            
            // Save changes to database
            serviceUser.modifier(currentUser);
            showSuccess("Modifications enregistrées avec succès");
            
            // Clear password fields
            oldPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            
        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set up the user
            ReclamationController reclamationController = loader.getController();
            reclamationController.setCurrentUser(currentUser);
            
            // Switch to the reclamations scene
            Scene scene = nomField.getScene();
            Stage stage = (Stage) scene.getWindow();
            
            scene.setRoot(root);
            stage.setTitle("Application d'Agence Urbaine - Réclamations");
        } catch (Exception e) {
            showError("Erreur lors du retour à la page principale");
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #e53e3e;");
    }
    
    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #38a169;");
    }
} 