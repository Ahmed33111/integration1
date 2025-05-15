package controllers;

import entities.Reclamation;
import entities.User;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import services.ServiceReclamations;
import services.ServiceUser;
import utils.EmailSender;
import javax.mail.MessagingException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import utils.BadWordFilter;

public class ReclamationController {
    @FXML private HBox menuBar;
    @FXML private Button logementBtn, evenementBtn, postBtn, reclamationsButton, monCompteBtn, logoutBtn;
    @FXML private TableView<Reclamation> reclamationsTable;
    @FXML private TableColumn<Reclamation, String> subjectColumn, etatColumn, citoyenColumn;
    @FXML private TableColumn<Reclamation, LocalDate> dateColumn;
    @FXML private TableColumn<Reclamation, Void> actionColumn;
    @FXML private VBox formBox;
    @FXML private TextField subjectField;
    @FXML private TextArea descriptionArea;
    @FXML private Button submitButton, updateButton;
    @FXML private Label messageLabel;
    @FXML private VBox reclamationSection;
    @FXML private VBox mainContent;
    @FXML private Label userNameLabel;
    @FXML private ComboBox<String> etatFilterCombo;

    private ServiceReclamations serviceReclamations;
    private ServiceUser serviceUser;
    private ObservableList<Reclamation> reclamationsList;
    private User currentUser;
    private BadWordFilter badWordFilter = BadWordFilter.getInstance();

    private void loadFXML(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null && currentUser != null) {
                try {
                    Method setUserMethod = controller.getClass().getMethod("setCurrentUser", User.class);
                    setUserMethod.invoke(controller, currentUser);
                } catch (NoSuchMethodException e) {
                    System.out.println("Methode setCurrentUser non trouvee dans le controleur de " + fxmlFile);
                }
            }

