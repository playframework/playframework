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
            throw new RuntimeException("No EntityManager bound to this thread. Try to annotation your action method with @play.db.jpa.Transactional");
        }
        return em;
    }
    
    public static void bindForCurrentThread(EntityManager em) {
        currentEntityManager.set(em);
    }
    
}