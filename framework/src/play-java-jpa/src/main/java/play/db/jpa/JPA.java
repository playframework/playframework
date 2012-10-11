package play.db.jpa;

import play.*;

import javax.persistence.*;

/**
 * JPA Helpers.
 */
public class JPA {

    static ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>();

    /**
     * Get the EntityManager for specified persistence unit for this thread.
     */
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

    /**
     * Get the default EntityManager for this thread.
     */
    public static EntityManager em() {
        EntityManager em = currentEntityManager.get();
        if(em == null) {
            throw new RuntimeException("No EntityManager bound to this thread. Try to annotate your action method with @play.db.jpa.Transactional");
        }
        return em;
    }

    /**
     * Bind an EntityManager to the current thread.
     */
    public static void bindForCurrentThread(EntityManager em) {
        currentEntityManager.set(em);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static <T> T withTransaction(play.libs.F.Function0<T> block) throws Throwable {
        return withTransaction("default", false, block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
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

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     */
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