package fr.wechat.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

public class ConnexionController {

    @FXML
    private TextField nomUtilisateurField;
    @FXML
    private PasswordField motDePasseField;
    @FXML
    private Label erreurLabel;

    @FXML
    public void initialize() {
        WeChatApp.getServiceReseau().setOnMessageReceived(this::traiterMessage);
    }

    @FXML
    private void gererConnexion() {
        String nom = nomUtilisateurField.getText();
        String mdp = motDePasseField.getText();

        if (nom.isEmpty() || mdp.isEmpty()) {
            erreurLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        WeChatApp.getServiceReseau().connecter(() -> {
            WeChatApp.getServiceReseau().envoyer("CONNEXION|" + nom + "|" + mdp);
        }, erreur -> erreurLabel.setText(erreur));
    }

    private void traiterMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts[0].equals("CONNEXION_SUCCES")) {
            WeChatApp.setUtilisateurActuel(parts[1]);
            try {
                WeChatApp.chargerScene("chat.fxml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (parts[0].equals("CONNEXION_ECHEC")) {
            erreurLabel.setText(parts[1]);
        }
    }

    @FXML
    private void allerAInscription() throws IOException {
        WeChatApp.chargerScene("inscription.fxml");
    }
}