            Stage stage = (Stage) (submitButton != null && submitButton.getScene() != null ?
                    submitButton.getScene().getWindow() : menuBar.getScene().getWindow());
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", "Impossible de charger " + fxmlFile);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur inattendue", e.getMessage());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            System.out.println("Utilisateur défini dans ReclamationController: " + user.getNom() + " " + user.getPrenom());
            if (userNameLabel != null) {
                userNameLabel.setText(user.getNom() + " " + user.getPrenom());
            }
            if (formBox != null) {
                boolean isEmployee = user.getRole() != null &&
                        (user.getRole().equalsIgnoreCase("employé") || user.getRole().equalsIgnoreCase("employe") ||
                                user.getRole().equalsIgnoreCase("citoyen"));
                formBox.setVisible(isEmployee);
                formBox.setManaged(isEmployee);
            }
            loadReclamations();
        } else {
            System.out.println("Aucun utilisateur connecté");
            if (formBox != null) {
                formBox.setVisible(false);
                formBox.setManaged(false);
            }
        }
    }

    private void setupTextFilters() {
        if (subjectField != null) {
            subjectField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Platform.runLater(() -> {
                        String filtered = badWordFilter.filterText(newValue);
                        if (!filtered.equals(newValue)) {
                            int caretPosition = subjectField.getCaretPosition();
                            subjectField.setText(filtered);
                            subjectField.positionCaret(Math.min(caretPosition, filtered.length()));
                        }
                    });
                }
            });
        }

        if (descriptionArea != null) {
            descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Platform.runLater(() -> {
                        String filtered = badWordFilter.filterText(newValue);
                        if (!filtered.equals(newValue)) {
                            int caretPosition = descriptionArea.getCaretPosition();
                            descriptionArea.setText(filtered);
                            descriptionArea.positionCaret(Math.min(caretPosition, filtered.length()));
                        }
                    });
                }
            });
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    @FXML
    private void initialize() {
        setupTextFilters();
        System.out.println("Initialisation du contrôleur...");
        serviceUser = new ServiceUser();
        serviceReclamations = new ServiceReclamations();
        reclamationsList = FXCollections.observableArrayList();

        if (formBox != null) {
            formBox.setVisible(false);
            formBox.setManaged(false);
        }

        subjectColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitre()));
        subjectColumn.setPrefWidth(300);

        dateColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDateCreation()));
        dateColumn.setPrefWidth(120);

        etatColumn.setCellValueFactory(cellData -> {
            String etat = cellData.getValue().getEtat();
            if (etat == null) etat = "";
            return new SimpleStringProperty(etat);
        });

        citoyenColumn.setCellValueFactory(cellData -> {
            Reclamation r = cellData.getValue();
            try {
                User u = serviceUser.getUserById(r.getIdUser());
                return new SimpleStringProperty(u != null ? (u.getNom() + " " + u.getPrenom()) : "");
            } catch (SQLException e) {
                return new SimpleStringProperty("");
            }
        });

        actionColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null));
        actionColumn.setCellFactory(col -> new TableCell<Reclamation, Void>() {
            private final Button traiterBtn = new Button("Traiter");
            private final Button refuserBtn = new Button("Refuser");
            private final HBox box = new HBox(5, traiterBtn, refuserBtn);

            {
                traiterBtn.getStyleClass().add("button-primary");
                refuserBtn.getStyleClass().add("button-danger");
                box.setAlignment(Pos.CENTER);

                traiterBtn.setOnAction(e -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    try {
                        if (r.getEtat() != null && r.getEtat().equalsIgnoreCase("traitee")) {
                            showAlert(Alert.AlertType.INFORMATION, "Information",
                                    "Réclamation déjà traitée",
                                    "Cette réclamation a déjà été traitée et ne peut plus être modifiée.");
                            return;
                        }

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Confirmation");
                        alert.setHeaderText("Changer l'état de la réclamation");
                        alert.setContentText("Voulez-vous vraiment passer cette réclamation à 'traitee' ?");

                        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                            r.setEtat("traitee");
                            r.setDateTraitement(LocalDate.now());
                            serviceReclamations.modifier(r);

                            try {
                                User user = serviceUser.getUserById(r.getIdUser());
                                if (user != null && user.getEmail() != null) {
                                    sendReclamationStatusEmail(user.getEmail(), r, "traitée");
                                }
                            } catch (MessagingException ex) {
                                System.err.println("Erreur lors de l'envoi de l'email: " + ex.getMessage());
                            }

                            loadReclamations();
                            showAlert(Alert.AlertType.INFORMATION, "Succès",
                                    "Réclamation traitée",
                                    "La réclamation a été marquée comme traitée.");
                        }
                    } catch (SQLException ex) {
                        handleReclamationError(ex);
                    }
                });

                refuserBtn.setOnAction(e -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    try {
                        if (r.getEtat() != null && r.getEtat().equalsIgnoreCase("traitee")) {
                            showAlert(Alert.AlertType.INFORMATION, "Information",
                                    "Réclamation déjà traitée",
                                    "Cette réclamation a déjà été traitée et ne peut plus être modifiée.");
                            return;
                        }

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Confirmation");
                        alert.setHeaderText("Changer l'état de la réclamation");
                        alert.setContentText("Voulez-vous vraiment passer cette réclamation à 'refusee' ?");

                        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                            r.setEtat("refusee");
                            r.setDateTraitement(LocalDate.now());
                            serviceReclamations.modifier(r);

                            try {
                                User user = serviceUser.getUserById(r.getIdUser());
                                if (user != null && user.getEmail() != null) {
                                    sendReclamationStatusEmail(user.getEmail(), r, "refusée");
                                }
                            } catch (MessagingException ex) {
                                System.err.println("Erreur lors de l'envoi de l'email: " + ex.getMessage());
                            }

                            loadReclamations();
                            showAlert(Alert.AlertType.INFORMATION, "Succès",
                                    "Réclamation refusée",
                                    "La réclamation a été refusée.");
                        }
                    } catch (SQLException ex) {
                        handleReclamationError(ex);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    if (currentUser != null && currentUser.getRole() != null &&
                            (currentUser.getRole().equalsIgnoreCase("admin") ||
                                    currentUser.getRole().equalsIgnoreCase("employé") ||
                                    currentUser.getRole().equalsIgnoreCase("employe"))) {
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        dateColumn.setCellFactory(column -> new TableCell<Reclamation, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        reclamationsTable.setTableMenuButtonVisible(true);
        reclamationsTable.setItems(reclamationsList);

        loadReclamations();

        reclamationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                subjectField.setText(newSelection.getTitre());
                descriptionArea.setText(newSelection.getContenu());
            }
        });

        if (reclamationsButton != null) {
            reclamationsButton.setOnAction(e -> loadReclamations());
        }

        if (etatFilterCombo != null) {
            etatFilterCombo.getItems().addAll("Tous", "en_cours", "traitee", "refusee");
            etatFilterCombo.setValue("Tous");
            etatFilterCombo.setOnAction(e -> loadReclamations());
        }
    }

    @FXML
    private void handlePrivateMessage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MessagePrive.fxml"));
            Parent root = loader.load();
            MessagePriveController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            if (currentUser != null && currentUser.getRole() != null &&
                    currentUser.getRole().equalsIgnoreCase("admin")) {
                Reclamation selectedReclamation = reclamationsTable.getSelectionModel().getSelectedItem();
                if (selectedReclamation == null) {
                    showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Aucune réclamation sélectionnée",
                            "Veuillez sélectionner une réclamation pour démarrer une discussion privée.");
                    return;
                }

                User reclamationUser = serviceUser.getUserById(selectedReclamation.getIdUser());
                if (reclamationUser == null) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur introuvable",
                            "L'utilisateur associé à cette réclamation n'a pas été trouvé.");
                    return;
                }

                controller.setReclamation(selectedReclamation);
                controller.setRecipient(reclamationUser);
            }

            Stage stage = (Stage) reclamationsTable.getScene().getWindow();
            stage.setTitle("Messagerie privée");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                    "Impossible de charger la messagerie privée.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadReclamations() {
        try {
            System.out.println("Chargement des réclamations...");
            List<Reclamation> allReclamations = serviceReclamations.recuperer();
            System.out.println("Nombre total de réclamations récupérées: " + allReclamations.size());

            List<Reclamation> filteredReclamations = filterReclamationsByUserRole(allReclamations);
            System.out.println("Nombre de réclamations après filtrage par rôle: " + filteredReclamations.size());

            String etatFiltre = etatFilterCombo != null ? etatFilterCombo.getValue() : null;
            if (etatFiltre != null && !etatFiltre.equals("Tous")) {
                filteredReclamations = filteredReclamations.stream()
                        .filter(r -> etatFiltre.equalsIgnoreCase(r.getEtat()))
                        .collect(Collectors.toList());
                System.out.println("Nombre de réclamations après filtrage par état: " + filteredReclamations.size());
            }

            final List<Reclamation> finalFilteredReclamations = new ArrayList<>(filteredReclamations);
            Platform.runLater(() -> {
                try {
                    Reclamation selectedReclamation = reclamationsTable.getSelectionModel().getSelectedItem();
                    int selectedIndex = reclamationsTable.getSelectionModel().getSelectedIndex();

                    reclamationsList.clear();
                    reclamationsList.addAll(finalFilteredReclamations);

                    if (selectedReclamation != null) {
                        final int idSelection = selectedReclamation.getId();
                        finalFilteredReclamations.stream()
                                .filter(r -> r.getId() == idSelection)
                                .findFirst()
                                .ifPresentOrElse(
                                        r -> reclamationsTable.getSelectionModel().select(r),
                                        () -> {
                                            if (selectedIndex >= 0 && selectedIndex < finalFilteredReclamations.size()) {
                                                reclamationsTable.getSelectionModel().select(selectedIndex);
                                            }
                                        }
                                );
                    }

                    System.out.println("Mise à jour de l'interface utilisateur terminée");
                } catch (Exception e) {
                    System.err.println("Erreur lors de la mise à jour de l'interface utilisateur: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des réclamations: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                        "Une erreur est survenue lors du chargement des réclamations: " + e.getMessage());
            });
        }
    }

    private List<Reclamation> filterReclamationsByUserRole(List<Reclamation> allReclamations) {
        if (currentUser == null) {
            return new ArrayList<>();
        }

        switch (currentUser.getRole().toUpperCase()) {
            case "ADMIN":
                return allReclamations;
            case "EMPLOYÉ":
            case "EMPLOYE":
                return filterEmployeeReclamations(allReclamations);
            case "CITOYEN":
                return filterCitizenReclamations(allReclamations);
            default:
                return new ArrayList<>();
        }
    }

    private List<Reclamation> filterEmployeeReclamations(List<Reclamation> allReclamations) {
        return allReclamations.stream()
                .filter(r -> {
                    try {
                        if (r.getIdUser() == currentUser.getId()) {
                            return true;
                        }
                        User reclamationUser = serviceUser.getUserById(r.getIdUser());
                        return reclamationUser != null && reclamationUser.getRole().equalsIgnoreCase("CITOYEN");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<Reclamation> filterCitizenReclamations(List<Reclamation> allReclamations) {
        return allReclamations.stream()
                .filter(r -> r.getIdUser() == currentUser.getId())
                .collect(Collectors.toList());
    }

    private void clearFields() {
        if (subjectField != null) subjectField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        if (reclamationsTable != null) reclamationsTable.getSelectionModel().clearSelection();
        if (messageLabel != null) messageLabel.setText("");
    }

    private void sendReclamationStatusEmail(String userEmail, Reclamation reclamation, String statusText) throws MessagingException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateCreation = reclamation.getDateCreation().format(formatter);
        String dateTraitement = reclamation.getDateTraitement() != null ?
                reclamation.getDateTraitement().format(formatter) : "Non traitée";

        String infoBoxContent = ""
                + "<p><strong>Titre de la réclamation:</strong> " + reclamation.getTitre() + "</p>"
                + "<p><strong>Date de création:</strong> " + dateCreation + "</p>"
                + "<p><strong>Date de traitement:</strong> " + dateTraitement + "</p>"
                + "<p><strong>Statut:</strong> <span class='status'>" + reclamation.getEtat() + "</span></p>";

        String content = "Nous vous informons que votre réclamation a été " + statusText + ".";

        String htmlTemplate = EmailSender.generateTemplate(
                "Mise à jour de votre réclamation",
                content,
                infoBoxContent
        );

        EmailSender.sendEmail(
                userEmail,
                "Mise à jour de votre réclamation - " + reclamation.getTitre(),
                htmlTemplate
        );
    }

    private void handleReclamationError(Exception ex) {
        String errorMessage = "Une erreur est survenue lors de l'opération : " + ex.getMessage();
        System.err.println("ERREUR: " + errorMessage);
        ex.printStackTrace();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'opération");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Aucune réclamation sélectionnée", "Veuillez sélectionner une réclamation à supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer la réclamation");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette réclamation ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceReclamations.supprimer(selected);
                    reclamationsList.remove(selected);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation supprimée", "La réclamation a été supprimée avec succès.");
                } catch (SQLException e) {
                    handleReclamationError(e);
                }
            }
        });
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        Reclamation selectedReclamation = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Sélection vide", "Veuillez sélectionner une réclamation à modifier");
            return;
        }

        if (subjectField.getText().isEmpty() || descriptionArea.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Champs vides", "Veuillez remplir tous les champs");
            return;
        }

        try {
            selectedReclamation.setTitre(subjectField.getText());
            selectedReclamation.setContenu(descriptionArea.getText());

            serviceReclamations.modifier(selectedReclamation);
            loadReclamations();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Modification", "Réclamation modifiée avec succès");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", "Erreur lors de la modification de la réclamation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMonCompte(ActionEvent event) {
        loadFXML("/MonCompte.fxml", "Mon Compte");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Stage currentStage = (Stage) source.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Connexion");
            loginStage.show();

            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de déconnexion",
                    "Une erreur est survenue lors de la déconnexion.");
        }
    }

    @FXML
    private void handleLogement(ActionEvent event) {
        loadFXML("/Logement.fxml", "Gestion des logements");
    }

    @FXML
    private void handleEvenement(ActionEvent event) {
        loadFXML("/Evenement.fxml", "Gestion des événements");
    }

    @FXML
    private void handlePost(ActionEvent event) {
        loadFXML("/Post.fxml", "Gestion des posts");
    }

    public void reloadReclamations() {
        loadReclamations();
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (subjectField.getText().isEmpty() || descriptionArea.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Champs obligatoires manquants",
                    "Veuillez remplir tous les champs obligatoires.");
            return;
        }

        try {
            Reclamation reclamation = new Reclamation(
                    subjectField.getText(),
                    descriptionArea.getText(),
                    LocalDate.now(),
                    (currentUser != null) ? currentUser.getId() : 0,
                    "en_cours",
                    0,
                    null
            );

            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté",
                        "Vous devez être connecté pour soumettre une réclamation.");
                return;
            }

            serviceReclamations.ajouter(reclamation);
            loadReclamations();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation soumise",
                    "Votre réclamation a été enregistrée avec succès.");

        } catch (SQLException e) {
            handleReclamationError(e);
        }
    }
}