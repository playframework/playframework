package play.db.jpa;

import play.*;

import java.io.*;
import java.util.*;

import javax.persistence.*;

public class JPA {
    
    static ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>();
    
    public static EntityManager em(String key) {
        Application app = Play.application();
        if(app == null) {
            throw new RuntimeException("No application running");
        }
        
        JPAPlugin jpaPlugin = app.plugin(JPAPlugin.class);
        if(jpaPlugin == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }
        
        EntityManager em = jpaPlugin.em(key);
        if(em == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }
        
        return em;
    } 
    
    public static EntityManager em() {
        EntityManager em = currentEntityManager.get();
        if(em == null) {
            throw new RuntimeException("No EntityManager bound to this thread. Try to annotate your action method with @play.db.jpa.Transactional");
        }
        return em;
    }
    
    public static void bindForCurrentThread(EntityManager em) {
        currentEntityManager.set(em);
    }
    
    public static <T> T withTransaction(play.libs.F.Function0<T> block) throws Throwable {
        return withTransaction("default", false, block);
    }
    
    public static void withTransaction(final play.libs.F.Callback0 block) {
        try {
            withTransaction("default", false, new play.libs.F.Function0<Void>() {
                public Void apply() throws Throwable {
                    block.invoke();
                    return null;
                }
            });
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public static <T> T withTransaction(String name, boolean readOnly, play.libs.F.Function0<T> block) throws Throwable {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            
            em = JPA.em(name);
            JPA.bindForCurrentThread(em);
            
            if(!readOnly) {
                tx = em.getTransaction();
                tx.begin();
            }
            
            T result = block.apply();
            
            if(tx != null) {
                if(tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
            
            return result;
            
        } catch(Throwable t) {
            if(tx != null) {
                try { tx.rollback(); } catch(Throwable e) {}
            }
            throw t;
        } finally {
            JPA.bindForCurrentThread(null);
            if(em != null) {
                em.close();
            }
        }
    }
    
}