package fr.wechat.serveur;

import fr.wechat.core.modele.StatutUtilisateur;
import fr.wechat.core.modele.Utilisateur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServeurChat {

    private static final int PORT = 8888;
    private static final Map<String, GestionnaireClient> clientsConnectes = new ConcurrentHashMap<>();
    private static final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private static final MessageDAO messageDAO = new MessageDAO();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur WeChat démarré sur le port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nouvelle connexion : " + socket.getInetAddress());
                GestionnaireClient handler = new GestionnaireClient(socket, utilisateurDAO, messageDAO);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        } finally {
            HibernateUtil.shutdown();
        }
    }

    public static void ajouterClient(String nom, GestionnaireClient handler) {
        clientsConnectes.put(nom, handler);
        journaliser(nom + " s'est connecté.");
    }

    public static void retirerClient(String nom) {
        clientsConnectes.remove(nom);
        journaliser(nom + " s'est déconnecté.");
    }

    public static GestionnaireClient getClient(String nom) {
        return clientsConnectes.get(nom);
    }

    public static Map<String, GestionnaireClient> getClientsConnectes() {
        return clientsConnectes;
    }

    public static void journaliser(String message) {
        System.out.println("[JOURNAL] " + message);
    }
}
