package fr.wechat.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WeChatApp extends Application {

    private static ServiceReseau serviceReseau = new ServiceReseau();
    private static Stage primaryStage;
    private static String utilisateurActuel;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("WeChat - Connexion");
        chargerScene("connexion.fxml");
        primaryStage.show();
    }

    public static void chargerScene(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(WeChatApp.class.getResource("/fr/wechat/client/" + fxml));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(WeChatApp.class.getResource("/fr/wechat/client/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public static ServiceReseau getServiceReseau() {
        return serviceReseau;
    }

    public static void setUtilisateurActuel(String nom) {
        utilisateurActuel = nom;
    }

    public static String getUtilisateurActuel() {
        return utilisateurActuel;
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        serviceReseau.fermer();
    }
}
