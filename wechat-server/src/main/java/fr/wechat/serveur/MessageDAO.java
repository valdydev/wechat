package fr.wechat.serveur;

import fr.wechat.core.modele.Message;
import fr.wechat.core.modele.Utilisateur;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class MessageDAO {

    public void sauvegarder(Message message) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(message);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
        }
    }

    public List<Message> trouverConversation(Utilisateur u1, Utilisateur u2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Message> query = session.createQuery(
                    "from Message where (expediteur = :u1 and destinataire = :u2) " +
                            "or (expediteur = :u2 and destinataire = :u1) order by dateEnvoi asc",
                    Message.class);
            query.setParameter("u1", u1);
            query.setParameter("u2", u2);
            return query.list();
        }
    }
}
