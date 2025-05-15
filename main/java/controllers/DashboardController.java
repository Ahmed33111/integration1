package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private VBox sidebar;
    @FXML private Button dashboardButton;
    @FXML private Button housingButton;
    @FXML private Button eventsButton;
    @FXML private Button complaintsButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation du contrôleur
        setupButtonActions();
    }

    private void setupButtonActions() {
        // Configuration des actions des boutons
        dashboardButton.setOnAction(event -> showDashboard());
        housingButton.setOnAction(event -> showHousing());
        eventsButton.setOnAction(event -> showEvents());
        complaintsButton.setOnAction(event -> showComplaints());
        settingsButton.setOnAction(event -> showSettings());
        logoutButton.setOnAction(event -> logout());
    }

    @FXML
    private void showDashboard() {
        // Logique pour afficher le tableau de bord
        System.out.println("Affichage du tableau de bord");
        setActiveButton(dashboardButton);
    }

    @FXML
    private void showHousing() {
        // Logique pour afficher la section logement
        System.out.println("Affichage des logements");
        setActiveButton(housingButton);
    }

    @FXML
    private void showEvents() {
        // Logique pour afficher la section événements
        System.out.println("Affichage des événements");
        setActiveButton(eventsButton);
    }

    @FXML
    private void showComplaints() {
        // Logique pour afficher la section réclamations
        System.out.println("Affichage des réclamations");
        setActiveButton(complaintsButton);
    }

    @FXML
    private void showSettings() {
        // Logique pour afficher les paramètres
        System.out.println("Affichage des paramètres");
        setActiveButton(settingsButton);
    }

    @FXML
    private void logout() {
        // Logique de déconnexion
        System.out.println("Déconnexion de l'utilisateur");
        // Fermer la fenêtre actuelle
        Stage stage = (Stage) sidebar.getScene().getWindow();
        stage.close();
        
        // Afficher la fenêtre de connexion
        // Vous devrez implémenter cette partie selon votre logique d'application
    }

    @FXML
    private void handleNewAction() {
        // Action pour le bouton "Nouveau"
        showAlert("Nouvelle action", "Création d'un nouvel élément");
    }

    @FXML
    private void newHousingRequest() {
        // Logique pour une nouvelle demande de logement
        showAlert("Nouvelle demande", "Création d'une nouvelle demande de logement");
    }

    @FXML
    private void newEvent() {
        // Logique pour un nouvel événement
        showAlert("Nouvel événement", "Création d'un nouvel événement");
    }

    @FXML
    private void newComplaint() {
        // Logique pour une nouvelle réclamation
        showAlert("Nouvelle réclamation", "Création d'une nouvelle réclamation");
    }

    private void setActiveButton(Button activeButton) {
        // Réinitialiser le style de tous les boutons
        Button[] buttons = {dashboardButton, housingButton, eventsButton, complaintsButton, settingsButton};
        for (Button button : buttons) {
            if (button != null) {
                button.getStyleClass().remove("active");
            }
        }
        
        // Définir le bouton actif
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
