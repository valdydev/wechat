package fr.wechat.serveur;

import org.mindrot.jbcrypt.BCrypt;

public class ServiceSecurite {

    public static String hacherMotDePasse(String motDePasse) {
        return BCrypt.hashpw(motDePasse, BCrypt.gensalt());
    }

    public static boolean verifierMotDePasse(String motDePasse, String hache) {
        try {
            return BCrypt.checkpw(motDePasse, hache);
        } catch (Exception e) {
            return false;
        }
    }
}
