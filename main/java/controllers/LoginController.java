package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.UserService;
import entities.User;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import javafx.util.StringConverter;
import java.util.prefs.Preferences;

public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox loginForm;

    @FXML
    private VBox registerForm;

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField registerEmailField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private DatePicker dateNaissancePicker;

    @FXML
    private TextField phoneField;

    @FXML
    private Label formTitle;

    @FXML
    private ImageView formIcon;

    @FXML
    private CheckBox rememberMeCheckbox;

    private UserService userService;

    @FXML
    private void initialize() {
        userService = new UserService();
        messageLabel.setText("");

        // Load saved credentials if they exist
        loadSavedCredentials();

        // Configure DatePicker to display dates in dd/MM/yyyy format
        dateNaissancePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                }
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        return LocalDate.parse(string, dateFormatter);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            }
        });

        // Force DatePicker text field to update
        dateNaissancePicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                dateNaissancePicker.getEditor().setText(
                        newValue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
                dateNaissancePicker.getEditor().positionCaret(dateNaissancePicker.getEditor().getText().length()); // Ensure cursor is at end
                System.out.println("Date selected: " + newValue);
            } else {
                dateNaissancePicker.getEditor().setText("");
            }
        });

        // Ensure DatePicker is editable and shows popup
        dateNaissancePicker.setShowWeekNumbers(false);

        // Set initial form title
        formTitle.setText("Connexion");
    }

    private void loadSavedCredentials() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        String savedEmail = prefs.get("savedEmail", "");
        String savedPassword = prefs.get("savedPassword", "");
        boolean rememberMe = prefs.getBoolean("rememberMe", false);

        if (!savedEmail.isEmpty()) {
            emailField.setText(savedEmail);
        }
        if (!savedPassword.isEmpty() && rememberMe) {
            passwordField.setText(savedPassword);
            rememberMeCheckbox.setSelected(true);
        }
    }

    private void saveCredentials(String email, String password, boolean rememberMe) {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        if (rememberMe) {
            prefs.put("savedEmail", email);
            prefs.put("savedPassword", password);
            prefs.putBoolean("rememberMe", true);
        } else {
            prefs.remove("savedEmail");
            prefs.remove("savedPassword");
            prefs.putBoolean("rememberMe", false);
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheckbox.isSelected();

        System.out.println("Tentative de connexion avec l'email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Veuillez entrer une adresse email valide");
            return;
        }

        User user = userService.authenticate(email, password);
        if (user != null) {
            // Save credentials if "Remember Me" is checked
            saveCredentials(email, password, rememberMe);

            System.out.println("Utilisateur authentifié: " + user.getNom() + " " + user.getPrenom());
            try {
                System.out.println("Chargement du fichier Reclamation.fxml...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation.fxml"));
                System.out.println("FXMLLoader créé");

                Parent root = loader.load();
                System.out.println("FXML chargé avec succès");

                ReclamationController reclamationController = loader.getController();
                System.out.println("Contrôleur récupéré: " + reclamationController);

                reclamationController.setCurrentUser(user);
                System.out.println("Utilisateur défini dans le contrôleur");

                Scene scene = loginButton.getScene();
                Stage stage = (Stage) scene.getWindow();

                System.out.println("Changement de scène...");
                scene.setRoot(root);
                stage.setTitle("Application d'Agence Urbaine - Tableau de bord");
                System.out.println("Navigation réussie vers Reclamation.fxml");
            } catch (Exception e) {
                System.err.println("ERREUR lors de la navigation: " + e.getMessage());
                e.printStackTrace();
                showError("Erreur lors du chargement de la page principale: " + e.getMessage());
            }
        } else {
            System.out.println("Échec de l'authentification pour l'email: " + email);
            showError("Email ou mot de passe incorrect");
        }
    }

    @FXML
    private void handleRegister() {
        String nom = nomField.getText();
        String prenom = prenomField.getText();
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String phoneNumber = phoneField.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || dateNaissance == null) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.Period age = java.time.Period.between(dateNaissance, now);
        if (age.getYears() < 18) {
            showError("Vous devez avoir au moins 18 ans pour créer un compte");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Veuillez entrer une adresse email valide");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        if (userService.getUserByEmail(email) != null) {
            showError("Un compte existe déjà avec cet email");
            return;
        }

        User newUser = new User(nom, prenom, email, password, dateNaissance, phoneNumber);
        if (userService.createUser(newUser)) {
            try {
                User createdUser = userService.getUserByEmail(email);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation.fxml"));
                Parent root = loader.load();

                ReclamationController reclamationController = loader.getController();
                reclamationController.setCurrentUser(createdUser);

                Scene scene = registerForm.getScene();
                Stage stage = (Stage) scene.getWindow();

                scene.setRoot(root);
                stage.setTitle("Application d'Agence Urbaine - Tableau de bord");
            } catch (Exception e) {
                showError("Erreur lors du chargement de la page principale");
                e.printStackTrace();
            }
        } else {
            showError("Erreur lors de la création du compte");
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ForgotPassword.fxml"));
            Parent root = loader.load();
            Scene scene = emailField.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            showError("Erreur lors du chargement de la page de réinitialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showRegisterForm() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        messageLabel.setText("");
        formTitle.setText("Inscription");
        try {
            java.io.InputStream imageStream = getClass().getResourceAsStream("/images/signup.png");
            if (imageStream != null) {
                formIcon.setImage(new Image(imageStream));
            } else {
                System.err.println("Warning: Could not load signup.png image");
            }
        } catch (Exception e) {
            System.err.println("Error loading signup icon: " + e.getMessage());
        }
    }

    @FXML
    private void showLoginForm() {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        messageLabel.setText("");
        formTitle.setText("Connexion");
        try {
            java.io.InputStream imageStream = getClass().getResourceAsStream("/images/login.png");
            if (imageStream != null) {
                formIcon.setImage(new Image(imageStream));
            } else {
                System.err.println("Warning: Could not load login.png image");
            }
        } catch (Exception e) {
            System.err.println("Error loading login icon: " + e.getMessage());
        }
    }

    private void clearRegistrationFields() {
        nomField.clear();
        prenomField.clear();
        registerEmailField.clear();
        registerPasswordField.clear();
        confirmPasswordField.clear();
        dateNaissancePicker.setValue(null);
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #e53e3e;");
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #38a169;");
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
}