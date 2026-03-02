package fr.wechat.client;

import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ServiceReseau {

    private static final String HOST = "localhost";
    private static final int PORT = 8888;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread threadEcoute;
    private Consumer<String> onMessageReceived;
    private Runnable onConnectionLost;

    public void connecter(Runnable onSuccess, Consumer<String> onFailure) {
        new Thread(() -> {
            try {
                socket = new Socket(HOST, PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                ecouterServeur();
                Platform.runLater(onSuccess);
            } catch (IOException e) {
                Platform.runLater(() -> onFailure.accept("Impossible de se connecter au serveur : " + e.getMessage()));
            }
        }).start();
    }

    private void ecouterServeur() {
        threadEcoute = new Thread(() -> {
            try {
                String ligne;
                while ((ligne = in.readLine()) != null) {
                    final String msg = ligne;
                    if (onMessageReceived != null) {
                        Platform.runLater(() -> onMessageReceived.accept(msg));
                    }
                }
            } catch (IOException e) {
                if (onConnectionLost != null) {
                    Platform.runLater(onConnectionLost);
                }
            }
        });
        threadEcoute.setDaemon(true);
        threadEcoute.start();
    }

    public void envoyer(String commande) {
        if (out != null) {
            new Thread(() -> out.println(commande)).start();
        }
    }

    public void setOnMessageReceived(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }

    public void setOnConnectionLost(Runnable callback) {
        this.onConnectionLost = callback;
    }

    public void fermer() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
