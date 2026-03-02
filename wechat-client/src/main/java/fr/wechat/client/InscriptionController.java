package fr.wechat.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import java.io.IOException;

public class InscriptionController {

    @FXML
    private TextField nomUtilisateurField;
    @FXML
    private PasswordField motDePasseField;
    @FXML
    private Label infoLabel;

    @FXML
    public void initialize() {
        WeChatApp.getServiceReseau().setOnMessageReceived(this::traiterMessage);
    }

    @FXML
    private void gererInscription() {
        String nom = nomUtilisateurField.getText();
        String mdp = motDePasseField.getText();

        if (nom.isEmpty() || mdp.isEmpty()) {
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        WeChatApp.getServiceReseau().connecter(() -> {
            WeChatApp.getServiceReseau().envoyer("INSCRIPTION|" + nom + "|" + mdp);
        }, erreur -> {
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText(erreur);
        });
    }

    private void traiterMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts[0].equals("INSCRIPTION_SUCCES")) {
            infoLabel.setTextFill(Color.GREEN);
            infoLabel.setText("Inscription réussie ! Vous pouvez vous connecter.");
        } else if (parts[0].equals("INSCRIPTION_ECHEC")) {
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText(parts[1]);
        }
    }

    @FXML
    private void allerAConnexion() throws IOException {
        WeChatApp.chargerScene("connexion.fxml");
    }
}
