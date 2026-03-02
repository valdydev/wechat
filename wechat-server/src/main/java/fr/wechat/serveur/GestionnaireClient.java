package fr.wechat.serveur;

import fr.wechat.core.modele.Message;
import fr.wechat.core.modele.StatutMessage;
import fr.wechat.core.modele.StatutUtilisateur;
import fr.wechat.core.modele.Utilisateur;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

public class GestionnaireClient implements Runnable {

    private Socket socket;
    private UtilisateurDAO utilisateurDAO;
    private MessageDAO messageDAO;
    private BufferedReader in;
    private PrintWriter out;
    private Utilisateur utilisateurActuel;

    public GestionnaireClient(Socket socket, UtilisateurDAO uDAO, MessageDAO mDAO) {
        this.socket = socket;
        this.utilisateurDAO = uDAO;
        this.messageDAO = mDAO;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String ligne;
            while ((ligne = in.readLine()) != null) {
                traiterCommande(ligne);
            }
        } catch (IOException e) {
            System.out.println("Connexion perdue avec un client.");
        } finally {
            deconnexion();
        }
    }

    private void traiterCommande(String commandeBrute) {
        String[] parts = commandeBrute.split("\\|");
        String type = parts[0];

        switch (type) {
            case "INSCRIPTION": // INSCRIPTION|nom|mdp
                gererInscription(parts[1], parts[2]);
                break;
            case "CONNEXION": // CONNEXION|nom|mdp
                gererConnexion(parts[1], parts[2]);
                break;
            case "MESSAGE": // MESSAGE|destinataire|contenu
                gererMessage(parts[1], parts[2]);
                break;
            case "LISTE_UTILISATEURS":
                gererListeUtilisateurs();
                break;
            case "HISTORIQUE": // HISTORIQUE|autreNom
                gererHistorique(parts[1]);
                break;
            default:
                out.println("ERREUR|Commande inconnue");
        }
    }

    private void gererInscription(String nom, String mdp) {
        if (utilisateurDAO.trouverParNom(nom).isPresent()) {
            out.println("INSCRIPTION_ECHEC|Nom d'utilisateur déjà pris");
            return;
        }
        Utilisateur u = new Utilisateur(nom, ServiceSecurite.hacherMotDePasse(mdp));
        utilisateurDAO.sauvegarder(u);
        out.println("INSCRIPTION_SUCCES");
        ServeurChat.journaliser("Nouvel utilisateur inscrit : " + nom);
    }

    private void gererConnexion(String nom, String mdp) {
        Optional<Utilisateur> uOpt = utilisateurDAO.trouverParNom(nom);
        if (uOpt.isPresent() && ServiceSecurite.verifierMotDePasse(mdp, uOpt.get().getMotDePasse())) {
            if (ServeurChat.getClient(nom) != null) {
                out.println("CONNEXION_ECHEC|Utilisateur déjà connecté");
                return;
            }
            this.utilisateurActuel = uOpt.get();
            this.utilisateurActuel.setStatut(StatutUtilisateur.EN_LIGNE);
            utilisateurDAO.mettreAJour(this.utilisateurActuel);

            ServeurChat.ajouterClient(nom, this);
            out.println("CONNEXION_SUCCES|" + nom);
            diffuserStatut(nom, "EN_LIGNE");
        } else {
            out.println("CONNEXION_ECHEC|Identifiants invalides");
        }
    }

    private void gererMessage(String destinataireNom, String contenu) {
        if (utilisateurActuel == null)
            return;
        if (contenu.isEmpty() || contenu.length() > 1000) {
            out.println("ERREUR|Contenu invalide");
            return;
        }

        Optional<Utilisateur> destOpt = utilisateurDAO.trouverParNom(destinataireNom);
        if (destOpt.isPresent()) {
            Utilisateur dest = destOpt.get();
            Message msg = new Message(utilisateurActuel, dest, contenu);

            GestionnaireClient destHandler = ServeurChat.getClient(destinataireNom);
            if (destHandler != null) {
                destHandler.envoyer("MESSAGE_RECU|" + utilisateurActuel.getNomUtilisateur() + "|" + contenu);
                msg.setStatut(StatutMessage.RECU);
            }
            messageDAO.sauvegarder(msg);
            ServeurChat.journaliser("Message de " + utilisateurActuel.getNomUtilisateur() + " à " + destinataireNom);
        }
    }

    private void gererListeUtilisateurs() {
        StringBuilder sb = new StringBuilder("LISTE_UTILISATEURS");
        List<Utilisateur> tous = utilisateurDAO.listerTout();
        for (Utilisateur u : tous) {
            sb.append("|").append(u.getNomUtilisateur()).append(":").append(u.getStatut());
        }
        out.println(sb.toString());
    }

    private void gererHistorique(String autreNom) {
        Optional<Utilisateur> autreOpt = utilisateurDAO.trouverParNom(autreNom);
        if (autreOpt.isPresent()) {
            List<Message> history = messageDAO.trouverConversation(utilisateurActuel, autreOpt.get());
            StringBuilder sb = new StringBuilder("HISTORIQUE_REPONSE|" + autreNom);
            for (Message m : history) {
                sb.append("|").append(m.getExpediteur().getNomUtilisateur()).append(">").append(m.getContenu());
            }
            out.println(sb.toString());
        }
    }

    private void diffuserStatut(String nom, String statut) {
        for (GestionnaireClient client : ServeurChat.getClientsConnectes().values()) {
            if (!client.utilisateurActuel.getNomUtilisateur().equals(nom)) {
                client.envoyer("UPDATE_STATUT|" + nom + "|" + statut);
            }
        }
    }

    public void envoyer(String message) {
        out.println(message);
    }

    private void deconnexion() {
        if (utilisateurActuel != null) {
            ServeurChat.retirerClient(utilisateurActuel.getNomUtilisateur());
            utilisateurActuel.setStatut(StatutUtilisateur.HORS_LIGNE);
            utilisateurDAO.mettreAJour(utilisateurActuel);
            diffuserStatut(utilisateurActuel.getNomUtilisateur(), "HORS_LIGNE");
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
