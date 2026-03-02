package fr.wechat.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.*;

public class ChatController {

    @FXML
    private Label monNomLabel;
    @FXML
    private Label destinataireLabel;
    @FXML
    private ListView<String> utilisateursListView;
    @FXML
    private VBox messagesContainer;
    @FXML
    private TextField messageField;
    @FXML
    private ScrollPane scrollPane;

    private String contactSelectionne;
    private Map<String, List<String>> historiques = new HashMap<>();

    @FXML
    public void initialize() {
        monNomLabel.setText(WeChatApp.getUtilisateurActuel());
        WeChatApp.getServiceReseau().setOnMessageReceived(this::traiterMessage);
        WeChatApp.getServiceReseau().setOnConnectionLost(this::gererPerteConnexion);

        utilisateursListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String nom = newVal.split(":")[0];
                changerConversation(nom);
            }
        });

        rafraichirListe();

        // Auto-scroll vers le bas
        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    private void rafraichirListe() {
        WeChatApp.getServiceReseau().envoyer("LISTE_UTILISATEURS");
    }

    private void traiterMessage(String message) {
        String[] parts = message.split("\\|");
        String type = parts[0];

        switch (type) {
            case "LISTE_UTILISATEURS":
                List<String> users = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    if (!parts[i].startsWith(WeChatApp.getUtilisateurActuel())) {
                        users.add(parts[i]);
                    }
                }
                utilisateursListView.getItems().setAll(users);
                break;

            case "UPDATE_STATUT":
                rafraichirListe();
                break;

            case "MESSAGE_RECU":
                String expediteur = parts[1];
                String contenu = parts[2];
                ajouterMessageAConversation(expediteur, expediteur + ">" + contenu);
                if (expediteur.equals(contactSelectionne)) {
                    afficherMessage(expediteur, contenu, false);
                }
                break;

            case "HISTORIQUE_REPONSE":
                String avecQui = parts[1];
                messagesContainer.getChildren().clear();
                for (int i = 2; i < parts.length; i++) {
                    String[] msgParts = parts[i].split(">");
                    afficherMessage(msgParts[0], msgParts[1], msgParts[0].equals(WeChatApp.getUtilisateurActuel()));
                }
                break;
        }
    }

    private void changerConversation(String nom) {
        this.contactSelectionne = nom;
        destinataireLabel.setText("Conversation avec " + nom);
        WeChatApp.getServiceReseau().envoyer("HISTORIQUE|" + nom);
    }

    private void ajouterMessageAConversation(String contact, String messageFormatte) {
        historiques.computeIfAbsent(contact, k -> new ArrayList<>()).add(messageFormatte);
    }

    @FXML
    private void envoyerMessage() {
        String contenu = messageField.getText();
        if (contactSelectionne == null) {
            alerte("Veuillez sélectionner un contact.");
            return;
        }
        if (!contenu.isEmpty()) {
            WeChatApp.getServiceReseau().envoyer("MESSAGE|" + contactSelectionne + "|" + contenu);
            afficherMessage(WeChatApp.getUtilisateurActuel(), contenu, true);
            messageField.clear();
        }
    }

    private void afficherMessage(String auteur, String contenu, boolean isMoi) {
        Label label = new Label(contenu);
        label.setWrapText(true);
        label.setMaxWidth(300);

        HBox hbox = new HBox(label);
        hbox.setAlignment(isMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        label.getStyleClass().add(isMoi ? "message-bulle-droite" : "message-bulle-gauche");

        messagesContainer.getChildren().add(hbox);
    }

    @FXML
    private void gererDeconnexion() {
        WeChatApp.getServiceReseau().fermer();
        try {
            WeChatApp.chargerScene("connexion.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void gererPerteConnexion() {
        Platform.runLater(() -> {
            alerte("Connexion perdue avec le serveur.");
            try {
                WeChatApp.chargerScene("connexion.fxml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void alerte(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }
}
