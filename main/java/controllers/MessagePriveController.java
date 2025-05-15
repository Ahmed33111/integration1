package controllers;

import entities.User;
import entities.Message;
import entities.Reclamation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import services.ServiceMessages;
import services.ServiceUser;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MessagePriveController implements Initializable {
    @FXML private ListView<User> usersListView;
    @FXML private ListView<Message> messagesListView;
    @FXML private TextArea messageArea;
    @FXML private Button sendButton;
    @FXML private Button backButton;
    @FXML private Label recipientLabel;
    @FXML private Label reclamationLabel;

    private ServiceMessages messageService;
    private ServiceUser userService;
    private User currentUser;
    private User selectedRecipient;
    private Reclamation reclamation;
    private ScheduledExecutorService pollingService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageService = new ServiceMessages();
        userService = new ServiceUser();
        setupUI();
        setupCellFactories();
        startPolling();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            loadUsers();
        }
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        if (reclamationLabel != null) {
            reclamationLabel.setText(reclamation != null ? "Réclamation: " + reclamation.getTitre() : "Discussion générale");
        }
    }

    public void setRecipient(User recipient) {
        this.selectedRecipient = recipient;
        if (recipient != null) {
            usersListView.getSelectionModel().select(recipient);
            recipientLabel.setText("À: " + recipient.getNom() + " " + recipient.getPrenom());
            loadMessages(recipient.getId());
        }
    }

    private void setupCellFactories() {
        usersListView.setCellFactory(lv -> new ListCell<User>() {
            private final VBox vbox = new VBox(3);
            private final Label nameLabel = new Label();
            private final Label emailLabel = new Label();

            {
                vbox.getChildren().addAll(nameLabel, emailLabel);
                nameLabel.getStyleClass().add("user-name");
                emailLabel.getStyleClass().add("user-email");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setStyle("-fx-padding: 8px;");
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(user.getNom() + " " + user.getPrenom());
                    emailLabel.setText(user.getEmail());
                    setGraphic(vbox);
                }
            }
        });

        messagesListView.setCellFactory(lv -> new ListCell<Message>() {
            private final HBox hbox = new HBox(10);
            private final VBox verticalBox = new VBox(5); // Increased spacing
            private final Label senderLabel = new Label();
            private final Label contentLabel = new Label();
            private final Label timeLabel = new Label();

            {
                hbox.getStyleClass().add("message-container");
                verticalBox.getStyleClass().add("message-content");
                senderLabel.getStyleClass().add("message-sender");
                contentLabel.getStyleClass().add("message-bubble");
                timeLabel.getStyleClass().add("message-time");
                verticalBox.getChildren().addAll(senderLabel, timeLabel);
                hbox.getChildren().addAll(verticalBox, contentLabel); // Default order
                HBox.setHgrow(contentLabel, Priority.ALWAYS); // Ensure content expands
                contentLabel.setMaxWidth(Double.MAX_VALUE); // Allow full width
                contentLabel.setWrapText(true); // Wrap long text
                verticalBox.setPrefWidth(150); // Fixed width for sender/time
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setStyle("-fx-padding: 10px 15px;");
            }

            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setGraphic(null);
                } else {
                    senderLabel.setText(message.getSenderName());
                    contentLabel.setText(message.getContent());
                    timeLabel.setText(message.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    hbox.getChildren().clear(); // Reorder children based on sender
                    if (message.getIdSender() == currentUser.getId()) {
                        hbox.getChildren().addAll(contentLabel, verticalBox); // Content on left, sender/time on right
                        hbox.setAlignment(Pos.CENTER_RIGHT); // Sent messages on the right
                        contentLabel.getStyleClass().remove("received");
                        contentLabel.getStyleClass().add("sent");
                    } else {
                        hbox.getChildren().addAll(verticalBox, contentLabel); // Sender/time on left, content on right
                        hbox.setAlignment(Pos.CENTER_LEFT); // Received messages on the left
                        contentLabel.getStyleClass().remove("sent");
                        contentLabel.getStyleClass().add("received");
                        if (!message.isRead()) {
                            try {
                                messageService.markAsRead(message.getId());
                            } catch (SQLException e) {
                                showError("Erreur lors du marquage du message comme lu");
                            }
                        }
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupUI() {
        usersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRecipient = newVal;
            if (newVal != null) {
                recipientLabel.setText("À: " + newVal.getNom() + " " + newVal.getPrenom());
                loadMessages(newVal.getId());
            } else {
                recipientLabel.setText("Sélectionnez un utilisateur");
                messagesListView.getItems().clear();
            }
        });

        messageArea.textProperty().addListener((obs, oldVal, newVal) -> {
            sendButton.setDisable(newVal.trim().isEmpty() || selectedRecipient == null);
        });

        sendButton.setDisable(true);
    }

    private void loadUsers() {
        try {
            usersListView.getItems().clear();
            List<User> users;
            if (currentUser.getRole().equalsIgnoreCase("admin")) {
                users = userService.recuperer();
            } else {
                users = userService.getAdmins();
            }
            users.removeIf(user -> user.getId() == currentUser.getId());
            usersListView.getItems().addAll(users);
        } catch (SQLException e) {
            showError("Erreur lors du chargement des utilisateurs");
            e.printStackTrace();
        }
    }

    private void loadMessages(int recipientId) {
        try {
            messagesListView.getItems().clear();
            List<Message> messages = messageService.getConversation(currentUser.getId(), recipientId);
            messagesListView.getItems().addAll(messages);
            if (!messages.isEmpty()) {
                messagesListView.scrollTo(messages.size() - 1);
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des messages");
            e.printStackTrace();
        }
    }

    @FXML
    private void sendMessage() {
        if (selectedRecipient == null || messageArea.getText().trim().isEmpty()) {
            showError("Veuillez sélectionner un destinataire et écrire un message");
            return;
        }

        try {
            Message message = new Message(
                    currentUser.getId(),
                    selectedRecipient.getId(),
                    messageArea.getText().trim(),
                    LocalDateTime.now(),
                    false
            );

            messageService.sendMessage(message);
            messageArea.clear();
            loadMessages(selectedRecipient.getId());
        } catch (SQLException e) {
            showError("Erreur lors de l'envoi du message");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            stopPolling();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation.fxml"));
            Parent root = loader.load();
            ReclamationController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.reloadReclamations();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des réclamations");
            stage.show();
        } catch (IOException e) {
            showError("Erreur lors du retour à l'écran des réclamations");
            e.printStackTrace();
        }
    }

    private void startPolling() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            if (selectedRecipient != null) {
                Platform.runLater(() -> loadMessages(selectedRecipient.getId()));
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (pollingService != null) {
            pollingService.shutdown();
            try {
                if (!pollingService.awaitTermination(2, TimeUnit.SECONDS)) {
                    pollingService.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollingService.shutdownNow();
            }
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}