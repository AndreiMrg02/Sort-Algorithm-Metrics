package com.ucv.model;

import com.ucv.hibernate.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SortResultDAO {
    public void save(SortResult result) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(result);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
    public void deleteAll() {
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            var transaction = session.beginTransaction();
            session.createQuery("DELETE FROM SortResult").executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace(); // sau logare
        }
    }

}
